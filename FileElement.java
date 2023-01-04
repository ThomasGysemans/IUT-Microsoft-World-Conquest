class FileElement {
  Element type;
  String name;
  String pwd;
  String fileContent;
  FileElement[] subElements;

  // If this is a folder
  FileElement(String n, String p, FileElement[] s) {
    type = Element.FOLDER;
    name = n;
    pwd = p;
    subElements = s;
  }

  // If this is a file
  FileElement(String n, String p, String c) {
    type = Element.FILE;
    name = n;
    pwd = p;
    fileContent = c;
  }

  // If this is a file which content cannot be displayed on the terminal (pictures for example) 
  FileElement(String n, String p) {
    type = Element.FILE;
    name = n;
    pwd = p;
    fileContent = "Erreur : Ce fichier ne peut pas être ouvert ici.";
  }
}