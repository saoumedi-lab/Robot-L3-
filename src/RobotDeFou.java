import java.util.ArrayList;
import lejos.hardware.Button;
import lejos.utility.Delay;

public class RobotDeFou {

	private static Action robot;
	private static Capteur capteur;
	
	// Boussole : 0 = axe de départ (face au camp adverse). Gauche (+), Droite (-)
	private static int boussole = 0;

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
			ajouterboussole(angleG); // Gauche = +
		} else {
			int angleD = 360 - (int) anglePalet;
			robot.tournerD(angleD);
			ajouterboussole(-angleD); // Droite = -
		}

		System.out.println("Boussole actuelle : " + boussole + " degres");
		return true;
	}

	// ----------------------------- Gestion Boussole -----------------------------
	// Met à jour l'boussole et la garde entre -180 et +180 degrés
	public static void ajouterboussole(int deltaAngle) {
		boussole = (boussole + deltaAngle) % 360;
		if (boussole > 180) {
			boussole -= 360;
		} else if (boussole <= -180) {
			boussole += 360;
		}
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
	}

	// ----------------------------- centrerEtAvancer -----------------------------
	public static void centrerEtAvancer(float distPalet) {
		int balaie = 6; // balaie de 6° de chaque côté
		robot.tournerG(balaie);
		ajouterboussole(balaie); // Mise à jour boussole

		ArrayList<Float> mesures = new ArrayList<Float>();
		robot.asyncTournerD(2 * balaie);
		while (robot.isMoving() && !Button.ESCAPE.isDown()) {
			mesures.add(capteur.distance());
			Delay.msDelay(10);
		}
		ajouterboussole(-(2 * balaie)); // Mise à jour boussole après le balayage à droite

		int n = mesures.size();
		int premier = -1, dernier = -1;
		for (int i = 0; i < n; i++) {
			if (Math.abs(mesures.get(i) - distPalet) <= 0.05f) {
				if (premier == -1) premier = i;
				dernier = i;
			}
		}
		if (premier == -1 || n == 0) {
			robot.tournerG(balaie);
			ajouterboussole(balaie); // Mise à jour boussole
			return;
		}
		
		// Calcul du milieu du palet
		float milieu = (premier + dernier) / 2f;
		float angleMilieu = milieu * (2f * balaie) / n;
		int retour = Math.round((2 * balaie) - angleMilieu);
		
		robot.tournerG(retour);
		ajouterboussole(retour); // Mise à jour boussole

		avancerEtPrendrePalet();
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

	public static void qualif() {
		// Exemple d'utilisation de la boussole pour se remettre face au camp adverse (0°) :
		// if (boussole > 0) { robot.tournerD(boussole); }
		// else if (boussole < 0) { robot.tournerG(-boussole); }
		// boussole = 0;
	}

	public static void main(String[] args) {
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
