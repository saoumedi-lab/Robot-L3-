import java.util.ArrayList;
import lejos.hardware.Button;

/**
 * Tests de la classe Robotdefou.
 *
 * Les tests de reconnaissance utilisent un scan enregistré.
 * Les tests physiques nécessitent la brique EV3.
 */
public class TestRobotdefou {

    private Action action;
    private Capteur capteurs;

    /** Distances enregistrées pendant un scan, en mètres. */
    private final float[] distancesTest = {
        0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
        0.45200002f, 0.45200002f, 0.46400002f, 0.48800004f,
        0.48800004f, 1.4280001f, 1.9970001f, 1.4430001f,
        1.4280001f, 1.4120001f, 0.38300002f, 0.36600003f,
        0.36400002f, 0.37600002f, 0.393f, 0.393f,
        0.393f, 0.393f, 0.393f, 0.393f,
        0.64500004f, 0.64500004f, 0.64100003f, 0.648f,
        0.64900005f, 0.64900005f, 0.47300002f, 0.45600003f,
        0.448f, 0.448f, 0.44700003f, 0.47400004f,
        0.49800003f, 0.49800003f, 0.49800003f, 0.49800003f,
        0.49800003f, 0.49800003f, 0.49800003f, 0.384f,
        0.372f, 0.35700002f, 0.34800002f, 0.347f,
        0.34500003f, 0.34f, 0.34500003f, 0.34500003f,
        0.347f, 0.34800002f, 0.35700002f, 0.36200002f,
        0.37f, 0.38000003f, 0.397f, 0.42000002f,
        0.437f, 0.437f, 1.406f, 1.393f,
        1.393f, 1.393f, 1.393f, 1.393f,
        1.393f, 1.393f, 1.393f
    };

    /**
     * Crée les composants nécessaires à un test physique.
     * Vérifie indirectement l'utilisation de initialiser().
     */
    private void preparerRobot() {
        action = new Action();
        capteurs = new Capteur();
        Robotdefou.initialiser(action, capteurs);
    }

    /** Arrête les roues et ferme les capteurs après un test physique. */
    private void terminerTestPhysique() {
        try {
            if (action != null) {
                action.stop();
            }
        } finally {
            if (capteurs != null) {
                capteurs.fermer();
            }
        }
    }

    /** Attend que le robot soit placé avant un test physique. */
    private void attendreDepart() {
        System.out.println("ENTER pour demarrer.");
        Button.ENTER.waitForPressAndRelease();
    }

    /**
     * Vérifie la reconnaissance d'un candidat et les principaux rejets.
     * Les résultats attendus correspondent aux critères actuels du code.
     */
    public void testEstUnPalet() {
        System.out.println("\n=== TEST : estUnPalet ===");

        float[][] jeuxEssai = {
            // Scan réel : candidat attendu à l'indice 20.
            distancesTest,

            // Sept mesures proches entre deux fonds éloignés.
            {
                1.20f, 1.20f, 1.20f,
                0.45f, 0.44f, 0.43f, 0.42f,
                0.43f, 0.44f, 0.45f,
                1.20f, 1.20f, 1.20f
            },

            // Trois mesures : signal trop court.
            {
                1.20f, 0.45f, 0.44f, 0.45f, 1.20f
            },

            // Onze mesures : signal trop large.
            {
                1.20f,
                0.45f, 0.45f, 0.45f, 0.45f, 0.45f,
                0.45f, 0.45f, 0.45f, 0.45f, 0.45f,
                0.45f,
                1.20f
            },

            // Distances hors de l'intervalle autorisé.
            {
                0.20f, 0.25f, 1.00f, 1.20f
            },

            // Aucun fond visible autour du signal.
            {
                0.45f, 0.45f, 0.45f, 0.45f,
                0.45f, 0.45f, 0.45f
            },

            // Mesures invalides.
            {
                0.0f, -1.0f, Float.NaN,
                Float.POSITIVE_INFINITY
            },

            // Tableau vide.
            {}
        };

        String[] descriptions = {
            "Scan enregistre",
            "Signal de sept mesures",
            "Signal trop court",
            "Signal trop large",
            "Distances hors intervalle",
            "Absence de bords visibles",
            "Mesures invalides",
            "Tableau vide"
        };

        int[] attendus = {20, 6, -1, -1, -1, -1, -1, -1};

        for (int i = 0; i < jeuxEssai.length; i++) {
            int obtenu = Robotdefou.estUnPalet(jeuxEssai[i]);

            System.out.println(descriptions[i]);
            System.out.println("Attendu : " + attendus[i]);
            System.out.println("Obtenu : " + obtenu);
            System.out.println("Test reussi : "
                    + (obtenu == attendus[i]));
        }
    }

    /**
     * Vérifie la détection de plusieurs candidats et le scan enregistré.
     * Ne commande aucun moteur.
     */
    public void testDetecterTousLesPalets() {
        System.out.println("\n=== TEST : detecterTousLesPalets ===");

        ArrayList<Integer> obtenus =
                Robotdefou.detecterTousLesPalets(distancesTest);

        boolean scanReussi = obtenus.size() == 1
                && obtenus.contains(20);

        System.out.println("Scan enregistre : attendu [20]");
        System.out.println("Obtenu : " + obtenus);
        System.out.println("Test reussi : " + scanReussi);

        // Deux signaux séparés, avec des distances différentes.
        float[] deuxPalets = {
            1.20f,
            0.45f, 0.44f, 0.43f, 0.44f,
            1.20f,
            0.65f, 0.64f, 0.63f, 0.64f,
            1.20f
        };

        obtenus = Robotdefou.detecterTousLesPalets(deuxPalets);

        boolean deuxPaletsReussi = obtenus.size() == 2
                && obtenus.contains(2)
                && obtenus.contains(7);

        System.out.println("Deux candidats : indices attendus 2 et 7");
        System.out.println("Obtenu : " + obtenus);
        System.out.println("Test reussi : " + deuxPaletsReussi);

        obtenus = Robotdefou.detecterTousLesPalets(new float[0]);

        System.out.println("Tableau vide : attendu []");
        System.out.println("Obtenu : " + obtenus);
        System.out.println("Test reussi : " + obtenus.isEmpty());
    }

    /**
     * Vérifie la sélection de la plus petite distance.
     * Les indices sont fournis directement pour isoler cette méthode.
     */
    public void testTrouverPaletPlusProche() {
        System.out.println("\n=== TEST : trouverPaletPlusProche ===");

        ArrayList<Integer> candidats = new ArrayList<Integer>();
        candidats.add(8);   // Environ 46,4 cm.
        candidats.add(20);  // Environ 39,3 cm.
        candidats.add(28);  // Environ 64,1 cm.

        int obtenu = Robotdefou.trouverPaletPlusProche(
                distancesTest, candidats);

        System.out.println("Indice attendu : 20");
        System.out.println("Indice obtenu : " + obtenu);
        System.out.println("Test reussi : " + (obtenu == 20));

        candidats.clear();

        obtenu = Robotdefou.trouverPaletPlusProche(
                distancesTest, candidats);

        System.out.println("Sans candidat : attendu -1");
        System.out.println("Obtenu : " + obtenu);
        System.out.println("Test reussi : " + (obtenu == -1));
    }

    /**
     * Vérifie qu'un scan réel se termine et produit des mesures.
     * La rotation doit également être vérifiée visuellement.
     */
    public void testScanner() {
        System.out.println("\n=== TEST PHYSIQUE : scanner ===");
        System.out.println("Libere l'espace autour du robot.");
        attendreDepart();

        ArrayList<Float> mesures = new ArrayList<Float>();

        try {
            preparerRobot();

            int resultat = Robotdefou.scanner(
                    action, capteurs, mesures);

            System.out.println("Code attendu : 0");
            System.out.println("Code obtenu : " + resultat);
            System.out.println("Nombre de mesures : " + mesures.size());
            System.out.println("Test reussi : "
                    + (resultat == 0 && !mesures.isEmpty()));
            System.out.println("Verifie aussi la rotation physique.");
        } finally {
            terminerTestPhysique();
        }
    }

    /** Vérifie l'enchaînement réel du scan et de l'orientation. */
    public void testAnalyser() {
        System.out.println("\n=== TEST PHYSIQUE : analyser ===");
        System.out.println("Place un palet entre 32 et 95 cm.");
        attendreDepart();

        try {
            preparerRobot();

            boolean resultat = Robotdefou.analyser();

            System.out.println("Attendu : true");
            System.out.println("Obtenu : " + resultat);
            System.out.println("Verifie : palet face aux pinces.");
            System.out.println("True ne garantit pas l'alignement physique.");
        } finally {
            terminerTestPhysique();
        }
    }

    /**
     * Vérifie l'orientation calculée depuis un indice du scan enregistré.
     * Le robot ne réalise pas de nouveau scan.
     */
    public void testOrienterVersPalet() {
        int indexPalet = 20;

        float angleAttendu =
                indexPalet * 360.0f / distancesTest.length;

        System.out.println("\n=== TEST PHYSIQUE : orienterVersPalet ===");
        System.out.println("Repere l'orientation initiale.");
        System.out.println("Angle estime : " + angleAttendu + " degres");
        attendreDepart();

        try {
            preparerRobot();

            Robotdefou.orienterVersPalet(
                    indexPalet, distancesTest.length);

            System.out.println("Verifie l'angle et le sens de rotation.");
        } finally {
            terminerTestPhysique();
        }
    }

    /**
     * Vérifie le centrage avec une distance cible issue du scan enregistré.
     * Le palet réel doit être presque face aux pinces, à environ 39,3 cm.
     */
    public void testCentrerEtAvancer() {
        float distanceCible = distancesTest[20];

        System.out.println("\n=== TEST PHYSIQUE : centrerEtAvancer ===");
        System.out.println("Place le palet presque face aux pinces.");
        System.out.println("Distance cible : "
                + distanceCible * 100 + " cm");
        System.out.println("Pour le centrage seul, commente l'appel");
        System.out.println("a avancerEtPrendrePalet dans centrerEtAvancer.");
        attendreDepart();

        try {
            preparerRobot();

            Robotdefou.centrerEtAvancer(distanceCible);

            System.out.println("Verifie : palet centre face aux pinces.");
        } finally {
            terminerTestPhysique();
        }
    }

    /**
     * Vérifie la sélection, l'orientation et le centrage d'une cible.
     * Replacer le robot et les palets comme pendant le scan enregistré.
     */
    public void testRecupererPaletPlusProche() {
        ArrayList<Integer> candidats = new ArrayList<Integer>();
        candidats.add(20);

        System.out.println(
                "\n=== TEST PHYSIQUE : recupererPaletPlusProche ===");
        System.out.println("Reproduis la position du scan enregistre.");
        System.out.println("Candidat fourni : indice 20.");
        System.out.println("La prise depend de son activation");
        System.out.println("dans centrerEtAvancer.");
        attendreDepart();

        try {
            preparerRobot();

            Robotdefou.recupererPaletPlusProche(
                    distancesTest, candidats);

            System.out.println("Verifie l'orientation et le centrage.");
            System.out.println("Si la prise est activee, verifie la capture.");
        } finally {
            terminerTestPhysique();
        }
    }

    /**
     * Vérifie l'approche et la prise d'un palet déjà face au robot.
     * ESCAPE permet d'interrompre les boucles d'approche.
     */
    public void testAvancerEtPrendrePalet() {
        System.out.println(
                "\n=== TEST PHYSIQUE : avancerEtPrendrePalet ===");
        System.out.println("Place le palet face aux pinces.");
        System.out.println("ESCAPE si le robot manque le palet.");
        attendreDepart();

        try {
            preparerRobot();

            Robotdefou.avancerEtPrendrePalet();

            if (Button.ESCAPE.isDown()) {
                System.out.println("Test interrompu.");
            } else {
                System.out.println("Verifie : roues arretees.");
                System.out.println("Verifie : palet retenu dans la pince.");
                System.out.println("Verifie : moteur de pince arrete.");
            }
        } finally {
            terminerTestPhysique();
        }
    }

    /**
     * Sélection des tests à lancer.
     * Un seul test physique doit être activé par exécution.
     */
    public static void main(String[] args) {
        TestRobotdefou tests = new TestRobotdefou();

        // Tests utilisant uniquement des tableaux.
        tests.testEstUnPalet();
        tests.testDetecterTousLesPalets();
        tests.testTrouverPaletPlusProche();

        // Tests physiques : activer UN seul appel à la fois.
        // tests.testScanner();
        // tests.testAnalyser();
        // tests.testOrienterVersPalet();
        // tests.testCentrerEtAvancer();
        // tests.testRecupererPaletPlusProche();
        // tests.testAvancerEtPrendrePalet();

        System.out.println("\nFin des tests.");
    }
}