/*
*
* À la base on voulait que ce soit BASH directement qui exécute les commandes entrées par l'utilisateur via (Runtime.getRuntime().exec(exactCommand)).
* Cependant, ceci pose des problèmes de sécurité et nous devons donc imposer des limites.
* En effet, rien n'empêcherait l'utilisateur de faire 'rm /', `cd ..` ou juste d'explorer son propre système lors du jeu.
* Par conséquent, j'ai (Thomas) écrit un programme qui permet de simplement lire et interpréter des commandes BASH.
* Ces commandes sont très rudimentaires. Il s'agit d'une copie du langage avec des options en moins, des fonctionnalités en moins,
* et quelques différences techniques sur les fonctionnalités originales.
* Pour le bien de ma santé mentale, j'ai utilisé du *vrai* Java (donc constructeur, ArrayList, etc.).
*
*/

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Base64;

class BashReader {
  final String COMMAND_REGEX = "([a-zA-Z\\d]+)((?: -{1,2}[a-z]+)*)([ \\w=./+\\\\\\-'\\\"]*)\\|?"; // I call this a nice Tuesday
  final String PATH_REGEX = "^(?:\\.{0,2}\\/?)(?:[\\w\\-.]+\\/?)*$";

  final String MAIN_HELP = """
    Donnez des instructions à l'ordinateur au moyen de commandes simples.
    Écrivez une commande puis tapez sur la touche Entrer de votre clavier.
    L'ordinateur contient deux types d'éléments : des dossiers et des fichiers.
    Les dossiers contiennent d'autres fichiers et d'autres dossiers,
    et les fichiers peuvent être du texte, des images etc.

    Plus d'informations pour accéder à un fichier, tapez : man cd
    Plus d'informations pour voir le contenu d'un fichier, tapez : man cat
    Plus d'informations pour voir le contenu de l'ordinateur, tapez : man ls
    Plus d'informations pour encoder ou décoder un texte secret encrypté, tapez : man base64
    Toutes les autres commandes disponibles : pwd, echo, tr, exit.
  """;

  final String MAN_BASE64 = """
    La commande \"base64\" vous permet de crypter ou de décrypter un texte secret.
    Crypter un texte signifie qu'il devient illisibile. C'est utile pour cacher un secret.

    Pour crypter un texte, tapez : base64 Bonjour
    Pour crypter le contenu d'un fichier, tapez : cat le_chemin_vers_le_fichier | base64

    Pour décrypter un texte, tapez : base64 --decode le_texte
    Pour décrypter un fichier, tapez : cat le_chemin_vers_le_fichier | base64 --decode

    Évidemment, remplacez 'le_chemin_vers_le_fichier' avec le chemin du fichier cible.
    Pour avoir plus d'informations quant au chemin d'un fichier, tapez : man cd
  """;

  final String MAN_CD = """
    La commande \"cd\" vous permet de naviguer dans les dossiers de l'ordinateur.
    L'ordinateur est rangé telle une bibliothèque, ou une armoire, et chaque section est un dossier, et chaque livre un fichier.
    Vous commencez à la racine de la bibliothèque, dont le nom du dossier est juste \"/\".

    Pour accéder à un dossier, il faut taper son chemin.
    Si le dossier est contenu dans celui dans lequel on est actuellement, alors il suffit de taper : cd le_nom_du_dossier.
    Exemple : \"cd Bureau\".

    Si vous voulez y accéder depuis la racine, tapez le chemin complet vers celui-ci.
    Exemple : \"cd /Bureau/answer_to_life.txt\", où \"/\" est la racine, \"Bureau\" un dossier dans la racine, et \"answer_to_life.txt\" un fichier.

    Plus d'informations pour connaître votre position actuelle, tapez : man pwd.
  """;

  final String MAN_LS = """
    La commande \"ls\" vous permet de lister le contenu du dossier dans lequel vous vous trouvez selon le PWD.
    Le PWD représente votre position actuelle dans l'ordinateur, que l'on peut comparer à une bibliothèque, relative à la racine.
    Pour plus d'informations sur le PWD, tapez : man pwd.

    Par exemple, si vous vous situez dans le dossier \"Famille\" par exemple, vous pourrez lister son contenu.

    Pour plus d'informations quant à la navigation, tapez : man cd.
  """;

  final String MAN_PWD = """
    La commande \"pwd\" vous permet de savoir où vous vous situez dans la navigation de l'ordinateur.
    Ceci vous donne le chemin absolu, c'est-à-dire le chemin complet depuis la racine,
    de sorte à ce que l'on puisse vous retrouver depuis n'importe où.

    Un chemin absolu commencera toujours avec la racine : \"/\".
    Un chemin relatif se base sur le chemin actuel (le PWD).
    Pour plus d'informations, tapez : man cd.
  """;
  
  final String MAN_ECHO = """
    La commande \"echo\" vous permet d'afficher du texte.
    Exemple : \"echo toto\". Ceci va afficher \"toto\".
  """;

  final String MAN_TR = """
    La commande \"tr\" vous permet de modifier le contenu de l'entrée standard.
    Vous pouvez modifier une partie du texte pour la remplacer par une autre.
    Par exemple : \"echo yoyo | tr y t\" donnera comme résultat : \"toto\", 
    car la commande a remplacé les \"y\" par des \"t\" dans le mot \"yoyo\".

    Ceci fonctionne pour n'importe quelle entrée.
    Cela vous permet ainsi de visualiser le contenu d'un fichier différement,
    avec par exemple : \"cat /Bureau/answer_to_life.txt | tr 4 3\" donnant \"32\" au lieu de \"42\".

    Pour plus d'informations sur la commande \"cat\", tapez : man cat.
  """;

  final String MAN_CAT = """
    La commande \"cat\" permet de lire le contenu d'un fichier texte.
    Les fichiers textuels ont traditionnellement, à la toute fin de leur nom, l'extension : \".txt\".

    Par exemple : ouvrez le dossier Bureau avec la commande \"cd\" (pour plus d'infos tapez : man cd),
    puis listez son contenu avec la commande \"ls\" (pour plus d'infos tapez : man ls),
    et enfin tapez \"cat le_nom_du_fichier\" en remplaçant \"le_nom_du_fichier\"
    par le nom complet d'un des fichiers du dossier ouvert.
  """;

  final String MAN_EXIT = """
    La commande \"exit\" stoppe le terminal.
  """;

  final String MAN_MAN = """
    La commande \"man\" ouvre le manuel d'utilisation sur une commande spécifique.
    Utilisez la commande la manière suivante : \"man nom_de_la_commande\".
    Remplacez \"nom de la commande\" par le nom de la commande sur laquelle vous avez besoin de plus d'informations.
    Exemple : \"man cd\".
  """;

  final String MAN_HELP = """
    La commande \"help\" est une commande unique au jeu.
    Elle vous permet d'avoir les premières informations essentielles afin de comprendre quoi faire.
    En suivant les instructions, vous serez capable d'accomplir votre objectif.
  """;

  String PWD = "/";
  FileElement root;
  Runnable onExit;

  BashReader(String p, FileElement r, Runnable o) {
    PWD = p;
    root = r;
    onExit = o;
  }

  /**
   * Parses a command so that the algorithm can more easily understand it during execution.
   * The parsing is done with regular expressions. This is not perfect as quotes may not be understood correctly.
   * This is not meant to be used as a professionnal tool so such issues can be neglected.
   * The way it works is that each line can have several commands separated by pipes.
   * A command that is preceeded by a pipe is called a "subcommand" and its standard input will count as an argument,
   * And this argument is the output of the previous command.
   * @param command The input of the user.
   * @return The command and its data. The command can then be executed.
   */
  public BashCommand parseCommand(String command) {
    final Pattern pattern = Pattern.compile(COMMAND_REGEX);
    final Matcher matcher = pattern.matcher(command);
    BashCommand bashCommand = new BashCommand();
    ArrayList<BashCommand> pipes = new ArrayList<>();
    int i = 0;
    while (matcher.find()) {
      final String fullMatch = matcher.group(0);
      final String commandName = matcher.group(1);
      final String options = matcher.group(2).trim();
      final String arguments = matcher.group(3).trim();
      if (i == 0) {
        bashCommand.fullMatch = fullMatch;
        bashCommand.commandName = commandName;
        bashCommand.options = (options.length() == 0 ? null : options.split(" ", -1));
        bashCommand.arguments = (arguments.length() == 0 ? null : arguments.split(" ", -1));
        i++;
      } else {
        BashCommand subCommand = new BashCommand();
        subCommand.fullMatch = fullMatch;
        subCommand.commandName = commandName;
        subCommand.options = (options.length() == 0 ? null : options.split(" ", -1));
        subCommand.arguments = (arguments.length() == 0 ? null : arguments.split(" ", -1));
        pipes.add(subCommand);
      }
    }
    if (pipes.size() > 0) {
      bashCommand.pipes = new BashCommand[pipes.size()];
      for (int j = 0; j < pipes.size(); j++) {
        bashCommand.pipes[j] = pipes.get(j);
      }
    }
    return bashCommand;
  }

  /**
   * Executes a parsed command.
   * @param command The command to be executed.
   * @return The result of the command. The result can be the output that needs to be printed, or an error.
   */
  public BashResult executeCommand(BashCommand command) {
    if (command.commandName == null) {
      return new BashResult(BashError.UNKNOWN_COMMAND, "");
    }
    switch (command.commandName) {
      case "echo":
        if (command.options != null) {
          return throwError(BashError.UNKNOWN_OPTION, command);
        } else if (command.arguments == null) {
          if (command.standardInput == null) {
            return throwError(BashError.EXPECTING_ARGUMENT, command);
          }
        }
        return distributePipes(command, new BashResult((command.arguments == null ? command.standardInput : command.arguments[0]).replaceAll("\"|'", "")));
      case "tr":
        if (command.options != null) {
          return throwError(BashError.UNKNOWN_OPTION, command);
        } else if (command.arguments != null && command.arguments.length != 2) {
          return throwError(BashError.EXPECTING_ARGUMENT, command);
        } else if (command.standardInput == null) {
          return throwError(BashError.EXPECTING_INPUT, command);
        }
        return distributePipes(command, new BashResult(command.standardInput.replaceAll(command.arguments[0].replaceAll("\"|'", ""), command.arguments[1].replaceAll("\"|'", ""))));
      case "pwd":
        if (command.options != null) {
          return throwError(BashError.UNKNOWN_OPTION, command);
        } else if (command.arguments != null) {
          return throwError(BashError.UNEXPECTED_ARGUMENT, command);
        } else if (command.standardInput != null) {
          return throwError(BashError.UNEXPECTED_INPUT, command);
        }
        return distributePipes(command, new BashResult(PWD));
      case "cd":
        if (command.options != null) {
          return throwError(BashError.UNKNOWN_OPTION, command);
        } else if (command.arguments == null) {
          return throwError(BashError.EXPECTING_ARGUMENT, command);
        } else if (command.arguments.length > 1) {
          return throwError(BashError.TOO_MANY_ARGUMENTS, command);
        } else if (command.standardInput != null) {
          return throwError(BashError.UNEXPECTED_INPUT, command);
        } else if (!isPathSyntaxCorrect(command.arguments[0])) {
          return throwError(BashError.PATH_SYNTAX_ERROR, command);
        }
        FileElement b = getFileElementFrom(PWD);
        if (b == null) {
          PWD = "/"; // rebase
          return throwError(BashError.UNEXISTING_PATH, command);
        }
        String targetPath = command.arguments[0];
        FileElement target = getFileElementFrom(targetPath);
        if (target == null) {
          return throwError(BashError.UNKNOWN_PATH, command);
        } else if (target.type != Element.FOLDER) {
          return throwError(BashError.NOT_A_DIRECTORY, command);
        }
        PWD = target.pwd;
        return new BashResult();
      case "ls":
        if (command.options != null) {
          if (command.options.length >= 2 || !command.options[0].equals("-a")) {
            return throwError(BashError.UNKNOWN_OPTION, command);
          }
        } else if (command.standardInput != null) {
          return throwError(BashError.UNEXPECTED_INPUT, command);
        } else if (command.arguments != null && command.arguments.length > 1) {
          return throwError(BashError.TOO_MANY_ARGUMENTS, command);
        }
        String f = getFileElementsHierarchyBasedOnPWD(command.arguments != null ? command.arguments[0].replaceAll("\"|'", "") : PWD, command.options != null && command.options[0].equals("-a"));
        if (f == null) {
          return throwError(BashError.UNKNOWN_PATH, command);
        }
        return distributePipes(command, new BashResult(f));
      case "cat":
        if (command.options != null) {
          return throwError(BashError.UNKNOWN_OPTION, command);
        } else if (command.arguments == null) {
          return throwError(BashError.EXPECTING_ARGUMENT, command);
        } else if (command.arguments.length > 1) {
          return throwError(BashError.UNEXPECTED_ARGUMENT, command);
        } else if (command.standardInput != null) {
          return throwError(BashError.UNEXPECTED_INPUT, command);
        } else if (!isPathSyntaxCorrect(command.arguments[0])) {
          return throwError(BashError.PATH_SYNTAX_ERROR, command);
        }
        FileElement cb = getFileElementFrom(PWD);
        if (cb == null) {
          PWD = "/"; // rebase
          return throwError(BashError.UNEXISTING_PATH, command);
        }
        FileElement file = getFileElementFrom(command.arguments[0].replaceAll("\"|'", ""));
        if (file == null) {
          return throwError(BashError.UNKNOWN_PATH, command);
        } else if (file.type != Element.FILE) {
          return throwError(BashError.NOT_A_FILE, command);
        }
        return distributePipes(command, new BashResult(file.fileContent));
      case "base64":
        if (command.standardInput != null && command.arguments != null) {
          return throwError(BashError.UNEXPECTED_ARGUMENT, command);
        } else if (command.standardInput == null && command.arguments == null) {
          return throwError(BashError.EXPECTING_ARGUMENT, command);
        } else if (command.standardInput == null && command.arguments != null && command.arguments.length > 1) {
          return throwError(BashError.TOO_MANY_ARGUMENTS, command);
        } else if (command.options != null && (command.options.length > 1 || !command.options[0].equals("--decode"))) {
          return throwError(BashError.UNKNOWN_OPTION, command);
        }
        String base64input = (command.standardInput != null ? command.standardInput : command.arguments[0]).replaceAll("\"|'", "");
        String base64output = "";
        try {
          if (command.options != null && command.options[0].equals("--decode")) {
            base64output = new String(Base64.getDecoder().decode(base64input));
          } else {
            base64output = Base64.getEncoder().encodeToString(base64input.getBytes());
          }
        } catch (Exception e) {
          return throwError(BashError.UNEXPECTED_ERROR, command);
        }
        return distributePipes(command, new BashResult(base64output));
      case "help":
        if (command.arguments != null) {
          return throwError(BashError.UNEXPECTED_ARGUMENT, command);
        } else if (command.options != null) {
          return throwError(BashError.UNKNOWN_OPTION, command);
        } else if (command.standardInput != null) {
          return throwError(BashError.UNEXPECTED_INPUT, command);
        }
        return distributePipes(command, new BashResult(MAIN_HELP));
      case "exit":
        if (command.arguments != null) {
          return throwError(BashError.UNEXPECTED_ARGUMENT, command);
        } else if (command.options != null) {
          return throwError(BashError.UNKNOWN_OPTION, command);
        } else if (command.standardInput != null) {
          return throwError(BashError.UNEXPECTED_INPUT, command);
        }
        onExit.run();
        return new BashResult();
      case "man":
        if (command.arguments == null) {
          return throwError(BashError.EXPECTING_ARGUMENT, command);
        } else if (command.arguments != null && command.arguments.length > 1) {
          return throwError(BashError.TOO_MANY_ARGUMENTS, command);
        } else if (command.options != null) {
          return throwError(BashError.UNKNOWN_OPTION, command);
        }
        String manText = "";
        // Usually I would have done this differently,
        // but considering the low number of commands,
        // we can stick with that for now.
        switch (command.arguments[0]) {
          case "echo": manText = MAN_ECHO; break;
          case "tr": manText = MAN_TR; break;
          case "pwd": manText = MAN_PWD; break;
          case "cd": manText = MAN_CD; break;
          case "ls": manText = MAN_LS; break;
          case "cat": manText = MAN_CAT; break;
          case "base64": manText = MAN_BASE64; break;
          case "help": manText = MAN_HELP; break;
          case "exit": manText = MAN_EXIT; break;
          case "man": manText = MAN_MAN; break;
          default: return throwError(BashError.UNKNOWN_COMMAND, command);
        }
        return distributePipes(command, new BashResult(manText));
      default:
        return throwError(BashError.UNKNOWN_COMMAND, command);
    }
  }

  /**
   * Each command can have unlimited so-called "subcommands"
   * whose standard input is the output of the previous command,
   * separated by a pipe (`|`).
   * Therefore, there is recursion for each command.
   * Whether there are pipes or not, this needs to be handled by each command while execution.
   * @param command The command that is being executed.
   * @param firstResult the first command's result, before any pipes.
   * @return The result of the last executed command, or an error as soon as one gets thrown.
   */
  private BashResult distributePipes(BashCommand command, BashResult firstResult) {
    if (command.pipes != null) {
      command.pipes[0].standardInput = firstResult.output;
      for (int i = 1; i < command.pipes.length; i++) {
        BashResult rI = executeCommand(command.pipes[i-1]);
        if (rI.error != null) {
          return rI;
        }
        command.pipes[i].standardInput = rI.output;
      }
      return executeCommand(command.pipes[command.pipes.length-1]);
    } else {
      return firstResult;
    }
  }

  /**
   * Checks if the syntax of a given path is correct.
   * @param path The path coming from the input of a user.
   * @return True if the syntax is correct, false otherwise.
   */
  public boolean isPathSyntaxCorrect(String path) {
    return Pattern.compile(PATH_REGEX).matcher(path).matches();
  }

  /**
   * Gets the reference and metadata of a path.
   * @param path The path of the desired element. It can be either absolute or relative to the current PWD.
   * @return The target's data.
   */
  public FileElement getFileElementFrom(String path) {
    String[] segments = path.split("/");
    FileElement base;
    if (isAbsolutePath(path)) {
      base = root;
      boolean found = false;
      for (int i = 1; i < segments.length; i++) {
        for (FileElement subElement : base.subElements) {
          if (subElement.name.equals(segments[i])) {
            base = subElement;
            found = true;
            break;
          }
        }
        if (!found) {
          return null;
        }
        found = false;
      }
    } else {
      base = getFileElementFrom(PWD);
      for (String segment : segments) {
        if (segment.equals(".")) {
          continue;
        } else {
          if (segment.equals("..")) {
            String dest = base.pwd.substring(0,base.pwd.lastIndexOf("/"));
            if (dest.length() == 0) {
              base = root;
            } else {
              base = getFileElementFrom(dest);
            }
          } else {
            base = getFileElementFrom(base.pwd + (base.pwd.equals("/") ? "" : "/") + segment);
          }
          if (base == null || base.type != Element.FOLDER) {
            return base;
          }
        }
      }
    }
    return base;
  }

  /**
   * Returns a string that represents all the containing elements of a folder.
   * It won't show the hidden files (those starting with a dot) except if `showHiddenFiles` is set to true.
   * @param pwd The absolute path of the folder whose contents should be displayed.
   * @param showHiddenFiles Set it to true if you want to display the files starting with a dot.
   * @return A string that can be printed to display the contents of the folder. Returns null if the path is not correct.
   */
  public String getFileElementsHierarchyBasedOnPWD(String pwd, boolean showHiddenFiles) {
    String structure = "";
    FileElement p = getFileElementFrom(pwd);
    if (p == null) {
      return null;
    }
    for (int i = 0; i < p.subElements.length; i++) {
      if (!showHiddenFiles && p.subElements[i].name.charAt(0) == '.') {
        continue;
      }
      if (p.subElements[i].type == Element.FOLDER) {
        structure += "\u001b[38;2;59;179;23m" + p.subElements[i].name + "\033[0m\n";
      } else {
        structure += p.subElements[i].name + "\n";
      }
    }
    return structure;
  }
  
  /**
   * Checks if a given path is absolute or relative.
   * @param path The path to verify.
   * @return True for absolute path, false for relative path.
   */
  public boolean isAbsolutePath(String path) {
    return path.charAt(0) == '/';
  }

  /**
   * Check if an absolute path is correct or not by trying to access it.
   * If it can't access the target, it means it is not correct.
   * @param path The absolute path to access.
   * @return A boolean that indicates whether the path is correct or not.
   */
  public boolean isAbsolutePathCorrect(String path) {
    return getFileElementFrom(path) != null;
  }

  /**
   * Send the error
   * @param error The enum to describe the error
   * @param failingCommand The command that failed.
   * @return The result which contains the reassembled command to be displayed in order to explain the error later.
   */
  private BashResult throwError(BashError error, BashCommand failingCommand) {
    return new BashResult(error, failingCommand.reassembleCommand());
  }
}