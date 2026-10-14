import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.robotics.chassis.Chassis;
import lejos.robotics.chassis.Wheel;
import lejos.robotics.chassis.WheeledChassis;
import lejos.robotics.navigation.MovePilot;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.Port;
import lejos.utility.Delay; //test
import lejos.hardware.motor.Motor;

public class Action {
	private final int SPEED = 300;

	private Chassis chassis ;
	private MovePilot pilot ;
	private int boussole;
	private final EV3LargeRegulatedMotor moteurPince;
	public Action() {
		this(MotorPort.C,MotorPort.B);
	}

	public Action(Port mG, Port mD) {
		EV3LargeRegulatedMotor m1 = new EV3LargeRegulatedMotor(mG);//associe le port B au moteur m1 
		EV3LargeRegulatedMotor m2 = new EV3LargeRegulatedMotor(mD);//associe le port C au moteur m2
		moteurPince = new EV3LargeRegulatedMotor(MotorPort.A);

		Wheel motorG = WheeledChassis.modelWheel(m1, 56).offset(-60);//diametre roue gauche + ecartement de 6 cm a gauche du centre
		Wheel motorD = WheeledChassis.modelWheel(m2, 56).offset(60);//diametre roue droite + ecartement de 6 cm a droite du centre
		chassis = new WheeledChassis(new Wheel[]{ motorG, motorD }, WheeledChassis.TYPE_DIFFERENTIAL);// traduit des centimetres ou des degres geometriques en tours de moteur reels.
		pilot = new MovePilot(chassis);//calcule automatiquement la vitesse et le nombre de tours exacts que chaque moteur doit effectuer
	}

	// methode qui arrete les roues du robot
	public void stop() {
		pilot.stop();
	}

	// ==========================================
	// ================BOUSSOLE==================
	// ==========================================

	private void actBoussole (int angle){
		boussole = ((boussole + angle) % 360 + 360) % 360;
	}

	public int getBoussole() {
		return boussole; 
	}



	// ==========================================
	// === DEPLACEMENT  (Distance)      ===
	// ==========================================

	// Méthode qui fait avancer le robot de la distance en paramètre (en cm)
	public void avancer(double d) {
		// pilot.travel attend des millimètres, donc on multiplie les cm par 10
		pilot.travel(d * 10);
	}
	//anciennce version de la méthode avancer 
	/* moteurGauche.setSpeed(SPEED);
		moteurDroit.setSpeed(SPEED); 

		// Conversion de la distance en cm vers les degrés de rotation du moteur
		// Formule : (distance / circonférence) * 360
		int degresMoteur = (int) Math.round((d / (Math.PI * 5.5)) * 360.0); 

		// Le "true" permet de lancer le moteur gauche sans bloquer la lecture, 
		// pour que le moteur droit démarre exactement en même temps.
		moteurGauche.rotate(degresMoteur, true);
		moteurDroit.rotate(degresMoteur);
	 */

	// Avance en continu 
	public void avancerSansFin(int speed) {
		pilot.setLinearSpeed(speed);
		pilot.forward();
	}
	// Méthode qui fait reculer le robot de la distance en paramètre (en cm)
	public void reculer(double d) {
		// pilot.travel attend des millimètres, donc on multiplie les cm par 10
		pilot.travel(-d * 10);
	}

	// ==========================================
	// ================TOURNER===================        
	// ==========================================

	// methode qui permet de tourner le robot a droite d'un certain angle a droite
	public void tournerD(int angle) { 
		pilot.setAngularSpeed(100);
		pilot.rotate(0.88*(double)angle);
	}

	// methode qui permet de tourner le robot a gauche d'un certain angle a droite
	public void tournerG(int angle) { 
		pilot.setAngularSpeed(100);
		pilot.rotate(-0.88*(double)angle);
	}

	// methode qui permet de tourner le robot a droite d'un certain angle de manière asynchrone
	public void asyncTournerG(int angle) { 
		pilot.setAngularSpeed(100);
		pilot.rotate(-0.89*(double)angle,true);
	}

	// methode qui permet de tourner le robot a gauche d'un certain angle de manière asynchrone
	public void asyncTournerD(int angle) { 
		pilot.setAngularSpeed(100);
		pilot.rotate(0.89*(double)angle,true);
	} 

	public boolean isMoving() { // renvoie true si le robot est en mouvement
		return pilot.isMoving();
	}
	// ==========================================
	// ================PINCE=====================      
	// ==========================================

	public void ouvrirPince() {
		moteurPince.setSpeed(SPEED);
		moteurPince.forward();
		Delay.msDelay(2000);
		moteurPince.stop();
	}
	// Méthode qui ferme la pince du robot

	public void serrerPince() {
		moteurPince.setSpeed(SPEED);
		moteurPince.backward();
		Delay.msDelay(5000);
		moteurPince.stop();
	}


	public void lacher(Capteur capteurs) {
		// 1. Le robot avance en continu vers la ligne
		pilot.forward();

		// 2. On attend d'arriver sur le blanc
		while(!capteurs.getCouleur().equals("blanc")) {
			// Rien dans la boucle, le robot avance tout seul
		}

		// 3. On s'arrête sur la ligne
		pilot.stop();

		// 4. On appelle juste ta méthode existante !
		ouvrirPince();
	}

}
