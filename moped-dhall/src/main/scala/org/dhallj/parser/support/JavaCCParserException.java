package org.dhallj.parser.support;


public class JavaCCParserException extends Exception {
  
  public int startLine, startColumn, endLine, endColumn;
  public JavaCCParserException(String message, Throwable cause, int startLine, int startColumn, int endLine, int endColumn) {
    super(message, cause);
    this.startLine = startLine;
    this.startColumn = startColumn;
    this.endLine = endLine;
    this.endColumn = endColumn;
  }

}
