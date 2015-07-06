package utility;

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
