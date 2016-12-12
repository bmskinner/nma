/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
/*
  -----------------------
  COMMAND LINE PARSER FOR NO
  -----------------------
  This class contains the parsing system
  to allow the NO analysis to be invoked
  from CLI with different parameter
*/

package com.bmskinner.nuclear_morphology.utility;

import java.util.HashMap;
import java.util.Scanner;

public class CommandLineParser {

	private HashMap<String, String> parameterList = new HashMap<String, String>();

	// take the entire set of arguments as a string array
	// combine into one string, so that the flags can be parsed
	// together with the prameters later. This should allow "" within
	// arguments - e.g in file paths.
	public CommandLineParser(String[] input){

		StringBuilder sj = new StringBuilder();
		for(String arg : input){
			sj.append(arg+"\t");
		}
		// expecting input to be:
		// -min 12 -max 15 ...
		parseInput(sj.toString());
	}

	public HashMap<String, String> getParameters(){
		return this.parameterList; 		
	}

	public String getParameter(String arg){
		return this.parameterList.get(arg);
	}

	public boolean contains(String arg){
		return this.parameterList.containsKey(arg);
	}

	public void parseInput(String input){
		Scanner lineScanner = new Scanner(input);
		lineScanner.useDelimiter("\t-"); // split on space followed by dash

	  while (lineScanner.hasNext()){

			Scanner parameterScanner = new Scanner(lineScanner.next());	 
			parameterScanner.useDelimiter("\t"); // now split on space

			if (parameterScanner.hasNext()){
				String arg = parameterScanner.next();
				String value = parameterScanner.next();
				parameterList.put(arg, value);
			}
			parameterScanner.close();
	  }
	  lineScanner.close();
	}
}