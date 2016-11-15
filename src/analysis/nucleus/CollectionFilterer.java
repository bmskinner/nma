/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
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
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package analysis.nucleus;

import stats.NucleusStatistic;
import components.ICell;
import components.ICellCollection;
import components.active.DefaultCell;
import components.active.DefaultCellCollection;
import components.generic.MeasurementScale;
import components.generic.ProfileType;
import components.nuclei.Nucleus;
import logging.Loggable;

public class CollectionFilterer implements Loggable {
	
	public static final int FAILURE_THRESHOLD = 1;
	public static final int FAILURE_FERET     = 2;
	public static final int FAILURE_ARRAY     = 4;
	public static final int FAILURE_AREA      = 8;
	public static final int FAILURE_PERIM     = 16;
	public static final int FAILURE_OTHER     = 32;
	public static final int FAILURE_SIGNALS   = 64;
	
	private static double maxDifferenceFromMedian = 1.6; // used to filter the nuclei, and remove those too small, large or irregular to be real
	private static double maxWibblinessFromMedian = 1.4; // filter for the irregular borders more stringently


	public boolean run(ICellCollection collection, ICellCollection failCollection){
		
		try{

			fine("Filtering collection...");
			refilterNuclei(collection, failCollection);
			fine("Filtering complete");
		} catch(Exception e){

			error("Error filtering collection "+collection.getName(), e);
			return false;
		}
		return true;
	}
	
	private void refilterNuclei(ICellCollection collection, ICellCollection failCollection) throws Exception{

	    double medianArea = collection.getMedianStatistic(NucleusStatistic.AREA, MeasurementScale.PIXELS);
	    double medianPerimeter = collection.getMedianStatistic(NucleusStatistic.PERIMETER, MeasurementScale.PIXELS);
	    double medianPathLength = collection.getMedianPathLength();
	    double medianArrayLength = collection.getMedianArrayLength();
	    double medianFeretLength = collection.getMedianStatistic(NucleusStatistic.MAX_FERET, MeasurementScale.PIXELS);

	    int beforeSize = collection.size();

	    double maxPathLength = medianPathLength * CollectionFilterer.maxWibblinessFromMedian;
	    double minArea = medianArea / CollectionFilterer.maxDifferenceFromMedian;
	    double maxArea = medianArea * CollectionFilterer.maxDifferenceFromMedian;
	    double maxPerim = medianPerimeter * CollectionFilterer.maxDifferenceFromMedian;
	    double minPerim = medianPerimeter / CollectionFilterer.maxDifferenceFromMedian;
	    double minFeret = medianFeretLength / CollectionFilterer.maxDifferenceFromMedian;

	    int area = 0;
	    int perim = 0;
	    int pathlength = 0;
	    int arraylength = 0;
	    int feretlength = 0;
	    
	    ICellCollection newFailCollection = new DefaultCellCollection(collection, "failed");

	    fine("Prefiltered values found");

	    for(ICell c : collection.getCells()){

	      Nucleus n = c.getNucleus();
	      int failureCode = 0;
	      
	      if(n.getStatistic(NucleusStatistic.AREA) > maxArea || n.getStatistic(NucleusStatistic.AREA) < minArea ){
	        failureCode = failureCode | FAILURE_AREA;
	        area++;
	      }
	      if(n.getStatistic(NucleusStatistic.PERIMETER) > maxPerim || n.getStatistic(NucleusStatistic.PERIMETER) < minPerim ){
	        failureCode = failureCode | FAILURE_PERIM;
	        perim++;
	      }
	      if(n.getPathLength(ProfileType.ANGLE) > maxPathLength){ // only filter for values too big here - wibbliness detector
	    	  failureCode = failureCode | FAILURE_THRESHOLD;
	    	  pathlength++;
	      }
	      if(n.getBorderLength() > medianArrayLength * maxDifferenceFromMedian || n.getBorderLength() < medianArrayLength / maxDifferenceFromMedian ){
	    	  failureCode = failureCode | FAILURE_ARRAY;
	    	  arraylength++;
	      }

	      if(n.getStatistic(NucleusStatistic.MAX_FERET) < minFeret){
	    	  failureCode = failureCode | FAILURE_FERET;
	    	  feretlength++;
	      }

	      if(failureCode > 0){
	    	  Nucleus faiNucleus = n.duplicate();
	    	  ICell failCell = new DefaultCell();
	    	  failCell.setNucleus(faiNucleus);
	    	  newFailCollection.addCell(c);
	      
	    	  failCollection.addCell(failCell);

	      }
	    }

	    for( ICell f : newFailCollection.getCells()){
	    	collection.removeCell(f);
	    }
	    
	    newFailCollection = null; // clean up temp collection


	    medianArea        = collection.getMedianStatistic(NucleusStatistic.AREA, MeasurementScale.PIXELS);
	    medianPerimeter   = collection.getMedianStatistic(NucleusStatistic.PERIMETER, MeasurementScale.PIXELS);
	    medianPathLength  = collection.getMedianPathLength();
	    medianArrayLength = collection.getMedianArrayLength();
	    medianFeretLength = collection.getMedianStatistic(NucleusStatistic.MAX_FERET, MeasurementScale.PIXELS);

//	    int afterSize = collection.getNucleusCount();
//	    int removed = beforeSize - afterSize;

	    log("Remaining: "+collection.size()+" nuclei");
	    
	  }
	
}
