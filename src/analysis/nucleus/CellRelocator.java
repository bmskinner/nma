package analysis.nucleus;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import analysis.AnalysisDataset;
import analysis.AnalysisWorker;
import analysis.ProfileManager;
import components.Cell;
import components.CellCollection;
import components.CellularComponent;
import components.generic.BorderTag;
import components.generic.ProfileCollection;
import components.generic.ProfileType;
import components.generic.XYPoint;
import components.nuclear.NucleusBorderSegment;

/**
 * This class is used to parse an input file of original positions and 
 * nuclei, and select those nuclei from the current dataset into a new
 * child dataset.
 * @author ben
 *
 */
public class CellRelocator extends AnalysisWorker {
	
	private File inputFile = null;
	
	public CellRelocator(final AnalysisDataset dataset, final Logger logger, final File file){
		super(dataset, logger);
		this.inputFile = file;
		this.setProgressTotal(1);
	}
	
	@Override
	protected Boolean doInBackground() {
		
		boolean result = false;
		
		try {
			findCells();
			log(Level.FINE, "Completed remapping");
			publish(1);
			result = true;
		} catch (Exception e) {
			logError("Error selecting cells", e);
		}
		
		return result;
	}
	
	private void findCells() throws Exception {
		List<Cell> cells = parsePathList();
		
		log(Level.FINE, "Parsing complete");
		log(Level.FINE, "Found "+cells.size()+" cells");
		
		if( ! cells.isEmpty()){

			CellCollection c = new CellCollection(getDataset(), inputFile.getName());
			
			for(Cell cell : cells){
				c.addCell(cell);
			}
			
			/*
			 * Copy profile offsets and make the median profile
			 */
			ProfileManager.copyCollectionOffsets(getDataset().getCollection(), c);
			
			
			/*
			 * Add the new collection  to a dataset, and set as a child
			 */
			
			AnalysisDataset d = new AnalysisDataset(c);
			getDataset().addChildDataset(d);
			
		}
		
	}
	
	private List<Cell> parsePathList() throws Exception {
		log(Level.FINE, "Input file: "+inputFile.toString());
		
		List<Cell> cells = new ArrayList<Cell>();
		
	    Scanner scanner =  new Scanner(inputFile);

	    while (scanner.hasNextLine()){
	    	  
	        Cell cell = processLine(scanner.nextLine());
	        if(cell!=null){
	        	cells.add(cell);
	        }
	    }
	    log(Level.FINE, "All cells found");
	    scanner.close();
	    return cells;
	  }
	
	private Cell processLine(String line){
		log(Level.FINE, "Processing line: "+line);
		
		if(line.length()<5){
			// not enough room for a path and number, skip
			return null;
		}
		
		// Line format is FilePath\tPosition as x-y
		
		// get file name
		
		File file = getFile(line);
		if(! file.isFile() || ! file.exists()){
			log(Level.FINE, "File does not exist or is malformed: "+file.toString());
			return null;
		}
		
		
		// get position
		XYPoint com;
		
		try {
			com = getPosition(line);
		} catch (Exception e) {
			log(Level.SEVERE, line);
			log(Level.SEVERE, file.getAbsolutePath());
			logError("Cannot get position", e);
			return null;
		}
		
		// find the nucleus
		List<Cell> cells = this.getDataset().getCollection().getCells(file);
		
		for(Cell c : cells){
						
			if(c.getNucleus().containsOriginalPoint(com)){
				return new Cell(c);
			}
		}
		return null;
		
	}
	
	private File getFile(String line) {
		String[] array = line.split("\\t");
		File f = new File(array[0]);
		return f;
	}
	
	private XYPoint getPosition(String line) throws Exception {
		String[] array = line.split("\\t");
		String position = array[1];
		
		String[] posArray = position.split("-");
		
		double x = Double.parseDouble(posArray[0]);
		double y = Double.parseDouble(posArray[1]);
		return new XYPoint(x, y);
	}

}
