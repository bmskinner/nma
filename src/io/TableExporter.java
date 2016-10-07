/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
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
package io;

import ij.IJ;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import utility.ArrayConverter;

// this will take columns of data, and write them out to a specified folder
// Since the data is arbitrary and only for export, convert everything to strings. 
public class TableExporter {
	
	private File exportFolder;
	private Map<String, List<String>> columns = new LinkedHashMap<String, List<String>>();
	
	public TableExporter(File f){
		if(f.exists()){
			this.exportFolder = f;
		} else{
			throw new IllegalArgumentException("Specified folder ("+f.getAbsolutePath()+") does not exist");
		}
	}
	
	public TableExporter(String s){
		File f = new File(s);
		if(f.exists()){
			this.exportFolder = f;
		} else{
			throw new IllegalArgumentException("Specified folder ("+f.getAbsolutePath()+") does not exist");
		}
	}
	
	public void addColumn(String s, String[] values){
		if(s==null || values==null){
			throw new IllegalArgumentException("Column or array is null");
		}
		columns.put(s, Arrays.asList( values));
	}
	
	public void addColumn(String s, double[] array){
		if(s==null || array==null){
			throw new IllegalArgumentException("Column or array is null");
		}
		
		String[] values = new ArrayConverter(array).toStringArray();
		
		this.addColumn(s, values);
	}
	
	public void addColumn(String s, Integer[] array){
		if(s==null || array==null){
			throw new IllegalArgumentException("Column or array is null");
		}
		String[] values = new ArrayConverter(array).toStringArray();
		this.addColumn(s, values);
	}
	
	public void addColumn(String s, int[] array){
		if(s==null || array==null){
			throw new IllegalArgumentException("Column or array is null");
		}
		String[] values = new ArrayConverter(array).toStringArray();
		this.addColumn(s, values);
	}
	
	public void addColumnHeading(String s){
		if(s==null){
			throw new IllegalArgumentException("Column heading is null");
		}
		if(!columns.containsKey(s)){
			List<String> values = new ArrayList<String>();
			columns.put(s,  values);
		}else{
			throw new IllegalArgumentException("Specified column ("+s+") already exists");
		}
	}
	
	public void addRow(String column, String value){
		if(column==null || value==null){
			throw new IllegalArgumentException("Column or value is null");
		}
		if(columns.containsKey(column)){
			List<String> values = columns.get(column);
			values.add(value);
		} else {
			throw new IllegalArgumentException("Specified column ("+column+") does not exist");
		}
	}
	public void addRow(String column, Double value){
		if(column==null || value==null){
			throw new IllegalArgumentException("Column or value is null");
		}
		if(columns.containsKey(column)){
			addRow(column, value.toString());
		} else {
			throw new IllegalArgumentException("Specified column ("+column+") does not exist");
		}
	}
	
	public void addRow(String column, Integer value){
		if(column==null || value==null){
			throw new IllegalArgumentException("Column or value is null");
		}
		if(columns.containsKey(column)){
			addRow(column, value.toString());
		} else {
			throw new IllegalArgumentException("Specified column ("+column+") does not exist");
		}
	}
	
	public String makeFile(String fileName){
		if(fileName==null){
			throw new IllegalArgumentException("Filename is null");
		}
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
	
	/**
	 * For debugging. Show everything in the logger
	 */
	public void print(){
		for(String s : columns.keySet()){
			IJ.log("    "+s);
			List<String> rows = columns.get(s);
			for(String row : rows){
				IJ.log("      "+row);
			}
		}
	}

}
