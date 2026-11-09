import lejos.hardware.Button;
import lejos.utility.Delay;
import java.util.ArrayList;

public class RobotDeFou {
 
    private static Action robot;
    private static Capteur capteur;
   
    
    
    public static int scanner(Action robot, Capteur capteur, ArrayList<Float> liste) {
        
        liste.clear();
        
      
        robot.asyncTournerG(360);

       
        while (robot.isMoving()) { 
        	float distanceCm = capteur.distance() * 100;
            System.out.println("Distance : " + distanceCm + " cm");
           
            if (Button.ESCAPE.isDown()) {
                robot.stop(); 
                return ARRET_BOUTON; 
            }
          
            if (capteur.detecterPression()) { 
                robot.stop();
                return ARRET_CONTACT;
            } 
          
            liste.add(capteur.distance());
           

            Delay.msDelay(10); 
        } 
        
     // delay vaut mieux faire cte pr tout le robot 
       
        
        return OK;
    }
    public static int estUnPalet(float[] tabOriginal) {
        float[] tab = new float[tabOriginal.length];
        System.arraycopy(tabOriginal, 0, tab, 0, tabOriginal.length);
        float tolerance = 0.1f;

        while (true) {
            float distMin = 999.0f;
            int indexMin = -1;

            for (int i = 0; i < tab.length; i++) {
                if (tab[i] > 0.1f && tab[i] < distMin) {
                    distMin = tab[i];
                    indexMin = i;
                }
            }

            if (indexMin == -1) break;

            int indexDebut = indexMin;
            int indexFin = indexMin;

            while (indexDebut > 0 && tab[indexDebut - 1] > 0.05f && Math.abs(tab[indexDebut - 1] - distMin) <= tolerance) {
                indexDebut--;
            }
            while (indexFin < tab.length - 1 && tab[indexFin + 1] > 0.1f && Math.abs(tab[indexFin + 1] - distMin) <= tolerance) {
                indexFin++;
            }

            int largeurSignal = (indexFin - indexDebut) + 1;

            if (largeurSignal >= 4 && largeurSignal <= 10) {
                // Renvoie le MILLIEU du palet (meilleure visée que juste le bord)
                return (indexDebut + indexFin) / 2; 
            } else if (largeurSignal >= 16) {
                for (int i = indexDebut; i <= indexFin; i++) tab[i] = -1.0f;
            } else {
                tab[indexMin] = -1.0f;
            }
        }

        return -1; // Aucun palet trouvé
    }

    
    public static boolean analyser() {
        ArrayList<Float> liste = new ArrayList<>();
        int resultat = scanner(robot, capteur, liste);

        if (resultat != OK || liste.isEmpty()) return false;

        float[] tab = new float[liste.size()];
        for (int i = 0; i < liste.size(); i++) tab[i] = liste.get(i);

        int indexPalet = estUnPalet(tab);
        if (indexPalet < 0) {
            System.out.println("Pas de palet");
            return false;
        }

        // Calcul direct de l'angle du palet par rapport à la rotation effectuée
        // Exemple : si indexPalet = 50 sur 200 mesures => (50 / 200) * 360 = 90°
        float anglePalet = ((float) indexPalet / tab.length) * 360.0f;

        // Le robot tourne directement de cet angle vers la gauche
        robot.tournerG((int) anglePalet);

        return true;
    }
    
    
    // Probablement un probleme dans le timing d'ouverture de la pince quand palet proche
    public static void avancerEtPrendrePalet() {
    	
    	 // 1. Le robot commence à avancer en continu (vitesse de 150 mm/s)
        robot.avancerSansFin(200);
        
        // 2. Avance tant que le palet est à plus de 40 cm
        while (capteur.distance() > 0.40f && !Button.ESCAPE.isDown()) {
            Delay.msDelay(20);
        }
        
        // 3. Dès qu'on s'approche du palet, on stoppe, on ouvre la pince et on termine l'approche
        System.out.println("Palet en vue ! Ouverture pince...");
        robot.ouvrirPince();
        robot.avancerSansFin(150); // Avance plus lentement vers le palet

        // 4. Boucle jusqu'à détection du palet par la pression (ou arrêt manuel avec ÉCHAP)
        while (!capteur.detecterPression() && !Button.ESCAPE.isDown()) {
            Delay.msDelay(20); // Pause pour ne pas surcharger le processeur EV3
        }
        
        // 5. Arrêt immédiat dès que le palet touche le capteur
        robot.stop();

        // 6.Fermeture de la pince
        if (!Button.ESCAPE.isDown()) {
            System.out.println("Palet touche !");
            System.out.println("Fermeture de la pince...");
            robot.serrerPince();
            System.out.println("Palet capture avec succes !");
        } 
        
        // Petite pause de sécurité
        Delay.msDelay(500);
        
    }
    

    public static void main(String[] args) {
        System.out.println("Lancement de RobotDeFou...");
    
        robot = new Action();
        capteur = new Capteur();
        
        System.out.println("=== Début de la recherche ===");

       
        // Lancement de l'analyse complet (scanner -> filtrage -> orientation)
        boolean paletTrouveEtOriente = analyser();
        

     
        
        if (paletTrouveEtOriente) {
            System.out.println("Palet verrouillé ! Le robot est face au palet.");
            // Placez ici l'action suivante (ex: robot.avancer();)
        } else {
            System.out.println("Échec : Aucun palet n'a pu être ciblé.");

        System.out.println("Fin du programme.");
        capteur.fermer();
        System.exit(0);
        }}
    }
