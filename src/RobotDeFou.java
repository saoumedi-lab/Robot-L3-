import java.util.ArrayList;
import lejos.hardware.Button;
import lejos.utility.Delay;

public class RobotDeFou {

	private static Action robot;
	private static Capteur capteur;
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
		ArrayList<Float> liste = new ArrayList<Float>();
		int resultat = scanner(robot, capteur, liste);

		System.out.println("Scan : code=" + resultat + ", mesures=" + liste.size());

		if (resultat != OK || liste.isEmpty() || Button.ESCAPE.isDown()) {
			return false;
		}

		float[] tab = new float[liste.size()];
		for (int i = 0; i < liste.size(); i++) {
			tab[i] = liste.get(i);
		}

		// Détection de tous les palets et sélection du plus proche
		ArrayList<Integer> candidats = detecterTousLesPalets(tab);
		int indexPalet = trouverPaletPlusProche(tab, candidats);

		if (indexPalet < 0 || Button.ESCAPE.isDown()) {
			return false;
		}

		// Estime l'angle à partir de la position du signal dans le scan
		float anglePalet = ((float) indexPalet / tab.length) * 360.0f;
		System.out.println("Angle estime : " + anglePalet + " degres");

		// Rotation vers le palet et mise à jour de la boussole
		if (anglePalet < 180) {
			int angleG = (int) anglePalet;
			robot.tournerG(angleG);
		} else {
			int angleD = 360 - (int) anglePalet;
			robot.tournerD(angleD);
		}

		return true;
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
				System.out.println("Palet valide ajoute : index=" + indexCentre
						+ " | dist=" + (tabOriginal[indexCentre] * 100) + " cm");
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
	        robot.ouvrirPinceAsync();      // la pince s'ouvre pendant qu'on avance
	        robot.avancerSansFin(200);

	        while (!capteur.detecterPression() && !Button.ESCAPE.isDown()) {
	            Delay.msDelay(20);
	        }

	        robot.stop();

	        if (!Button.ESCAPE.isDown()) {
	            System.out.println("Contact detecte : fermeture de la pince.");
	            robot.serrerPince();
	            System.out.println("Pince fermee.");
	        }

	        Delay.msDelay(500);
	    } finally {
	        robot.stop();
	    }
	}

	// ----------------------------- centrerEtAvancer -----------------------------
	public static void centrerEtAvancer(float distInitiale) {
		final float DISTANCE_PRISE = 0.32f; // À 32 cm, le palet est devant la pince
		final float TOLERANCE_PERTE = 0.04f; // Écart de 4 cm = le palet sort de l'axe

		// 1. Calibrage initial fin dès la fin de analyser() (±15°)
		float distCible = sondeDroiteGauche(distInitiale, 15);
		if (distCible < 0 || Button.ESCAPE.isDown()) {
			System.out.println("Palet introuvable au calibrage initial.");
			return;
		}

		int corrections = 0;

		// 2. Boucle d'approche avec corrections gauche/droite en chemin
		while (!Button.ESCAPE.isDown() && distCible > DISTANCE_PRISE) {
			robot.avancerSansFin(140);
			float distPrecedente = distCible;
			boolean decale = false;
			Delay.msDelay(50); //verifié si c'est utile

			// Surveillance en temps réel pendant que le robot avance
			while (!Button.ESCAPE.isDown()) {
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
					System.out.println("Palet perdu apres 4 corrections.");
					return;
				}

				// Sonde de ±12° pour retrouver le milieu du palet
				float nouvelleDist = sondeDroiteGauche(distPrecedente, 12);
				if (nouvelleDist < 0) {
					System.out.println("Impossible de se recentrer.");
					return;
				}
				distCible = nouvelleDist;
			}
		}

		// 4. Le robot est centré et à 32 cm : capture finale
		
			avancerEtPrendrePalet();
		
	}
// ----------------------------- sondeDroiteGauche -----------------------------
	/**
	 * Effectue un léger balayage gauche -> droite (-angleMax à +angleMax)
	 * pour repérer le centre exact du palet et y réaligner le robot.
	 */
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

		System.out.println("Total palets : " + candidats.size()
				+ " | Cible la plus proche : " + (distPlusProche * 100) + " cm");
		return indexPlusProche;
	}
	// true = pince déjà ouverte (au départ, on suppose qu'elle est fermée)
	private static boolean pinceOuverte = false;

	private static void ouvrirPinceSiBesoin() {
	    if (!pinceOuverte) {
	        robot.ouvrirPinceAsync();   // non bloquant, voir point 2
	        pinceOuverte = true;
	    }
	}

	private static void fermerPince() {
	    robot.serrerPince();
	    pinceOuverte = false;
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
	    robot.ouvrirPince();           // attend la fin de l'ouverture
	    robot.reculer(10);
	    

	    // 4. 2e palet : la pince est déjà ouverte, elle ne bouge pas
	    if (analyser()) {
	        avancerEtPrendrePalet();
	    } else {
	        System.out.println("Deuxieme palet introuvable.");
	    }

	    robot.stop();
	}


	
	public static void revenirAxe() {
	    int cap = robot.getBoussole();
	    if (cap > 0) {
	        robot.tournerG(cap);      // tournerD ajoute + à la boussole, tournerG retire
	    } else if (cap < 0) {
	        robot.tournerD(-cap);
	    }
	}
	
	public static void deposerPalet() {
	    robot.avancerSansFin(200);
	    while (capteur.distance() > 0.25f && !Button.ESCAPE.isDown()) {  // 0.25 à régler
	        Delay.msDelay(20);
	    }
	    robot.stop();
	    robot.ouvrirPince();
	    robot.reculer(15);
	}
	
	
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
	
	public static void main(String[] args) {

		//System.out.println("Appuie sur DROITE pour continuer...");
		//Button.RIGHT.waitForPressAndRelease();
		
		System.out.println("Lancement de RobotDeFou...");

		robot = new Action();
		capteur = new Capteur();

		try {
			System.out.println("=== Debut de la recherche ===");

			if (analyser()) {
				System.out.println("Palet possible : rotation terminee.");
				// avancerEtPrendrePalet();
			} else {
				System.out.println("Aucun palet cible ou recherche interrompue.");
			}
		} finally {
			robot.stop();
			capteur.fermer();
		}

		System.out.println("Fin du programme.");
	}
}
