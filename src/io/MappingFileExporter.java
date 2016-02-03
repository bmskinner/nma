package io;

import ij.IJ;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

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
		
		StringBuilder builder = new StringBuilder();
		final String newline = System.getProperty("line.separator"); 
		
		for(Cell c : d.getCollection().getCells()){
			
//			IJ.log("Cell "+c.getNucleus().getNameAndNumber());
			
			double[] originalPosition = c.getNucleus().getPosition();

			XYPoint com = c.getNucleus().getCentreOfMass();
			
			double x = com.getX()+originalPosition[CellularComponent.X_BASE];
			double y = com.getY()+originalPosition[CellularComponent.Y_BASE];
			
			
			IJ.log("   Found position: "+x+"-"+y);
			
			try{
			
				if(c.getNucleus().getSourceFile()!=null){
					
//					IJ.log("   Found nucleus source image: "+c.getNucleus().getSourceFile().getAbsolutePath());
				
					builder.append( c.getNucleus().getSourceFile().getAbsolutePath() );
					builder.append( "\t" );
					builder.append( x );
					builder.append( "-" );
					builder.append( y );
					
//					IJ.log("   Added all but newline");
					
					builder.append( newline );
					

//					IJ.log("   Appended position");
				} else {
//					IJ.log("   Cannot get nucleus image path");
				}
			} catch(Exception e){
//				IJ.log("Cannot make line: "+e.getMessage());
				return false;
			}
			
		}
		
//		IJ.log("Made string");
		
		try {
			export(builder.toString(), exportFile);
		} catch (FileNotFoundException e) {
			return false;
		}
//		IJ.log("Exported");
		return true;
	}
	
	private static void export(String s, File f) throws FileNotFoundException{

		PrintWriter out;
		out = new PrintWriter(f);
		out.print(s);
		out.close();
		
	}

}
