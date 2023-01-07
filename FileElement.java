import java.util.function.Consumer;

interface FilterFunction {
  boolean run(FileElement element);
}

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

  /**
   * Adds a new element to a folder.
   * If the element is not a folder, request is ignored.
   * @param newElement The new element to be added. It can be either a file or a directory.
   */
  public void appendFileElement(FileElement newElement) {
    if (type == Element.FOLDER) {
      FileElement[] copy = new FileElement[subElements.length + 1];
      for (int i = 0; i < subElements.length; i++) {
        copy[i] = subElements[i];
      }
      copy[copy.length-1] = newElement;
      subElements = copy;
    }
  }

  /**
   * Checks if one of the files contained in this folder matches a filter condition, recursively.
   * @param filter The filter function.
   * @return A boolean that indicates whether the file is contained in this folder or not.
   */
  public boolean doesContainFile(FilterFunction filter) {
    if (type == Element.FOLDER) {
      for (int i = 0; i < subElements.length; i++) {
        if (filter.run(subElements[i])) {
          return true;
        } else {
          if (subElements[i].type == Element.FOLDER && subElements[i].doesContainFile(filter)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * Returns a copy of the element
   */
  public FileElement copy() {
    if (type == Element.FOLDER) {
      return new FileElement(name, pwd, subElements);
    } else {
      return new FileElement(name, pwd, fileContent);
    }
  }

  /**
   * Returns the list of the copied subelements.
   * We want to make sure we do not have a shallow copy.
   */
  public FileElement[] copyOfSubElements() {
    if (type == Element.FOLDER) {
      FileElement[] c = new FileElement[subElements.length];
      for (int i = 0; i < c.length; i++) {
        c[i] = subElements[i].copy();
      }
      return c;
    } else {
      return null;
    }
  }
}