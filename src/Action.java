import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.Port;
import lejos.robotics.chassis.Chassis;
import lejos.robotics.chassis.Wheel;
import lejos.robotics.chassis.WheeledChassis;
import lejos.robotics.navigation.MovePilot;
import lejos.utility.Delay; //testc


// La classe Action gère l'ensemble des mouvements physiques du robot

public class Action {

	private final int SPEED = 300; // Vitesse de référence par défaut

	private Chassis chassis ;
	private MovePilot pilot ;
	private int boussole = 0;
	private final EV3LargeRegulatedMotor moteurPince;

	private boolean pinceOuverte = false;               // la pince doit être fermée au lancement du programme
	private final int VITESSE_OUVERTURE = 800;
	private final int ANGLE_OUVERTURE = 950;
	private final int POSITION_SERRAGE_PALET = 98;
	private double dernierAngleRotation = 0;
	// Constructeur qui initialise les roues sur les ports C (gauche) et B (droit).

	public Action() {
		this(MotorPort.C,MotorPort.B);
	}


	//configuration du châssis, les roues et le moteur de la pince.

	public Action(Port mG, Port mD) {
		EV3LargeRegulatedMotor m1 = new EV3LargeRegulatedMotor(mG);//associe le port B au moteur m1 
		EV3LargeRegulatedMotor m2 = new EV3LargeRegulatedMotor(mD);//associe le port C au moteur m2

		moteurPince = new EV3LargeRegulatedMotor(MotorPort.A);
		// Pince fermee et vide au lancement du programme.
		moteurPince.resetTachoCount();

		// Modélisation des roues
		Wheel motorG = WheeledChassis.modelWheel(m1, 56).offset(-60);//diametre roue gauche + ecartement de 6 cm a gauche du centre
		Wheel motorD = WheeledChassis.modelWheel(m2, 56).offset(60);//diametre roue droite + ecartement de 6 cm a droite du centre

		chassis = new WheeledChassis(new Wheel[]{ motorG, motorD }, WheeledChassis.TYPE_DIFFERENTIAL);// traduit des centimetres ou des degres geometriques en tours de moteur reels.

		pilot = new MovePilot(chassis);//calcule automatiquement la vitesse et le nombre de tours exacts que chaque moteur doit effectuer
	}


	// ==========================================
	// ================BOUSSOLE==================
	// ==========================================
	private boolean rotationAsyncEnCours = false;
	private final double COEFFICIENT_ROTATION_ASYNC = 0.89;
	public int getBoussole() {
		return boussole; 
	}

	/**
	 * Actualise la boussole à partir de la rotation mesurée par le pilote.
	 */
	private void terminerRotationAsync() {
	    if (!rotationAsyncEnCours) {
	        return;
	    }

	    double angleLu = pilot.getMovement().getAngleTurned();

	    // Conserve la derniere mesure si la lecture finale revient a zero.
	    if (angleLu != 0) {
	        dernierAngleRotation = angleLu;
	    }

	    int angleParcouru = (int) Math.round(
	            dernierAngleRotation / COEFFICIENT_ROTATION_ASYNC);

	    ajouterboussole(angleParcouru);
	    rotationAsyncEnCours = false;
	}

	// ----------------------------- Gestion Boussole -----------------------------
	// Met à jour l'boussole et la garde entre 0 et 360 degrés
	public void ajouterboussole(int deltaAngle) {
		boussole = ((boussole + deltaAngle) % 360 + 360) % 360;
	}


	// ==========================================
	// === DEPLACEMENT  (Distance)      ===
	// ==========================================

	// Méthode qui fait avancer le robot de la distance en paramètre (en cm)

	public void avancer(double d) {
		if (rotationAsyncEnCours) {
		    stop();
		}
		// pilot.travel attend des millimètres, donc on multiplie les cm par 10
		pilot.travel(d * 10);
	}


	// Avance en continu 
	public void avancerSansFin(int speed) {
	    if (rotationAsyncEnCours) {
	        stop();
	    }

	    pilot.setLinearSpeed(speed);

	    if (!pilot.isMoving()) {
	        pilot.forward();
	    }
	}

	// Méthode qui fait reculer le robot de la distance en paramètre (en cm)
	public void reculer(double d) {
		if (rotationAsyncEnCours) {
		    stop();
		}
		// pilot.travel attend des millimètres, donc on multiplie les cm par 10
		pilot.travel(-d * 10);
	}

	// ==========================================
	// ================TOURNER===================        
	// ==========================================

	// methode qui permet de tourner le robot a droite d'un certain angle a droite
	public void tournerD(int angle) { 
		stop();
		pilot.setAngularSpeed(100);
		pilot.rotate(0.88*(double)angle);
		ajouterboussole(angle);
	}

	// methode qui permet de tourner le robot a gauche d'un certain angle a droite
	public void tournerG(int angle) { 
		stop();
		pilot.setAngularSpeed(100);
		pilot.rotate(-0.88*(double)angle);
		ajouterboussole(-angle);
	}

	// methode qui permet de tourner le robot a droite d'un certain angle de manière asynchrone
	public void asyncTournerG(int angle) {
	    stop();
	    dernierAngleRotation = 0;

	    pilot.setAngularSpeed(100);
	    pilot.rotate(-COEFFICIENT_ROTATION_ASYNC * angle, true);
	    rotationAsyncEnCours = true;
	}

	public void asyncTournerD(int angle) {
	    stop();
	    dernierAngleRotation = 0;

	    pilot.setAngularSpeed(100);
	    pilot.rotate(COEFFICIENT_ROTATION_ASYNC * angle, true);
	    rotationAsyncEnCours = true;
	} 

	public void stop() {
		// Lecture avant stop(), qui peut modifier les informations du mouvement.
		terminerRotationAsync();
		pilot.stop();
	}

	public boolean isMoving() {
	    if (rotationAsyncEnCours) {
	        double angleLu = pilot.getMovement().getAngleTurned();

	        if (angleLu != 0) {
	            dernierAngleRotation = angleLu;
	        }
	    }

	    boolean enMouvement = pilot.isMoving();

	    if (!enMouvement) {
	        terminerRotationAsync();
	    }

	    return enMouvement;
	}
	// ==========================================
	// ================PINCE=====================      
	// ==========================================

	/**
	 * Ouvre la pince sans attendre la fin du mouvement.
	 */
	public void ouvrirPinceAsync() {
		if (pinceOuverte) {
			return;
		}

		moteurPince.setSpeed(VITESSE_OUVERTURE);
		moteurPince.rotateTo(ANGLE_OUVERTURE, true);
		pinceOuverte = true;
	}

	/**
	 * Ouvre la pince et attend la fin du mouvement.
	 */
	public void ouvrirPince() {
		ouvrirPinceAsync();
		moteurPince.waitComplete();
	}

	/**
	 * Ferme avec palet a 98 degres, ou sans palet a zero.
	 */
	public void serrerPince(boolean avecPalet) {
		moteurPince.waitComplete();
		moteurPince.setSpeed(600);

		if (avecPalet) {
			moteurPince.rotateTo(POSITION_SERRAGE_PALET);
		} else {
			moteurPince.rotateTo(0);
		}

		pinceOuverte = false;
	}

	/**
	 * Ferme completement la pince sans palet.
	 */
	public void serrerPince() {
		serrerPince(false);
	}

	/**
	 * Renvoie la position du moteur pour les tests.
	 */
	public int getPositionPince() {
		return moteurPince.getTachoCount();
	}

	// Comportement intelligent de dépôt : avance en continu jusqu'à la ligne blanche,
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
