import lejos.hardware.BrickFinder;
import lejos.hardware.lcd.GraphicsLCD;
import lejos.hardware.motor.Motor;
import lejos.utility.Delay;

public class Action {
	
	protected static final int SPEED = 500;
	
	// Méthode qui fait avancer le robot de la distance en paramètre (en cm)
	public void avancer(double d) { 
		Motor.B.setSpeed(SPEED); 
		Motor.C.setSpeed(SPEED);
		// Conversion de la distance en cm vers les degrés de rotation du moteur 
		int degresMoteur = (int) Math.round((d / ((Math.PI * 5.5)) * 360.0)); //pi*diamètre = circonference

		// Le paramètre true indique à leJOS d'exécuter la commande en arrière-plan sans bloquer le programme, // ce qui permet de lancer Motor.D instantanément et de faire avancer les deux roues en parallèle. 
		Motor.B.rotate(degresMoteur, true); 
		Motor.C.rotate(degresMoteur); 
		} 
	
	
	// Méthode qui fait reculer le robot de la distance en paramètre (en cm)
	public void reculer(double d) {
	Motor.B.setSpeed(SPEED); 
	Motor.C.setSpeed(SPEED);
	// Conversion de la distance en cm vers les degrés de rotation du moteur 
	int degresMoteur = (int) Math.round((d /(Math.PI * 5.5)) * 360.0); //pi*diamètre = circonference
	Motor.B.rotate(-degresMoteur, true); 
	Motor.C.rotate(-degresMoteur); 
	} 

	
	public void tournerVers (int angle) {
		if (angle > 180) {
			angle = 360 -angle ;
			int angleRoues = (int) Math.round(angle * 1.98);
			// ajuste l'angle pour qu'il coresponde a quoi faire avec les roues
			//1,98 vinet de distance entre les roues diviser par diamettre roue (plus un petit ajustament de chatGPT)
			Motor.B.rotate(-angleRoues, true);
			Motor.C.rotate(angleRoues);
		}else {
			int angleRoues = (int) Math.round(angle * 1.98);
			// ajuste l'angle pour qu'il coresponde a quoi faire avec les roues
			//1,98 vinet de distance entre les roues diviser par diamettre roue (plus un petit ajustament de chatGPT)
			Motor.B.rotate(angleRoues, true);
			Motor.C.rotate(-angleRoues);
		}
	}

		// Méthode qui ferme la pince du robot

		public void fermerPince() {
				Motor.A.setSpeed(SPEED); 
				Motor.A.backward(); 
				Delay.msDelay(5000);
			    Motor.A.stop();
			}
		
		//Une méthode qui ouvre la pince du robot

		public void ouvrirPince() {
				Motor.C.setSpeed(SPEED); 
				Motor.C.forward(); 
				Delay.msDelay(5000);
			    Motor.A.stop();
			}
}

	
