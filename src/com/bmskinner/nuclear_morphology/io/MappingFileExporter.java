/*******************************************************************************
 *  	Copyright (C) 2015, 2016 Ben Skinner
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
 *     GNU General Public License for more details. Gluten-free. May contain 
 *     traces of LDL asbestos. Avoid children using heavy machinery while under the
 *     influence of alcohol.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package com.bmskinner.nuclear_morphology.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.UUID;

import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.gui.GlobalOptions;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Export the locations of the centre of mass of nuclei in a dataset
 * to a file
 * @author ben
 *
 */
public class MappingFileExporter implements Exporter, Loggable {
		
	public boolean exportCellLocations(IAnalysisDataset d){
		
		String fileName = d.getName()+"."+Importer.LOC_FILE_EXTENSION;
		File exportFile = new File(d.getCollection().getOutputFolder(), fileName);

		
		
		if( ! exportFile.getParentFile().isDirectory()){
			// the desired output folder does not exist
			warn("The intended export folder does not exist");
			
			File folder = GlobalOptions.getInstance().getDefaultDir();
//			warn("Defaulting to: "+folder.getAbsolutePath());
			exportFile = new File(folder, fileName);
			warn("Exporting to "+exportFile.getAbsolutePath());
		}
		
		if(exportFile.exists()){
			exportFile.delete();
		}
		
		StringBuilder builder = new StringBuilder();
		
		/*
		 * Add the cells from the root dataset
		 */
		builder.append(makeDatasetHeaderString(d, d.getUUID()));
		builder.append(makeDatasetCellsString(d));
		
		/*
		 * Add cells from all child datasets
		 */
		builder.append(makeChildString(d));

		
		try {
			export(builder.toString(), exportFile);
		} catch (FileNotFoundException e) {
			stack(e);
			return false;
		}
		return true;
	}
	
	/**
	 * Add the cells from all child datasets recursively
	 * @param d
	 * @return
	 */
	private static String makeChildString(IAnalysisDataset d){
		StringBuilder builder = new StringBuilder();
		
		for(IAnalysisDataset child : d.getChildDatasets()){
			builder.append( makeDatasetHeaderString(child, d.getUUID() ) );
			builder.append( makeDatasetCellsString( child ) );
			
			if(child.hasChildren()){
				builder.append(  makeChildString(child) );
			}
		}
		
		return builder.toString();
	}
	
	/**
	 * Add the cell positions and image names
	 * @param d
	 * @return
	 */
	private static String makeDatasetCellsString(IAnalysisDataset d){
		StringBuilder builder = new StringBuilder();
		
		for(ICell c : d.getCollection().getCells()){
			
//			IJ.log("Cell "+c.getNucleus().getNameAndNumber());
			
			int[] originalPosition = c.getNucleus().getPosition();

			IPoint com = c.getNucleus().getCentreOfMass();
			
			double x = com.getX()+originalPosition[CellularComponent.X_BASE];
			double y = com.getY()+originalPosition[CellularComponent.Y_BASE];
			
			
//			IJ.log("   Found position: "+x+"-"+y);
			
			try{
			
				if(c.getNucleus().getSourceFile()!=null){
					
//					IJ.log("   Found nucleus source image: "+c.getNucleus().getSourceFile().getAbsolutePath());
				
					builder.append( c.getNucleus().getSourceFile().getAbsolutePath() );
					builder.append( "\t" );
					builder.append( x );
					builder.append( "-" );
					builder.append( y );
					
//					IJ.log("   Added all but newline");
					
					builder.append( NEWLINE );
					

//					IJ.log("   Appended position");
				} else {
//					IJ.log("   Cannot get nucleus image path");
				}
			} catch(Exception e){
//				IJ.log("Cannot make line: "+e.getMessage());
				return null;
			}
			
		}
		return builder.toString();
	}
	
	/**
	 * Add the dataset id, name, and parent
	 * @param child
	 * @param parent
	 * @return
	 */
	private static String makeDatasetHeaderString(IAnalysisDataset child, UUID parent){
		StringBuilder builder = new StringBuilder();

		builder.append("UUID\t");
		builder.append(child.getUUID().toString());
		builder.append(NEWLINE);
		
		builder.append("Name\t");
		builder.append(child.getName());
		builder.append(NEWLINE);
		
		builder.append("ChildOf\t");
		builder.append(parent.toString());
		builder.append(NEWLINE);
		return builder.toString();
	}
	
	private static void export(String s, File f) throws FileNotFoundException{

		PrintWriter out;
		out = new PrintWriter(f);
		out.print(s);
		out.close();
		
	}

}
