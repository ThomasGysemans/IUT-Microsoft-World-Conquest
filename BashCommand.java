class BashCommand {
  String fullMatch;
  String commandName;
  String[] options;
  String[] arguments;
  String standardInput;
  BashCommand[] pipes;

  /**
   * Reassembles the command without the pipes.
   * It is not using the `fullMatch` property because
   * it might contain too many white spaces in between options or arguments,
   * or the pipe itself at the very end (`|`).
   * @return The command without its pipes written in the following format: "command_name ...options ...arguments"
   */
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

  /**
   * Makes a string containing the whole command (with the pipes).
   * @return A string that represents the line of code.
   */
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