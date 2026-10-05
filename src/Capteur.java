import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.port.Port;
import lejos.hardware.sensor.EV3TouchSensor;
import lejos.robotics.SampleProvider;
import lejos.hardware.sensor.EV3ColorSensor;        
import lejos.robotics.Color;                                   
import lejos.robotics.filter.MeanFilter;



public class Capteur {

	// ==========================================
    // === PARTIE CAPTEUR DE PRESSION         ===
    // ==========================================
    // Représente la prise physique sur la brique EV3
    Port portPression;

    // Objet représentant notre capteur de contact physique
    EV3TouchSensor capteurPression;

    // Le mode de lecture du capteur (le cptr prend pas de mesures) 
    SampleProvider modePression; 

    // peu importe ce que le cptr mesure, il renvoie les données en un tableau de décimaux 
    float[] echantillonPression;

	// ==========================================
    // ===     PARTIE CAPTEUR DE COULEUR      ===
    // ==========================================

	private Port portCouleur;

	// Le capteur de couleur physique
	private EV3ColorSensor capteurCouleur;

	//fait la moyenne de plusieurs mesure à chaque lecture
	private SampleProvider moyenne;

	// Tableau qui reçoit la mesure: rouge = [0], vert = [1], bleu = [2]
	private float[] mesure;

	// Noms des couleurs reconnues 
	private final String[] noms = {"fond", "blanc", "vert", "gris", "bleu", "rouge", "noir", "jaune"};

	// Valeurs R, G, B 
	// remplacer nbr par valeur qu'on va mesurer
	private final float[][] references = {
		{0.12f, 0.12f, 0.11f}, // fond
		{0.20f, 0.20f, 0.18f}, // blanc
		{0.03f, 0.12f, 0.04f}, // vert
		{0.05f, 0.05f, 0.05f}, // gris
		{0.02f, 0.04f, 0.10f}, // bleu fonce
		{0.18f, 0.03f, 0.02f}, // rouge
		{0.01f, 0.01f, 0.01f}, // noir
		{0.20f, 0.16f, 0.03f}  // jaune
	};

	//distance max acceptée entre ce que voit le capteur et une reference
	private final static float ERREUR = 0.05f;

    // Initialisation dans le constructeur de la classe
    public Capteur() {

	// ==========================================
    // === PARTIE CAPTEUR DE PRESSION         ===
    // ==========================================
		
        // le port s1 est par défaut , faut vérifier sur le robot lequel est branché 
        portPression = LocalEV3.get().getPort("S3"); 

        // associer le cptr au port choisi
        capteurPression = new EV3TouchSensor(portPression);

        // extraction du mode "Touch" du cptr pour pouvoir lire s'il est pressé ou non
        modePression = capteurPression.getMode("Touch");
        
        // initialisation du tableau avec la taille exacte demandée par le mode
        echantillonPression = new float[modePression.sampleSize()];

	// ==========================================
    // ===     PARTIE CAPTEUR DE COULEUR      ===
    // ==========================================

		portCouleur = LocalEV3.get().getPort("S2");

		//crée le capteur, et relier au port ou il est branché
		capteurCouleur = new EV3ColorSensor(portCouleur);

		//allume la lumiere du capteur
		capteurCouleur.setFloodlight(Color.WHITE);

		//mode RGB, la moyenne de 2 mesures à chaque lecture
		moyenne = new MeanFilter(capteurCouleur.getRGBMode(), 2);

		//crée un ttableau qui reçoit les mesures (R,G etB)
		mesure = new float[moyenne.sampleSize()];
    }

	// ==========================================
    // === PARTIE CAPTEUR DE PRESSION         ===
    // ==========================================
	
    public boolean detecterPression() { 
        // le cptr prend la mesure et la range dans le tab 
        modePression.fetchSample(echantillonPression, 0); 
        
        // Renvoie true si la valeur à l'index 0 vaut 1.0 (activé)
        return echantillonPression[0] == 1.0f; 
    }

  // ==========================================
    // ===     PARTIE CAPTEUR DE COULEUR      ===
    // ==========================================

	//calcul la distance entre deux couleur c1 et c2
		public float distanceClr (float[] c1, float[] c2) {

			float dr = c1[0]-c2[0];//ecart sur le rouge
			float dg = c1[1]-c2[1];//ecart sur le vert
			float db = c1[2]-c2[2];//ecart sur le bleu

			//on multiplie par lui meme permet d'avoir une distance positive
			return (float) Math.sqrt(dr*dr+dg*dg+db*db);
		} 


		//renvoi le nom de la ligne sous le capteur
		public String getCouleur() {
			//realise une mesure, 2 mesure sont fait, la moyenne est calculé et le resultat et rangé dans le tableau mesure
			moyenne.fetchSample(mesure, 0);

			//cherche la reference le plus proche
			int meilleur = 0;//numeré du gagnant 
			float plusPetite=distanceClr(mesure,references[0]);//sa distance avec la mesure

			// On teste chaque référence 
			for (int i = 1 ; i < noms.length; i++) {
				float d=distanceClr(mesure,references[i]);

				//si cette reference est plus proche que la meilleure d'avnat, alors elle devient la nouvelle meilleur
				if (d < plusPetite) {
					plusPetite = d;
					meilleur = i;
				}
			}
			// si la distance est superieur à l'erreur, la meilleur reference est alors trop differente de ce que perçoit le capteur
			if (plusPetite > ERREUR) {
				return "inconnu";
			}

			//sinon on renvoie son nom
			return noms [meilleur];
		}

	public void fermer() {
		capteurCouleur.close();
	}

	// ==========================================
    // === PARTIE CAPTEUR DE distance         ===
    // ==========================================
public float distance() {
		 SampleProvider distance = cs.getDistanceMode();
		    float[] mesure = new float[1];
		    distance.fetchSample(mesure, 0);
		    return mesure[0];
	}



	
	public static void main(String[] args) {
	System.out.println("Allumage des capteurs...");
        Capteur monCapteur = new Capteur();
        System.out.println("Prets (S3 et S2) !");
        
        // Boucle infinie jusqu'à appui sur le bouton Echap
        while (!Button.ESCAPE.isDown()) {
            
            boolean estPresse = monCapteur.detecterPression();
            String couleurVue = monCapteur.getCouleur();
            
            // Affiche les deux infos en même temps
            System.out.println("Bouton: " + estPresse + " | Coul: " + couleurVue);
            
            Delay.msDelay(300);
        }
        
        monCapteur.fermer();
    }
}
