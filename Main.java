import extensions.File;
import extensions.CSVFile;

/**
 * Microsoft World Conquest ft. Julien Baste
 * Le jeu RPG dans le Terminal absolument magnifique.
 * Il vous faudra une RTX 4070 et un processeur Ryzen 7950X au minimum pour l'exécuter. 
 * @author Thomas Gysemans, Manon Leclercq, S1-C, 2022
 */
class Main extends Program {
    /*
    /!\ ATTENTION
    Lors de nos tests, le fichier Main.java est dans le dossier `.`.
    Mais le fichier `.class` est automatiquement généré dans le dossier `./classes`,
    alors tous les chemins doivent être relatifs à ce dossier.
    En conséquence, les chemins seront tous préfixés de `../`.
    todo: il faudrait changer ce comportement
    */

    // Les valeurs numériques arbitraires associées aux flèches du clavier.
    // Pour trouver certaines valeurs, nous avons regarder le code source de iJava.
    final int TOP_ARROW_KEY = 17;
    final int BOTTOM_ARROW_KEY = 18;
    final int LEFT_ARROW_KEY = 19;
    final int RIGHT_ARROW_KEY = 20;
    final int ENTER_KEY = 13;

    // Un booléan pour savoir quand l'utilisateur a terminé de se déplacer ou d'intéragir
    // Une valeur à `false` stoppe la saisie si elle est déjà démarrée.
    boolean finishedTyping = false;
    boolean selectingInMenu = true; // true si le joueur est dans le menu principal du jeu, il l'est quand il lance le jeu
    int selectedMenu = 1; // par défaut, le joueur va sélectionner le premier élément du menu principal
    int menuPosX = 0; // l'écran est comme un graphique orthonormé dont le centre est en haut à gauche
    int menuPosY = 0; // alors pas le choix, faut donner à tout mouvement des coordonnées et jouer avec les nombres
    final int MENU_ITEMS_NUMBER = 4; // 4 choix possible dans le menu

    final String COLORS_FILE_NAME = "0-colors.csv";
    final String COLORS_PATH = "../assets/" + COLORS_FILE_NAME; // convention
    final String PIXEL = "  "; // En réalité, un pixel est deux espaces dont le fond est coloré avec ANSI
    final int PIXEL_SIZE = length(PIXEL); // On aura besoin de cette constante dans le calcul de mouvement vers la droite/gauche
    int currentMapIndex = 0; // la carte actuellement affichée
    int playerX = 0; // la position en X du joueur
    int playerY = 0; // la position en Y du joueur

    // On veut stocker les informations au préalable (on ne veut pas charger le fichier d'une carte)
    // Pour éviter de charger les fichiers, et de les recharger quand nécessaire,
    // on charge tout avant le début du jeu.
    // Problème : on ne sait pas en avance le nombre de couleurs ni le nombre de cartes.
    // On initialise le bordel ici, puis on changera ça après.
    Color[] COLORS = new Color[1];
    int[][][] MAPS = new int[1][1][1];

    boolean game_started = false;

    void algorithm() {
        clearMyScreen();
        println("One sec...");
        loadSplashScreen();
        /*
         * Nous ne voulons pas utiliser la méthode `readString` pour capturer les touches que le joueur saisit,
         * car il faudrait appuyer sur Entrer à chaque fois. Avec la tonne de déplacements que le joueur peut faire,
         * c'est trop handicapant.
         * On va donc écouter l'input de l'utilisateur en direct.
         * Ici, on initialise l'événement, et la fonction d'écoute est `void keyTypedInConsole(int key)`.
         */
        enableKeyTypedInConsole(true);
        while (!finishedTyping) {
            delay(500);
        }
        enableKeyTypedInConsole(false);
    }

    void loadSplashScreen() {
        clearMyScreen();
        printEqualsRow(50);
        printEmptyLines(1);
        int n = printASCII("../assets/ascii/main-title.txt");
        print(repeatChar(" ", 25) + RGBToANSI(new int[]{252, 191, 203}, false) + "ft. Julien Baste" + ANSI_RESET);
        printEmptyLines(2);
        printEqualsRow(50);
        printEmptyLines(1);

        // s'il n'y a pas de maps, ça veut dire que c'est la première fois qu'on charge le jeu.
        if (!game_started) {
            saveCursorPosition(); // on sauvegarde avant le chargement pour pouvoir effacer le texte ensuite
            print(repeatChar(" ", 20) + "Chargement...");
            initializeColors();
            initializeAllMaps();
            printEmptyLines(2);

            delay(500); // au cas où le chargement est trop vite, on veut que le joueur voit le splashscreen

            restoreCursorPosition(); // on va écrire à la place du mot "Chargement..."
        }

        print(repeatChar(" ", 12));
        printBoldText("Microsoft World Conquest");
        printEmptyLines(2);

        // Le menu principal
        // ---

        // constante qui correspond au nombre de lignes affichées depuis le coin supérieur gauche de l'écran sur l'axe Y
        menuPosY = n + 9;
        println(repeatChar(" ", 20) + "> Jouer");
        println(repeatChar(" ", 20) + "  Commandes");
        println(repeatChar(" ", 20) + "  Crédits");
        println(repeatChar(" ", 20) + "  Succès");
        printEmptyLines(2);
        println("Appuie sur Entrer pour confirmer.");

        game_started = true;
    }

    /**
     * Renvoie une chaine dans laquelle une autre chaine a été répétée autant de fois que précisé.
     * @param c La chaine à répéter.
     * @param times Le nombre de fois que la chaîne doit être répétée.
     */
    String repeatChar(String c, int times) {
        String str = "";
        for (int i = 0; i < times; i++) {
            str += c;
        }
        return str;
    }

    /**
     * Affiche une ligne dans laquelle il y a un certain nombre de fois le signe "="
     * @param times Le nombre de "=".
     */
    void printEqualsRow(int times) {
        print("\r");
        for (int i = 0; i < times; i++) {
            print("=");
        }
        println("");
    }
    
    /**
     * Saute une ligne pour créer un effet d'espacement entre du texte.
     * @param times Le nombre de lignes vides.
     */
    void printEmptyLines(int times) {
        for (int i = 0; i < times; i++) {
            println("");
        }
    }

    /**
     * Sélectionne un choix dans le menu principal en fonction de la touche captée.
     * Le joueur peut appuyer sur la flèche du haut pour sélectionner le choix du haut,
     * ou appuyez sur la flèche du bas pour sélectionner le choix du bas.
     * Pour modéliser ça, on utilise un entier, "movement".
     * @param movement -1 pour un mouvement vers le haut, 1 pour un movement vers le bas
     */
    void selectMenuItem(int movement) {
        int futureChoice = selectedMenu + movement;
        if (futureChoice < 1 || futureChoice > MENU_ITEMS_NUMBER) {
            return; // do nothing.
        }
        // 20 est une constante qui correspond au nombre d'espaces
        // depuis le côté gauche de l'écran dans le menu principal
        saveCursorPosition();
        moveCursorTo(20, menuPosY);
        print("  "); // on supprime le ">" devant le choix actuellement sélectionné
        if (movement == 1) {
            print("\033[1D\033[1B" + ">"); // recule le curseur et le descend d'une ligne
        } else {
            print("\033[1D\033[1A" + ">"); // recule le curseur et le monte d'une ligne
        }
        restoreCursorPosition();
        selectedMenu = futureChoice;
        menuPosY += movement;
    }

    /**
     * Lorsque l'utilisateur appuie sur une touche,
     * cette fonction reçoit un code numérique correspond à la touche entrée.
     * Il s'agit d'une fonction qui capture l'événement en direct.
     * @param a Le code numérique correspondant à la touche entrée par l'utilisateur lors de l'écoute de l'événement.
     */
    void keyTypedInConsole(int a) {
        if (selectingInMenu) {
            switch (a) {
                case TOP_ARROW_KEY:
                    selectMenuItem(-1);
                    break;
                case BOTTOM_ARROW_KEY:
                    selectMenuItem(1);
                    break;
                case ENTER_KEY:
                    selectingInMenu = false;
                    // Switch imbriqué ! C'est si beauuu
                    switch (selectedMenu) {
                        case 1:
                            playGame();
                            break;
                        case 2:
                            shortcutsPage();
                            break;
                        case 3:
                            creditsPage();
                            break;
                        case 4:
                            achievementsPage();
                            break;
                        default:
                            println("HOLD ON!! Fix your code, bro! This doesn't make any sense.");
                    }
                    break;
                case 'q':
                    finishedTyping = true; // ceci va fermer le programme
                    break;
            }
        } else {
            switch (a) {
                case TOP_ARROW_KEY:
                    moveCursorUp();
                    break;
                case BOTTOM_ARROW_KEY:
                    moveCursorDown();
                    break;
                case LEFT_ARROW_KEY:
                    moveCursorToLeft();
                    break;
                case RIGHT_ARROW_KEY:
                    moveCursorToRight();
                    break;
                case 'q':
                    loadSplashScreen();
                    selectingInMenu = true;
                    break;
            }
        }
    }

    void creditsPage() {
        // todo.
    }

    void shortcutsPage() {
        // todo.
    }

    void achievementsPage() {
        // todo.
    }

    void playGame() {
        //finishedTyping = false; // il va falloir écrire

        clearMyScreen();
        moveCursorTo(0,0);
        displayMap(MAPS[currentMapIndex]);
        
        displayPlayer(0,0);
        //handleUserInput();
    }

    /**
     * On veut positionner le joueur à une position précise (x;y).
     * Avec (0;0) le coin supérieur gauche.
     * Pour cela, on sauvegarde la position actuelle du curseur,
     * On réécrit la couleur de la carte censée être à la position actuelle du joueur,
     * puis on bouge le curseur à la position souhaitée
     * afin d'écrire la couleur du pixel représentant le joueur.
     * Enfin, on remet le curseur à sa position initiale pour écouter les entrées suivantes.
     * @param x La position en X
     * @param y La position en Y
     */
    void displayPlayer(int x, int y) {
        // Si le joueur essaie de sortir de la carte,
        // on va juste ignorer le mouvement et ne rien faire.
        if (!isMoveAllowed(x,y)) {
            return;
        }

        saveCursorPosition();

        // Première étape : réécrire la bonne couleur à la position actuelle du joueur.
        int previousPos = MAPS[currentMapIndex][playerY][playerX/PIXEL_SIZE]; // les coordonnées seront toujours un multiple de PIXEL_SIZE
        moveCursorTo(playerX, playerY + 1);
        if (previousPos == -1) {
            printTransparentPixel();
        } else {
            printPixel(COLORS[previousPos]);
        }

        // Deuxième étape : écrire le pixel du joueur
        moveCursorTo(x, y + 1);
        printPixel("\u001b[48;2;255;0;0m");

        // On revient à la position initiale du curseur.
        restoreCursorPosition();
        // On définit les coordonnées données
        // comme étant la nouvelle position du joueur.
        playerY = y;
        playerX = x;
    }

    /**
     * Place le curseur à une certaine position sur la carte.
     */
    void moveCursorTo(int x, int y) {
        print("\033[" + y + ";" + x + "H");
    }

    /**
     * Supprime tout ce qu'il y avait dans le terminal avant l'entrée de la commande d'exécution du programme.
     * On utilise une fonction différente de celle de iJava (`clearScreen`) car celle-ci force le curseur
     * à se positionner au (0;0) tel que `moveCursorTo` le dicte.
     * Étonnamment, nous avons observer un comportement différent entre notre fonction et `clearScreen`.
     * 
     * Il est très important de repositionner correctement l'affichage dans le coin supérieur gauche.
     * Sans ça, il était possible que le curseur ne soit pas sur la bonne ligne, causant des comportements inattendus et étranges.
     */
    void clearMyScreen() {
        print("\033[2J");
        moveCursorTo(0,0);
    }

    /**
     * Enregistre la position actuelle du curseur.
     * Le curseur sera dirigé vers cette position lors d'un appelle à la fonction `restoreCursorPosition`.
     */
    void saveCursorPosition() {
        print("\033[s");
    }

    /**
     * On revient à la dernière position sauvegardée du curseur.
     */
    void restoreCursorPosition() {
        print("\033[u");
    }

    void moveCursorUp() {
        displayPlayer(playerX, playerY-1);
    }

    void moveCursorDown() {
        displayPlayer(playerX, playerY+1);
    }

    void moveCursorToLeft() {
        displayPlayer(playerX-PIXEL_SIZE, playerY);
    }

    void moveCursorToRight() {
        if (playerX == 0) {
            displayPlayer(playerX+PIXEL_SIZE+1, playerY);
        } else {
            displayPlayer(playerX+PIXEL_SIZE, playerY);
        }
    }

    /**
     * Vérifie si le mouvement souhaité par le joueur est possible.
     * Il y a deux raisons pour laquelle l'utilisateur peut ne pas pouvoir bouger vers la position souhaitée :
     * - il y a un mur (hors de la map)
     * - il y a une case infranchissable (on va pas marcher sur un PC, une table ou de l'eau)
     * @param posX La nouvelle position en X.
     * @param posY La nouvelle position en Y.
     * @return Vrai si le mouvement est autorisé, faux sinon.
     */
    boolean isMoveAllowed(int posX, int posY) {
        int[][] map = MAPS[currentMapIndex];
        int maxY = length(map);
        int maxX = length(map[0]) * PIXEL_SIZE;
        if (posX >= maxX || posY >= maxY || posX < 0 || posY < 0) {
            return false;
        }
        int colorIndex = map[posY][posX/PIXEL_SIZE];
        if (colorIndex == -1) {
            return false;
        } else if (!COLORS[colorIndex].x) {
            return false;
        }
        return true;
    }

    /**
     * Converti une couleur RGB au format ANSI.
     * La standardisation ANSI nous permet de colorer du texte, ou de le surligner.
     * @param rgb Une liste de 3 nombres entre 0 et 255 inclus, selon le format RGB.
     * @param backgroundColor Vrai si l'on veut que la couleur soit sur le fond du texte. Le texte aura donc la couleur par défaut.
     */
    String RGBToANSI(int[] rgb, boolean backgroundColor) {
        return "\u001b[" + (backgroundColor ? "48" : "38") + ";2;" + rgb[0] + ";" + rgb[1] + ";" + rgb[2] + "m";
    }

    /**
     * Lis le fichier contenant toutes les couleurs et autre métadonnées associées à ces couleurs.
     * Chaque couleur doit être définie comme étant franchissable ou non.
     * Une couleur dite "infranchissable" est un pixel sur lequel l'utilisateur ne peut pas marcher.
     * Cette fonction ne sera appelé qu'une seule fois lors de l'initialisation du jeu.
     * Le fichier est appelé "0-colors.csv" dans "/assets/".
     */
    void initializeColors() {
        CSVFile colors = loadCSV(COLORS_PATH);
        int nbLignes = rowCount(colors);
        int x,i,r,g,b;

        // Nous sommes obligés de redefinir COLORS
        // pour que nous ayons une liste de taille prédéfinie
        // car on peut toujours pas utiliser ArrayList!!!!!
        COLORS = new Color[nbLignes-1];

        for (int lig=1;lig<nbLignes;lig++) {
            x = stringToInt(getCell(colors, lig, 1));
            i = stringToInt(getCell(colors, lig, 2));
            r = stringToInt(getCell(colors, lig, 3));
            g = stringToInt(getCell(colors, lig, 4));
            b = stringToInt(getCell(colors, lig, 5));
            //print("(" + r + ";" + g + ";" + b + "), i = " + i + " and x = " + x);
            COLORS[lig-1] = newColor(r,g,b,x==1,i==1);
        }
    }

    /**
     * Fake constructor of the Color class.
     */
    Color newColor(int r, int g, int b, boolean x, boolean i) {
        Color color = new Color();
        color.ANSI = RGBToANSI(new int[]{r,g,b}, true);
        color.x = x;
        color.i = i;
        return color;
    }

    /**
     * Pour gagner en fluidité lors du jeu, nous allons charger toutes les cartes du jeu lors de l'initialisation du jeu.
     * Toutes les cartes sont stockées dans "/assets/maps/".
     * Chaque carte est une liste à deux dimensions (une grille) de nombres,
     * où chaque nombre est le numéro unique associé à une couleur dans la charte (`0-colors.csv`)
     */
    void initializeAllMaps() {
        String path = "../assets/maps/";
        String[] mapsFiles = getAllFilesFromDirectory(path); // /!\ "../" because the .class file is in `./classes` and assets is in `./assets`
        int numberOfMaps = length(mapsFiles);

        // Encore une fois, on n'est pas autorisés à utiliser ArrayList,
        // ce qui nous limite dans nos efforts d'optimisation.
        // Ici, on utilise une astuce pour que Java ne renvoie pas d'erreurs.
        // Même si on définit que les maps seront d'une taille 2 x 2,
        // elles ne le seront pas, et aucune erreur ne sera renvoyée.
        MAPS = new int[2][2][numberOfMaps];


        for (int i = 0; i < numberOfMaps; i++) {
            CSVFile file = loadCSV(path + mapsFiles[i]);
            int[][] map = createMapFromCSVContent(file);
            MAPS[i] = map;
        }
    }

    /**
     * Lis un fichier CSV pour le convertir en une liste de nombres utilisable dans le programme.
     * @param file Le fichier CSV chargé.
     * @return Une grille.
     */
    int[][] createMapFromCSVContent(CSVFile file) {
        int nbLignes = rowCount(file);
        int nbCol = columnCount(file);
        int[][] map = new int[nbLignes-1][nbCol-2];
        for (int lig=1;lig<nbLignes;lig++) { // starting at 1 to avoid the first line of the CSV file
            for (int col=2;col<nbCol;col++) { // starting at 2 to avoid the metadata for now
                String cell = getCell(file,lig,col);
                int index = stringToInt(cell);
                map[lig-1][col-2] = index;
            }
        }
        return map;
    }

    /**
     * Affiche une carte dans le terminal.
     * On notera qu'un pixel = 2 caractères (deux espaces) car ensemble ils forment un carré.
     * Un caractère, par défaut, forme un rectangle.
     * @param map La carte à afficher.
     */
    void displayMap(int[][] map) {
        int nLines = length(map);
        int nCol = length(map[0]);
        for (int lig=0;lig<nLines;lig++) {
            for (int col=0;col<nCol;col++) {
                int n = map[lig][col];
                if (n == -1) {
                    printTransparentPixel();
                } else {
                    printPixel(COLORS[n]);
                }
            }
            println(""); // very important
        }
    }

    /**
     * Colore un pixel.
     */
    void printPixel(Color color) {
        print(color.ANSI + PIXEL + ANSI_RESET);
    }

    /**
     * Colore un pixel transparent
     */
    void printTransparentPixel() {
        print(ANSI_BG_DEFAULT_COLOR + PIXEL + ANSI_RESET);
    }

    /**
     * Colore un pixel qui ne provient pas d'une carte.
     * C'est le cas pour le pixel du joueur.
     */
    void printPixel(String color) {
        print(color + PIXEL + ANSI_RESET);
    }

    /**
     * Affiche un texte sur la ligne courant en gras.
     */
    void printBoldText(String text) {
        print("\033[1m" + text + ANSI_RESET);
    }

    /**
     * Affiche une image ASCII depuis un fichier texte.
     * @param path Le chemin vers le fichier texte.
     * @return Un entier qui correspond au nombre de lignes affichées.
     */
    int printASCII(String path) {
        File unTexte = newFile(path);
        int i = 0;
        while (ready(unTexte)) {
            println(readLine(unTexte));
            i++;
        }
        return i;
    }

    /*
     *
     * C'est le moment de parler d'une dinguerie...
     * En bref, la façon dont fonctionne `enableKeyTypedInConsole` c'est de changer
     * le mode d'entrée des commandes du terminal via `stty raw`.
     * Le problème n'est pas très clair, mais en gros, quand on fait un clearMyScreen(),
     * tout en étant en mode `raw`, les "carriage return" sont oubliés (\r), et par conséquent,
     * on obtient le "staircase effect" comme décrit ci-dessous.
     * J'ai donc manuellement ajouté le "\r" aux méthodes `println`.
     *
     * Exemple pour le "staircaise effect" causé par le `println` de iJava (un `println` par ligne) :
     * ```
     * hello
     *      my
     *        name
     *            is
     *              JOHN
     *                  CENA
     * ```
     *
     * Plus d'info ici : https://unix.stackexchange.com/a/366426
     *
     */

    void println(String str) {
        print("\r" + str + "\r\n");
    }

    void println(int a) {
        print("\r" + a + "\r\n");
    }

    /*
    *
    * Les fonctions ci-dessous devront être retirées du rendu final,
    * car elles ne sont que des fonctions de tests si l'on rencontre
    * des bugs difficiles à fixer durant le développement.
    * 
    */

    // Affiche le contenu d'une carte dans le terminal de manière à en comprendre son contenu
    // pour débugger plus facilement s'il y a un problème.
    // C'est l'équivalent d'un console.log d'une liste en JavaScript.
    void debugMap(int[][] map) {
        int height = length(map);
        int width = length(map[0]);
        for (int i = 0; i < height; i++) {
            print("[");
            for (int j = 0; j < width; j++) {
                print(map[i][j] + ", ");
            }
            print("]\n");
        }
    }
}
