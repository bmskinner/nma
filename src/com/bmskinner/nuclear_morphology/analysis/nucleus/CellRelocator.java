package com.bmskinner.nuclear_morphology.analysis.nucleus;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;

import com.bmskinner.nuclear_morphology.analysis.AnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.ChildAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.VirtualCellCollection;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;

/**
 * This class is used to parse an input file of original positions and 
 * nuclei, and select those nuclei from the current dataset into a new
 * child dataset.
 * @author ben
 *
 */
public class CellRelocator extends AnalysisWorker {
	
	private File inputFile = null;
	
	public CellRelocator(final IAnalysisDataset dataset, final File file){
		super(dataset);
		this.inputFile = file;
		this.setProgressTotal(1);
	}
	
	@Override
	protected Boolean doInBackground() {
		
		boolean result = false;
		
		try {
			findCells();
			fine("Completed remapping");
			publish(1);
			result = true;
		} catch (Exception e) {
			warn("Error selecting cells");
			stack("Error selecting cells", e);
		}
		
		return result;
	}
	
	private void findCells() {
		Set<UUID> newDatasets;
		try {
			newDatasets = parsePathList();
		} catch (CellRelocationException e) {
//			warn("Error relocating cells");
			stack("Error relocating cells", e);
			return;
		}
		
		fine("Parsing complete");
		int newSize = newDatasets.size();
		fine( "Found "+newSize+" datasets in file");
		
		if( newDatasets.size()>0){
			
			try {
			
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
			} catch (ProfileException e) {
				warn("Unable to profile new collections");
				stack("Unable to profile new collections", e);
				return;
			}
		}
		
	}
	
	
	
	private Set<UUID> parsePathList() throws CellRelocationException {
		fine("Input file: "+inputFile.toString());
				
	    Scanner scanner;
		try {
			scanner = new Scanner(inputFile);
		} catch (FileNotFoundException e) {
			throw new CellRelocationException("Input file does not exist", e);
		}
	    
	    UUID   activeID   = null;
	    String activeName = null;
	    
	    Map<UUID, IAnalysisDataset> map = new HashMap<UUID, IAnalysisDataset>();

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
	    		
	    		if(getDataset().getUUID().equals(activeID) || getDataset().hasChild(activeID)){
	    			// the dataset already exists with this id - we must fail
	    			scanner.close();
	    			warn("Dataset in cell file already exists");
	    			warn("Cancelling relocation");
	    			throw new CellRelocationException("Dataset already exists");
	    		}
	    		
	    		continue;
	    	}
	    	
	    	if(line.startsWith("Name")){
	    		/*
	    		 * Name of new dataset
	    		 */
	    		
	    		activeName =  line.split("\\t")[1];
	    		
	    		ICellCollection c = new VirtualCellCollection(getDataset(), 
	    				  activeName, 
	    				  activeID);
	    		c.createProfileCollection();
	    		
	    		IAnalysisDataset d = new ChildAnalysisDataset(getDataset(), c);
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
	    	  
	        ICell cell = getCellFromLine(line);
	        if(cell!=null){
	        	map.get(activeID).getCollection().addCell(cell);
	        }
	    }
	    fine("All cells found");
	    scanner.close();
	    return map.keySet();
	  }
	
	
	
	private ICell getCellFromLine(String line){
		fine("Processing line: "+line);
		
		if(line.length()<5){
			// not enough room for a path and number, skip
			return null;
		}
		
		// Line format is FilePath\tPosition as x-y
		
		// get file name
		
		File file = getFile(line);
		if(! file.isFile() || ! file.exists()){
			
			// Get the image name and substitute the parent dataset path.
			File newFolder = getDataset().getCollection().getFolder();
			if(newFolder.exists()){
				fine("Updating folder to "+newFolder.getAbsolutePath());
				file = new File(newFolder+File.separator+file.getName());
				fine("Updating path to "+file);
			} else {
				fine("File does not exist or is malformed: "+file.toString());
				return null;
			}
			
//			
		}
		
		
		// get position
		IPoint com;
		
		try {
			com = getPosition(line);
		} catch (Exception e) {
			warn(line);
			warn(file.getAbsolutePath());
			stack("Cannot get position", e);
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
	private ICell copyCellFromRoot(File f, IPoint com){
		// find the nucleus
		Set<ICell> cells = this.getDataset().getCollection().getCells(f);

		for(ICell c : cells){

			if(c.getNucleus().containsOriginalPoint(com)){
//				return new DefaultCell(c);
				return c;
			}
		}
		return null;
	}
	
	private File getFile(String line) {
		String[] array = line.split("\\t");
		File f = new File(array[0]);
		return f;
	}
	
	private IPoint getPosition(String line) throws Exception {
		String[] array = line.split("\\t");
		String position = array[1];
		
		String[] posArray = position.split("-");
		
		double x = Double.parseDouble(posArray[0]);
		double y = Double.parseDouble(posArray[1]);
		return IPoint.makeNew(x, y);
	}
	
	public class CellRelocationException extends Exception {
		private static final long serialVersionUID = 1L;
		public CellRelocationException() { super(); }
		public CellRelocationException(String message) { super(message); }
		public CellRelocationException(String message, Throwable cause) { super(message, cause); }
		public CellRelocationException(Throwable cause) { super(cause); }
	}

}
