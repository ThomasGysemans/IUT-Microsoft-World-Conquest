import extensions.File;
import extensions.CSVFile;

/**
 * Microsoft World Conquest ft. Julien Baste
 * Le jeu RPG dans le Terminal absolument magnifique.
 * Il vous faudra une RTX 4070 et un processeur Ryzen 7950X au minimum pour l'exécuter. 
 * @author Thomas Gysemans, Manon Leclercq, S1-C, 2022
 */
class Main extends Program {
    final String VERSION = "alpha";
    final int GUI_HORIZONTAL_MARGIN = 2;
    final int GUI_VERTICAL_MARGIN = 1;

    // Les valeurs numériques arbitraires associées aux flèches du clavier.
    // Nous avons trouver certaines valeurs en regardant le code source de iJava ;)
    final int TOP_ARROW_KEY = 17;
    final int BOTTOM_ARROW_KEY = 18;
    final int LEFT_ARROW_KEY = 19;
    final int RIGHT_ARROW_KEY = 20;
    final int ENTER_KEY = 13;

    final String WALKING_COMMAND = "Marcher";
    final String INTERACTIVE_COMMAND = "Interaction";
    final String SYSTEM_COMMAND = "Système";

    // Chaque commande peut être personnalisée,
    // donc si on ne peut pas les reconnaître de manière définitive avec la touche associée,
    // alors il nous faut un identifiant unique.
    // Il faut donc bien faire attention à ce que les noms correspondent dans le fichier CSV.
    final String KEY_INTERACT = "INTERACT";
    final String KEY_CONTACT = "CONTACT";
    final String KEY_QUIT = "QUIT";
    final String KEY_WALK_TOP = "TOP";
    final String KEY_WALK_RIGHT = "RIGHT";
    final String KEY_WALK_BOTTOM = "BOTTOM";
    final String KEY_WALK_LEFT = "LEFT";
    // Si on utilisait vraiment Java sans restrictions,
    // on aurait fait les choses autrement, et d'une meilleure façon...
    final int NUMBER_OF_WALKING_COMMANDS = 4;
    final int NUMBER_OF_INTERACTION_COMMANDS = 2;
    final int NUMBER_OF_SYSTEM = 1;

    // Un booléan pour savoir quand l'utilisateur a terminé de se déplacer ou d'intéragir.
    // Une valeur à `true` stoppe la saisie si elle est déjà démarrée, et arrête le programme.
    boolean finishedTyping = false;

    Page page = Page.MENU; // la page actuelle
    int selectedMenu = 1; // par défaut, le joueur va sélectionner le premier élément du menu principal
    int menuPosY = 0;
    final int MENU_ITEMS_NUMBER = 4; // 4 choix possibles dans le menu

    int selectedCommand = 1; // la première commande sélectionnée dans la page des commandes
    int selectedCommandPosY = 0;
    boolean isWaitingForKeyInput = false; // true si on attend que l'utilisateur entre une nouvelle touche pour réassigner une commande 

    final String CREDITS_TITLE_PATH = "./assets/ascii/credits-title.txt";
    final String CREDITS_PATH = "./assets/credits.csv";
    final String GAME_MAIN_TITLE_PATH = "./assets/ascii/main-title.txt";
    final String COMMANDS_TITLE_PATH = "./assets/ascii/commands-title.txt";
    final String MAPS_PATH = "./assets/maps/"; // le fichier contenant toutes les cartes
    final String COLORS_PATH = "./assets/0-colors.csv"; // le fichier contenant toutes les couleurs
    final String TELEPORTATIONS_PATH = "./assets/0-teleportations.csv"; // le fichier contenant toutes les passerelles entre les maps
    final String DIALOGS_PATH = "./assets/0-dialogs.csv"; // le fichier contenant tous les dialogues des couleurs interactives
    final String COMMANDS_PATH = "./assets/0-commands.csv"; // le fichier contenant toutes les commandes par défaut
    final String TVINFO_PATH = "./assets/0-tv.csv"; // le fichier contenant toutes les infos diffusées par la télé de la cellule du joueur
    final String PIXEL = "  "; // En réalité, un pixel est deux espaces dont le fond est coloré avec ANSI
    final int PIXEL_SIZE = length(PIXEL); // On aura besoin de cette constante dans le calcul de mouvement vers la droite/gauche
    String currentMap = "cellule-du-joueur"; // la carte actuellement affichée
    int playerX = 0; // la position en X du joueur, par défaut ça devrait être 18 dans la bibliothèque
    int playerY = 0; // la position en Y du joueur, par défaut ça devrait 9 dans la bibliothèque
    int nearestInteractiveCell = -1; // obtiens la valeur de la couleur si le joueur est proche d'une cellule interactive (redéfinie quand on utilise `hasInteractiveCellNearPlayer` dans `displayPlayer`)

    // On veut stocker les informations au préalable
    // Pour éviter de charger les fichiers, et de les recharger quand nécessaire,
    // on charge tout une seule fois avant le début du jeu.
    // Problème : on ne sait pas en avance le nombre de couleurs ni le nombre de cartes.
    // On initialise le bordel ici, puis on changera ça après.
    // Pour notre plus grand malheur, ArrayList n'est pas autorisée
    Color[] COLORS = new Color[1];
    Dialog[] DIALOGS = new Dialog[1];
    Map[] MAPS = new Map[1]; // lol avant on avait int[][][] MAPS = new int[1][1][1], quel délire !
    Command[] COMMANDS = new Command[1];
    TVInfo[] TV_INFO = new TVInfo[1];
    Credit[] CREDITS = new Credit[1];
    int TV_INDEX; // l'indice de la couleur correspondant à la télé de la cellule, obtenu en lisant `0-tv.csv`
    int lastIndexOfTVInfo = 0; // l'indice de la dernière news affichée

    Dialog[] currentDialogs = null;
    int currentGroupIndex = 0; // l'indice du message du groupe actuel dans `currentDialogs`

    final int DAY_DELAY = 2*60; // nombre de secondes (in-game) pour un jour
    final int HOUR_DELAY = DAY_DELAY/24; // délai pour une heure
    final int deadline = 30; // si `day` atteint cette valeur, alors game over!
    int hour = 17; // l'heure actuelle (in-game)
    int day = 0; // le jour actuel (in-game)
    Thread time; // le thread séparé correspondant au temps
    
    // Pour contrôler le défilement des crédits, on a besoin de variables globales.
    // On utilise des fonctions lambda, et elles requièrent des variables finales ou définies dans un contexte supérieur.
    // On utilise également un Thread pour que l'on puisse quitter à tout moment, même si le défilement n'est pas fini.
    Thread creditsThread;
    int nDisplayedCredits = 0;
    String currentCategory = "";

    boolean game_started = false;

    void algorithm() {
        loadMainMenu();
        /*
        * Sur un thread séparé, on compte l'heure et le jour
        */
        createTime(() -> {
            // Pour des raisons inconnues, time.interrupt() interrompt bien le thread,
            // mais on arrive pas à le remettre en route.
            // Donc on fait en sorte ici que `hour` et `day`
            // ne soient incrémentés que lorsqu'on est sur la page de jeu
            if (page == Page.GAME) {
                hour++;
                if (hour == 24) {
                    hour = 0;
                    day++;
                }
                displayTime();
            }
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
     * Nous créons le temps.
     * Cette fonction crée un thread séparé pour incrémenter le temps de manière asynchrone.
     * @param runnable la fonction à exécuter au bout du délai
     * @param delay Le délai entre chaque exécution de la fonction `runnable`
     */
    void createTime(Runnable runnable, int delay) {
        time = new Thread(() -> {
            try {
                time.sleep(delay);
                runnable.run();
            } catch (Exception e) {
                // ignore, for now at least
            }
        });
        time.start();
    }

    /**
     * Charge le menu principal.
     * Ceci est la page `Page.MENU`.
     */
    void loadMainMenu() {
        page = Page.MENU;
        selectedMenu = 1; // default value

        int width = 50;
        clearMyScreen();
        printEqualsRow(width);
        printEmptyLine();
        int n = printASCII(GAME_MAIN_TITLE_PATH);
        print(repeat(" ", 25) + RGBToANSI(new int[]{252, 191, 203}, false) + "ft. Julien Baste" + ANSI_RESET);
        printEmptyLines(2);
        printEqualsRow(width);
        printEmptyLine();

        if (!game_started) {
            saveCursorPosition(); // on sauvegarde avant le chargement pour pouvoir effacer le texte ensuite
            print(repeat(" ", 20) + "Chargement...");
            printEmptyLines(2);
            initializeColorsAndDialogs();
            initializeAllMaps();
            initializeAllCommands();
            initializeAllTVInfo();
            initializeAllCredits();

            delay(250); // au cas où le chargement est trop rapide, on veut que le joueur le voit

            restoreCursorPosition(); // on va écrire à la place du mot "Chargement..."
        }

        print(repeat(" ", 12));
        printBoldText("Microsoft World Conquest");
        printEmptyLines(2);

        // Le menu principal
        // ---

        // constante qui correspond au nombre de lignes affichées depuis le coin supérieur gauche de l'écran sur l'axe Y
        menuPosY = n + 9;
        println(repeat(" ", 20) + "> Jouer");
        println(repeat(" ", 20) + "  Commandes");
        println(repeat(" ", 20) + "  Crédits");
        println(repeat(" ", 20) + "  Succès");
        printEmptyLines(2);
        println("Appuie sur Entrer pour confirmer.");
        println("Appuie sur '" + getCommandOfUID(KEY_QUIT).getCurrentChar() + "' pour quitter.");
        printEmptyLine();
        printlnRightAlinedText("version " + VERSION, width);

        game_started = true;
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
                            println("HOLD ON!! Fix your code, bro! Are you trying to add a page or something?");
                    }
                    break;
                default:
                    if (a == getKeyForCommandUID(KEY_QUIT)) {
                        finishedTyping = true; // ceci va fermer le programme.
                    }
            }
        } else if (page == Page.GAME) {
            // On ne peut pas utiliser un `switch` ici
            // car les case doivent être des constantes.
            if (a == getKeyForCommandUID(KEY_WALK_TOP)) {
                moveCursorUp();
            } else if (a == getKeyForCommandUID(KEY_WALK_BOTTOM)) {
                moveCursorDown();
            } else if (a == getKeyForCommandUID(KEY_WALK_LEFT)) {
                moveCursorToLeft();
            } else if (a == getKeyForCommandUID(KEY_WALK_RIGHT)) {
                moveCursorToRight();
            } else if (a == getKeyForCommandUID(KEY_INTERACT)) {
                if (currentDialogs != null) {
                    clearDialogAndMessage();
                    currentGroupIndex++;
                    if (currentDialogs[currentGroupIndex] == null) {
                        resetDialogState();
                    } else {
                        writeDialog(currentDialogs[currentGroupIndex]);
                        printEmptyLine();
                        if (currentDialogs[currentGroupIndex+1] == null) {
                            println("[" + getCommandOfUID(KEY_INTERACT).getCurrentChar() + "] pour terminer le dialogue.");
                        } else {
                            println("[" + getCommandOfUID(KEY_INTERACT).getCurrentChar() + "] pour continuer le dialogue.");
                        }
                    }
                } else if (nearestInteractiveCell == TV_INDEX) {
                    for (int i = lastIndexOfTVInfo; i < length(TV_INFO); i++) {
                        if (TV_INFO[i].day == (day+1)) {
                            writeMessage("Vous écoutez la télé qui dit : " + TV_INFO[i].text);
                            lastIndexOfTVInfo = i;
                            break;
                        }
                    }
                    printEmptyLine();
                    println("Il reste " + (deadline - day) + " jour" + (deadline - day >= 2 ? 's' : "") + " avant la fin!");
                } else {
                    for (int i = 0; i < length(DIALOGS); i++) {
                        if (DIALOGS[i].colorIndex == nearestInteractiveCell) {
                            // Certains dialogues ne peuvent se produire que dans une seule map précise.
                            // Cependant, certains dialogues peuvent débuter dans n'importe quelle map (donc valeur à `null`).
                            if (DIALOGS[i].map != null && !DIALOGS[i].map.equals(currentMap)) {
                                continue;
                            }
                            // Puisqu'askip on peut pas utiliser ArrayList,
                            // on se fiche un peu des perf donc on donne une limite au pif
                            // et on se débrouille après
                            Dialog[] d = new Dialog[20]; // les messages du dialogue (chaque dialogue a un groupe unique, un entier, de messages)
                            int trueLength = 0;
                            boolean found = false;
                            for (int y = 0, e = 0; y < length(DIALOGS); y++) {
                                if (DIALOGS[y].group == DIALOGS[i].group) {
                                    d[e++] = DIALOGS[y];
                                    found = true;
                                } else {
                                    // Si on a déjà trouvé le groupe,
                                    // sachant que tous les messages d'un même groupe se suivent,
                                    // alors dès qu'on est dans un autre groupe, inutile de continuer la boucle.
                                    if (found) {
                                        break;
                                    }
                                }
                            }
                            for (int j = 0; j < length(d); j++) {
                                if (d[j] == null) {
                                    trueLength = j;
                                    break;
                                }
                            }
                            writeDialog(d[0]);
                            printEmptyLine();
                            if (trueLength > 1) {
                                currentDialogs = d;
                                println("[" + getCommandOfUID(KEY_INTERACT).getCurrentChar() + "] pour continuer le dialogue en court.");
                            }
                            break;
                        }
                    }
                }
            } else if (a == getKeyForCommandUID(KEY_QUIT)) {
                loadMainMenu();
            }
        } else if (page == Page.COMMANDS) {
            if (isWaitingForKeyInput) {
                // Si les touches ne changent pas d'ordre dans le fichier,
                // et il n'y a aucune raison que cela se produise,
                // alors selectedCommand - 1 correspond à l'indice de la commande sélectionnée dans COMMANDS
                int commandIndex = selectedCommand - 1;
                // On veut maintenant réassigner la touche et actualiser l'interface
                Command command = COMMANDS[commandIndex];
                command.key = a;
                saveCursorPosition();
                moveCursorTo(0, selectedCommandPosY);
                clearLine();
                println("[*] " + command.name + " (" + command.getOriginalChar() + ") : " + command.getCurrentChar());
                restoreCursorPosition();
                isWaitingForKeyInput = false;
            } else {
                switch (a) {
                    case TOP_ARROW_KEY:
                        isWaitingForKeyInput = false;
                        selectCommandItem(-1);
                        break;
                    case BOTTOM_ARROW_KEY:
                        isWaitingForKeyInput = false;
                        selectCommandItem(1);
                        break;
                    case ENTER_KEY:
                        isWaitingForKeyInput = !isWaitingForKeyInput;
                        break;
                    default:
                        if (a == getKeyForCommandUID(KEY_QUIT)) {
                            loadMainMenu();
                        }
                }
            }
        } else {
            if (a == getKeyForCommandUID(KEY_QUIT)) {
                loadMainMenu();
            }
        }
    }

    void loadEmptyPage() {
        clearMyScreen();
        println("Wooooow! Même si on a pas de vie, on a pas encore eu le temps de coder ça, sorry!");
        println("Appuie sur '" + getCommandOfUID(KEY_QUIT).getCurrentChar() + "' pour revenir au menu.");
    }

    void creditsPage() {
        page = Page.CREDITS;
        clearMyScreen();

        int width = 52;
        printEqualsRow(width);
        printEmptyLine();
        printASCII(CREDITS_TITLE_PATH);
        printEmptyLine();
        printEqualsRow(width);
        printEmptyLine();

        creditsThread = new Thread(() -> {
            try {
                creditsThread.sleep(200);
                if (page != Page.CREDITS) {
                    creditsThread.interrupt();
                    creditsThread = null;
                    nDisplayedCredits = 0;
                    currentCategory = "";
                    return;
                }
                // Techniquement, on devrait dans un premier temps réarranger la liste
                // de manière à trier les éléments en fonction de leur catégorie,
                // afin d'être sûr que pour une certaine catégorie tous les éléments se suivent.
                // Dans ce cas-ci, avec les restrictions imposées, on veut pas se mettre une balle :)
                // donc on fait juste gaffe à bien arranger le fichier directement sans vérification
                for (int i = nDisplayedCredits; i < nDisplayedCredits + 1 && i < length(CREDITS); i++) {
                    if (!currentCategory.equals(CREDITS[i].category)) {
                        currentCategory = CREDITS[i].category;
                        printEmptyLine();
                        printlnCenterAlignedText(makeBoldText(currentCategory), width);
                    }
                    printlnCenterAlignedText(CREDITS[i].name, width);
                }
                nDisplayedCredits++;
                if (nDisplayedCredits < length(CREDITS)) {
                    creditsThread.run();
                }
            }
            catch (Exception e){
                System.err.println(e);
            }
        });

        creditsThread.start();
    }

    void shortcutsPage() {
        page = Page.COMMANDS;
        selectedCommand = 1;

        clearMyScreen();
        printEqualsRow(81);
        printEmptyLine();
        int title_height = printASCII(COMMANDS_TITLE_PATH);
        printEmptyLine();
        printEqualsRow(81);
        printEmptyLine();
        println("Changer les commandes : ");
        printEmptyLine();
        println("Commande (valeur par défaut) : valeur actuelle");
        printEmptyLine();
        println("    > " + WALKING_COMMAND);
        selectedCommandPosY = title_height + 11;
        for (int i = 0; i < length(COMMANDS); i++) {
            if (COMMANDS[i].category == CommandCategory.WALK) {
                println("[" + (i == 0 ? '*' : ' ') + "] " + COMMANDS[i].name + " (" + COMMANDS[i].getOriginalChar() + ") : " + COMMANDS[i].getCurrentChar());
            }
        }
        printEmptyLine();
        println("    > " + INTERACTIVE_COMMAND);
        for (int i = 0; i < length(COMMANDS); i++) {
            if (COMMANDS[i].category == CommandCategory.INTERACT) {
                println("[ ] " + COMMANDS[i].name + " (" + COMMANDS[i].getOriginalChar() + ") : " + COMMANDS[i].getCurrentChar());
            }
        }
        printEmptyLine();
        println("    > " + SYSTEM_COMMAND);
        for (int i = 0; i < length(COMMANDS); i++) {
            if (COMMANDS[i].category == CommandCategory.SYSTEM) {
                println("[ ] " + COMMANDS[i].name + " (" + COMMANDS[i].getOriginalChar() + ") : " + COMMANDS[i].getCurrentChar());
            }
        }
        printEmptyLines(2);
        println("Sélectionner la commande à configurer avec les flèches directionnelles du clavier");
        println("et modifier la touche en appuyant d'abord sur Entrer, et ensuite sur la touche de remplacement désirée.");
        println("Pour quitter cette page, utilisez la touche configurée du sytème (par défaut 'q').");
    }

    void selectCommandItem(int movement) {
        int futureChoice = selectedCommand + movement;
        int numberOfCommands = length(COMMANDS);
        if (futureChoice < 1 || futureChoice > numberOfCommands) {
            return;
        }
        saveCursorPosition();
        moveCursorTo(2, selectedCommandPosY);
        print(" ");
        print("\033[1D");
        if (movement == 1) {
            if (selectedCommand == NUMBER_OF_WALKING_COMMANDS || selectedCommand == NUMBER_OF_WALKING_COMMANDS + NUMBER_OF_INTERACTION_COMMANDS) {
                print(repeat("\033[1B", 3));
                selectedCommandPosY += 3;
            } else {
                selectedCommandPosY += movement;
                print("\033[1B");
            }
            print("*");
        } else {
            if (selectedCommand == NUMBER_OF_WALKING_COMMANDS + 1 || selectedCommand == NUMBER_OF_WALKING_COMMANDS + NUMBER_OF_INTERACTION_COMMANDS + NUMBER_OF_SYSTEM) {
                print(repeat("\033[1A", 3));
                selectedCommandPosY -= 3;
            } else {
                selectedCommandPosY += movement;
                print("\033[1A");
            }
            print('*');
        }
        restoreCursorPosition();
        selectedCommand = futureChoice;
    }

    void achievementsPage() {
        page = Page.ACHIEVEMENTS;
        loadEmptyPage();
        // todo.
    }

    /**
     * Initialise le jeu (Page.GAME).
     * Affiche la map actuelle, ainsi que le joueur.
     */
    void playGame() {
        page = Page.GAME;

        clearMyScreen();
        Map map = getMapOfName(currentMap);
        displayMap(map);
        printPlayer(playerX,playerY);

        displayTime();
    }

    /**
     * Affiche le temps dans l'interface graphique à une position précise.
     */
    void displayTime() {
        if (page != Page.GAME) {
            return;
        }
        Map map = getMapOfName(currentMap);
        int height = getGUIHeight(map);
        saveCursorPosition();
        moveCursorTo(0,height-1); // entre la map et la ligne de "=" du dessous
        // la largeur de la ligne change souvent (si hour < 10 par exemple)
        // donc on risquerait de pas supprimer le texte précédent,
        // alors on ajoute quelques d'espaces pour être sûr qu'on efface tout sur cette ligne.
        println(hour + "h, jour " + (day + 1) + "    ");
        restoreCursorPosition();
    }

    /**
     * On veut positionner le joueur à une position précise (x;y).
     * Avec (0;0) le coin supérieur gauche.
     * Dans un premier temps, on doit vérifier s'il s'agit d'un mouvement autorisé,
     * S'il s'agit d'une téléportation, alors on la réalise directement ici.
     * 
     * Pour bouger le joueur, on sauvegarde la position actuelle du curseur,
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
        int currentColorIndex = grid[playerY][playerX];
        Color currentColor = COLORS[currentColorIndex];
        // S'il s'agit d'une passerelle vers une autre carte,
        // on vérifie si le mouvement appliqué permet d'y accéder.
        if (currentColor.t) {
            int shiftX = x - playerX; // le déplacement qui a été réalisé sur l'axe X
            int shiftY = y - playerY; // le déplacement qui a été réalisé sur l'axe Y
            if (shiftX == currentColor.movX && shiftY == currentColor.movY) {
                teleportPlayerToNewMap(currentColor.toMap, currentColor.toX, currentColor.toY);
                return;
            }
        }
        
        // Il s'agit d'un déplacement normal quelque part sur la carte,
        // il nous faut vérifier si ce mouvement ne fait pas sortir le joueur en-dehors de celle-ci.
        int maxX = length(grid[0]);
        int maxY = length(grid);
        if (x >= maxX || y >= maxY || x < 0 || y < 0) {
            return;
        }

        // Il peut y avoir des murs, qui correspondent à des pixels transparents limitrophes.
        int colorIndexOfTarget = grid[y][x];
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
        int previousPos = map.grid[playerY][playerX];
        Color color = previousPos == -1 ? null : COLORS[previousPos];
        int xShift = getXShift();
        int yShift = getYShift();
        moveCursorTo(playerX*PIXEL_SIZE + xShift, playerY + yShift);
        if (previousPos == -1) {
            printTransparentPixel();
        } else {
            printPixel(color);
        }

        // Deuxième étape : écrire le pixel du joueur
        moveCursorTo(x*PIXEL_SIZE + xShift, y + yShift);
        printPlayer();

        restoreCursorPosition();
        // On définit les coordonnées données
        // comme étant la nouvelle position du joueur.
        playerX = x;
        playerY = y;

        saveCursorPosition();
        int previousValue = nearestInteractiveCell;
        if ((nearestInteractiveCell = hasInteractiveCellNearPlayer()) != previousValue) {
            displayCommandsPanel();
        }
        restoreCursorPosition();

        clearDialogAndMessage();
        resetDialogState();
    }

    /**
     * Vérifie si dans un carré autour du joueur il existe une case interactive.
     * S'il y en a une, on renvoie l'indice de la couleur du pixel, afin de l'identifier
     * et savoir quoi faire lorsque le joueur appuie sur la touche d'interactivité (par défaut, 'f').
     * Une case interactive peut être :
     * - une couleur possédant un dialogue
     * - la télé
     * @return La couleur de la cellule interactive la plus proche s'il y a une case interactive proche du joueur, `-1` sinon.
     */
    int hasInteractiveCellNearPlayer() {
        int[][] grid = getMapOfName(currentMap).grid;
        int height = length(grid);
        int width = length(grid[0]);
        int cell = -1;
        if (playerY-1 >= 0 && isCellInteractive(cell = grid[playerY-1][playerX])) return cell; // top
        if (playerX+1 < width && isCellInteractive(cell = grid[playerY][playerX+1])) return cell; // right
        if (playerY+1 < height && isCellInteractive(cell = grid[playerY+1][playerX])) return cell; // bottom
        if (playerX-1 >= 0 && isCellInteractive(cell = grid[playerY][playerX-1])) return cell; // left
        return -1;
    }

    /**
     * Retourne `true` si la cellule donnée est interactive.
     * @param cell Le numéro de la couleur de cette cellule.
     * @return Un booléan indiquant si cette couleur est interactive ou non.
     */
    boolean isCellInteractive(int cell) {
        return cell != -1 && (COLORS[cell].i || cell == TV_INDEX);
    }

    /**
     * Retourne la largeur entre le côté gauche (x=0) et le début de la map actuellement affichée.
     * Le but est de positionner le joueur correctement dans la map malgré le GUI.
     * Note: ceci tient compte de `PIXEL_SIZE`.
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
        return GUI_VERTICAL_MARGIN + 3; // +3 pour lignes vides, equals row et le nom de la map
    }

    /**
     * Retourne la hauteur totale de l'interface graphique
     * @param map La map actuelle dont nous avons besoin de connaître la hauteur.
     * @return Un entier correspondant à la hauteur totale de l'interface graphique.
     */
    int getGUIHeight(Map map) {
        return (GUI_VERTICAL_MARGIN * 2) + length(map.grid) + 4; // +2 pour les lignes de "=" et +1 car on y affiche aussi le temps +1 pour le nom de la map
    }

    /**
     * Retourne la largeur totale de l'interface graphique.
     * @param map La map actuelle dont nous avons besoin de connaître la largeur.
     * @return Un entier correspondant à la largeur totale de l'interface graphique.
     */
    int getGUIWidth(Map map) {
        return length(map.grid[0])*PIXEL_SIZE+(GUI_HORIZONTAL_MARGIN*2);
    }

    /**
     * Retourne la hauteur du GUI + la hauteur du panneau de commandes.
     */
    int getTotalHeight() {
        return getGUIHeight(getMapOfName(currentMap)) + getNumberOfCommandsForCategory(CommandCategory.INTERACT);
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
     * On veut effacer le message de la télé
     * donc on place le curseur après la map et le panneau de commandes,
     * et on clear les 5 lignes suivantes.
     */
    void clearDialogAndMessage() {
        int totalHeight = getTotalHeight() + 1;
        for (int i = 0; i < 5; i++) {
            moveCursorTo(0,totalHeight+i);
            clearLine();
        }
        moveCursorTo(0,totalHeight);
        saveCursorPosition();
    }

    /**
     * Si le joueur quitte le dialogue, on veut que ce soit reset, aussi quand c'est fini.
     */
    void resetDialogState() {
        currentDialogs = null;
        currentGroupIndex = 0;
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
        displayPlayer(playerX-1, playerY);
    }

    void moveCursorToRight() {
        displayPlayer(playerX+1, playerY);
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
        playerX = targetX;
        playerY = targetY;
        printPlayer(playerX,playerY);
        displayTime();
    }

    /**
     * Converti une couleur RGB au format ANSI.
     * La standardisation ANSI nous permet de colorer du texte, ou de le surligner.
     * @param rgb Une liste de 3 nombres entre 0 et 255 inclus, selon le format RGB.
     * @param backgroundColor Vrai si l'on veut que la couleur soit sur le fond du texte. Le texte aurait donc la couleur par défaut.
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
     * - être interactive (un PNJ par exemple, ce qui n'est pas encore implémenté)
     * - être un chemin vers une autre map (exemple: une porte menant sur une autre salle), ce qu'on appelle la "téléportation"
     * Pour avoir toutes les données nécessaires, on lit plusieurs fichiers :
     * - La charte de couleurs (`./assets/0-colors.csv`)
     * - Les téléportations possibles (`./assets/0-teleportations.csv`)
     * - Les dialogyes (`./assets/0-dialogs.csv`)
     * Cette fonction ne sera appelée qu'une seule fois lors de l'initialisation du jeu.
     */
    void initializeColorsAndDialogs() {
        CSVFile colors = loadCSV(COLORS_PATH);
        CSVFile teleportations = loadCSV(TELEPORTATIONS_PATH);
        CSVFile dialogs = loadCSV(DIALOGS_PATH);
        int nColors = rowCount(colors); // nombre de couleurs
        int nTeleportations = rowCount(teleportations); // nombre de pixels menant à une téléportation
        int nDialogs = rowCount(dialogs);
        int x,i,r,g,b,color; // toutes les données sous forme d'entier qu'on peut extraire des fichiers

        // Nous sommes obligés de redefinir COLORS
        // pour que nous ayons une liste de taille prédéfinie
        // car askip on peut pas utiliser ArrayList!
        COLORS = new Color[nColors-1];
        DIALOGS = new Dialog[nDialogs-1];

        for (int y=1;y<nColors;y++) {
            x = stringToInt(getCell(colors, y, 1));
            r = stringToInt(getCell(colors, y, 2));
            g = stringToInt(getCell(colors, y, 3));
            b = stringToInt(getCell(colors, y, 4));
            COLORS[y-1] = newColor(r,g,b,x==1);
        }

        for (int y=1;y<nTeleportations;y++) {
            color = stringToInt(getCell(teleportations, y, 0));
            COLORS[color].t = true;
            COLORS[color].toMap = getCell(teleportations, y, 1);
            COLORS[color].movX = stringToInt(getCell(teleportations, y, 2));
            COLORS[color].movY = stringToInt(getCell(teleportations, y, 3));
            COLORS[color].toX = stringToInt(getCell(teleportations, y, 4));
            COLORS[color].toY = stringToInt(getCell(teleportations, y, 5));
        }

        for (int y=1;y<nDialogs;y++) {
            color = stringToInt(getCell(dialogs, y, 1));
            COLORS[color].i = true; // for easier detections near the player
            Dialog dialog = new Dialog();
            dialog.group = stringToInt(getCell(dialogs, y, 0));
            dialog.colorIndex = color;
            dialog.setMap(getCell(dialogs, y, 2).replaceAll("^\"(.*)\"$", "$1")); // removing the quotes
            dialog.narratorName = getCell(dialogs, y, 3);
            dialog.text = getCell(dialogs, y, 4).replaceAll("\\$", ","); // replacing the "$" by "," because iJava doesn't read CSV correctly
            DIALOGS[y-1] = dialog;
        }
    }

    /**
     * Crée une instance de Color avec les metadonnées de base.
     * Les autres métadonnées sont ajoutés ensuite (elles ont des valeurs par défaut).
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
     * @return Une instance de Map
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
        
        COMMANDS = new Command[nCommands-1];

        for (int i=1;i<nCommands;i++) {
            Command command = new Command();
            command.name = getCell(file, i, 0);
            command.category = getCategoryBasedOnName(getCell(file, i, 1));
            command.key = stringToInt(getCell(file, i, 2));
            command.originalKey = stringToInt(getCell(file, i, 2));
            command.uid = getCell(file, i, 3);
            COMMANDS[i-1] = command;
        }
    }

    /**
     * Retourne une instance de la commande demandée.
     * @param uid L'identifiant unique attachée à cette commande.
     * @return L'instance de Command.
     */
    Command getCommandOfUID(String uid) {
        for (int i = 0; i < length(COMMANDS); i++) {
            if (COMMANDS[i].uid.equals(uid)) {
                return COMMANDS[i];
            }
        }
        return null;
    }

    /**
     * Retourne le numéro de la touche attachée à la commande désirée.
     * @param uid L'identifiant unique attachée à cette commande.
     * @return L'entier qui correspond au code numérique de la touche.
     */
    int getKeyForCommandUID(String uid) {
        Command c = getCommandOfUID(uid);
        return c == null ? -1 : c.key;
    }

    int getNumberOfCommandsForCategory(CommandCategory category) {
        int n = 0;
        for (int i = 0; i < length(COMMANDS); i++) {
            if (COMMANDS[i].category == category) {
                n++;
            }
        }
        return n;
    }

    /**
     * Retourne un enum de la catégorie de la commande basée sur le nom de la catégorie telle écrite dans le CSV.
     * @param name Le nom de la catégorie d'une commande.
     * @return La valeur de l'enum correspondant.
     */
    CommandCategory getCategoryBasedOnName(String name) {
        switch (name) {
            case WALKING_COMMAND: return CommandCategory.WALK;
            case INTERACTIVE_COMMAND: return CommandCategory.INTERACT;
            case SYSTEM_COMMAND: return CommandCategory.SYSTEM;
        }
        return null;
    }

    /**
     * Initialise toutes les infos de la télé (dans la cellule du joueur)
     */
    void initializeAllTVInfo() {
        CSVFile file = loadCSV(TVINFO_PATH);
        int nInfo = rowCount(file);

        TV_INFO = new TVInfo[nInfo-1];
        TV_INDEX = stringToInt(getCell(file, 1, 1)); // on veut l'info juste une fois

        for (int i=1;i<nInfo;i++) {
            TVInfo info = new TVInfo();
            info.day = stringToInt(getCell(file, i, 0));
            info.text = getCell(file, i, 2);
            TV_INFO[i-1] = info;
        }
    }

    /**
     * Initialise tous les crédits.
     * Le but est de pouvoir arranger l'affichage ordonné par catégorie.
     */
    void initializeAllCredits() {
        CSVFile file = loadCSV(CREDITS_PATH);
        int nCredits = rowCount(file);

        CREDITS = new Credit[nCredits-1];
        
        for (int y=1;y<nCredits;y++) {
            Credit credit = new Credit();
            credit.name = getCell(file, y, 0);
            credit.category = getCell(file, y, 1);
            CREDITS[y-1] = credit;
        }
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
     * On notera qu'un pixel = 2 caractères (deux espaces blancs) car ensemble ils forment un carré.
     * Un caractère, par défaut, forme un rectangle.
     */
    void displayMap() {
        Map map = getMapOfName(currentMap);
        displayMap(map);
    }

    /**
     * Ici, la fonction qui permet d'afficher n'importe quelle map au milieu de son interface.
     * Ceci comprend donc les lignes de "=" et le nom de la map, en plus des espaces vides pour aérer l'interface.
     * @param Map La carte à afficher
     */
    void displayMap(Map map) {
        int[][] grid = map.grid;
        int mapHeight = length(grid);
        int mapWidth = length(grid[0]);
        int equalsRowLength = getGUIWidth(map);
        printEqualsRow(equalsRowLength);
        printEmptyLines(GUI_VERTICAL_MARGIN);
        println("(" + map.name + ")");
        for (int lig=0;lig<mapHeight;lig++) {
            print(repeat(" ", GUI_HORIZONTAL_MARGIN));
            for (int col=0;col<mapWidth;col++) {
                int n = grid[lig][col];
                if (n == -1) {
                    printTransparentPixel();
                } else {
                    printPixel(COLORS[n]);
                }
            }
            println(""); // très important
        }
        printEmptyLines(GUI_VERTICAL_MARGIN + 1); // +1 pour la ligne réservée à l'heure
        printEqualsRow(equalsRowLength);
        displayCommandsPanel();
    }

    /**
     * Affiche le panneau avec toutes les touches que l'utilisateur peut utiliser.
     * Ceci est vérifié à chaque déplacement.
     * Si l'on détecte une nouvelle touche,
     * alors on recrée l'intégralité du panneau en appelant cette fonction.
     */
    void displayCommandsPanel() {
        Map map = getMapOfName(currentMap);
        int width = getGUIWidth(map);
        int height = getGUIHeight(map);
        moveCursorTo(0,height+1);
        for (int i = 0; i < length(COMMANDS); i++) {
            if (COMMANDS[i].category == CommandCategory.INTERACT) {
                if (COMMANDS[i].uid.equals(KEY_INTERACT) && nearestInteractiveCell == -1) {
                    clearLine();
                    print("\r\n"); // just to make sure that the cursor goes back to the right place afterwards
                    continue;
                }
                println("   [" + COMMANDS[i].getCurrentChar() + "] " + COMMANDS[i].name + "   ");
            }
        }
    }

    /**
     * Renvoie une chaine dans laquelle une autre chaine a été répétée autant de fois que précisé.
     * @param c La chaine à répéter.
     * @param times Le nombre de fois que la chaîne doit être répétée.
     * @return Une nouvelle chaine contenant le caractère `c` répété `times` fois.
     */
    String repeat(String c, int times) {
        String str = "";
        for (int i = 0; i < times; i++) {
            str += c;
        }
        return str;
    }

    /**
     * Affiche une ligne dans laquelle il y a un certain nombre de fois le signe "=".
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
     * Saute une seule ligne pour créer un léger effet d'espacement entre du texte.
     */
    void printEmptyLine() {
        println("");
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
        moveCursorTo(x*PIXEL_SIZE+getXShift(), y+getYShift());
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
        print(makeBoldText(text));
    }

    /**
     * Retourne le texte en gras à afficher.
     */
    String makeBoldText(String text) {
        return ANSI_BOLD + text + ANSI_RESET;
    }

    /**
     * Affiche un texte au milieu de l'écran, selon la largeur totale donnée.
     * Pour calculer la bonne position en fonction de la largeur du texte,
     * il suffit de répéter des " " x fois avant le texte, tel que x = (largeur totale / 2) - (longueur du texte / 2),
     * où la longueur du texte est calculée de manière à retirer les codes ANSI potentiels au début et fin de chaine.
     * @param text Le texte à afficher
     * @param totalWidth La largeur totale de la fenêtre
     */
    void printlnCenterAlignedText(String text, int totalWidth) {
        println(repeat(" ", totalWidth/2-length(text.replaceAll("\\033\\[.*m(.+)\\033\\[.*m", "$1"))/2) + text);
    }

    /**
     * Affiche du texte aligné sur la droite.
     * Pour cela, on affiche des espaces sur le côté gauche jusqu'au côté droit - la taille du texte.
     * @param text Le texte à afficher
     * @param maxWidth La largeur maximale de l'interface (la limite du côté droit)
     */
    void printlnRightAlinedText(String text, int maxWidth) {
        println(repeat(" ", maxWidth - length(text)) + text);
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

    /**
     * Affiche un message venant de la télé.
     * @param message Le message textuel à afficher sur une ligne.
     */
    void writeMessage(String message) {
        Map map = getMapOfName(currentMap);
        int height = getGUIHeight(map) + getNumberOfCommandsForCategory(CommandCategory.INTERACT);
        moveCursorTo(0,height+2);
        clearLine();
        println(message);
    }

    /**
     * Affiche le message d'un dialogue.
     */
    void writeDialog(Dialog dialog) {
        writeMessage(dialog.narratorName + " - " + dialog.text);
    }

    /*
     *
     * C'est le moment de parler d'une dinguerie...
     * En bref, la façon dont fonctionne `enableKeyTypedInConsole` c'est de changer
     * le mode d'entrée des commandes du terminal via `stty raw`.
     * Le problème n'est pas très clair, mais en gros, quand on fait un clearMyScreen(),
     * tout en étant en mode `raw`, les "carriage return" sont oubliés (\r) lors de l'écriture d'un '\n', et par conséquent,
     * on obtient le "staircase effect" comme décrit ci-dessous.
     * Nous avons donc manuellement ajouté le '\r' aux méthodes `println`.
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
}
