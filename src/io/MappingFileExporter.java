package io;

import ij.IJ;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.UUID;

import utility.Constants;
import analysis.AnalysisDataset;
import components.Cell;
import components.CellularComponent;
import components.generic.XYPoint;

/**
 * Export the locations of the centre of mass of nuclei in a dataset
 * to a file
 * @author ben
 *
 */
public class MappingFileExporter {
	
	private static final String NEWLINE = System.getProperty("line.separator"); 
	
	public static boolean exportCellLocations(AnalysisDataset d){
		
		File exportFile = new File(d.getCollection().getOutputFolder()
				+File.separator
				+d.getName()
				+"."
				+Constants.LOC_FILE_EXTENSION);
		
//		IJ.log("Export to "+exportFile.getAbsolutePath());
//		
		if(exportFile.exists()){
			exportFile.delete();
		}
		
//		IJ.log("Making string");
		
		//
		
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
			return false;
		}
//		IJ.log("Exported");
		return true;
	}
	
	/**
	 * Add the cells from all child datasets recursively
	 * @param d
	 * @return
	 */
	private static String makeChildString(AnalysisDataset d){
		StringBuilder builder = new StringBuilder();
		
		for(AnalysisDataset child : d.getChildDatasets()){
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
	private static String makeDatasetCellsString(AnalysisDataset d){
		StringBuilder builder = new StringBuilder();
		
		for(Cell c : d.getCollection().getCells()){
			
//			IJ.log("Cell "+c.getNucleus().getNameAndNumber());
			
			double[] originalPosition = c.getNucleus().getPosition();

			XYPoint com = c.getNucleus().getCentreOfMass();
			
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
	private static String makeDatasetHeaderString(AnalysisDataset child, UUID parent){
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
