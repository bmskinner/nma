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
	}
	
	@Override
	protected Boolean doInBackground() {
		
		boolean result = false;
		
		try {
			findCells();
			result = true;
		} catch (Exception e) {
			logError("Error selecting cells", e);
		}
		
		return result;
	}
	
	private void findCells() throws Exception {
		List<Cell> cells = parsePathList();
		
		if( ! cells.isEmpty()){
			
			log(Level.FINE, "Found "+cells.size()+" cells");
			
			CellCollection c = new CellCollection(getDataset(), "Subset");
			
			for(Cell cell : cells){
				c.addCell(cell);
			}
			
			/*
			 * Copy profile offsets and make the median profile
			 */
			
			ProfileCollection pc = getDataset().getCollection().getProfileCollection(ProfileType.REGULAR);
			List<NucleusBorderSegment> segments = pc.getSegments(BorderTag.REFERENCE_POINT);
			ProfileCollection newProfileCollection = new ProfileCollection();
			newProfileCollection.createProfileAggregate(getDataset().getCollection(), 
					ProfileType.REGULAR, 
					(int) getDataset().getCollection().getMedianArrayLength());
			
			for(BorderTag key : pc.getOffsetKeys()){
				newProfileCollection.addOffset(key, pc.getOffset(key));
			}
			newProfileCollection.addSegments(BorderTag.REFERENCE_POINT, segments);

			c.setProfileCollection(ProfileType.REGULAR, newProfileCollection);
			
			
			/*
			 * Add the new collection  to a dataset, and set as a child
			 */
			
			AnalysisDataset d = new AnalysisDataset(c);
			getDataset().addChildDataset(d);
			
		}
		
	}
	
	private List<Cell> parsePathList() throws IOException {
		
		List<Cell> cells = new ArrayList<Cell>();
		
	    Scanner scanner =  new Scanner(inputFile);
	    int i=0;
	    while (scanner.hasNextLine()){
	      if(i>0){ // ignore first line
	    	  
	        Cell cell = processLine(scanner.nextLine());
	        if(cell!=null){
	        	cells.add(cell);
	        }
	      }
	      i++;
	    }
	    scanner.close();
	    return cells;
	  }
	
	private Cell processLine(String line){
		log(Level.FINE, "Processing line: "+line);
		
		// Line format is FilePath\tPosition as x-y
		
		// get file name
		File file = getFile(line);
		
		// get position
		XYPoint com = getPosition(line);
		
		// find the nucleus
		List<Cell> cells = this.getDataset().getCollection().getCells(file);
		
		for(Cell c : cells){
			
			double[] originalPosition = c.getNucleus().getPosition();
			XYPoint p = c.getNucleus().getCentreOfMass();
			
			XYPoint offset = new XYPoint(p.getX()+originalPosition[CellularComponent.X_BASE],
					p.getY()+originalPosition[CellularComponent.Y_BASE]);
			
			if(offset.equals(com)){
				return new Cell(c);
			}
		}
		return null;
		
	}
	
	private File getFile(String line){
		String[] array = line.split("\t");
		File f = new File(array[0]);
		return f;
	}
	
	private XYPoint getPosition(String line){
		String[] array = line.split("\t");
		String position = array[1];
		
		String[] posArray = position.split("-");
		
		double x = Double.parseDouble(posArray[0]);
		double y = Double.parseDouble(posArray[1]);
		return new XYPoint(x, y);
	}

}
