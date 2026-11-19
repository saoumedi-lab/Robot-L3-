import java.util.ArrayList;
import lejos.hardware.Button;
import lejos.utility.Delay;

public class Robotdefou {

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
				System.out.println("Distance : " + (distance * 100) + " cm");

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

		System.out.println("Boussole actuelle : " + robot.getBoussole() + " degres");
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
				System.out.println("Contact detecte : fermeture de la pince.");
				robot.serrerPince();
				System.out.println("Pince fermee.");
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
	public static void centrerEtAvancer (float[] tabOriginal, ArrayList<Integer> candidats) {
        	
        	//cherche le palais le plus proche parmi les palets détectés
        	int indexPalet = trouverPaletPlusProche (tabOriginal, candidats);
        	
        	 // Si aucun palet n'a été trouvé (index = -1)
        	if (indexPalet == -1 || Button.ESCAPE.isDown()) {
        		return;
        	}
        	
        	//On récupère la distance initiale du palet, On utilise l'index trouvé pour retrouver la distance
        	float distPrecedente=tabOriginal[indexPalet];
        	
        	// Distance à partir de laquelle le robot considère qu'il est suffisamment proche du palet pour le prendre.
        	final float DistancePrise = 0.35f;
        	
        	//Tolérance qui permet d'éviter de réagir à une petite variation de mesure de capteur
        	final float Tolerance = 0.03f;
        	
        	while (!Button.ESCAPE.isDown()) {
        		//Si le robot est déjà à 40 cm ou moins du palet, plus besoin d'avancer
        		if(distPrecedente <= DistancePrise) {
        			break;
        		}
        		robot.avancerSansFin(5);
        		boolean decale = false;
        		
        		while(!Button.ESCAPE.isDown()) {
        			
        			float distActuelle = capteur.distance();
        			
        			if(distActuelle <=0  || Float.isNaN(distActuelle) || Float.isInfinite(distActuelle)) {
        				robot.stop();
        				return;
        				}
					// Si le capteur renvoie une valeur nulle ou négative, la mesure n'est pas exploitable.
        			if (distActuelle <= DistancePrise) {
        				robot.stop();
        				distPrecedente= distActuelle;
        				break;
        			}
        			if (distActuelle > distPrecedente + Tolerance) {
        				robot.stop();
        				decale = true;
        				break;
        			}
        			distPrecedente= distActuelle; 
        			Delay.msDelay(20);
        		}
        		robot.stop();
        		if (Button.ESCAPE.isDown()) {
        			return;
        		}
        		if (decale) {
        		}	
			}
        	robot.stop(); 
        	if (!Button.ESCAPE.isDown()) {
				
        		avancerEtPrendrePalet();
        	}
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

	


public static void debut(boolean campGauche) {

    // 1. Contournement initial
    int angle = campGauche ? 45 : -45;

    if (angle > 0) {
        robot.tournerD(45);
    } else {
        robot.tournerG(45);
    }

    robot.avancer(15);

    if (angle > 0) {
        robot.tournerG(45);
    } else {
        robot.tournerD(45);
    }

    // 2. Chercher et récupérer le premier palet
    analyser();
    avancerEtPrendrePalet();

    // 3. Aller vers le camp adverse
    robot.tournerD(180);
    robot.avancer(250);

    // Déposer le premier palet
    robot.lacher(capteur);

    // 4. Se retourner vers le terrain
    robot.tournerD(180);

    // 5. Chercher et récupérer le deuxième palet
    analyser();
    avancerEtPrendrePalet();

    // 6. Retourner au camp adverse
    robot.tournerD(180);
    robot.avancer(250);

    // Déposer le deuxième palet
    robot.lacher(capteur);

    robot.stop();
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
