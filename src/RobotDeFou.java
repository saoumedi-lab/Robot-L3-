import lejos.hardware.Button;
import lejos.utility.Delay;
import java.util.ArrayList;

public class RobotDeFou {
 
    private static Action robot;
    private static Capteur capteur;
    

    //Pas precis et ne differencie pas un mur d'un palet. Il faut tester dans de vrai condition
    public static void tournerVers() {
    	
        float distanceMin = 0.80f;
        int indexDebut = 0;
        boolean enCoursDeDetection = false;

        float[] tab = new float[0];
        
        robot.asyncTournerG(360);

        while (robot.isMoving() && !Button.ESCAPE.isDown()) {
            tab = capteur.recherche(tab);

            int derniereValeur = tab.length - 1;

            if (derniereValeur < 0) {
                continue;
            }

            float distActuel = tab[derniereValeur];
			
        }}
 // Les constantes d'état (à placer en haut de ta classe, sous les déclarations)
    public static final int OK = 0;
    public static final int ARRET_BOUTON = 1;
    public static final int ARRET_CONTACT = 2;
    public static final int VIDE = 3;

    // La méthode scanner
    public static int scanner(Action robot, Capteur capteur, int angle, ArrayList<Float> liste) {
        
        liste.clear();
        
      
        robot.asyncTournerG(360);

       
        while (robot.isMoving()) { 
            
           
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
	   public float estUnPalet(float[] distancesTableau) {
        // Copie du tableau d'origine pour pouvoir marquer les éléments supprimés à -1.0f
        float[] tab = new float[distancesTableau.length];
        System.arraycopy(distancesTableau, 0, tab, 0, distancesTableau.length);

        float tolerance = 0.05f; // 5 cm
        boolean resteDesValeurs = true;

        while (resteDesValeurs) {
            // 1. Recherche du minimum parmi les valeurs non supprimées (!= -1.0f)
            float distMin = 999.0f;
            int indexMin = -1;

            for (int i = 0; i < tab.length; i++) {
                if (tab[i] > 0.05f && tab[i] < distMin) {
                    distMin = tab[i];
                    indexMin = i;
                }
            }

            // S'il n'y a plus aucune valeur valide dans le tableau, on arrête
            if (indexMin == -1) {
                resteDesValeurs = false;
                break;
            }

            // 2. Détermination du signal autour du minimum
            int indexDebut = indexMin;
            int indexFin = indexMin;

            // Étendre vers la gauche
            while (indexDebut > 0) {
                if (tab[indexDebut - 1] > 0.05f && Math.abs(tab[indexDebut - 1] - distMin) <= tolerance) {
                    indexDebut--;
                } else {
                    break;
                }
            }

            // Étendre vers la droite
            while (indexFin < tab.length - 1) {
                if (tab[indexFin + 1] > 0.05f && Math.abs(tab[indexFin + 1] - distMin) <= tolerance) {
                    indexFin++;
                } else {
                    break;
                }
            }

            int largeurSignal = (indexFin - indexDebut) + 1;

            // 3. Analyse de la taille du signal
            if (largeurSignal >= 4 && largeurSignal <= 15) {
                return distMin; // Palet trouvé !
            } 
            else if (largeurSignal >= 16) {
                // Mur : on efface toutes les valeurs de cette plage en les mettant à -1.0f
                for (int i = indexDebut; i <= indexFin; i++) {
                    tab[i] = -1.0f;
                }
            } 
            else {
                // Valeur isolée/bruit : on efface uniquement l'index minimal
                tab[indexMin] = -1.0f;
            }
        }

        return -1.0f; // Aucun palet trouvé
    }

	//envoie tru si le robot est face à un palet
        public static boolean analyser () {
        	
        	ArrayList<Float> liste =new ArrayList<Float>();
        	int resultat =scanner (robot, capteur, 360, liste);
        	
        	if (resultat != OK || liste.size()==0)
        		return false;
        	
        	// passe de liste à tableau
        	 float[] tab = new float[liste.size()];
        	    for (int i = 0; i< liste.size(); i++) {
        	        tab[i] = liste.get(i);
        	    }
        	    
        	    //distance palet, -1 si pas palet (mur/ robot)
        	    float distPalet = estUnPalet(tab);
        	    if (distPalet< 0) {
        	        System.out.println("Pas de palet");
        	        return false;
        	    }
        	    
        	    //tourne par pas, jusqu'à la distance devant le robot corresponde à celle du palet
        	    int pas = 0;
        	    while (Math.abs(capteur.distance() - distPalet)> 0.05f
        	            && pas< 120 && !Button.ESCAPE.isDown()) {
        	        robot.tournerG(3);
        	        pas++;
        	    }
        	    return pas< 120;
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
        
        RobotDeFou.tournerVers();
        //RobotDeFou.avancerEtPrendrePalet();
        
        System.out.println("Fin du programme.");
        capteur.fermer();
        System.exit(0);
    }
}
