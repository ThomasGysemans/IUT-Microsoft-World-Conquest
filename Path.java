import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Path {
  final static String REGEX = "^(?<travelback>(?:\\.{0,2}\\/?)*)(?<name>[\\w\\-]+\\/?)*(?<extension>\\.[\\w]+)?$";
  
  String fullPath;
  String[] segments;
  String fileName;
  String extension;
  Element type;

  /**
   * Parses a path to analyze it more easily.
   * @param fullPath The path to be parsed.
   * @return An instance of Path, or null if the path is not valid.
   */
  public static Path parse(String fullPath) {
    fullPath = fullPath.trim().replaceAll("\"|'", "");
    if (fullPath.length() == 0) {
      return null;
    }
    final Pattern pattern = Pattern.compile(REGEX);
    final Matcher matcher = pattern.matcher(fullPath);
    if (!matcher.find()) {
      return null;
    }
    Path path = new Path();
    path.fullPath = fullPath;
    path.segments = (fullPath.endsWith("/") ? fullPath.substring(0,fullPath.length()-1) : fullPath).split("/");
    path.fileName = matcher.group(2); // might be null because "/" is valid
    if (path.fileName == null) {
      path.type = Element.FOLDER;
    } else {
      if (path.fileName.endsWith("/")) {
        path.fileName = path.fileName.substring(0,path.fileName.length()-1);
        path.type = Element.FOLDER;
      } else {
        path.type = Element.FILE;
        path.extension = matcher.group(3);
      }
    }
    return path;
  }

  /**
   * Gets the parent element of the latest element in the path.
   * For example, for the path "foo/bar.txt" then "foo" would be the parent element.
   * If the path does not contain any segments, then the function returns "/".
   * @return The latest segment or null if there is none.
   */
  public String getParent() {
    if (isTravelingPath() && segments.length > 0) {
      // The segments will contain the name of the folder/file as the last item.
      // This is a problem as we want the parent of this element, so the segment **before** that.
      String parent = segments[segments.length-2];
      if (parent.length() == 0) {
        return "/";
      } else {
        return parent;
      }
    } else {
      return null;
    }
  }

  /**
   * @return The full path.
   */
  public String toString() {
    return fullPath;
  }

  /**
   * @return A boolean that indicates whether the path is absolute or not.
   */
  public boolean isAbsolute() {
    return fullPath.charAt(0) == '/';
  }

  /**
   * @return A boolean that indicates whether the path is leading to a folder or not.
   */
  public boolean isLeadingToFolder() {
    return type == Element.FOLDER;
  }

  /**
   * @return True if the path is leading to a file, false otherwise.
   */
  public boolean isFile() {
    return type == Element.FILE;
  }

  /**
   * @return True if the path contains "/", false if it's just the name of a file contained in the PWD.
   */
  public boolean isTravelingPath() {
    return fullPath.indexOf("/") >= 0;
  }

  /**
   * @return The name of the file + the extension.
   */
  public String getCompleteFileName() {
    if (fileName != null) {
      return fileName + (extension == null ? "" : extension);
    }
    return "";
  }
}