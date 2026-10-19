import lejos.hardware.Button;
import lejos.utility.Delay;

public class RobotDeFou {

    public static void main(String[] args) {
        System.out.println("Initialisation...");
        
        // 1. Instanciation des capteurs et du robot
        Capteur capteur = new Capteur();
        Action robot = new Action();

     
        System.out.println("Recherche du palet...");

        // 2. Le robot commence à avancer en continu (vitesse de 150 mm/s)
        robot.avancerSansFin(200);
        
        // 3. Avance tant que le palet est à plus de 40 cm
        while (capteur.distance() > 0.40f && !Button.ESCAPE.isDown()) {
            Delay.msDelay(20);
        }
        
        // 4. Dès qu'on s'approche du palet, on stoppe, on ouvre la pince et on termine l'approche
        System.out.println("Palet en vue ! Ouverture pince...");
        robot.ouvrirPince();
        
        robot.avancerSansFin(100); // Avance lentement vers le palet

        // 4. Boucle jusqu'à détection du palet par la pression (ou arrêt manuel avec ÉCHAP)
        while (!capteur.detecterPression() && !Button.ESCAPE.isDown()) {
            Delay.msDelay(20); // Pause pour ne pas surcharger le processeur EV3
        }

        // 5. Arrêt immédiat dès que le palet touche le capteur
        robot.stop();
        
        if (!Button.ESCAPE.isDown()) {
            System.out.println("Palet touche !");
            System.out.println("Fermeture de la pince...");
            robot.serrerPince();
            System.out.println("Palet capture avec succes !");
            
        } 
       
        capteur.fermer();
        
        // Petite pause de sécurité avant de quitter
        Delay.msDelay(500);
        
        // Forcer la sortie propre de la JVM si des threads leJOS restent accrochés
        System.exit(0);
    }
}