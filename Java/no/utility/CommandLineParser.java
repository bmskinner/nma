  /*
  -----------------------
  COMMAND LINE PARSER FOR NO
  -----------------------
  This class contains the parsing system
  to allow the NO analysis to be invoked
  from CLI with different parameter
 */

package no.utility;

import java.util.*;
import java.util.Scanner;

public class CommandLineParser {

	private HashMap<String, String> parameterList = new HashMap<String, String>();

	// take the entire set of arguments as one string, and separate after
	public CommandLineParser(String input){
		// expecting input to be:
		// -min 12 -max 15 ...
		parseInput(string);
	}

	public HashMap<String, String> getParameters(){
		return this.parameterList; 		
	}

	public parseInput(String input){
		Scanner lineScanner = new Scanner(inputString);
	  lineScanner.useDelimiter("\s-"); // split on space followed by dash

	  while (lineScanner.hasNext()){

			Scanner parameterScanner = new Scanner(lineScanner.next());	 
			parameterScanner.useDelimiter("\s"); // now split on space

			if (parameterScanner.hasNext()){
				String arg = parameterScanner.next();
				String value = parameterScanner.next();
				parameterList.put(arg, value);
			}
	  }
	}
}