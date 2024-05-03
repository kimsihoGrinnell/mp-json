import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.ArrayList;

/**
 * Utilities for our simple implementation of JSON.
 */
public class JSON {
  // +---------------+-----------------------------------------------
  // | Static fields |
  // +---------------+

  /**
   * The current position in the input.
   */
  static int pos;

  // +----------------+----------------------------------------------
  // | Static methods |
  // +----------------+

  /**
   * Parse a string into JSON.
   */
  /*
  public static JSONValue parse(String source) throws ParseException, IOException {
    return parse(new StringReader(source));
  } // parse(String)
  */
  /**
   * Parse a file into JSON.
   */
  /*
  public static JSONValue parseFile(String filename) throws ParseException, IOException {
    FileReader reader = new FileReader(filename);
    JSONValue result = parse(reader);
    reader.close();
    return result;
  } // parseFile(String)
  */


  /**
   * Parse JSON from a reader.
   */
  public static JSONValue parse(Reader source) throws ParseException, IOException {
    pos = 0;
    JSONValue result = parseKernel(source);
    if (-1 != skipWhitespace(source)) {
      throw new ParseException("Characters remain at end", pos);
    }
    return result;
  } // parse(Reader)

  // +---------------+-----------------------------------------------
  // | Local helpers |
  // +---------------+

  /**
   * Parse JSON from a reader, keeping track of the current position
   * check for opening characters for a JSONValue
   * @throws ParseException 
   */

  static JSONValue parseKernel(Reader source) throws ParseException, IOException {
    int ch;
    ch = skipWhitespace(source);
    if (-1 == ch) {
      throw new ParseException("Unexpected end of file", pos);
    }

    if (ch == Character.valueOf('"')) {
      return parseString(source);
    } else if (ch >= Character.valueOf('0') && ch <= Character.valueOf('9')) {
      return parseNumber(source, ch);
    } else if (ch == Character.valueOf('{')) {
      return parseHash(source); 
    } else if (ch == Character.valueOf('-')){
      return parseNegNum(source, ch);
    } else if (ch == Character.valueOf('f') || ch == Character.valueOf('t') || ch == Character.valueOf('n')) {
      return parseConstant(source, ch);
    } else if (ch == Character.valueOf('[')) {
      return parseArray(source);
    } else {
      throw new ParseException("Illegal opening" + (char)ch, pos);
    }
  } // parseKernel


  public static JSONHash parseHash(Reader source) throws IOException, ParseException {
    int ch;
    JSONHash hash = new JSONHash();
    // continue to parse key value pairs until encounter end other than a comma
    do {
      JSONValue key = parseKernel(source);
      if (!(key instanceof JSONString)) {
        throw new ParseException("Illegal key value" + key, pos);
      }
      ch = skipWhitespace(source);
      if (ch != Character.valueOf(':')) {
        throw new ParseException("Missing colon", pos);
      }
      JSONValue value = parseKernel(source);
      hash.set((JSONString)key, value);
      ch = skipWhitespace(source);
    } while (ch == Character.valueOf(','));
    // check if it's legal end
    if (ch != Character.valueOf('}')) {
      throw new ParseException("Unexpected end " + (char)ch, pos);
    }
    return hash;
  }

  public static JSONArray parseArray(Reader source) throws IOException, ParseException {
    int ch;
    JSONArray arr = new JSONArray();
    // continue to parse key value pairs until encounter end other than a comma
    do {
      JSONValue value = parseKernel(source);
      arr.add(value);
      ch = skipWhitespace(source);
    } while (ch == Character.valueOf(','));
    // check if it's legal end
    if (ch != Character.valueOf(']')) {
      throw new ParseException("Unexpected end " + (char)ch, pos);
    }
    return arr;
  }

  public static JSONString parseString(Reader source) throws IOException, ParseException {
    String str = "";
    int ch = skipWhitespace(source);
    while (ch != Character.valueOf('"') && ch != -1) {
      if (ch == Character.valueOf('\\')) {
        ch = source.read();
        if (ch == 'n') {
          str += '\n';
        } else if (ch == '"') {
          str += '\"';
        } else {
          str += (char)ch;
        }
        ch = source.read();
        continue;
      }
      str += (char) ch;
      ch = source.read(); 
    }
    if (ch == -1) {
      throw new ParseException("Unexpected end", pos);
    }
    return new JSONString(str);
  }


  public static JSONValue parseNegNum(Reader source, int ch) throws IOException, ParseException {
    JSONValue num = parseNumber(source, ch);
    String numStr = "-" + num.toString();
    System.out.println(num.toString());
    if (num instanceof JSONInteger) {
      return new JSONInteger(numStr);
    } else if (num instanceof JSONReal) {
      return new JSONReal(numStr);
    } else {
      throw new ParseException("Unexpected output while parsing negative number", pos);
    }
  }
  
  public static JSONConstant parseConstant(Reader source, int ch) throws IOException, ParseException {
    String str = "" + (char) ch;
    ch = source.read();
    while (ch >= Character.valueOf('a') && ch <= Character.valueOf('z')) {
      str += (char) ch;
      source.mark(1);
      ch = source.read();
    }
    source.reset();
    System.out.println(str);
      
    // Check if string is indicating true/false/null
    if (str.equals("null")) {
      return JSONConstant.NULL;
    } else if (str.equals("true")) {
      return JSONConstant.TRUE;
    } else if (str.equals("false")) {
      return JSONConstant.FALSE;
    } else {
      throw new ParseException("Illegal constant value", pos);
    }
}

  public static JSONValue parseNumber(Reader source, int ch) throws IOException, ParseException {
    String numStr = String.valueOf((char)ch);
    // bad solution
    source.mark(1);
    ch = source.read();
    while (ch >= Character.valueOf('0') && ch <= Character.valueOf('9')) {
      numStr += (char)ch;
      source.mark(1);
      ch = source.read();
    }
    if (ch == Character.valueOf('.')) {
      return parseReal(source, numStr);
    }
    source.reset();
    // if it's not digit or decimal, throw exception
    if (ch >= Character.valueOf('0') && ch <= Character.valueOf('9') && ch != Character.valueOf('.')) {
      throw new ParseException("Illegal character " + (char)ch, pos);
    }
    // if we come to end of file, throw exception
    if (ch == -1) {
      throw new ParseException("Unexpected end", pos);
    }
    return new JSONInteger(new BigInteger(numStr.trim()));
  }

  public static JSONReal parseReal(Reader source, String numStr) throws IOException, ParseException {
    int ch = source.read();
    numStr += '.';
    while (ch >= Character.valueOf('0') && ch <= Character.valueOf('9')) {
      numStr += (char)ch;
      source.mark(1);
      ch = source.read();
    }

    source.reset();
    return new JSONReal(numStr);

    // Comma case
    // if (ch == Character.valueOf(',') || ch == Character.valueOf(']') || ch == Character.valueOf('}') || ch == Character) {
    //   return new JSONReal(numStr);
    // } else {
    //   throw new ParseException("Illegal character " + (char)ch, pos);
    // }
  }
  /**
   * Get the next character from source, skipping over whitespace.
   */
  static int skipWhitespace(Reader source) throws IOException {
    int ch;
    do {
      ch = source.read();
      ++pos;
    } while (isWhitespace(ch));
    return ch;
  } // skipWhitespace(Reader)

  /**
   * Determine if a character is JSON whitespace (newline, carriage return,
   * space, or tab).
   */
  static boolean isWhitespace(int ch) {
    return (' ' == ch) || ('\n' == ch) || ('\r' == ch) || ('\t' == ch);
  } // isWhiteSpace(int)

} // class JSON
