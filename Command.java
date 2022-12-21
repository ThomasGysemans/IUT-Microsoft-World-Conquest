class Command {
  String name; // the name of the command
  CommandCategory category; // its category
  int key; // the numeric code of the character
  int originalKey; // the default numeric code of the character
  String uid; // the unique identifier given to that key so that we can recognize it in the code even if the user changed the key

  private char getChar(int keyCode) {
    switch (keyCode) {
      case 17: return '↑';
      case 18: return '↓';
      case 19: return '←';
      case 20: return '→';
      default: return (char)keyCode;
    }
  }

  char getCurrentChar() {
    return getChar(key);
  }

  char getOriginalChar() {
    return getChar(originalKey);
  }
}