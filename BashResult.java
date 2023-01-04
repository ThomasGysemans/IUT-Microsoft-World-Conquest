class BashResult {
  BashError error;
  String output;
  String failingCommand;

  BashResult(String o) {
    output = o;
  }

  BashResult(BashError e, String f) {
    error = e;
    failingCommand = f;
  }

  // It's possible to have an empty output,
  // with commands like `cd` for example.
  BashResult() {  }

  public String toString() {
    return toString(false);
  }

  public String toString(boolean fixCarriageReturn) {
    String r = output == null ? (error == null ? "" : error.explain(failingCommand)) : output;
    if (fixCarriageReturn) {
      return r.replaceAll("\\n","\r\n");
    } else {
      return r;
    }
  }
}