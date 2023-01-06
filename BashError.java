enum BashError {
  UNKNOWN_COMMAND {
    public String explain(String failingCommand) {
      return errorIn(failingCommand) + "Commande inconnue.";
    }
  },
  UNKNOWN_OPTION {
    public String explain(String failingCommand) {
      return errorIn(failingCommand) + "La commande entrée comporte des options inconnues.";
    }
  }, 
  EXPECTING_ARGUMENT {
    public String explain(String failingCommand) {
      return errorIn(failingCommand) + "La commande entrée a reçu moins d'informations que nécessaire.";
    }
  },
  EXPECTING_INPUT {
    public String explain(String failingCommand) {
      return errorIn(failingCommand) + "La commande doit être précédée d'une 'pipe' ('|'). Consultez le manuel.";
    }
  },
  UNEXPECTED_INPUT {
    public String explain(String failingCommand) {
      return errorIn(failingCommand) + "La commande doit être exécutée de manière unique et indépendante.";
    }
  },
  UNEXPECTED_ARGUMENT {
    public String explain(String failingCommand) {
      return errorIn(failingCommand) + "La commande n'attend pas d'arguments.";
    }
  },
  UNEXPECTED_ERROR {
    public String explain(String failingCommand) {
      return errorIn(failingCommand) + "Une erreur inconnue et inattendue s'est produite.";
    }
  },
  TOO_MANY_ARGUMENTS {
    public String explain(String failingCommand) {
      return errorIn(failingCommand) + "La commande a reçu trop d'arguments, trop d'informations. Pensez à eviter l'utilisation d'espaces dans les arguments.";
    }
  },
  UNKNOWN_PATH {
    public String explain(String failingCommand) {
      return errorIn(failingCommand) + "Le fichier, ou le dossier, ciblé n'existe pas.";
    }
  }, // if the path we want to access doesn't exist.
  NOT_A_DIRECTORY {
    public String explain(String failingCommand) {
      return errorIn(failingCommand) + "La cible n'est pas un dossier.";
    }
  },
  NOT_A_FILE {
    public String explain(String failingCommand) {
      return errorIn(failingCommand) + "La cible n'est pas un fichier.";
    }
  },
  PATH_SYNTAX_ERROR {
    public String explain(String failingCommand) {
      return errorIn(failingCommand) + "Le chemin spécifié comporte une erreur de syntaxe. Le système ne peut le comprendre.";
    }
  },
  INVALID_ARGUMENTS {
    public String explain(String failingCommand) {
      return errorIn(failingCommand) + "Le ou les arguments passés sont invalides.";
    }
  },
  UNEXPECTED_PIPES {
    public String explain(String failingCommand) {
      return errorIn(failingCommand) + "Cette commande ne peut pas être suivie d'autres commandes séparées par des \"pipes\" (|).";
    }
  };

  public abstract String explain(String failingCommand);
  private static String errorIn(String failingCommand) {
    return "Erreur dans '" + failingCommand + "'\n";
  }
}