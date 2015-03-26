package no.export;

import ij.IJ;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import no.utility.Utils;

// this will take columns of data, and write them out to a specified folder
// Since the data is arbitrary and only for export, convert everything to strings. 
public class Logger {
	
	private File exportFolder;
	private Map<String, List<String>> columns = new LinkedHashMap<String, List<String>>();
	
	public Logger(File f){
		if(f.exists()){
			this.exportFolder = f;
		} else{
			throw new IllegalArgumentException("Specified folder ("+f.getAbsolutePath()+") does not exist");
		}
	}
	
	public Logger(String s){
		File f = new File(s);
		if(f.exists()){
			this.exportFolder = f;
		} else{
			throw new IllegalArgumentException("Specified folder ("+f.getAbsolutePath()+") does not exist");
		}
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
	
	public void addColumnHeading(String s){
		if(!columns.containsKey(s)){
			List<String> values = new ArrayList<String>();
			columns.put(s,  values);
		}else{
			throw new IllegalArgumentException("Specified column ("+s+") already exists");
		}
	}
	
	public void addRow(String column, String value){
		if(columns.containsKey(column)){
			List<String> values = columns.get(column);
			values.add(value);
		} else {
			throw new IllegalArgumentException("Specified column ("+column+") does not exist");
		}
	}
	public void addRow(String column, Double value){
		if(columns.containsKey(column)){
			addRow(column, value.toString());
		} else {
			throw new IllegalArgumentException("Specified column ("+column+") does not exist");
		}
	}
	
	public void addRow(String column, Integer value){
		if(columns.containsKey(column)){
			addRow(column, value.toString());
		} else {
			throw new IllegalArgumentException("Specified column ("+column+") does not exist");
		}
	}
	
	public String makeFile(String fileName){
		File f = new File(this.exportFolder.getAbsolutePath()+File.separator+fileName+".txt");
		if(f.exists()){
			f.delete();
		}
		return f.getAbsolutePath();
	}
	
	public int length(){
		int size = 0;
		for(String heading : columns.keySet()){
			size = columns.get(heading).size();
		}
		return size;
	}

	public void export(String fileName){
		String exportFile = makeFile(fileName);

		StringBuilder outLine = new StringBuilder();

		for(String heading : columns.keySet()){
			outLine.append(heading+"\t");
		}
		outLine.append("\r\n");


		for(int i=0;i<this.length();i++){
			
			for(String heading : columns.keySet()){
				List<String> column = columns.get(heading);
				outLine.append(column.get(i)+"\t");
			}
			outLine.append("\r\n");
		}
		IJ.append(  outLine.toString(), exportFile);
	}

}
