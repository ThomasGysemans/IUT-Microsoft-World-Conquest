class ManipulationConsole extends Program {
  final int TOP_ARROW_KEY = 17;
  final int BOTTOM_ARROW_KEY = 18;
  final int RIGHT_ARROW_KEY = 20;
  final int LEFT_ARROW_KEY = 19;

  boolean finished = false;

  void algorithm() {
    enableKeyTypedInConsole(true);
    while (!finished) {
      delay(500);
    }
    println("Done.");
  }

  void keyTypedInConsole(int a) {
    switch (a) {
      case TOP_ARROW_KEY:
        println("Top arrow key pressed");
        break;
      case 'q':
        finished = true;
        break;
      default:
        println(a);
    }
  }
}
