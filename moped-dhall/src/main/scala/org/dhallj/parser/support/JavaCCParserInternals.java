package org.dhallj.parser.support;

import org.dhallj.core.Expr;

public class JavaCCParserInternals {
  public static Expr.Parsed parse(String input) throws JavaCCParserException {
    try {
      return new JavaCCParser(new StringProvider(input)).TOP_LEVEL();
    } catch (ParseException underlying) {
      String message = formatMessage(
        underlying.currentToken,
        underlying.expectedTokenSequences,
        underlying.tokenImage
      );
      
      throw new JavaCCParserException(message, underlying, 
        underlying.currentToken.beginLine,
        underlying.currentToken.beginColumn,
        underlying.currentToken.endLine,
        underlying.currentToken.endColumn
      );
    }
  }


  // NOTE(olafur): this code is copy-pasted from the auto-generated parser in Dhallj.
  public static String formatMessage(Token currentToken,
                                     int[][] expectedTokenSequences,
                                     String[] tokenImage) {
	  StringBuilder sb = new StringBuilder();
    StringBuffer expected = new StringBuffer();
    
    int maxSize = 0;
    java.util.TreeSet<String> sortedOptions = new java.util.TreeSet<String>();
    for (int i = 0; i < expectedTokenSequences.length; i++) {
      if (maxSize < expectedTokenSequences[i].length) {
        maxSize = expectedTokenSequences[i].length;
      }
      for (int j = 0; j < expectedTokenSequences[i].length; j++) {
    	  sortedOptions.add(tokenImage[expectedTokenSequences[i][j]]);
      }
    }
    
    boolean first = true;
    for (String option : sortedOptions) {
        if (first) {
          first = false;
        } else {
          expected.append(" ");
        }
        expected.append(option);
      }
    
    sb.append("Encountered unexpected token: ");
    
    Token tok = currentToken.next;
    for (int i = 0; i < maxSize; i++) {
      String tokenText = tok.image;
  	  String escapedTokenText = ParseException.add_escapes(tokenText);
      if (i != 0) {
      	sb.append(" ");
      }
      if (tok.kind == 0) {
      	sb.append(tokenImage[0]);
        break;
      }
      sb.append(" \"");
	  sb.append(escapedTokenText);
      sb.append("\"");
      sb.append(" " + tokenImage[tok.kind]);
      tok = tok.next;
    }
    
    if (expectedTokenSequences.length == 0) {
        // Nothing to add here
    } else {
    	int numExpectedTokens = expectedTokenSequences.length;
    	sb.append(". Was expecting"+ (numExpectedTokens == 1 ? ":" : " one of: "));
    	sb.append(expected);
    }
    
    return sb.toString();
  }
}
