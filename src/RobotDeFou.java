import lejos.hardware.Button;
import lejos.utility.Delay;
import java.util.ArrayList;

public class RobotDeFou {

    private static Action robot;
    private static Capteur capteur;

    private static final int OK = 0;
    private static final int ARRET_BOUTON = 1;
    private static final int ARRET_CONTACT = 2;

    public static int scanner(Action robot, Capteur capteur,
                              ArrayList<Float> liste) {
        liste.clear();

        try {
            robot.asyncTournerG(360);

            while (robot.isMoving()) {
                if (Button.ESCAPE.isDown()) {
                    return ARRET_BOUTON;
                }

                if (capteur.detecterPression()) {
                    return ARRET_CONTACT;
                }

                // Une seule lecture : stockage en mètres, affichage en cm.
                float distance = capteur.distance();
                liste.add(distance);
                System.out.println("Distance : " + distance * 100 + " cm");

                Delay.msDelay(10);
            }

            return OK;
        } finally {
            robot.stop();
        }
    }

    public static int estUnPalet(float[] tabOriginal) {
        float[] tab = new float[tabOriginal.length];
        System.arraycopy(tabOriginal, 0, tab, 0, tabOriginal.length);

        // Garde les distances strictement comprises entre 32 et 95 cm.
        for (int i = 0; i < tab.length; i++) {
            if (tab[i] <= 0.32f || tab[i] >= 0.95f) {
                tab[i] = -1.0f;
            }
        }

        float tolerance = 0.10f; // Écart maximal au minimum : 10 cm.

        while (true) {
            float distMin = Float.POSITIVE_INFINITY;
            int indexMin = -1;

            // Cherche la plus petite distance encore valide.
            for (int i = 0; i < tab.length; i++) {
                if (tab[i] > 0.32f && tab[i] < 0.95f && tab[i] < distMin) {
                    distMin = tab[i];
                    indexMin = i;
                }
            }

            if (indexMin == -1) {
                return -1;
            }

            int indexDebut = indexMin;
            int indexFin = indexMin;

            // Étend le signal aux mesures voisines proches du minimum.
            while (indexDebut > 0
                    && tab[indexDebut - 1] > 0
                    && Math.abs(tab[indexDebut - 1] - distMin) <= tolerance) {
                indexDebut--;
            }

            while (indexFin < tab.length - 1
                    && tab[indexFin + 1] > 0
                    && Math.abs(tab[indexFin + 1] - distMin) <= tolerance) {
                indexFin++;
            }

            int largeurSignal = indexFin - indexDebut + 1;
            boolean largeurCorrecte = largeurSignal >= 4 && largeurSignal <= 10;
            boolean creuxVisible = false;

            if (indexDebut > 0 && indexFin < tabOriginal.length - 1) {
                // Utilise le scan original pour lire le fond autour du signal.
                float avant = tabOriginal[indexDebut - 1];
                float apres = tabOriginal[indexFin + 1];
                float marge = 0.05f; // 5 cm de différence sur chaque bord.

                creuxVisible = avant >= tabOriginal[indexDebut] + marge
                        && apres >= tabOriginal[indexFin] + marge;
            }

            System.out.println("Candidat : " + distMin * 100 + " cm"
                    + " | largeur=" + largeurSignal
                    + " | creux=" + creuxVisible);

            if (largeurCorrecte && creuxVisible) {
                return (indexDebut + indexFin) / 2;
            }

            // Ignore tout le groupe rejeté, en conservant ses indices.
            for (int i = indexDebut; i <= indexFin; i++) {
                tab[i] = -1.0f;
            }
        }
    }

    public static boolean analyser() {
        ArrayList<Float> liste = new ArrayList<>();
        int resultat = scanner(robot, capteur, liste);

        System.out.println("Scan : code=" + resultat
                + ", mesures=" + liste.size());

        if (resultat != OK || liste.isEmpty() || Button.ESCAPE.isDown()) {
            return false;
        }

        float[] tab = new float[liste.size()];
        for (int i = 0; i < liste.size(); i++) {
            tab[i] = liste.get(i);
        }

        int indexPalet = estUnPalet(tab);
        if (indexPalet < 0 || Button.ESCAPE.isDown()) {
            return false;
        }

        // Estime l'angle à partir de la position du signal dans le scan.
        float anglePalet = ((float) indexPalet / tab.length) * 360.0f;
        System.out.println("Angle estime : " + anglePalet + " degres");

        if (anglePalet<180) robot.tournerG((int) anglePalet);
        else robot.tournerD(360-(int)anglePalet);
        
        return true;
    }

    public static void avancerEtPrendrePalet() {
        try {
            robot.avancerSansFin(200);

            while (capteur.distance() > 0.40f && !Button.ESCAPE.isDown()) {
                Delay.msDelay(20);
            }

            // Arrête les roues avant d'ouvrir la pince.
            robot.stop();
            if (Button.ESCAPE.isDown()) {
                return;
            }

            System.out.println("Objet proche : ouverture de la pince.");
            robot.ouvrirPince();

            if (Button.ESCAPE.isDown()) {
                return;
            }

            robot.avancerSansFin(150);

            while (!capteur.detecterPression() && !Button.ESCAPE.isDown()) {
                Delay.msDelay(20);
            }

            robot.stop();

            if (!Button.ESCAPE.isDown()) {
                System.out.println("Contact detecte : fermeture de la pince.");
                robot.serrerPince();
                System.out.println("Pince fermee.");
            }

            Delay.msDelay(500);
        } finally {
            robot.stop();
        }
    }

    public static void main(String[] args) {
        System.out.println("Lancement de RobotDeFou...");

        robot = new Action();
        capteur = new Capteur();

        try {
            System.out.println("=== Debut de la recherche ===");

            if (analyser()) {
                System.out.println("Palet possible : rotation terminee.");

                // Active cette ligne apres avoir verifie la visee.
                // avancerEtPrendrePalet();
            } else {
                System.out.println("Aucun palet cible ou recherche interrompue.");
            }
        } finally {
            robot.stop();
            capteur.fermer();
        }

        System.out.println("Fin du programme.");
    }
}
