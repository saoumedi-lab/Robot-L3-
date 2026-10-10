import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.robotics.RegulatedMotor;

public class Action {
	
	// Vitesse par défaut
	private final int SPEED = 300; 
	
	// Déclaration des moteurs 
	private RegulatedMotor moteurGauche;
	private RegulatedMotor moteurDroit;
	
	public Action() {
		// Initialisation des roues motrices sur les ports B et C
		moteurGauche = new EV3LargeRegulatedMotor(MotorPort.B);
		moteurDroit = new EV3LargeRegulatedMotor(MotorPort.C);
	}

	// ==========================================
	// === DEPLACEMENT  (Distance)      ===
	// ==========================================
	
	// Méthode qui fait avancer le robot de la distance en paramètre (en cm)
	public void avancer(double d) {
		moteurGauche.setSpeed(SPEED);
		moteurDroit.setSpeed(SPEED); 

		// Conversion de la distance en cm vers les degrés de rotation du moteur
		// Formule : (distance / circonférence) * 360
		int degresMoteur = (int) Math.round((d / (Math.PI * 5.5)) * 360.0); 

		// Le "true" permet de lancer le moteur gauche sans bloquer la lecture, 
		// pour que le moteur droit démarre exactement en même temps.
		moteurGauche.rotate(degresMoteur, true);
		moteurDroit.rotate(degresMoteur);
	}
}