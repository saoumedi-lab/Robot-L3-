import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.robotics.chassis.Chassis;
import lejos.robotics.chassis.Wheel;
import lejos.robotics.chassis.WheeledChassis;
import lejos.robotics.navigation.MovePilot;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.Port;
import lejos.utility.Delay; //test

public class Action {
	private Chassis chassis ; 
	private MovePilot pilot ;
	
	public Action() {
		this(MotorPort.B,MotorPort.C);
	}
	
	public Action(Port mG, Port mD) {
		EV3LargeRegulatedMotor m1 = new EV3LargeRegulatedMotor(mG);//associe le port B au moteur m1 
		EV3LargeRegulatedMotor m2 = new EV3LargeRegulatedMotor(mD);//associe le port C au moteur m2
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
				
		public boolean isMoving() { // renvoie true si le robot est en mouvement
			return pilot.isMoving();
		}
	// ==========================================
	// ================PINCE=====================      
	// ==========================================

public void ouvrirPince() {
		Motor.A.setSpeed(SPEED); 
		Motor.A.forward(); 
		Delay.msDelay(2000);
	}

	
