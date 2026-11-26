import java.util.ArrayList;
import lejos.hardware.Button;
import lejos.utility.Delay;
import lejos.robotics.SampleProvider;
import lejos.hardware.sensor.EV3ColorSensor;        
import lejos.robotics.Color;                                   
import lejos.robotics.filter.MeanFilter;
import lejos.hardware.port.SensorPort;
import lejos.hardware.lcd.LCD;

public class RobotDeFou {

	private static Action robot;
	private static Capteur capteur;
	private static boolean avecPalet = false;
	
	/**
	 * Associe les moteurs et les capteurs aux méthodes du robot.
	 * Les objets sont créés par le test puis réutilisés ici.
	 */
	public static void initialiser(Action action, Capteur capteurs) {
	    robot = action;
	    capteur = capteurs;
	}
	// Boussole : 0 = axe de départ (face au camp adverse). Gauche (+), Droite (-)
	private static final int OK = 0;
	private static final int ARRET_BOUTON = 1;
	private static final int ARRET_CONTACT = 2;

	// ----------------------------- scanner -----------------------------
	public static int scanner(Action robot, Capteur capteur, ArrayList<Float> liste) {
		liste.clear();

		try {
			robot.asyncTournerG(360);

			while (robot.isMoving()) {
				if (Button.ESCAPE.isDown()) {
					return ARRET_BOUTON;
				}

				if (capteur.detecterPression()) {
					return ARRET_CONTACT;
				}

				// Une seule lecture : stockage en mètres, affichage en cm.
				float distance = capteur.distance();
				liste.add(distance);

				Delay.msDelay(10);
			}

			return OK;
		} finally {
			robot.stop();
		}
	}

	// ----------------------------- Analyser -----------------------------
	public static boolean analyser() {
		
		    return analyser(false);        // utilisé par debut : on veut les palets derrière nous
		}

		public static boolean analyser(boolean ignorerArriere) {
		    ArrayList<Float> liste = new ArrayList<Float>();
		    int resultat = scanner(robot, capteur, liste);
		    avecPalet = false;

		    if (resultat != OK || liste.isEmpty() || Button.ESCAPE.isDown()) {
		        return false;
		    }

		    float[] tab = new float[liste.size()];
		    for (int i = 0; i < liste.size(); i++) {
		        tab[i] = liste.get(i);
		    }

		    // Détection de tous les palets
		    ArrayList<Integer> candidats = detecterTousLesPalets(tab);

		    // Après une dépose, on ignore les palets situés derrière le robot (dans le camp)
		    if (ignorerArriere) {
		        for (int k = candidats.size() - 1; k >= 0; k--) {
		            float a = ((float) candidats.get(k) / tab.length) * 360.0f;
		            if (a > 120 && a < 240) {
		                candidats.remove(k);
		            }
		        }
		    }

		    // Sélection du plus proche parmi ceux qui restent
		    int indexPalet = trouverPaletPlusProche(tab, candidats);

		    if (indexPalet < 0 || Button.ESCAPE.isDown()) {
		        return false;
		    }

		    // Estime l'angle à partir de la position du signal dans le scan
		    float anglePalet = ((float) indexPalet / tab.length) * 360.0f;

		    // Rotation vers le palet et mise à jour de la boussole
		    if (anglePalet < 180) {
		        robot.tournerG((int) anglePalet);
		    } else {
		        robot.tournerD(360 - (int) anglePalet);
		    }

		    centrerEtAvancer(tab[indexPalet]);
		    return avecPalet;              // true seulement si le palet est vraiment en pince
		}

	// ----------------------------- detecterTousLesPalets -----------------------------
	public static ArrayList<Integer> detecterTousLesPalets(float[] tabOriginal) {
		ArrayList<Integer> paletsValides = new ArrayList<Integer>();

		float[] tab = new float[tabOriginal.length];
		System.arraycopy(tabOriginal, 0, tab, 0, tabOriginal.length);

		for (int i = 0; i < tab.length; i++) {
			if (tab[i] <= 0.32f || tab[i] >= 0.95f) {
				tab[i] = -1.0f;
			}
		}

		float tolerance = 0.10f;

		while (true) {
			float distMin = Float.POSITIVE_INFINITY;
			int indexMin = -1;

			for (int i = 0; i < tab.length; i++) {
				if (tab[i] > 0.32f && tab[i] < 0.95f && tab[i] < distMin) {
					distMin = tab[i];
					indexMin = i;
				}
			}

			if (indexMin == -1) {
				break;
			}

			int indexDebut = indexMin;
			int indexFin = indexMin;

			while (indexDebut > 0
					&& tab[indexDebut - 1] > 0
					&& Math.abs(tab[indexDebut - 1] - distMin) <= tolerance) {
				indexDebut--;
			}

			while (indexFin < tab.length - 1
					&& tab[indexFin + 1] > 0
					&& Math.abs(tab[indexFin + 1] - distMin) <= tolerance) {
				indexFin++;
			}

			int largeurSignal = indexFin - indexDebut + 1;
			boolean largeurCorrecte = largeurSignal >= 4 && largeurSignal <= 10;
			boolean creuxVisible = false;

			if (indexDebut > 0 && indexFin < tabOriginal.length - 1) {
				float avant = tabOriginal[indexDebut - 1];
				float apres = tabOriginal[indexFin + 1];
				float marge = 0.05f;

				creuxVisible = avant >= tabOriginal[indexDebut] + marge
						&& apres >= tabOriginal[indexFin] + marge;
			}

			if (largeurCorrecte && creuxVisible) {
				int indexCentre = (indexDebut + indexFin) / 2;
				paletsValides.add(indexCentre);
				}

			for (int i = indexDebut; i <= indexFin; i++) {
				tab[i] = -1.0f;
			}
		}

		return paletsValides;
	}

	// ----------------------------- avancerEtPrendrePalet -----------------------------
	public static void avancerEtPrendrePalet() {
		try {
			robot.ouvrirPince();
			robot.avancerSansFin(200);

			if (Button.ESCAPE.isDown()) {
				return;
			}

			while (!capteur.detecterPression() && !Button.ESCAPE.isDown()) {
				Delay.msDelay(20);
			}

			robot.stop();

			if (!Button.ESCAPE.isDown()) {
				robot.serrerPince(true);
				avecPalet = true;
				}

			Delay.msDelay(500);
		} finally {
			robot.stop();
		}
		if (Button.ESCAPE.isDown()) {
    return;
}
	}

	// ----------------------------- centrerEtAvancer -----------------------------
	public static boolean centrerEtAvancer(float distInitiale) {
		final float DISTANCE_PRISE = 0.32f;  // distance à laquelle on lance la prise
		final float PAS_CM = 10;             // longueur d'une étape d'approche
		final long TEMPS_MAX = 20000;        // sécurité : 20 s maximum
		final int ECHECS_MAX = 2;            // pertes de palet consécutives tolérées

		long debut = System.currentTimeMillis();

		// 1. Centrage initial : sonde étroite, puis élargie si le palet n'est pas trouvé
		float dist = sondeDroiteGauche(distInitiale, 15);
		if (dist < 0) {
			dist = sondeDroiteGauche(distInitiale, 30);
		}
		if (dist < 0 || Button.ESCAPE.isDown()) {
			return false;
		}

		int echecs = 0;

		// 2. Approche par étapes : avancer, s'arrêter, se recentrer
		while (dist > DISTANCE_PRISE && !Button.ESCAPE.isDown()) {

			if (System.currentTimeMillis() - debut > TEMPS_MAX) {
				robot.stop();
				return false;
			}

			// Palet déjà au contact : on passe directement à la prise
			if (capteur.detecterPression()) {
				break;
			}

			// Étape de 10 cm au maximum, sans descendre sous la distance de prise
			float pas = Math.min(PAS_CM, (dist - DISTANCE_PRISE) * 100);
			if (pas < 2) {
				break;   // assez proche
			}
			robot.avancer(pas);

			// Après l'avance, le palet doit être plus proche d'autant
			float attendu = dist - pas / 100f;

			// Recentrage : sonde étroite, puis élargie en cas d'échec
			float nouvelle = sondeDroiteGauche(attendu, 12);
			if (nouvelle < 0) {
				nouvelle = sondeDroiteGauche(attendu, 25);
			}

			if (nouvelle < 0) {
				echecs++;
				if (echecs > ECHECS_MAX) {
					return false;     // palet perdu : analyser() pourra refaire un scan
				}
				dist = attendu;       // on suppose le palet encore là et on retente
			} else {
				echecs = 0;
				dist = nouvelle;
			}
		}

		// 3. Capture finale
		if (Button.ESCAPE.isDown()) {
			return false;
		}
		avancerEtPrendrePalet();
		return avecPalet;
	}
	/*public static void centrerEtAvancer(float distInitiale) {
		final float DISTANCE_PRISE = 0.32f; // À 32 cm, le palet est devant la pince
		final float TOLERANCE_PERTE = 0.04f; // Écart de 4 cm = le palet sort de l'axe

		// 1. Calibrage initial fin dès la fin de analyser() (±15°)
		float distCible = sondeDroiteGauche(distInitiale, 15);
		if (distCible < 0 || Button.ESCAPE.isDown()) {
			return;
		}

		int corrections = 0;

		// 2. Boucle d'approche avec corrections gauche/droite en chemin
		while (!Button.ESCAPE.isDown() && distCible > DISTANCE_PRISE) {
			robot.avancerSansFin(140);
			float distPrecedente = distCible;
			boolean decale = false;

			// Surveillance en temps réel pendant que le robot avance
			while (robot.isMoving() && !Button.ESCAPE.isDown()) {
				float distActuelle = capteur.distance();

				if (distActuelle <= 0) {
					continue; // Ignore les lectures parasites ponctuelles
				}

				// Arrivé à distance de prise : on stoppe l'approche
				if (distActuelle <= DISTANCE_PRISE) {
					robot.stop();
					distCible = distActuelle;
					break;
				}

				// Si la distance remonte brusquement, le robot a légèrement dévié du centre
				if (distActuelle > distPrecedente + TOLERANCE_PERTE) {
					robot.stop();
					decale = true;
					break;
				}

				distPrecedente = distActuelle;
				distCible = distActuelle;
				Delay.msDelay(15);
			}

			robot.stop();

			if (Button.ESCAPE.isDown()) {
				return;
			}

			// 3. Léger balayage droite/gauche pour se remettre au centre du palet
			if (decale) {
				corrections++;
				if (corrections > 4) {
					return;
				}

				// Sonde de ±12° pour retrouver le milieu du palet
				float nouvelleDist = sondeDroiteGauche(distPrecedente, 12);
				if (nouvelleDist < 0) {
					return;
				}
				distCible = nouvelleDist;
			}
		}

		// 4. Le robot est centré et à 32 cm : capture finale
		if (!Button.ESCAPE.isDown()) {
			avancerEtPrendrePalet();
		}
	}

	/**
	 * Effectue un léger balayage gauche -> droite (-angleMax à +angleMax)
	 * pour repérer le centre exact du palet et y réaligner le robot.
	 */
	
	// ----------------------------- sonderDroiteGauche -----------------------------
	private static float sondeDroiteGauche(float distAttendue, int angleMax) {
		// Se décale légèrement à gauche
		robot.tournerG(angleMax);

		ArrayList<Float> mesures = new ArrayList<Float>();
		int amplitudeTotale = 2 * angleMax;

		// Balaie vers la droite en enregistrant les mesures sans affichage LCD
		robot.asyncTournerD(amplitudeTotale);
		while (robot.isMoving() && !Button.ESCAPE.isDown()) {
			mesures.add(capteur.distance());
			Delay.msDelay(10);
		}
		robot.stop();
		robot.ajouterboussole(amplitudeTotale);	
		if (mesures.isEmpty() || Button.ESCAPE.isDown()) {
			return -1.0f;
		}

		// Repère le début et la fin du signal correspondant au palet (±8 cm de tolérance)
		int premier = -1;
		int dernier = -1;
		float distMin = Float.POSITIVE_INFINITY;

		for (int i = 0; i < mesures.size(); i++) {
			float d = mesures.get(i);
			if (d > 0.12f && Math.abs(d - distAttendue) <= 0.08f) {
				if (premier == -1) {
					premier = i;
				}
				dernier = i;
				if (d < distMin) {
					distMin = d;
				}
			}
		}

		// Si le palet n'est plus dans ce secteur, on revient à la position de départ
		if (premier == -1) {
			robot.tournerG(angleMax);
			return -1.0f;
		}

		// Calcule l'angle de retour vers la gauche pour viser le milieu exact du palet
		float indexMilieu = (premier + dernier) / 2.0f;
		float angleParcouru = (indexMilieu / mesures.size()) * amplitudeTotale;
		int angleRetourGauche = Math.round(amplitudeTotale - angleParcouru);

		if (angleRetourGauche > 0) {
			robot.tournerG(angleRetourGauche);
		}

		return distMin;
	}

	// ----------------------------- trouverPaletPlusProche -----------------------------
	public static int trouverPaletPlusProche(float[] tabOriginal, ArrayList<Integer> candidats) {
		if (candidats.isEmpty()) {
			return -1;
		}

		int indexPlusProche = candidats.get(0);
		float distPlusProche = tabOriginal[indexPlusProche];

		for (int i = 1; i < candidats.size(); i++) {
			int indexActuel = candidats.get(i);
			float distActuelle = tabOriginal[indexActuel];

			if (distActuelle < distPlusProche) {
				distPlusProche = distActuelle;
				indexPlusProche = indexActuel;
			}
		}

		return indexPlusProche;
	}

	
	// ----------------------------- debut -----------------------------
	public static void debut(boolean campGauche) {

	    // 1. Premier palet : juste en face du robot
	    avancerEtPrendrePalet();

	    // 2. Contournement : 45° -> 30 cm -> retour à l'axe
	    if (campGauche) {
	        robot.tournerD(45);
	    } else {
	        robot.tournerG(45);
	    }

	    robot.avancer(30);

	    if (campGauche) {
	        robot.tournerG(45);
	    } else {
	        robot.tournerD(45);
	    }

	    // 3. Tout droit jusqu'au camp adverse, puis dépose du palet
	    robot.avancer(180);
	    robot.ouvrirPince();
	    avecPalet = false;
	    Delay.msDelay(300);

	    // 4. Reculer de quelques cm pour dégager le palet déposé
	    robot.reculer(10);
	    robot.serrerPince();
	    // 5. Chercher et récupérer le 2e palet, côté camp adverse
	    analyser();
	    robot.stop();
	}

	// ----------------------------- revenirAxe -----------------------------
public static void revenirAxe() {
		int cap = robot.getBoussole();
		if (cap > 180) {
			robot.tournerD(360 - cap);
		} else if (cap > 0) {
			robot.tournerG(cap);
		}
	}
	
	// ----------------------------- deposerPalet -----------------------------
	public static void deposerPalet() {
		    long limite = System.currentTimeMillis() + 10000;   // sécurité
		    robot.avancerSansFin(200);
		    while (!Button.ESCAPE.isDown()
		            && !capteur.getCouleur().equals("blanc")
		            && System.currentTimeMillis() < limite) {
		        Delay.msDelay(20);
		    }
		    robot.stop();
		    robot.avancer(15);        //  le palet doit être entièrement derrière la ligne
		    robot.ouvrirPince();
		    avecPalet = false;
		    robot.reculer(15);
		    robot.serrerPince();      // pince fermée pour le scan
		}
	
	// ----------------------------- recupererLesPalets -----------------------------
	public static void recupererLesPalets(int nbMax) {
	    int echecs = 0;
	    int deposes = 0;

	    while (deposes < nbMax && echecs < 3 && !Button.ESCAPE.isDown()) {
	        if (!analyser()) {
	            echecs++;
	            robot.avancer(20);
	            continue;
	        }
	        echecs = 0;
	        avancerEtPrendrePalet();
	        revenirAxe();
	        deposerPalet();
	        deposes++;
	        robot.tournerG(180);   // face au terrain pour le scan suivant
	    }
	}
	
	// ----------------------------- surveillerObstacle -----------------------------
	public static void surveillerObstacle() {
	    final float distanceArret = 0.20f; // 20 cm, à ajuster.
	    try {
	        while (!Button.ESCAPE.isDown()) {
	            float distance = capteur.distance();
	            // Arrêt si la mesure est inexploitable.
	            if (distance <= 0 || Float.isNaN(distance)) {
	                System.out.println("Mesure invalide : arret.");
	                return;
	            }
	            if (distance <= distanceArret) {
	                System.out.println("Obstacle a "
	                        + distance * 100 + " cm : arret.");
	                return;
	            }
	            Delay.msDelay(20);
	        }
	    } finally {
	        robot.stop();
	    }
	}
	

	public static void main(String[] args) {
		    robot = new Action();
		    capteur = new Capteur();
		    avecPalet = false;

		    try {
		        System.out.println("Pince fermee et vide au depart.");
		        System.out.println("Place un palet face aux pinces.");
		        System.out.println("ENTER pour commencer.");
		        Button.ENTER.waitForPressAndRelease();

		        avancerEtPrendrePalet();

		        if (Button.ESCAPE.isDown()) {
		            System.out.println("Test interrompu.");
		            return;
		        }

		        System.out.println("Prise memorisee : " + avecPalet);
		        System.out.println("Position pince : "
		                + robot.getPositionPince() + " degres");
		        System.out.println("Position attendue : 98 degres.");
		        System.out.println("Verifie que le palet tient sans forcer.");
		        System.out.println("ENTER pour ouvrir et liberer le palet.");

		        Button.ENTER.waitForPressAndRelease();

		        robot.ouvrirPince();
		        avecPalet = false;

		        System.out.println("Position ouverte : "
		                + robot.getPositionPince() + " degres");
		        System.out.println("Position attendue : 800 degres.");

		        System.out.println("Retire le palet, puis ENTER pour fermer.");
		        Button.ENTER.waitForPressAndRelease();

		        robot.serrerPince(false);

		        System.out.println("Position fermee : "
		                + robot.getPositionPince() + " degres");
		        System.out.println("Position attendue : 0 degre.");

		    } finally {
		        robot.stop();
		        capteur.fermer();
		    }

		    System.out.println("Fin du test.");
		}
}
