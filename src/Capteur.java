import java.util.Arrays;
import lejos.hardware.Button;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.port.Port;
import lejos.hardware.sensor.EV3TouchSensor;
import lejos.robotics.SampleProvider;
import lejos.hardware.sensor.EV3ColorSensor;        
import lejos.robotics.Color;                                   
import lejos.robotics.filter.MeanFilter;
import lejos.utility.Delay;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3UltrasonicSensor;


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

	// Valeurs R, G, B d chaque surface
	// remplacer nbr par valeur qu'on va mesurer
	private final float[][] references = {
		{0.12f, 0.12f, 0.11f}, // fond
		{0.20f, 0.20f, 0.18f}, // blanc
		{0.03f, 0.12f, 0.04f}, // vert
		{0.05f, 0.05f, 0.05f}, // gris
		{0.02f, 0.04f, 0.10f}, // bleu
		{0.18f, 0.03f, 0.02f}, // rouge
		{0.01f, 0.01f, 0.01f}, // noir
		{0.20f, 0.16f, 0.03f}  // jaune
	};

	//distance max acceptée entre ce que voit le capteur et une reference, au-delà la couleur est considérée comme inconnue
	private final static float ERREUR = 0.05f;

    // Initialisation dans le constructeur de la classe
    public Capteur() {

	// ==========================================
    // ===    PARTIE CAPTEUR DE PRESSION      ===
    // ==========================================
		
        // le cptr pression est branché sur le port S3 
        portPression = LocalEV3.get().getPort("S3"); 

        // crée le capteur pression et l'associer le cptr au port choisi
        capteurPression = new EV3TouchSensor(portPression);

        // extraction du mode "Touch" du cptr pour savoir si le capteur est pressé ou non
        modePression = capteurPression.getMode("Touch");
        
        // initialisation du tableau avec la taille exacte demandée par le mode
        echantillonPression = new float[modePression.sampleSize()];

	// ==========================================
    // ===     PARTIE CAPTEUR DE COULEUR      ===
    // ==========================================

		// le cptr couleur est branché sur le port S2
		portCouleur = LocalEV3.get().getPort("S2");

		// associer le cptr au port choisi
		capteurCouleur = new EV3ColorSensor(portCouleur);

		// allume la lumiere du capteur
		capteurCouleur.setFloodlight(Color.WHITE);

		// mode RGB, la moyenne de 2 mesures à chaque lecture
		moyenne = new MeanFilter(capteurCouleur.getRGBMode(), 2);

		// crée un tableau qui reçoit les mesures (R, G et B)
		mesure = new float[moyenne.sampleSize()];
    }

	// ==========================================
    // ===    PARTIE CAPTEUR DE PRESSION      ===
    // ==========================================

	// renvoie true si le cptr est pressé
    public boolean detecterPression() { 
        // le cptr prend la mesure et la range dans le tableau
        modePression.fetchSample(echantillonPression, 0); 
		
        // Renvoie true si la valeur à l'index 0 vaut 1.0 (activé)
        return echantillonPression[0] == 1.0f; 
    }

    // ==========================================
    // ===     PARTIE CAPTEUR DE COULEUR      ===
    // ==========================================

	// calcul la distance entre deux couleur c1 et c2
	// plus le resultat est peit, plus les deux couleurs se ressemble
		public float distanceClr (float[] c1, float[] c2) {

			float dr = c1[0]-c2[0];//ecart sur le rouge
			float dg = c1[1]-c2[1];//ecart sur le vert
			float db = c1[2]-c2[2];//ecart sur le bleu

			// les carrés donnes les écarts positifs, la racine donne la distance
			return (float) Math.sqrt(dr*dr+dg*dg+db*db);
		} 


		// renvoi le nom de la couleur sous le cptr
		public String getCouleur() {
			// realise 2 mesure, la moyenne est rangé dans le tableau "mesure"
			moyenne.fetchSample(mesure, 0);

			// cherche la reference le plus proche de la mesure
			int meilleur = 0;// numero de référence gagnante pour l'instant 
			float plusPetite=distanceClr(mesure,references[0]);// sa distance avec la mesure

			// On teste chaque référence 
			for (int i = 1 ; i < noms.length; i++) {
				float d=distanceClr(mesure,references[i]);

				// si cette reference est plus proche que la meilleure trouvé, alors elle la remplace
				if (d < plusPetite) {
					plusPetite = d;
					meilleur = i;
				}
			}
			// si la meilleur référence est superieur à l'erreur (trop loin de la mesure), alors la couleur est inconnue 
			if (plusPetite > ERREUR) {
				return "inconnu";
			}

			// sinon on renvoie le nom de la référence gagnante
			return noms [meilleur];
		}


	// ==========================================
    // ===     PARTIE CAPTEUR DE DISTANCE     ===
    // ==========================================

	 // déclare que le capteur d'ultrason a pour nom cs, il est créé des la création de l'objet, sur le port S4
    private EV3UltrasonicSensor cs = new EV3UltrasonicSensor(SensorPort.S4);

	 //méthode utilisée dans la méthode tournerVers du principal qui récupère toutes les distance pendant la rotation du robot
    public float[] recherche(float[] tab) {
		tab = Arrays.copyOf(tab, tab.length + 1);
		tab[tab.length-1]=distance();
		return tab;
	}
	
	// renvoie la distance entre le cptr et l'obstacle devant lui
	public float distance() {
		 SampleProvider distance = cs.getDistanceMode();
		    // tableau pour recevoir la mesure
		    float[] mesure = new float[1];
		    // il mesure, puis range dans le tableau
		    distance.fetchSample(mesure, 0);
		    // renvoie la distance mesurée
		    return mesure[0];
	}


	// ferme les 3 capteurs 
	public void fermer() {
		capteurCouleur.close();
		 capteurPression.close();
   		 cs.close();
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
