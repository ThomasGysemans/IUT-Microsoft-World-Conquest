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

    final String VERSION = "alpha";
    final int GUI_HORIZONTAL_MARGIN = 2;
    final int GUI_VERTICAL_MARGIN = 1;

    // Les valeurs numériques arbitraires associées aux flèches du clavier.
    // Pour trouver certaines valeurs, nous avons regarder le code source de iJava ;)
    final int TOP_ARROW_KEY = 17;
    final int BOTTOM_ARROW_KEY = 18;
    final int LEFT_ARROW_KEY = 19;
    final int RIGHT_ARROW_KEY = 20;
    final int ENTER_KEY = 13;

    // Un booléan pour savoir quand l'utilisateur a terminé de se déplacer ou d'intéragir
    // Une valeur à `true` stoppe la saisie si elle est déjà démarrée, et arrête le programme.
    boolean finishedTyping = false;
    Page page = Page.MENU; // la page actuelle
    int selectedMenu = 1; // par défaut, le joueur va sélectionner le premier élément du menu principal
    int menuPosX = 0; // l'écran est comme un graphique orthonormé dont le centre est en haut à gauche
    int menuPosY = 0; // alors pas le choix, faut donner à tout mouvement des coordonnées et jouer avec les nombres
    final int MENU_ITEMS_NUMBER = 4; // 4 choix possibles dans le menu

    final String MAPS_PATH = "../assets/maps/";
    final String COLORS_PATH = "../assets/0-colors.csv"; // le fichier contenant toutes les couleurs
    final String TELEPORTATIONS_PATH = "../assets/0-teleportations.csv"; // le fichier contenant toutes les passerelles entre les maps
    final String COMMANDS_PATH = "../assets/0-commands.csv"; // le fichier contenant toutes les commandes par défaut
    final String PIXEL = "  "; // En réalité, un pixel est deux espaces dont le fond est coloré avec ANSI
    final int PIXEL_SIZE = length(PIXEL); // On aura besoin de cette constante dans le calcul de mouvement vers la droite/gauche
    final String FIRST_MAP = "bibliotheque"; // la première map sur laquelle le joueur démarre
    String currentMap = FIRST_MAP; // la carte actuellement affichée
    int playerX = 0; // la position en X du joueur
    int playerY = 0; // la position en Y du joueur

    // On veut stocker les informations au préalable
    // Pour éviter de charger les fichiers, et de les recharger quand nécessaire,
    // on charge tout une seule fois avant le début du jeu.
    // Problème : on ne sait pas en avance le nombre de couleurs ni le nombre de cartes.
    // On initialise le bordel ici, puis on changera ça après.
    // Pour notre plus grand malheur, ArrayList n'est pas autorisée
    Color[] COLORS = new Color[1];
    Map[] MAPS = new Map[1]; // lol avant on avait int[][][] MAPS = new int[1][1][1], quel délire !
    Command[] COMMANDS = new Command[1];

    final int DAY_DELAY = 6*60; // 6 minutes = 1 jour
    //final int HOUR_DELAY = DAY_DELAY/24; // délai pour une heure
    final int HOUR_DELAY = 2; // le nombre de secondes nécessaires pour incrémenter l'heure
    int hour = 17; // l'heure actuelle (in-game)
    int day = 0; // le jour actuel (in-game)
    Thread time; // le thread séparé correspondant au temps

    boolean game_started = false;

    void algorithm() {
        loadMainMenu();
        /*
        * Sur un thread séparé, on compte l'heure et le jour
        */
        createTime(() -> {
            hour++;
            if (hour == 24) {
                hour = 0;
                day++;
            }
            displayTime();
            time.run();
        }, HOUR_DELAY*1000);
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

    /**
     * Noous créons le temps.
     * Cette fonction crée un thread séparé pour incrémenter le temps de manière asynchrone.
     * @param runnable la fonction à exécuter au bout du délai
     * @param delay Le délai entre chaque exécution de la fonction `runnable`
     */
    void createTime(Runnable runnable, int delay) {
        time = new Thread(() -> {
            try {
                Thread.sleep(delay);
                runnable.run();
            } catch (Exception e){
                // ignore, for now at least
            }
        });
    }

    /**
     * Charge le menu principal.
     * Ceci est la page `Page.MENU`.
     */
    void loadMainMenu() {
        page = Page.MENU;
        selectedMenu = 1; // default valuew

        int width = 50;
        clearMyScreen();
        printEqualsRow(width);
        printEmptyLines(1);
        int n = printASCII("../assets/ascii/main-title.txt");
        print(repeatChar(" ", 25) + RGBToANSI(new int[]{252, 191, 203}, false) + "ft. Julien Baste" + ANSI_RESET);
        printEmptyLines(2);
        printEqualsRow(width);
        printEmptyLines(1);

        // s'il n'y a pas de maps, ça veut dire que c'est la première fois qu'on charge le jeu.
        if (!game_started) {
            saveCursorPosition(); // on sauvegarde avant le chargement pour pouvoir effacer le texte ensuite
            print(repeatChar(" ", 20) + "Chargement...");
            initializeColors();
            initializeAllMaps();
            initializeAllCommands();
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
        println("Appuie sur 'q' pour quitter.");
        printEmptyLines(1);
        printlnRightAlinedText("version " + VERSION, width);

        game_started = true;
    }

    /**
     * Renvoie une chaine dans laquelle une autre chaine a été répétée autant de fois que précisé.
     * @param c La chaine à répéter.
     * @param times Le nombre de fois que la chaîne doit être répétée.
     * @return Une nouvelle chaine contenant le caractère `c` répété `times` fois.
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
        saveCursorPosition();
        // 20 est une constante qui correspond au nombre d'espaces
        // depuis le côté gauche de l'écran dans le menu principal
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
     * cette fonction reçoit un code numérique correspondant à la touche entrée.
     * Il s'agit d'une fonction qui capture l'événement en direct.
     * @param a Le code numérique correspondant à la touche entrée par l'utilisateur lors de l'écoute de l'événement.
     */
    void keyTypedInConsole(int a) {
        if (page == Page.MENU) {
            switch (a) {
                case TOP_ARROW_KEY:
                    selectMenuItem(-1);
                    break;
                case BOTTOM_ARROW_KEY:
                    selectMenuItem(1);
                    break;
                case ENTER_KEY:
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
        } else if (page == Page.GAME) {
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
                    loadMainMenu();
                    time.interrupt(); // met le temps en pause
                    break;
            }
        } else {
            if (a == 'q') {
                loadMainMenu();
            }
        }
    }

    void loadEmptyPage() {
        clearMyScreen();
        println("Wooooow! Même si on a pas de vie, on a pas encore eu le temps de coder ça, sorry!");
        println("Appuie sur 'q' pour revenir au menu.");
    }

    void creditsPage() {
        page = Page.CREDITS;
        loadEmptyPage();
        // todo.
    }

    void shortcutsPage() {
        page = Page.COMMANDS;
        loadEmptyPage();
        // todo.
    }

    void achievementsPage() {
        page = Page.ACHIEVEMENTS;
        loadEmptyPage();
        // todo.
    }

    /**
     * Initialise le jeu (Page.GAME).
     * Débute le "temps".
     * Et affiche la map actuelle, ainsi que le joueur aux coordonnées initiales.
     */
    void playGame() {
        page = Page.GAME;
        time.start();

        clearMyScreen();
        Map map = getMapOfName(currentMap);
        displayMap(map);
        printPlayer(0,0);

        displayTime();
    }

    /**
     * Affiche le temps dans l'interface graphique à une position donnée.
     */
    void displayTime() {
        if (page != Page.GAME) {
            return;
        }
        Map map = getMapOfName(currentMap);
        int height = getGUIHeight(map);
        saveCursorPosition();
        moveCursorTo(0,height-1); // entre la map et la ligne de "=" du dessous
        // la largeur de la ligne change souvent (si heur < 10 par exemple)
        // donc on risquerait de pas supprimer le texte précédent,
        // alors on ajoute plein d'espaces pour être sûr qu'on efface tout sur cette ligne.
        // Pour des raisons inconnues, `clearLine()` de iJava ne marche pas ici.
        println(hour + "h, jour " + (day + 1) + "    ");
        restoreCursorPosition();
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
        /*
        * Vérifie si le mouvement souhaité par le joueur est possible.
        * Il y a deux raisons pour lesquelles l'utilisateur peut ne pas pouvoir bouger vers la position souhaitée :
        * - il y a un mur (hors de la map)
        * - il y a une case infranchissable (on va pas marcher sur un PC, une table ou de l'eau)
        * On notera qu'un mouvement sur une case permettant
        * le transport du joueur vers une autre map est considiéré comme autorisé,
        * et on réalise la téléportation immédiatement.
        */
        Map map = getMapOfName(currentMap);
        int[][] grid = map.grid;
        int currentColorIndex = grid[playerY][playerX/PIXEL_SIZE];
        Color currentColor = COLORS[currentColorIndex];
        // S'il s'agit d'une passerelle vers une autre carte,
        // on vérifie si le mouvement appliqué permet d'y accéder.
        if (currentColor.t) {
            int shiftX = (x - playerX) / PIXEL_SIZE; // le déplacement qui a été réalisé sur l'axe X
            int shiftY = y - playerY; // le déplacement qui a été réalisé sur l'axe Y
            if (shiftX == currentColor.movX && shiftY == currentColor.movY) {
                teleportPlayerToNewMap(currentColor.toMap, currentColor.toX, currentColor.toY);
                return;
            }
        }
        
        // Il s'agit d'un déplacement normal quelque part sur la carte,
        // il nous faut vérifier si ce mouvement ne fait pas sortir le joueur en-dehors de celle-ci.
        int maxX = length(grid[0]) * PIXEL_SIZE;
        int maxY = length(grid);
        if (x >= maxX || y >= maxY || x < 0 || y < 0) {
            return;
        }

        // Il peut y avoir des murs, qui correspondent à des pixels transparents limitrophes.
        int colorIndexOfTarget = grid[y][x/PIXEL_SIZE];
        if (colorIndexOfTarget == -1) {
            return;
        }

        // Enfin, le pixel ciblé peut être considéré comme infranchissable.
        Color colorOfTarget = COLORS[colorIndexOfTarget];
        if (!colorOfTarget.x) {
            return;
        }

        // ---
        // Il s'agit d'un déplacement autorisé sur la carte actuelle

        saveCursorPosition();

        // Première étape : réécrire la bonne couleur à la position actuelle du joueur.
        int previousPos = map.grid[playerY][playerX/PIXEL_SIZE]; // les coordonnées seront toujours un multiple de PIXEL_SIZE
        Color color = previousPos == -1 ? null : COLORS[previousPos];
        int xShift = getXShift();
        int yShift = getYShift();
        moveCursorTo(playerX + xShift, playerY + yShift);
        if (previousPos == -1) {
            printTransparentPixel();
        } else {
            printPixel(color);
        }

        // Deuxième étape : écrire le pixel du joueur
        moveCursorTo(x + xShift, y + yShift);
        printPlayer();

        // On revient à la position initiale du curseur.
        restoreCursorPosition();
        // On définit les coordonnées données
        // comme étant la nouvelle position du joueur.
        playerY = y;
        playerX = x;
    }

    /**
     * Retourne la largeur entre le côté gauche (x=0) et le début de la map actuellement affichée.
     * Le but est de positionner le joueur correctement dans la map malgré le GUI.
     * @return Le décalage en X.
     */
    int getXShift() {
        return GUI_HORIZONTAL_MARGIN * PIXEL_SIZE - 1;
    }

    /**
     * Retourne la hauteur entre le haut (y=0) et le début de la map actuellement affichée.
     * Le but est de positionner le joueur correctement dans la map malgré le GUI.
     * @return Le décalage en Y.
     */
    int getYShift() {
        return GUI_VERTICAL_MARGIN + 2;
    }

    /**
     * Retourne la hauteur totale de l'interface graphique
     * @param map La map actuelle dont nous avons besoin de connaître la hauteur.
     * @return Un entier correspondant à la hauteur totale de l'interface graphique.
     */
    int getGUIHeight(Map map) {
        return (GUI_VERTICAL_MARGIN * 2) + length(map.grid) + 3; // +2 pour les lignes de "=" et +1 car on y affiche aussi le temps
    }

    /**
     * Retourne la largeur totale de l'interface graphique.
     * @param map La map actuelle dont nous avons besoin de connaître la largeur.
     * @return Un entier correspondant à la largeur totale de l'interface graphique.
     */
    int getGUIWidth(Map map) {
        return (GUI_HORIZONTAL_MARGIN * 2) + length(map.grid[0]);
    }

    /**
     * Place le curseur à une certaine position sur la carte.
     * @param x La coordonnée en X
     * @param y La coordonnée en Y
     */
    void moveCursorTo(int x, int y) {
        print("\033[" + y + ";" + x + "H");
    }

    /**
     * Supprime tout ce qu'il y avait dans le terminal avant l'entrée de la commande d'exécution du programme.
     * On utilise une fonction différente de celle de iJava (`clearScreen`) car la nôtre force le curseur
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
        displayPlayer(playerX+PIXEL_SIZE, playerY);
    }

    /**
     * Le joueur s'est déplacé sur une case dite de "passerelle" de telle façon qu'il doit être amené sur une autre carte.
     * C'est comme ça que l'on gère la "téléportation" entre deux cartes.
     * En bref, c'est le passage d'une carte à une autre quand on franchit une porte, par exemple.
     * @param map La carte vers laquelle rediriger le joueur.
     * @param targetX La position en X du joueur dans la nouvelle carte.
     * @param targetY La position en Y du joueur dans la nouvelle carte.
     */
    void teleportPlayerToNewMap(String map, int targetX, int targetY) {
        clearMyScreen();
        currentMap = map;
        displayMap();
        playerX = targetX * PIXEL_SIZE;
        playerY = targetY;
        printPlayer(playerX,playerY);
        displayTime();
    }

    /**
     * Converti une couleur RGB au format ANSI.
     * La standardisation ANSI nous permet de colorer du texte, ou de le surligner.
     * @param rgb Une liste de 3 nombres entre 0 et 255 inclus, selon le format RGB.
     * @param backgroundColor Vrai si l'on veut que la couleur soit sur le fond du texte. Le texte aura donc la couleur par défaut.
     * @return La couleur donnée au format ANSI, colorant le fond ou le texte.
     */
    String RGBToANSI(int[] rgb, boolean backgroundColor) {
        return "\u001b[" + (backgroundColor ? "48" : "38") + ";2;" + rgb[0] + ";" + rgb[1] + ";" + rgb[2] + "m";
    }

    /**
     * Lis le fichier contenant toutes les couleurs et autre métadonnées associées à ces couleurs.
     * Chaque couleur a des metadonnées associées à elle qui définissent des propriétés uniques.
     * Elles peuvent avoir les propriétés suivantes :
     * - être franchissable ou non (est-ce qu'on peut marcher dessus ou non)
     * - être interactive (un PNJ par exemple)
     * - être un chemin vers une autre map (exemple: une porte menant sur une autre salle), ce qu'on appelle la "téléportation"
     * Pour avoir toutes les données nécessaires, on lit plusieurs fichiers :
     * - La charte de couleurs (`./assets/0-colors.csv`)
     * - Les téléportations possibles (`./assets/0-teleportations.csv`)
     * Cette fonction ne sera appelée qu'une seule fois lors de l'initialisation du jeu.
     */
    void initializeColors() {
        CSVFile colors = loadCSV(COLORS_PATH);
        CSVFile teleportations = loadCSV(TELEPORTATIONS_PATH);
        int nColors = rowCount(colors); // nombre de couleurs
        int nTeleportations = rowCount(teleportations); // nombre de pixels menant à une téléportation
        int x,i,r,g,b,color,movX,movY,posX,posY; // toutes les données sous forme d'entier qu'on peut extraire des fichiers
        String map; // map cible de la téléportation

        // Nous sommes obligés de redefinir COLORS
        // pour que nous ayons une liste de taille prédéfinie
        // car on peut toujours pas utiliser ArrayList!!!
        COLORS = new Color[nColors-1];

        for (int lig=1;lig<nColors;lig++) {
            x = stringToInt(getCell(colors, lig, 1));
            r = stringToInt(getCell(colors, lig, 2));
            g = stringToInt(getCell(colors, lig, 3));
            b = stringToInt(getCell(colors, lig, 4));
            COLORS[lig-1] = newColor(r,g,b,x==1);
        }

        for (int lig=1;lig<nTeleportations;lig++) {
            color = stringToInt(getCell(teleportations, lig, 0));
            map = getCell(teleportations, lig, 1);
            movX = stringToInt(getCell(teleportations, lig, 2));
            movY = stringToInt(getCell(teleportations, lig, 3));
            posX = stringToInt(getCell(teleportations, lig, 4));
            posY = stringToInt(getCell(teleportations, lig, 5));
            COLORS[color].t = true;
            COLORS[color].toMap = map;
            COLORS[color].movX = movX;
            COLORS[color].movY = movY;
            COLORS[color].toX = posX;
            COLORS[color].toY = posY;
        }
    }

    /**
     * Crée une instance de Color avec les metadonnées de base.
     * Les autres métadonnées sont ajoutés ensuite.
     * Elles ont des valeurs par défaut.
     * @param r La composante rouge
     * @param g La composante verte
     * @param b La composante bleue
     * @param x Si c'est franchissable ou non
     */
    Color newColor(int r, int g, int b, boolean x) {
        Color color = new Color();
        color.ANSI = RGBToANSI(new int[]{r,g,b}, true);
        color.x = x;
        return color;
    }

    /**
     * Pour gagner en fluidité lors du jeu, nous allons charger toutes les cartes du jeu lors de l'initialisation du jeu.
     * Toutes les cartes sont stockées dans "./assets/maps/".
     * Chaque carte est une matrice où chaque nombre est
     * le numéro unique associé à une couleur dans la charte (`0-colors.csv`)
     */
    void initializeAllMaps() {
        String[] mapsFiles = getAllFilesFromDirectory(MAPS_PATH);
        int numberOfMaps = length(mapsFiles);

        // Encore une fois, on n'est pas "autorisés" à utiliser ArrayList ;(
        // c'est trop "avancé" :(
        // Dcp, on redéfinit la variable globale ici pour avoir la bonne longueur
        MAPS = new Map[numberOfMaps];

        for (int i = 0; i < numberOfMaps; i++) {
            String filename = mapsFiles[i];
            CSVFile file = loadCSV(MAPS_PATH + filename);
            int[][] grid = createMapGridFromCSVContent(file);
            MAPS[i] = newMap(filename, grid);
        }
    }

    /**
     * On crée une carte du jeu en fonction des données lues du fichier CSV.
     * @param name Le nom de la map, chaque map aura un nom unique.
     * @param grid La matrice de la carte.
     * @return Une instance de Maps
     */
    Map newMap(String name, int[][] grid) {
        Map map = new Map();
        map.name = name.substring(0,name.length()-4); // on veut retirer le ".csv", donc -4
        map.grid = grid;
        return map;
    }

    /**
     * Initialise toutes les commandes stockées dans le fichier `./assets/0-commands.csv`.
     */
    void initializeAllCommands() {
        CSVFile file = loadCSV(COMMANDS_PATH);
        int nCommands = rowCount(file);
        
        COMMANDS = new Command[nCommands];

        for (int lig=1;lig<nCommands;lig++) {
            String name = getCell(file, lig, 0);
            char character = charAt(getCell(file, lig, 1), 0);
            COMMANDS[lig-1] = newCommand(name, character);
        }
    }

    /**
     * Fake constructor of the Command class
     * @param name Le nom de la commande
     * @param character La touche du clavier associée à cette commande.
     * @return Une instance de Command
     */
    Command newCommand(String name, char character) {
        Command command = new Command();
        command.name = name;
        command.key = character;
        return command;
    }

    /**
     * Lis un fichier CSV pour le convertir en une liste de nombres utilisable dans le programme.
     * @param file Le fichier CSV chargé.
     * @return Une grille où chaque élément est le pixel d'une carte.
     */
    int[][] createMapGridFromCSVContent(CSVFile file) {
        int nbLignes = rowCount(file);
        int nbCol = columnCount(file);
        int[][] grid = new int[nbLignes-1][nbCol];
        for (int lig=1;lig<nbLignes;lig++) { // on commence à 1 pour éviter la première ligne du fichier csv
            for (int col=0;col<nbCol;col++) {
                String cell = getCell(file,lig,col);
                int index = stringToInt(cell);
                grid[lig-1][col] = index;
            }
        }
        return grid;
    }

    /**
     * Retourne l'instance de Map en fonction du nom de la carte actuelle.
     * @param name Le nom de la carte.
     * @return Une instance de Map stockée dans `MAPS`.
     */
    Map getMapOfName(String name) {
        for (int i = 0; i < length(MAPS); i++) {
            if (MAPS[i].name.equals(name)) {
                return MAPS[i];
            }
        }
        return null; // should never happen, except if we made a typo
    }

    /**
     * Affiche la carte actuelle dans le terminal.
     * On notera qu'un pixel = 2 caractères (deux espaces) car ensemble ils forment un carré.
     * Un caractère, par défaut, forme un rectangle.
     */
    void displayMap() {
        Map map = getMapOfName(currentMap);
        displayMap(map);
    }

    /**
     * Ici, la fonction qui permet d'afficher n'importe quelle map.
     * @param Map La carte à afficher
     */
    void displayMap(Map map) {
        int[][] grid = map.grid;
        int mapHeight = length(grid);
        int mapWidth = length(grid[0]);
        int equalsRowLength = mapWidth*PIXEL_SIZE+(GUI_HORIZONTAL_MARGIN*2);
        printEqualsRow(equalsRowLength);
        printEmptyLines(GUI_VERTICAL_MARGIN);
        for (int lig=0;lig<mapHeight;lig++) {
            print("  ");
            for (int col=0;col<mapWidth;col++) {
                int n = grid[lig][col];
                if (n == -1) {
                    printTransparentPixel();
                } else {
                    printPixel(COLORS[n]);
                }
            }
            println(""); // very important
        }
        printEmptyLines(GUI_VERTICAL_MARGIN + 1);
        printEqualsRow(equalsRowLength);
        String exampleCommand = getCommandText('F');
        println(exampleCommand);
    }

    /**
     * Retourne le texte à afficher pour une commande spécifique.
     * Selon le format : 
     * @param key Le caractère associé à cette commande.
     * @return Le texte formaté
     */
    String getCommandText(char key) {
        Command command;
        for (int i = 0; i < length(COMMANDS); i++) {
            if (COMMANDS[i].key == key) {
                return "   [" + key + "] " + COMMANDS[i].name.toUpperCase() + "   ";
            }
        }
        return "";
    }

    /**
     * Colore un pixel.
     * @param color La couleur à utiliser pour colorer le pixel
     */
    void printPixel(Color color) {
        print(color.ANSI + PIXEL + ANSI_RESET);
    }

    /**
     * Colore un pixel qui ne provient pas d'une carte.
     * C'est le cas pour le pixel du joueur.
     * @param color La couleur ANSI.
     */
    void printPixel(String color) {
        print(color + PIXEL + ANSI_RESET);
    }

    /**
     * Affiche le pixel correspondant au joueur.
     */
    void printPlayer() {
        printPixel("\u001b[48;2;255;204;187m");
    }

    /**
     * On affiche le joueur avec des coordonnées précisées.
     * Ceci est utile s'il n'est pas nécessaire de vérifier 
     * si les coordonnées sont valides ou non.
     * @param x La coordonnée en X
     * @param y La coordonnée en Y
     */
    void printPlayer(int x, int y) {
        saveCursorPosition();
        moveCursorTo(x+getXShift(), y+getYShift());
        printPlayer();
        restoreCursorPosition();
    }

    /**
     * Colore un pixel transparent
     */
    void printTransparentPixel() {
        print(ANSI_BG_DEFAULT_COLOR + PIXEL + ANSI_RESET);
    }

    /**
     * Affiche un texte sur la ligne courant en gras.
     */
    void printBoldText(String text) {
        print("\033[1m" + text + ANSI_RESET);
    }

    /**
     * Affiche du texte aligné sur la droite.
     * Pour cela, on affiche des espaces sur le côté gauche jusqu'au côté droit - la taille du texte.
     * @param text Le texte à afficher
     * @param maxWidth La largeur maximale de l'interface (la limite du côté droit)
     */
    void printlnRightAlinedText(String text, int maxWidth) {
        println(repeatChar(" ", maxWidth - length(text)) + text);
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
