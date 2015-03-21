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
import java.util.StringJoiner;

public class CommandLineParser {

	private HashMap<String, String> parameterList = new HashMap<String, String>();

	// take the entire set of arguments as a string array
	// combine into one string, so that the flags can be parsed
	// together with the prameters later. This should allow "" within
	// arguments - e.g in file paths.
	public CommandLineParser(String[] input){

		StringJoiner sj = new StringJoiner("\t");
		for(String arg : input){
			sj.add(arg);
		}
		// expecting input to be:
		// -min 12 -max 15 ...
		parseInput(sj.toString);
	}

	public HashMap<String, String> getParameters(){
		return this.parameterList; 		
	}

	public String getParameter(String arg){
		return this.parameterList.get(arg);
	}

	public boolean contains(String arg){
		return this.parameterList.containKey(arg);
	}

	public void parseInput(String input){
		Scanner lineScanner = new Scanner(inputString);
	  lineScanner.useDelimiter("\t-"); // split on space followed by dash

	  while (lineScanner.hasNext()){

			Scanner parameterScanner = new Scanner(lineScanner.next());	 
			parameterScanner.useDelimiter("\t"); // now split on space

			if (parameterScanner.hasNext()){
				String arg = parameterScanner.next();
				String value = parameterScanner.next();
				parameterList.put(arg, value);
			}
	  }
	}
}