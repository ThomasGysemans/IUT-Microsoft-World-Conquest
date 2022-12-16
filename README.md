# Microsoft World Conquest Game

Un RPG unique en son genre qui se joue dans le terminal ! Il s'agit d'un projet d'école, intitulé "Saé 1.2" codé avec un langage de programmation customisé : `iJava`.

repo: [GitHub](https://github.com/CodoPixel/IUT-Microsoft-World-Conquest)

> **IMPORTANT** : ceci est la documentation pour la version `alpha`. Elle changera beaucoup une fois toutes les fonctionnalités implémentées.

## Equipe

- Thomas GYSEMANS ([CodoPixel](https://github.com/CodoPixel/))
- Manon LECLERCQ ([tuturmann](https://github.com/tuturmann))

## Description du jeu

Le jeu débute sur une carte nommée "bibliotheque". Vous incarnez un pixel dans un monde dystopique en 2D où le monopole de Microsoft s'apparente à une conquête mondiale. Il s'agit de l'ennemi et votre but sera de collecter des informations avec l'aide de votre professeur préféré rencontré dans la bibliothèque (J.B). Cependant, Microsoft essaie d'éliminer les gens qui ne croient pas en eux, alors vous serez kidnappé(e) et enfermé(e) dans une sorte de prison censée vous laver le cerveau.

La quête principale est de sortir de la prison en emmenant JB avec vous, et au passage les autres membres de l'organisation rebelle. Collectez des informations auprès des autres prisonniers, trouvez des secrets cachés dans les cartes du jeu et apprenez des choses durant les cours de lavage de cerveau. Pour réussir, vous devrez regrouper des codes secrets et hacker discrètement le système informatique du centre via des commandes BASH (des commandes qui vous permettent de contrôler un ordinateur) que JB vous expliquera à la perfection.

_Le jeu est rempli d'easter eggs (de surprises qui ne sont pas en rapport avec l'histoire) et c'est à vous de les trouver !_

## Comment débuter le jeu

Vous débutez dans le menu. La plupart des pages ne sont pas encore disponibles, mais `Jouer` peut être sélectionné. À tout moment, appuyez sur `q` pour quitter.

Vous incarnez un pixel rose (`#ffccbb`), déplacez-vous à l'aide des flèches directionnelles du clavier.

Dans cette version alpha, vous débutez dans la première salle du jeu : la bibliothèque, où vous rencontrez JB. Pour le moment, les dialogues ne sont pas disponibles, mais vous pouvez d'ores et déjà vous déplacer librement ainsi que sortir de la bibliothèque pour admirer la scène suivante. L'accès se trouve tout en bas du tapis rouge, une fois sur la case devant la porte de la bibliothèque, il suffit de vous déplacer vers le bas pour que le joueur se téléporte dans la future carte. Il s'agit très simplement d'une porte.

L'heure et le jour de votre partie sont affichés en-dessous de la carte active. Pour le moment, ceci n'a aucun effet et ça déroulera indéfiniment. Le temps ne défile pas lorsque que vous êtes dans le menu.

## Risques d'incompatibilité

> **ATTENTION** : certains terminaux ne sont pas compatibles avec ANSI.

Nous vous conseillons fortement d'utiliser le Terminal de **[Visual Studio Code](https://code.visualstudio.com/)** (du moins pour la version alpha). Il est important de noter que Visual Studio Code n'est pas identique à Codium, car ce dernier affiche une représentation ASCII des caractères entrés dans le terminal courant, alors qu'il ne devrait pas.

Le terminal de IntelliJ IDEA n'est pas idéal, et dans le terminal par défaut d'un Mac les couleurs ne s'affichent même pas.

Enfin, notez que si votre terminal est trop petit pour afficher correctement le jeu et toutes ses cartes, des problèmes inattendus risquent d'émerger. Il est **fortement conseillé** de **réduire la taille de la police d'écriture** du Terminal si possible.

## Explications techniques

Le jeu est très complexe et possède de nombreuses fonctionnalités différentes expliquées et détaillées d'une part ici et d'autre part dans les commentaires du code source lui-même.

### Organisation du code

Le code principal se trouve dans `Main.java`. Nous utilisons les types suivants :

- `Color` : une couleur possible des cartes
- `Map` : une carte (et non le type natif de la plupart des langages ;))
- `Page` : une page parmi tant d'autres (car il y a un menu, une page de crédits, une page pour jouer etc.)

Présent dans le code mais pas encore accessibles dans la version alpha :

- `TVInfo` : la liste des informations diffusées par la télévision de la cellule du joueur (incomplet).
- `Command` : une commande, un raccourci (incomplet).

### Fonctionnement des cartes

Nous affichons dans le Terminal des cartes colorées. Pour réaliser cela, nous utilisons les normes [ANSI](https://www.ansi.org/) qui permettent, entre autres, de contrôler le curseur du Terminal et de colorer du texte.

Nous considérons **deux espaces** comme étant **un pixel**, d'où les constantes précisées ci-dessous. Avoir deux espaces comme étant un pixel nous permet d'avoir une forme carrée pour ceux-ci (un seul espace blanc est un rectangle, deux espaces blancs forment un carré - du moins à l'oeil nu).

```java
final String PIXEL = "  "; 
final int PIXEL_SIZE = length(PIXEL);
```

Le pixel est affiché et le fond est coloré. Nous avons accès à 16 millions de couleurs différentes, dans un premier temps au format RGB et dans un deuxième temps converties au format ANSI lors de l'initialisation des données.

* Comment convertir du RGB en ANSI ? Voici le format :

```java
"\u001b[<p>;2;<r>;<g>;<b>m"
```

où 

- `<p>` est soit 38 (pour colorer le texte), soit 48 (pour colorer le fond).
- `<r>` la composante rouge du format RGB
- `<g>` la composante verte du format RGB
- `<b>` la composante bleue du format RGB

Il suffit d'écrire ceci dans le terminal et tout ce qui suit se colore tel que précisé. Si l'on veut colorer une seule partie d'un texte par exemple, on termine la sélection avec `\u001b[0m` ce qui redonne à la suite le format par défaut.

> **NOTE**: il est possible d'afficher une couleur transparente (la même que celle du terminal) avec `\u001b[49m`

À l'aide d'une matrice où chaque élément est la couleur d'un pixel, nous sommes capables d'afficher tout ce dont nous avons besoin.

> **NOTE** : Nous utilisons `-1` pour une couleur transparente.

### Initialisation

En premier lieu, comment le jeu s'initialise-t-il ? Dans le dossier [assets](./assets) se trouvent de nombreux fichiers au format `CSV` ou `txt`.

#### Valeurs par défaut

Les fichiers CSV précédés d'un `0-` sont soit des constantes utilisées pour charger le jeu correctement, soit des valeurs par défaut que le joueur pourra customiser plus tard. Nous avons :

- `0-colors.csv`

Ce fichier est appelé la **charte de couleurs**. Celui-ci répertorie l'ensemble des couleurs utilisées dans le jeu. Elles ont toutes un numéro unique et une couleur RGB associée.

Ce fichier est complexe et la façon dont nous exploitons les couleurs est très particulière. Puisque chaque pixel représente quelque chose de spécifique, nous devons attribuer des métadonnées à celles-ci. En effet, une couleur peut représenter une table, de l'eau, un mur etc. Et puisque le joueur se déplace dans la carte, il ne doit pas pouvoir marcher sur ces éléments ! Et oui, le joueur ne peut pas marcher sur l'eau !

Les couleurs peuvent avoir plusieurs métadonnées :

- `x` : un booléan qui précise si le joueur peut marcher (_franchir_) le pixel ou non.
- `t` : un booléan qui précise si ce pixel est une passerelle vers une autre carte. En effet, la vue du joueur se porte sur la salle dans laquelle il se trouve et se déplace. Il est important qu'il puisse explorer l'intégralité des scènes mises à disposition. Par conséquent, certains pixels, si l'utilisateur applique le mouvement correct (vers le haut, vers la droite, vers le bas ou vers la gauche), permettent la "téléportation" vers une autre carte. Tout est précisé dans `0-teleportations.csv`. Avec ceci nous précisons les données suivantes :
  - `toMap` : la map cible
  - `movX` : le déplacement sur l'axe X (-1 ou 1)
  - `movY` : le déplacement sur l'axe Y (-1 ou 1)
  - `targetX` : la position cible sur l'axe X dans la map cible
  - `targetY` : la position cible sur l'axe Y dans la map cible
- `0-commands.csv` : ce fichier contiendra toutes les commandes par défaut du jeu. Dans la version alpha, ce fichier n'est pas complet et n'est pas utilisé non plus.
- `0-tv.csv` : ce fichier contient toutes les informations diffusées par la télévision présente dans la cellule du joueur. Ce fichier n'est pas complet non plus.

#### Les cartes

Chaque carte est une matrice où chaque élément est le numéro unique d'une couleur définie dans la charte. En apparence, ces fichiers semblent illisibiles et difficiles à comprendre. Bien évidemment, nous n'avons pas rédigé ça à la main, Thomas a conçu un site permettant de dessiner les cartes puis ensuite de générer la charte (`0-colors.csv`), les cartes (`./assets/maps/*`) et les téléportations (`0-teleportations.csv`) : [2dmapcreator](https://2dmapcreator.sciencesky.fr/) avec système de connexion à un compte client pour travailler sur plusieurs machines (via Firebase).

Les cartes, ainsi que les autres fichiers CSV, sont initialisés une seule et unique fois au lancement du programme.

#### Les autres éléments graphiques

Nous utilisons également des formes en ASCII qui permettent d'illustrer des éléments. Pour le moment, nous n'utilisons qu'un seul fichier `.txt` utilisé pour le titre du jeu affiché dans le menu.

Tous ces fichiers seront dans le dossier `./assets/ascii`.

#### Le temps

Afin d'avoir un système de jour et de nuit (utile pour la suite du jeu), ainsi qu'un calendrier, nous avons dû créé un Thread séparé qui incrémente, à un délai fixe, le temps.

Il est essentiel d'avoir un Thread séparé pour un comportement asynchrone, c'est-à-dire non bloquant. Le joueur peut se mouvoir et en parallèle le code responsable de faire passer le temps continue.

### Difficultés techniques rencontrées et patchs appliqués

Nous développons avec deux contraintes majeures :

- iJava et ses limitations
- Les limitations imposées par les professeurs (`ArrayList` nous manque).

Même si iJava nous donne déjà de nombreux outils très utiles à notre programme, nous avons rencontré des soucis avec celui-ci :

- Nous avons dû redéfinir la méthode `println`. Tout est expliqué dans les commentaires du code, mais en bref la façon dont iJava capture en direct les touches sur lesquelles l'utilisateur appuie a engendré des comportements inattendus tel que le [staircase effect](https://unix.stackexchange.com/questions/366423/unexpected-indentation-behaviour-when-i-set-the-terminal-to-raw-mode-why-is-th/366426).
- Nous avons également dû redéfinir `clearScreen`. Cette méthode est censée effacer le contenu du terminal, certes, mais surtout **remettre le curseur à la position (0;0)**. Le problème est qu'avec cette méthode seule, il arrivait que le curseur ne soit pas exactement positionné au bon endroit, et par conséquent tous les calculs de coordonnées étaient erronés, causant des problèmes très particuliers, étranges et inattendus. Nous avons solutionné le problème en forçant le curseur à la position 0 avec la méthode `clearMyScreen`. 

Enfin, nous aurions aimé utiliser **Java** :(.

### Gestion des mouvements

ANSI nous permet de positionner le curseur à l'aide d'un système de **coordonnées**. Lorsque l'on veut écrire quelque chose à une position donnée, nous calculons les coordonnées de la position cible et écrivons le contenu à cet endroit, avant de remettre le curseur à sa position initiale.

C'est selon cet algorithme qu'à chaque déplacement du joueur, le programme place, très rapidement, le curseur à la position actuelle du joueur, écris la couleur en-dessous de lui (celle de la map), puis décale vers la position cible pour réécrire le pixel avec la couleur donnée du joueur (qui pour l'instant est de couleur `#ffccbb`).

On replace le curseur à une position précise sous l'interface afin d'éviter d'avoir constamment un rectangle blanc à côté du joueur (le curseur).

iJava nous permet de capturer les touches saisies par l'utilisateur en direct (on capture l'événement) ce qui est bien plus ergonomique.

[Plus d'informations sur ANSI ici](https://tldp.org/HOWTO/Bash-Prompt-HOWTO/x361.html)

## Remerciements

Merci beaucoup à toute la team, aux profs qui ont aidé et merci à JB pour nous laisser utiliser son nom dans le jeu.