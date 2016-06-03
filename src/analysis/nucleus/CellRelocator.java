package analysis.nucleus;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import utility.Constants;
import analysis.AnalysisDataset;
import analysis.AnalysisWorker;
import analysis.profiles.ProfileManager;
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
	
	public CellRelocator(final AnalysisDataset dataset, final File file){
		super(dataset);
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
		Set<UUID> newDatasets = parsePathList();
		
		log(Level.FINE, "Parsing complete");
		int newSize = newDatasets.size();
		log(Level.FINE, "Found "+newSize+" datasets in file");
		
		if( newDatasets.size()>0){
			
			for(UUID id : newDatasets){
				
				if( ! id.equals(getDataset().getUUID())){
					/*
					 * Copy profile offsets and make the median profile
					 */
					getDataset().getCollection()
						.getProfileManager()
						.copyCollectionOffsets(getDataset().getChildDataset(id).getCollection());
					
				}
			}			
		}
		
	}
	
	
	
	private Set<UUID> parsePathList() throws Exception {
		log(Level.FINE, "Input file: "+inputFile.toString());
		
//		List<Cell> cells = new ArrayList<Cell>();
		
	    Scanner scanner =  new Scanner(inputFile);
	    
	    UUID   activeID   = null;
	    String activeName = null;
	    
	    Map<UUID, AnalysisDataset> map = new HashMap<UUID, AnalysisDataset>();
//	    map.put(getDataset().getUUID(), getDataset());

	    while (scanner.hasNextLine()){
	    	
	    	/*
	    	 * File format:
	    	 * UUID	57320dbb-bcde-49e3-ba31-5b76329356fe
			 * Name	Testing
             * ChildOf	57320dbb-bcde-49e3-ba31-5b76329356fe
             * J:\Protocols\Scripts and macros\Testing\s75.tiff	602.0585522824504-386.38060239306236
	    	 */
	    	
	    	String line = scanner.nextLine();
	    	if(line.startsWith("UUID")){
	    		
	    		/*
	    		 * New dataset found
	    		 */
	    		activeID = UUID.fromString( line.split("\\t")[1] );
	    		continue;
	    	}
	    	
	    	if(line.startsWith("Name")){
	    		/*
	    		 * Name of new dataset
	    		 */
	    		
	    		activeName =  line.split("\\t")[1];
	    		CellCollection c = new CellCollection(getDataset().getCollection().getFolder(), 
	    				getDataset().getCollection().getOutputFolderName(), 
	    				  activeName, 
	    				  getDataset().getCollection().getNucleusType(),
	    				  activeID);
	    		AnalysisDataset d = new AnalysisDataset(c);
	    		d.setAnalysisOptions(getDataset().getAnalysisOptions());
	    		map.put(activeID, d);
	    		continue;
	    	}
	    	
	    	if(line.startsWith("ChildOf")){
	    		/*
	    		 * Parent dataset
	    		 */
	    		UUID parentID = UUID.fromString( line.split("\\t")[1] );
	    		
	    		if(parentID.equals(activeID)){
	    			getDataset().addChildDataset(map.get(activeID));
	    		} else {
	    			map.get(parentID).addChildDataset(map.get(activeID));
	    		}
	    		continue;
	    	}
	    	
	    	/*
    		 * No header line, must be a cell for the current dataset
    		 */
	    	  
	        Cell cell = getCellFromLine(line);
	        if(cell!=null){
	        	map.get(activeID).getCollection().addCell(cell);
//	        	cells.add(cell);
	        }
	    }
	    log(Level.FINE, "All cells found");
	    scanner.close();
	    return map.keySet();
	  }
	
	
	
	private Cell getCellFromLine(String line){
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
		
		return copyCellFromRoot(file, com);
		
	}
	
	/**
	 * Make a new cell based on the cell in the root dataset with
	 * the given location in an image file
	 * @param f
	 * @param com
	 * @return
	 */
	private Cell copyCellFromRoot(File f, XYPoint com){
		// find the nucleus
		List<Cell> cells = this.getDataset().getCollection().getCells(f);

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
