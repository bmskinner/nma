package io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

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
	
	public static void exportCellLocations(AnalysisDataset d){
		
		File exportFile = new File(d.getCollection().getOutputFolder()+File.separator+d.getName()+".txt");
		
		if(exportFile.exists()){
			exportFile.delete();
		}
		
		StringBuilder builder = new StringBuilder();
		
		for(Cell c : d.getCollection().getCells()){
			double[] originalPosition = c.getNucleus().getPosition();

			XYPoint p = c.getNucleus().getCentreOfMass();
			
			double x = p.getX()+originalPosition[CellularComponent.X_BASE];
			
			double y = p.getY()+originalPosition[CellularComponent.Y_BASE];
			
			builder.append(c.getNucleus().getOriginalImagePath()+"\t"+x+"-"+y+System.lineSeparator());
			
		}
		
		export(builder.toString(), exportFile);
	}
	
	private static void export(String s, File f){

		PrintWriter out;
		try {

			out = new PrintWriter(f);
			out.println(s);
			out.close();
		} catch (FileNotFoundException e) {

		}
	}

}
