package com.bmskinner.nuclear_morphology.io;

import ij.IJ;

import java.io.File;
import java.util.List;

import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICytoplasm;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Export all the stats from a dataset to a text file for downstream analysis
 * @author ben
 * @since 1.13.4
 *
 */
public class DatasetStatsExporter implements Exporter, Loggable {
	
	private File exportFolder;
	private static final String DEFAULT_MULTI_FILE_NAME = "Multiple_stats_export";
	
	/**
	 * Create specifying the folder stats will be exported into
	 * @param folder
	 */
	public DatasetStatsExporter(File folder){
		
		if(folder.exists()){
			this.exportFolder = folder;
		} else{
			throw new IllegalArgumentException("Specified folder ("+folder.getAbsolutePath()+") does not exist");
		}
	}
	
	/**
	 * Export stats from the dataset to a file
	 * @param d
	 */
	public void export(IAnalysisDataset d){
		
		File exportFile = makeFile(d.getName());
		
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
		
		File exportFile = makeFile(DEFAULT_MULTI_FILE_NAME);
		
		StringBuilder outLine = new StringBuilder();
		
		writeHeader(outLine);
		
		for(IAnalysisDataset d : list){
			export(d, outLine, exportFile);
		}
		IJ.append(  outLine.toString(), exportFile.getAbsolutePath());
		log("Exported stats to "+exportFile.getAbsolutePath());
	}
	
	/**
	 * Write a column header line to the StringBuilder
	 * @param outLine
	 */
	private void writeHeader(StringBuilder outLine){
		
		outLine.append("Dataset\tCellID\tComponent\tImage\t");
		
		for(PlottableStatistic s : PlottableStatistic.getNucleusStats()){
			outLine.append(s+"\t");
		}
		outLine.append(NEWLINE);
	}
	
	public void export(IAnalysisDataset d, StringBuilder outLine, File exportFile){
		log("Exporting stats...");

		for(ICell cell : d.getCollection().getCells()){

			if(cell.hasCytoplasm()){
				
				ICytoplasm c = cell.getCytoplasm();
				outLine.append(d.getName()+"\t");
				outLine.append(cell.getId()+"\t");
				outLine.append("Cytoplasm\t");
				outLine.append(c.getSourceFileName()+"\t");
				appendNucleusStats(outLine, d, cell, c);
				outLine.append(NEWLINE);
			}
			
			if(cell.hasNucleus()){
				
				for(Nucleus n : cell.getNuclei()){
					
					outLine.append(d.getName()+"\t");
					outLine.append(cell.getId()+"\t");
					outLine.append("Nucleus_"+n.getNameAndNumber()+"\t");
					outLine.append(n.getSourceFileName()+"\t");
					appendNucleusStats(outLine, d, cell, n);
					outLine.append(NEWLINE);
				}
				
			}
			
		}
	}
	
	private void appendNucleusStats(StringBuilder outLine, IAnalysisDataset d, ICell cell, CellularComponent c){
		
		for(PlottableStatistic s : PlottableStatistic.getNucleusStats()){
			double var = 0;
			
			if(s.equals(PlottableStatistic.VARIABILITY)){
				
				try {
					var = d.getCollection().getNormalisedDifferenceToMedian(Tag.REFERENCE_POINT, cell);
				} catch (UnavailableBorderTagException e) {
					stack("Tag not present in component", e);
					var = -1;
				}
			} else {
				var = c.getStatistic(s);
				
			}
			
			outLine.append(var+"\t");
		}		
	}
	
	private File makeFile(String fileName){
		if(fileName==null){
			throw new IllegalArgumentException("Filename is null");
		}
		File f = new File(exportFolder, fileName+TAB_FILE_EXTENSION);
		if(f.exists()){
			f.delete();
		}
		return f;
	}
	

}
