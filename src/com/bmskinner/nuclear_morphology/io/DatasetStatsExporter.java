package com.bmskinner.nuclear_morphology.io;

import java.io.File;
import java.util.List;

import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICytoplasm;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.IJ;

/**
 * Export all the stats from a dataset to a text file for downstream analysis
 * @author ben
 * @since 1.13.4
 *
 */
public class DatasetStatsExporter implements Exporter, Loggable {
	
//	private File exportFolder;
	private File exportFile;
	private static final String DEFAULT_MULTI_FILE_NAME = "Multiple_stats_export"+Exporter.TAB_FILE_EXTENSION;
	
	/**
	 * Create specifying the folder stats will be exported into
	 * @param folder
	 */
	public DatasetStatsExporter(File file){
		
		if(file.isDirectory()){
			file = new File(file, DEFAULT_MULTI_FILE_NAME);
		}
		exportFile = file;
		
		if(exportFile.exists()){
			exportFile.delete();
		}
		
	}
	
	/**
	 * Export stats from the dataset to a file
	 * @param d
	 */
	public void export(IAnalysisDataset d){
		
//		File exportFile = makeFile(d.getName());
		
		StringBuilder outLine = new StringBuilder();
		writeHeader(outLine);
		export(d, outLine, exportFile);
		IJ.append(  outLine.toString(), exportFile.getAbsolutePath());
		log("Exported stats to "+exportFile.getAbsolutePath());
	}
	
	/**
	 * Export stats from all datasets in the list to the same file
	 * @param list
	 */
	public void export(List<IAnalysisDataset> list){
		
//		File exportFile = makeFile(DEFAULT_MULTI_FILE_NAME);
		
		StringBuilder outLine = new StringBuilder();
		
//		NucleusType type = IAnalysisDataset.getBroadestNucleusType(list);

		
		writeHeader(outLine);
		
		for(IAnalysisDataset d : list){
			export(d, outLine, exportFile);
		}
		IJ.append(  outLine.toString(), exportFile.getAbsolutePath());
		log("Exported stats to "+exportFile.getAbsolutePath());
	}
	
	/**
	 * Write a column header line to the StringBuilder. Only nuclear stats for now
	 * @param outLine
	 */
	private void writeHeader(StringBuilder outLine){
		
		outLine.append("Dataset\tCellID\tComponent\tImage\tCentre_of_mass\t");
		
		for(PlottableStatistic s : PlottableStatistic.getNucleusStats()){
			outLine.append(s.label(MeasurementScale.PIXELS)+"\t");
			
			if(!s.isDimensionless() && !s.isAngle()){ // only give micron measurements when length or area
				outLine.append(s.label(MeasurementScale.MICRONS)+"\t");
			}
			
		}
		outLine.append(NEWLINE);
	}
	
	/**
	 * Test if the given component is present in the dataset
	 * @param d
	 * @param component
	 * @return
	 */
	private boolean hasComponent(IAnalysisDataset d, String component){
		
		if(CellularComponent.CYTOPLASM.equals(component)){
			return d.getCollection().getCells().stream().allMatch(  c -> c.hasCytoplasm()  );
		}
		
		if(CellularComponent.NUCLEUS.equals(component)){
			return d.getCollection().getCells().stream().allMatch(  c -> c.hasNucleus()  );
		}
		
		return false;
		
	}
	
	/**
	 * Write the dataset level info that will always be present	 * 
	 */
	private void writeDatasetHeader(){
		
		
	}
	
	public void export(IAnalysisDataset d, StringBuilder outLine, File exportFile){
		log("Exporting stats...");

		for(ICell cell : d.getCollection().getCells()){

//			if(cell.hasCytoplasm()){
//				
//				ICytoplasm c = cell.getCytoplasm();
//				outLine.append(d.getName()+"\t")
//					.append(cell.getId()+"\t")
//					.append("Cytoplasm\t")
//					.append(c.getSourceFileName()+"\t");
//				
//				appendNucleusStats(outLine, d, cell, c);
//				outLine.append(NEWLINE);
//			}
			
			if(cell.hasNucleus()){
				
				for(Nucleus n : cell.getNuclei()){
					
					outLine.append(d.getName()+"\t")
						.append(cell.getId()+"\t")
						.append("Nucleus_"+n.getNameAndNumber()+"\t")
						.append(n.getSourceFileName()+"\t")
						.append(n.getOriginalCentreOfMass().toString()+"\t");
					appendNucleusStats(outLine, d, cell, n);
					outLine.append(NEWLINE);
				}
				
			}
			
		}
	}
	
	private void appendNucleusStats(StringBuilder outLine, IAnalysisDataset d, ICell cell, CellularComponent c){
		
		for(PlottableStatistic s : PlottableStatistic.getNucleusStats()){
			double varP = 0;
			double varM = 0;
			
			if(s.equals(PlottableStatistic.VARIABILITY)){
				
				try {
					varP = d.getCollection().getNormalisedDifferenceToMedian(Tag.REFERENCE_POINT, cell);
					varM = varP;
				} catch (UnavailableBorderTagException e) {
					stack("Tag not present in component", e);
					varP = -1;
					varM = -1;
				}
			} else {
				varP = c.getStatistic(s, MeasurementScale.PIXELS);
				varM = c.getStatistic(s, MeasurementScale.MICRONS);
			}
			
			outLine.append(varP+"\t");
			if(!s.isDimensionless() && !s.isAngle()){
				outLine.append(varM+"\t");
			}
		}		
	}
	
//	private File makeFile(String fileName){
////		if(fileName==null){
////			throw new IllegalArgumentException("Filename is null");
////		}
////		File f = new File(exportFolder, fileName+TAB_FILE_EXTENSION);
//		if(exportFile.exists()){
//			exportFile.delete();
//		}
////		return f;
//	}
	

}
