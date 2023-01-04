class BashCommand {
  String fullMatch;
  String commandName;
  String[] options;
  String[] arguments;
  String standardInput;
  BashCommand[] pipes;

  public String reassembleCommand() {
    String str = commandName;
    if (options != null) {
      for (int i = 0; i < options.length; i++) {
        str += " " + options[i];
      }
    }
    if (arguments != null) {
      for (int i = 0; i < arguments.length; i++) {
        str += " " + arguments[i];
      }
    }
    return str;
  }

  public String toString() {
    String str = fullMatch;
    if (pipes != null) {
      for (int i = 0; i < pipes.length; i++) {
        str += " " + pipes[i].fullMatch;
      }
    }
    return str;
  }
}