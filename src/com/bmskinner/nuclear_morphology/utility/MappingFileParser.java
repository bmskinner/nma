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
package com.bmskinner.nuclear_morphology.utility;

import ij.IJ;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MappingFileParser {

	static List<String> result = new ArrayList<String>(0);

	public static List<String> parse(File file) {
		try{
		Scanner scanner =  new Scanner(file);
		int i=0;
		while (scanner.hasNextLine()){
			if(i>0){
				processLine(scanner.nextLine());
			}
			i++;
		}
		scanner.close();
		
		} catch(Exception e){
			IJ.log("Error parsing mapping file");
		}
		return result;
	}


	private static void processLine(String line){
		// IJ.log("Processing line: "+line);
		//use a second Scanner to parse the content of each line 
		Scanner scanner = new Scanner(line);
		scanner.useDelimiter("\t");
		String path = "";

		if (scanner.hasNext()){
			path     = scanner.next();
		}
		if(path.equals("ID")){
			scanner.close();
			return;
		}

		Scanner positionScanner = new Scanner(path);
		String nucleusPath = null;
		String nucleusNumber = null;
		positionScanner.useDelimiter("-");
		if (positionScanner.hasNext()){
			nucleusPath = positionScanner.next();
			nucleusNumber = positionScanner.next();
		}
		positionScanner.close();

		result.add(nucleusPath+"\t"+nucleusNumber);
		scanner.close();
	}

}
