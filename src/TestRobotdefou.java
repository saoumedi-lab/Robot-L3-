import java.util.ArrayList;
import lejos.hardware.Button;

/**
 * Tests de la classe Robotdefou.
 *
 * Les tests sur tableaux ne commandent aucun moteur.
 * Exécuter un seul test physique par lancement du programme.
 */
public class TestRobotdefou {

    private Action action;
    private Capteur capteurs;

    /** Distances enregistrées pendant un scan, en mètres. */
    private final float[] distancesTest = {
        0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
        0.45200002f, 0.45200002f, 0.46400002f, 0.48800004f,
        0.48800004f, 1.4280001f, 1.9970001f, 1.4430001f,
        1.4280001f, 1.4120001f, 0.38300002f, 0.36600003f,
        0.36400002f, 0.37600002f, 0.393f, 0.393f,
        0.393f, 0.393f, 0.393f, 0.393f,
        0.64500004f, 0.64500004f, 0.64100003f, 0.648f,
        0.64900005f, 0.64900005f, 0.47300002f, 0.45600003f,
        0.448f, 0.448f, 0.44700003f, 0.47400004f,
        0.49800003f, 0.49800003f, 0.49800003f, 0.49800003f,
        0.49800003f, 0.49800003f, 0.49800003f, 0.384f,
        0.372f, 0.35700002f, 0.34800002f, 0.347f,
        0.34500003f, 0.34f, 0.34500003f, 0.34500003f,
        0.347f, 0.34800002f, 0.35700002f, 0.36200002f,
        0.37f, 0.38000003f, 0.397f, 0.42000002f,
        0.437f, 0.437f, 1.406f, 1.393f,
        1.393f, 1.393f, 1.393f, 1.393f,
        1.393f, 1.393f, 1.393f
    };

    /** Attend le placement du robot avant un test physique. */
    private void attendreDepart() {
        System.out.println("ENTER pour demarrer.");
        Button.ENTER.waitForPressAndRelease();
    }

    /** Crée les composants et les associe à Robotdefou. */
    private void preparerRobot() {
        action = new Action();
        capteurs = new Capteur();
        Robotdefou.initialiser(action, capteurs);
    }

    /** Arrête les roues et ferme les capteurs. */
    private void terminerTestPhysique() {
        try {
            if (action != null) {
                action.stop();
            }
        } finally {
            if (capteurs != null) {
                capteurs.fermer();
            }
        }
    }

    /**
     * Vérifie la détection sur le scan enregistré,
     * sur deux signaux distincts et sur un tableau vide.
     */
    public void testDetecterTousLesPalets() {
        System.out.println("\n=== TEST : detecterTousLesPalets ===");

        ArrayList<Integer> obtenus =
                Robotdefou.detecterTousLesPalets(distancesTest);

        boolean scanReussi = obtenus.size() == 1
                && obtenus.contains(20);

        System.out.println("Scan enregistre : attendu [20]");
        System.out.println("Obtenu : " + obtenus);
        System.out.println("Test reussi : " + scanReussi);

        float[] deuxPalets = {
            1.20f,
            0.45f, 0.44f, 0.43f, 0.44f,
            1.20f,
            0.65f, 0.64f, 0.63f, 0.64f,
            1.20f
        };

        obtenus = Robotdefou.detecterTousLesPalets(deuxPalets);

        boolean deuxPaletsReussi = obtenus.size() == 2
                && obtenus.contains(2)
                && obtenus.contains(7);

        System.out.println("Deux candidats : indices attendus 2 et 7");
        System.out.println("Obtenu : " + obtenus);
        System.out.println("Test reussi : " + deuxPaletsReussi);

        obtenus = Robotdefou.detecterTousLesPalets(new float[0]);

        System.out.println("Tableau vide : attendu []");
        System.out.println("Obtenu : " + obtenus);
        System.out.println("Test reussi : " + obtenus.isEmpty());
    }

    /**
     * Vérifie la sélection du candidat le plus proche.
     * Les indices sont fournis directement pour isoler cette méthode.
     */
    public void testTrouverPaletPlusProche() {
        System.out.println("\n=== TEST : trouverPaletPlusProche ===");

        ArrayList<Integer> candidats = new ArrayList<Integer>();
        candidats.add(8);
        candidats.add(20);
        candidats.add(28);

        int obtenu = Robotdefou.trouverPaletPlusProche(
                distancesTest, candidats);

        System.out.println("Indice attendu : 20");
        System.out.println("Indice obtenu : " + obtenu);
        System.out.println("Test reussi : " + (obtenu == 20));

        candidats.clear();
        candidats.add(8);

        obtenu = Robotdefou.trouverPaletPlusProche(
                distancesTest, candidats);

        System.out.println("Candidat unique : attendu 8");
        System.out.println("Obtenu : " + obtenu);
        System.out.println("Test reussi : " + (obtenu == 8));

        candidats.clear();

        obtenu = Robotdefou.trouverPaletPlusProche(
                distancesTest, candidats);

        System.out.println("Sans candidat : attendu -1");
        System.out.println("Obtenu : " + obtenu);
        System.out.println("Test reussi : " + (obtenu == -1));
    }

    /**
     * Vérifie qu'un scan réel se termine et produit des mesures.
     * Contrôler également la rotation physique du robot.
     */
    public void testScanner() {
        System.out.println("\n=== TEST PHYSIQUE : scanner ===");
        System.out.println("Libere l'espace autour du robot.");
        attendreDepart();

        ArrayList<Float> mesures = new ArrayList<Float>();

        try {
            preparerRobot();

            int resultat = Robotdefou.scanner(
                    action, capteurs, mesures);

            System.out.println("Code attendu : 0");
            System.out.println("Code obtenu : " + resultat);
            System.out.println("Nombre de mesures : " + mesures.size());

            boolean reussi = resultat == 0 && !mesures.isEmpty();

            System.out.println("Test reussi : " + reussi);
            System.out.println("Verifie aussi la rotation physique.");
        } finally {
            terminerTestPhysique();
        }
    }

    /**
     * Vérifie le scan, la reconnaissance et l'orientation.
     * Placer un palet entre 32 et 95 cm du capteur.
     */
    public void testAnalyser() {
        System.out.println("\n=== TEST PHYSIQUE : analyser ===");
        System.out.println("Place un palet entre 32 et 95 cm.");
        System.out.println("Repere sa direction avant le depart.");
        attendreDepart();

        try {
            preparerRobot();

            boolean resultat = Robotdefou.analyser();

            System.out.println("Attendu : true");
            System.out.println("Obtenu : " + resultat);
            System.out.println("Verifie : palet face aux pinces.");
            System.out.println("True ne garantit pas l'alignement physique.");
        } finally {
            terminerTestPhysique();
        }
    }

    /**
     * Vérifie l'approche d'un palet déjà aligné.
     * Le candidat fourni correspond à une distance de 39,3 cm.
     *
     * La méthode actuelle lance ensuite la prise du palet.
     * Sa correction d'orientation n'est pas encore implémentée.
     */
    public void testCentrerEtAvancer() {
        ArrayList<Integer> candidats = new ArrayList<Integer>();
        candidats.add(20);

        System.out.println("\n=== TEST PHYSIQUE : centrerEtAvancer ===");
        System.out.println("Place le palet face aux pinces.");
        System.out.println("Distance depuis le capteur : environ 39,3 cm.");
        System.out.println("Le robot avancera puis prendra le palet.");
        System.out.println("ESCAPE pour interrompre l'approche.");
        attendreDepart();

        try {
            preparerRobot();

            Robotdefou.centrerEtAvancer(distancesTest, candidats);

            if (Button.ESCAPE.isDown()) {
                System.out.println("Test interrompu.");
            } else {
                System.out.println("Verifie : roues arretees.");
                System.out.println("Verifie : palet capture dans la pince.");
            }
        } finally {
            terminerTestPhysique();
        }
    }

    /**
     * Vérifie l'approche et la fermeture de la pince.
     * Le palet doit déjà être aligné avec les pinces.
     */
    public void testAvancerEtPrendrePalet() {
        System.out.println(
                "\n=== TEST PHYSIQUE : avancerEtPrendrePalet ===");
        System.out.println("Place le palet face aux pinces.");
        System.out.println("ESCAPE si le robot manque le palet.");
        attendreDepart();

        try {
            preparerRobot();

            Robotdefou.avancerEtPrendrePalet();

            if (Button.ESCAPE.isDown()) {
                System.out.println("Test interrompu.");
            } else {
                System.out.println("Verifie : roues arretees.");
                System.out.println("Verifie : palet retenu dans la pince.");
                System.out.println("Verifie : moteur de pince arrete.");
            }
        } finally {
            terminerTestPhysique();
        }
    }

    /**
     * Sélection des tests à lancer.
     * Activer un seul test physique par exécution.
     */
    public static void main(String[] args) {
        TestRobotdefou tests = new TestRobotdefou();

        // Tests sur tableaux : aucun mouvement.
        tests.testDetecterTousLesPalets();
        tests.testTrouverPaletPlusProche();

        // Tests physiques : activer UN seul appel.
        // tests.testScanner();
        // tests.testAnalyser();
        // tests.testCentrerEtAvancer();
        // tests.testAvancerEtPrendrePalet();

        System.out.println("\nFin des tests.");
    }
}
