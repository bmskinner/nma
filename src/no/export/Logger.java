package no.export;

import ij.IJ;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import no.utility.Utils;

// this will take columns of data, and write them out to a specified folder
public class Logger {
	
	private File exportFolder;
	private Map<String, List<String>> columns = new LinkedHashMap<String, List<String>>();
	
	public Logger(File f){
		this.exportFolder = f;
	}
	
	public void addColumn(String s, String[] values){
		columns.put(s, Arrays.asList( values));
	}
	
	public void addColumn(String s, double[] array){
		String[] values = Utils.getStringFromDouble(array);
		this.addColumn(s, values);
	}
	
	public void addColumn(String s, int[] array){
		String[] values = Utils.getStringFromInt(array);
		this.addColumn(s, values);
	}
	
	public String makeFile(String fileName){
		File f = new File(this.exportFolder.getAbsolutePath()+File.separator+fileName);
		if(f.exists()){
			f.delete();
		}
		return f.getAbsolutePath();
	}

	public void export(String fileName){
		String exportFile = makeFile(fileName);

		StringBuilder outLine = new StringBuilder();

		for(String heading : columns.keySet()){
			outLine.append(heading+"\t");
		}
		outLine.append("\r\n");


		for(int i=0;i<columns.get(0).size();i++){
			
			for(String heading : columns.keySet()){
				List<String> column = columns.get(heading);
				outLine.append(column.get(i)+"\t");
			}
			outLine.append("\r\n");
		}
		IJ.append(  outLine.toString(), exportFile);
	}

}
