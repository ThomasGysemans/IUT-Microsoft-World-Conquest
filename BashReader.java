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
import java.util.Hashtable;
import java.util.Base64;

interface HistoryHandler {
  String onUp();
  String onDown();
}

class BashReader {
  protected final String COMMAND_REGEX = "([a-zA-Z\\d]+)((?: -{1,2}[a-z]+)*)([ \\w=./+\\\\\\-'\\\"]*)\\|?"; // I call this a nice Tuesday
  protected final String ANSI_FOUND_MATCH = "\u001b[38;2;59;179;23m";
  protected final String ANSI_FOUND_PATTERN = "\u001b[38;2;24;56;217m";
  protected final String ANSI_RESET = "\033[0m";

  protected final String HELP = """
    Donnez des instructions à l'ordinateur au moyen de commandes simples.
    Écrivez une commande puis tapez sur la touche Entrer de votre clavier.
    L'ordinateur contient deux types d'éléments : des dossiers et des fichiers.
    Les dossiers contiennent d'autres fichiers et d'autres dossiers,
    et les fichiers peuvent être du texte, des images etc.

    Plus d'informations pour accéder à un fichier, tapez : man cd
    Plus d'informations pour voir le contenu d'un fichier, tapez : man cat
    Plus d'informations pour voir le contenu de l'ordinateur, tapez : man ls
    Plus d'informations pour encoder ou décoder un texte secret encrypté, tapez : man base64
    Plus d'informations pour copier le contenu d'un fichier ou d'un dossier, tapez : man cp
    Toutes les autres commandes disponibles : pwd, echo, tr, exit, head, tail, grep
  """;

  public final Hashtable<String, String> MANUAL = new Hashtable<String, String>() {{
    put("base64", """
      La commande \"base64\" vous permet de crypter ou de décrypter un texte secret.
      Crypter un texte signifie qu'il devient illisibile. C'est utile pour cacher un secret.

      Pour crypter un texte, tapez : base64 Bonjour
      Pour crypter le contenu d'un fichier, tapez : cat le_chemin_vers_le_fichier | base64

      Pour décrypter un texte, tapez : base64 --decode le_texte
      Pour décrypter un fichier, tapez : cat le_chemin_vers_le_fichier | base64 --decode

      Évidemment, remplacez 'le_chemin_vers_le_fichier' avec le chemin du fichier cible.
      Pour avoir plus d'informations quant au chemin d'un fichier, tapez : man cd
    """);
    put("cd", """
      La commande \"cd\" vous permet de naviguer dans les dossiers de l'ordinateur.
      L'ordinateur est rangé telle une bibliothèque, ou une armoire, et chaque section est un dossier, et chaque livre un fichier.
      Vous commencez à la racine de la bibliothèque, dont le nom du dossier est juste \"/\".

      Pour accéder à un dossier, il faut taper son chemin.
      Si le dossier est contenu dans celui dans lequel on est actuellement, alors il suffit de taper : cd le_nom_du_dossier.
      Exemple : \"cd Bureau\".

      Si vous voulez y accéder depuis la racine, tapez le chemin complet vers celui-ci.
      Exemple : \"cd /Bureau/answer_to_life.txt\", où \"/\" est la racine, \"Bureau\" un dossier dans la racine, et \"answer_to_life.txt\" un fichier.

      Plus d'informations pour connaître votre position actuelle, tapez : man pwd.
    """);
    put("ls", """
      La commande \"ls\" vous permet de lister le contenu du dossier dans lequel vous vous trouvez selon le PWD.
      Le PWD représente votre position actuelle dans l'ordinateur, que l'on peut comparer à une bibliothèque, relative à la racine.
      Pour plus d'informations sur le PWD, tapez : man pwd.

      Par exemple, si vous vous situez dans le dossier \"Famille\" par exemple, vous pourrez lister son contenu.

      Pour plus d'informations quant à la navigation, tapez : man cd.
    """);
    put("pwd", """
      La commande \"pwd\" vous permet de savoir où vous vous situez dans la navigation de l'ordinateur.
      Ceci vous donne le chemin absolu, c'est-à-dire le chemin complet depuis la racine,
      de sorte à ce que l'on puisse vous retrouver depuis n'importe où.

      Un chemin absolu commencera toujours avec la racine : \"/\".
      Un chemin relatif se base sur le chemin actuel (le PWD).
      Pour plus d'informations, tapez : man cd.
    """);
    put("echo", """
      La commande \"echo\" vous permet d'afficher du texte.
      Exemple : \"echo toto\". Ceci va afficher \"toto\".
    """);
    put("tr", """
      La commande \"tr\" vous permet de modifier le contenu de l'entrée standard.
      Vous pouvez modifier une partie du texte pour la remplacer par une autre.
      Par exemple : \"echo yoyo | tr y t\" donnera comme résultat : \"toto\", 
      car la commande a remplacé les \"y\" par des \"t\" dans le mot \"yoyo\".

      Ceci fonctionne pour n'importe quelle entrée.
      Cela vous permet ainsi de visualiser le contenu d'un fichier différement,
      avec par exemple : \"cat /Bureau/answer_to_life.txt | tr 4 3\" donnant \"32\" au lieu de \"42\".

      Pour plus d'informations sur la commande \"cat\", tapez : man cat.
    """);
    put("cat", """
      La commande \"cat\" permet de lire le contenu d'un fichier texte.
      Les fichiers textuels ont traditionnellement, à la toute fin de leur nom, l'extension : \".txt\".

      Par exemple : ouvrez le dossier Bureau avec la commande \"cd\" (pour plus d'infos tapez : man cd),
      puis listez son contenu avec la commande \"ls\" (pour plus d'infos tapez : man ls),
      et enfin tapez \"cat le_nom_du_fichier\" en remplaçant \"le_nom_du_fichier\"
      par le nom complet d'un des fichiers du dossier ouvert.
    """);
    put("head", """
      La commande \"head\" permet de sélectionner un certain nombre de lignes à partir du début du texte sortant.
      Cette commande devra toujours être précédée d'une autre, séparée par une pipe ('|').
      Exemple : \"man cd | head\".

      Par défaut, les 5 premières lignes seront affichées.
      Changez ceci avec l'option \"-n\".
      Exemple : \"man cd | head -n 2\" (qui affiche les deux premières lignes du manuel de la commande \"cd\").
    """);
    put("tail", """
      La commande \"tail\" permet de sélectionner un certain nombre de lignes à partir de la fin du texte sortant.
      Cette commande devra toujours être précédée d'une autre, séparée par une pipe ('|').
      Exemple : \"man cd | tail\".

      Par défaut, les 5 dernières lignes seront affichées.
      Changez ceci avec l'option \"-n\".
      Exemple : \"man cd | tail -n 2\" (qui affiche les deux dernières lignes du manuel de la commande \"cd\").
    """);
    put("grep", """
      La commande \"grep\" affiche les lignes du texte sortant contenant un certain mot.
      Vous pouvez l'utiliser pour lire un grand fichier et ne sélectionner que quelques lignes intéressantes.
      Exemple : \"cat toto.txt | grep la\" (ce qui affiche toutes les lignes du fichier \"toto.txt\" contenant le texte \"la\").
    """);
    put("cp", """
      La commande \"cp\" copie le contenu d'un fichier, ou d'un dossier, vers une cible,
      remplaçant l'intégralité de son contenu actuel si elle existe déjà ou créant le fichier du même nom.
      Exemple : \"cp toto.txt tata.txt\" (copie le contenu de toto.txt vers un fichier qui n'existe pas. La commande le crée.).

      Cette commande peut également copier le contenu d'un dossier pour le fusionner avec le contenu d'un autre.
      Exemple : \"cp MonSuperDossier UnNouveauDossier/\" (copie tous les fichiers du dossier dans un dossier que la commande crée également au passage).
      Notes :
        - Si la cible n'existe pas et qu'il s'agit d'un dossier, il faut faire comprendre à la commande qu'il s'agit d'un dossier en terminant le nom par un slash ("/").
        - Au contraire, si le dossier cible contient déjà des fichiers de même nom, alors le contenu de ces fichiers sera remplacé par ceux du même nom, et la fusion continue.
    """);
    put("exit", "La commande \"exit\" stoppe le terminal.");
    put("man", """
      La commande \"man\" ouvre le manuel d'utilisation sur une commande spécifique.
      Utilisez la commande la manière suivante : \"man nom_de_la_commande\".
      Remplacez \"nom de la commande\" par le nom de la commande sur laquelle vous avez besoin de plus d'informations.
      Exemple : \"man cd\".
    """);
    put("help", """
      La commande \"help\" est une commande unique au jeu.
      Elle vous permet d'avoir les premières informations essentielles afin de comprendre quoi faire.
      En suivant les instructions, vous serez capable d'accomplir votre objectif.
    """);
  }};

  protected final int HISTORY_MAX_COMMANDS = 20;
  public String[] history = new String[HISTORY_MAX_COMMANDS]; // the list of the 20 previous commands
  public int currentHistoryIndex = -1;
  public final HistoryHandler historyHandler = new HistoryHandler() {
    public String onUp() {
      if (currentHistoryIndex + 1 < HISTORY_MAX_COMMANDS) {
        String previousCommand = history[currentHistoryIndex + 1];
        if (previousCommand != null) {
          return history[++currentHistoryIndex];
        }
      }
      if (currentHistoryIndex >= 0) {
        return history[currentHistoryIndex];
      } else {
        return "";
      }
    }
    public String onDown() {
      if (currentHistoryIndex == 0) {
        currentHistoryIndex = -1;
      } else if (currentHistoryIndex - 1 >= 0) {
        return history[--currentHistoryIndex];
      }
      return "";
    }
  };

  public String PWD = "/";
  public FileElement root;
  public Runnable onExit;

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
   * and this argument is the output of the previous command.
   * @param command The input of the user.
   * @return The command and its data. The command can then be executed.
   */
  public BashCommand parseCommand(String command) {
    // We register the command in the history here
    // because we can't do it in `executeCommand`
    // because it is recursive on each subcommand.
    // Therefore, each sub command, separated from pipes, would be registered too.
    currentHistoryIndex = -1;
    appendToHistory(command.toString());

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
          return makeError(BashError.UNKNOWN_OPTION, command);
        } else if (command.arguments == null) {
          if (command.standardInput == null) {
            return makeError(BashError.EXPECTING_ARGUMENT, command);
          }
        }
        return distributePipes(command, new BashResult((command.arguments == null ? command.standardInput : command.arguments[0]).replaceAll("\"|'", "")));
      case "tr":
        if (command.options != null) {
          return makeError(BashError.UNKNOWN_OPTION, command);
        } else if (command.arguments != null && command.arguments.length != 2) {
          return makeError(BashError.EXPECTING_ARGUMENT, command);
        } else if (command.standardInput == null) {
          return makeError(BashError.EXPECTING_INPUT, command);
        }
        return distributePipes(command, new BashResult(command.standardInput.replaceAll(command.arguments[0].replaceAll("\"|'", ""), command.arguments[1].replaceAll("\"|'", ""))));
      case "pwd":
        if (command.options != null) {
          return makeError(BashError.UNKNOWN_OPTION, command);
        } else if (command.arguments != null) {
          return makeError(BashError.UNEXPECTED_ARGUMENT, command);
        } else if (command.standardInput != null) {
          return makeError(BashError.UNEXPECTED_INPUT, command);
        }
        return distributePipes(command, new BashResult(PWD));
      case "cd":
        if (command.options != null) {
          return makeError(BashError.UNKNOWN_OPTION, command);
        } else if (command.arguments == null) {
          return makeError(BashError.EXPECTING_ARGUMENT, command);
        } else if (command.arguments.length > 1) {
          return makeError(BashError.TOO_MANY_ARGUMENTS, command);
        } else if (command.standardInput != null) {
          return makeError(BashError.UNEXPECTED_INPUT, command);
        } else if (command.pipes != null) {
          return makeError(BashError.UNEXPECTED_PIPES, command);
        }
        Path targetPath = Path.parse(command.arguments[0]);
        if (targetPath == null) {
          return makeError(BashError.PATH_SYNTAX_ERROR, command);
        }
        FileElement target = getFileElementFrom(targetPath);
        if (target == null) {
          return makeError(BashError.UNKNOWN_PATH, command);
        } else if (target.type != Element.FOLDER) {
          return makeError(BashError.NOT_A_DIRECTORY, command);
        }
        PWD = target.pwd;
        return new BashResult();
      case "ls":
        if (command.options != null) {
          if (command.options.length >= 2 || !command.options[0].equals("-a")) {
            return makeError(BashError.UNKNOWN_OPTION, command);
          }
        } else if (command.standardInput != null) {
          return makeError(BashError.UNEXPECTED_INPUT, command);
        } else if (command.arguments != null && command.arguments.length > 1) {
          return makeError(BashError.TOO_MANY_ARGUMENTS, command);
        }
        FileElement lsTarget = getFileElementFrom(command.arguments != null ? command.arguments[0] : PWD);
        if (lsTarget == null) {
          return makeError(BashError.UNKNOWN_PATH, command);
        } else if (lsTarget.type != Element.FOLDER) {
          return makeError(BashError.NOT_A_DIRECTORY, command);
        }
        String f = getFileElementsHierarchyBasedOnPWD(lsTarget, command.options != null && command.options[0].equals("-a"));
        return distributePipes(command, new BashResult(f));
      case "cat":
        if (command.options != null) {
          return makeError(BashError.UNKNOWN_OPTION, command);
        } else if (command.arguments == null) {
          return makeError(BashError.EXPECTING_ARGUMENT, command);
        } else if (command.arguments.length > 1) {
          return makeError(BashError.UNEXPECTED_ARGUMENT, command);
        } else if (command.standardInput != null) {
          return makeError(BashError.UNEXPECTED_INPUT, command);
        }
        Path catPath = Path.parse(command.arguments[0]);
        if (catPath == null) {
          return makeError(BashError.PATH_SYNTAX_ERROR, command);
        }
        FileElement catFile = getFileElementFrom(catPath);
        if (catFile == null) {
          return makeError(BashError.UNKNOWN_PATH, command);
        } else if (catFile.type != Element.FILE) {
          return makeError(BashError.NOT_A_FILE, command);
        }
        return distributePipes(command, new BashResult(catFile.fileContent));
      case "base64":
        if (command.standardInput != null && command.arguments != null) {
          return makeError(BashError.UNEXPECTED_ARGUMENT, command);
        } else if (command.standardInput == null && command.arguments == null) {
          return makeError(BashError.EXPECTING_ARGUMENT, command);
        } else if (command.standardInput == null && command.arguments != null && command.arguments.length > 1) {
          return makeError(BashError.TOO_MANY_ARGUMENTS, command);
        } else if (command.options != null && (command.options.length > 1 || !command.options[0].equals("--decode"))) {
          return makeError(BashError.UNKNOWN_OPTION, command);
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
          return makeError(BashError.UNEXPECTED_ERROR, command);
        }
        return distributePipes(command, new BashResult(base64output));
      case "help":
        if (command.arguments != null) {
          return makeError(BashError.UNEXPECTED_ARGUMENT, command);
        } else if (command.options != null) {
          return makeError(BashError.UNKNOWN_OPTION, command);
        } else if (command.standardInput != null) {
          return makeError(BashError.UNEXPECTED_INPUT, command);
        }
        return distributePipes(command, new BashResult(HELP));
      case "exit":
        if (command.arguments != null) {
          return makeError(BashError.UNEXPECTED_ARGUMENT, command);
        } else if (command.options != null) {
          return makeError(BashError.UNKNOWN_OPTION, command);
        } else if (command.standardInput != null) {
          return makeError(BashError.UNEXPECTED_INPUT, command);
        } else if (command.pipes != null) {
          return makeError(BashError.UNEXPECTED_PIPES, command);
        }
        PWD="/";
        onExit.run();
        return new BashResult();
      case "head":
        if (command.options != null && (!command.options[0].equals("-n") || command.options.length > 1)) {
          return makeError(BashError.UNKNOWN_OPTION, command);
        } else if (command.options == null && command.arguments != null) {
          return makeError(BashError.TOO_MANY_ARGUMENTS, command);
        } else if (command.standardInput == null) {
          return makeError(BashError.EXPECTING_INPUT, command);
        }
        int headN = command.arguments != null ? Integer.parseInt(command.arguments[0]) : 5;
        if (headN < 0) {
          return makeError(BashError.INVALID_ARGUMENTS, command);
        }
        String headContent = command.standardInput.trim();
        int headLinePos = headContent.indexOf('\n');
        while (--headN > 0 && headLinePos != -1) {
          headLinePos = headContent.indexOf('\n', headLinePos + 1);
        }
        return distributePipes(command, new BashResult(headLinePos <= 0 ? headContent : headContent.substring(0,headLinePos)));
      case "tail":
         if (command.options != null && (!command.options[0].equals("-n") || command.options.length > 1)) {
          return makeError(BashError.UNKNOWN_OPTION, command);
        } else if (command.options == null && command.arguments != null) {
          return makeError(BashError.TOO_MANY_ARGUMENTS, command);
        } else if (command.standardInput == null) {
          return makeError(BashError.EXPECTING_INPUT, command);
        }
        int tailN = command.arguments != null ? Integer.parseInt(command.arguments[0]) : 5;
        if (tailN < 0) {
          return makeError(BashError.INVALID_ARGUMENTS, command);
        }
        String tailContent = command.standardInput.trim();
        int tailLinePos = tailContent.lastIndexOf('\n');
        while (--tailN > 0 && tailLinePos != -1) {
          tailLinePos = tailContent.substring(0,tailLinePos-1).lastIndexOf('\n');
        }
        return distributePipes(command, new BashResult(tailLinePos <= 0 ? tailContent : tailContent.substring(tailLinePos)));
      case "grep":
        if (command.options != null) {
          return makeError(BashError.UNKNOWN_OPTION, command);
        } else if (command.arguments == null) {
          return makeError(BashError.EXPECTING_ARGUMENT, command);
        } else if (command.arguments.length > 1) {
          return makeError(BashError.TOO_MANY_ARGUMENTS, command);
        }
        final Pattern grepPattern = Pattern.compile(command.arguments[0]);
        final String[] grepFileLines = command.standardInput.split("\n");
        String grepContent = "";
        for (String line : grepFileLines) {
          if (grepPattern.matcher(line).find()) {
            grepContent += line.replaceAll(command.arguments[0], ANSI_FOUND_PATTERN + command.arguments[0] + ANSI_RESET) + "\n";
          }
        }
        return distributePipes(command, new BashResult(grepContent));
      case "cp":
        if (command.options != null) {
          return makeError(BashError.UNKNOWN_OPTION, command);
        } else if (command.arguments == null) {
          return makeError(BashError.EXPECTING_ARGUMENT, command);
        } else if (command.arguments.length != 2) {
          return makeError(BashError.INVALID_ARGUMENTS, command);
        } else if (command.pipes != null) {
          return makeError(BashError.UNEXPECTED_PIPES, command);
        }
        Path cp1 = Path.parse(command.arguments[0].startsWith("./") ? command.arguments[0].substring(2) : command.arguments[0]);
        Path cp2 = Path.parse(command.arguments[1].startsWith("./") ? command.arguments[1].substring(2) : command.arguments[1]);
        if (cp1 == null || cp2 == null) {
          return makeError(BashError.PATH_SYNTAX_ERROR, command);
        }
        FileElement cp1File = getFileElementFrom(cp1);
        FileElement cp2File = getFileElementFrom(cp2);
        if (cp1File == null) {
          return makeError(BashError.UNKNOWN_PATH, command);
        }
        if (cp1File.type == Element.FILE) {
          if (cp2File != null) {
            if (cp2File.type == Element.FILE) {
              cp2File.fileContent = cp1File.fileContent;
            } else if (cp2File.type == Element.FOLDER) {
              boolean foundFileWithSameNameInsideFolder = false;
              for (FileElement childElement : cp2File.subElements) {
                if (childElement.name.equals(cp1File.name)) {
                  childElement.fileContent = cp1File.fileContent;
                  foundFileWithSameNameInsideFolder = true;
                  break;
                }
              }
              if (!foundFileWithSameNameInsideFolder) {
                cp2File.appendFileElement(cp1File.copy());
              }
            }
          } else {
            // The destination doesn't exist, so we must know if the user wants to create a folder or a file
            // We must keep in mind that the origin is a file.
            FileElement newFile;
            FileElement parentElement;

            if (cp2.getParent() == null) {
              parentElement = getFileElementFrom(PWD);
            } else {
              parentElement = getFileElementFrom(cp2.getParent());
              if (parentElement == null) {
                return makeError(BashError.UNKNOWN_PATH, command);
              }
            }

            String completeFileName = cp2.getCompleteFileName();
            if (cp2.isLeadingToFolder()) {
              newFile = new FileElement(completeFileName, parentElement.pwd + "/" + completeFileName, new FileElement[]{ cp1File.copy() });
            } else {
              newFile = new FileElement(completeFileName, parentElement.pwd + "/" + completeFileName, cp1File.fileContent);
            }
            parentElement.appendFileElement(newFile.copy());
          }
        } else { // The origin is a folder
          if (cp2File != null) {
            if (cp2File.type != Element.FOLDER) {
              return makeError(BashError.NOT_A_DIRECTORY, command);
            } else {
              // Merge content of folder 1 into folder 2
              boolean foundSameFilenameDuringMerge = false;
              for (FileElement childOf1 : cp1File.subElements) {
                for (FileElement childOf2 : cp2File.subElements) {
                  if (childOf2.name.equals(childOf1)) {
                    childOf1.fileContent = childOf1.fileContent;
                    foundSameFilenameDuringMerge = true;
                    break;
                  }
                }
                if (!foundSameFilenameDuringMerge) {
                  cp2File.appendFileElement(childOf1.copy());
                } else {
                  foundSameFilenameDuringMerge = false;
                }
              }
            }
          } else {
            // The destination doesn't exist.
            // if cp2 is filename, then throw an error
            // if cp2 is folder, then create the folder and copy the entire content as its subElements
            if (cp2.isLeadingToFolder()) {
              String parent = cp2.getParent();
              FileElement parentElement = getFileElementFrom(parent == null ? PWD : parent);
              FileElement destination = new FileElement(cp2.fileName, parentElement.pwd + "/" + cp2.fileName, cp1File.copyOfSubElements());
              parentElement.appendFileElement(destination);
            } else {
              return makeError(BashError.NOT_A_DIRECTORY, command);
            }
          }
        }
        return new BashResult();
      case "man":
        if (command.arguments == null) {
          return makeError(BashError.EXPECTING_ARGUMENT, command);
        } else if (command.arguments != null && command.arguments.length > 1) {
          return makeError(BashError.TOO_MANY_ARGUMENTS, command);
        } else if (command.options != null) {
          return makeError(BashError.UNKNOWN_OPTION, command);
        }
        String manText = MANUAL.get(command.arguments[0]);
        if (manText == null) {
          return makeError(BashError.UNKNOWN_COMMAND, command);
        }
        return distributePipes(command, new BashResult(manText));
      default:
        return makeError(BashError.UNKNOWN_COMMAND, command);
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
   * Gets the reference and metadata of a path.
   * @param path The path of the desired element. It can be either absolute or relative.
   * @return The target's data.
   */
  public FileElement getFileElementFrom(Path path) {
    FileElement base;
    if (path.isAbsolute()) {
      base = root;
      boolean found = false;
      for (int i = 1; i < path.segments.length; i++) {
        for (FileElement subElement : base.subElements) {
          if (subElement.name.equals(path.segments[i])) {
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
      for (String segment : path.segments) {
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
   * Gets the file element according to a path.
   * This function is meant to be used when we want to get the file element of PWD.
   * @param path The path as a string, which is then parsed.
   * @return An instance of FileElement corresponding to the element found at the given path.
   */
  public FileElement getFileElementFrom(String path) {
    return getFileElementFrom(Path.parse(path));
  }

  /**
   * Returns a string that represents all the containing elements of a folder.
   * It won't show the hidden files (those starting with a dot) except if `showHiddenFiles` is set to true.
   * @param target The folder whose contents must be displayed.
   * @param showHiddenFiles Set it to true if you want to display the files starting with a dot.
   * @return A string that can be printed to display the contents of the folder. Returns null if the path is not correct.
   */
  public String getFileElementsHierarchyBasedOnPWD(FileElement target, boolean showHiddenFiles) {
    String structure = "";
    for (int i = 0; i < target.subElements.length; i++) {
      if (!showHiddenFiles && target.subElements[i].name.charAt(0) == '.') {
        continue;
      }
      if (target.subElements[i].type == Element.FOLDER) {
        structure += ANSI_FOUND_MATCH + target.subElements[i].name + ANSI_RESET + "\n";
      } else {
        structure += target.subElements[i].name + "\n";
      }
    }
    return structure;
  }

  /**
   * Append to history of commands.
   * It shifts the whole list by one,
   * so the new command takes index 0,
   * and the oldest command is removed.
   * For example, with `array = ['a', 'b', 'c', 'd', 'e']`, it would become `['newElement' 'a', 'b', 'c', 'd']`
   * This changes the `history` variable.
   * @param command The command to be registered.
   */
  public void appendToHistory(String command) {
    for (int i = history.length - 1; i > 0; i--) {
      history[i] = history[i-1];
    }
    history[0] = command;
  }

  /**
   * Send the error
   * @param error The enum to describe the error
   * @param failingCommand The command that failed.
   * @return The result which contains the reassembled command to be displayed in order to explain the error later.
   */
  private BashResult makeError(BashError error, BashCommand failingCommand) {
    return new BashResult(error, failingCommand.reassembleCommand());
  }
}