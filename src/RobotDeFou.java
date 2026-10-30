import lejos.hardware.Button;
import lejos.utility.Delay;

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

           // 1. Détecte le début de l'obstacle
        if (distActuel != 0 && distActuel < distanceMin && !enCoursDeDetection) {
            indexDebut = derniereValeur;
            enCoursDeDetection = true;
            distanceMin = distActuel;
        }

         //   if (distActuel != 0 && (distActuel + 0.1f) < distanceMin) {
            //    indexDebut = derniereValeur;
          //  }

           // if (distActuel != 0 && distActuel < distanceMin) {
           //     distanceMin = distActuel;
       //     }

          //  if (tab.length > 1 && indexDebut != 0 && distActuel != 0 
              //      && (tab[derniereValeur - 1] - 0.1f < distanceMin) 
                    // && (distActuel + 0.1f > distanceMin)) {

               // robot.stop();
         
          // Maintient la distance minimale pendant qu'on passe devant l'objet
            if (enCoursDeDetection && distActuel != 0 && distActuel < distanceMin) {
                distanceMin = distActuel;
            }

            // 2. Détecte la fin de l'obstacle (quand la distance s'éloigne à nouveau)
            if (enCoursDeDetection && (distActuel > distanceMin + 0.15f || distActuel == 0)) {
                int indexFin = derniereValeur;
                enCoursDeDetection = false; // Ferme la fenêtre de détection

                // 3. INTÉGRATION : On appelle ta méthode estUnPalet ici !
                if (estUnPalet(tab, indexDebut, indexFin)) {
                    System.out.println("Palet confirmé par sa largeur ! Arrêt du robot.");
                    robot.stop();
                    break; // On sort de la boucle de recherche
                } else {
                    System.out.println("C'est un mur ou trop large, on continue...");
                    distanceMin = 0.80f; // On réinitialise pour chercher le prochain objet
                }
            }
            
            Delay.msDelay(10);
        }
    }
    
// distingue un palet d'un mur 
 
	    public static boolean estUnPalet(float[] tab, int indexDebut, int indexFin) {
	        int largeur = indexFin - indexDebut;

	        // Sécurité pour éviter un plantage
	        if (indexDebut < 0 || indexFin > tab.length || largeur <= 0) {
	            return false;
	        }

	        // Test 1 : l'objet est court (ton seuil d'avant, avec un minimum en plus)
	        boolean courtOk = largeur >= 2 && largeur <= 12;

	        // Test 2 : l'objet est plat (la distance change peu)
	        float min = tab[indexDebut];
	        float max = tab[indexDebut];
	        int nbValides = 0;
	        
	        for (int i = indexDebut; i < indexFin; i++) {
	            float v = tab[i];
	            if (v <= 0) continue; // Ignore les mesures invalides / zéros
	            if (v < min) min = v;
	            if (v > max) max = v;
	        }
	        if(nbValides == 0) {
	        	return false;
	        }
	        
	        float ecart = max - min;
	        boolean platOk = ecart < 0.05f;   // moins de 5 cm d'écart

	        System.out.println("n=" + largeur + " ecart=" + (int) (ecart * 100) + "cm");

	        return courtOk && platOk;
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

