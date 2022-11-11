import extensions.File;

class Main extends Program {
    void algorithm() {
        displayASCIIPicture("../assets/1-2.txt");
    }

    void displayASCIIPicture(String path) {
        File unTexte = newFile(path);
        while (ready(unTexte)) {
            println(readLine(unTexte));
        }
    }
}
