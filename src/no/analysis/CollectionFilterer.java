package no.analysis;

import no.collections.INuclearCollection;
import no.nuclei.INuclearFunctions;
import no.utility.Logger;

public class CollectionFilterer {

	private static Logger logger;
	
	public static final int FAILURE_THRESHOLD = 1;
	public static final int FAILURE_FERET     = 2;
	public static final int FAILURE_ARRAY     = 4;
	public static final int FAILURE_AREA      = 8;
	public static final int FAILURE_PERIM     = 16;
	public static final int FAILURE_OTHER     = 32;
	public static final int FAILURE_SIGNALS   = 64;
	
	private static double maxDifferenceFromMedian = 1.6; // used to filter the nuclei, and remove those too small, large or irregular to be real
	private static double maxWibblinessFromMedian = 1.4; // filter for the irregular borders more stringently


	public static boolean run(INuclearCollection collection, INuclearCollection failCollection){

		logger = new Logger(collection.getDebugFile(), "CollectionFilterer");
		try{

			logger.log("Filtering collection...");
			refilterNuclei(collection, failCollection);
		} catch(Exception e){

			logger.log("Error filtering: "+e.getMessage(), Logger.ERROR);
			for(StackTraceElement el : e.getStackTrace()){
				logger.log(el.toString(), Logger.STACK);
			}
			return false;
		} 
		logger.log("Filtering complete");
		return true;
	}
	
	private static void refilterNuclei(INuclearCollection collection, INuclearCollection failCollection){

	    double medianArea = collection.getMedianNuclearArea();
	    double medianPerimeter = collection.getMedianNuclearPerimeter();
	    double medianPathLength = collection.getMedianPathLength();
	    double medianArrayLength = collection.getMedianArrayLength();
	    double medianFeretLength = collection.getMedianFeretLength();

	    int beforeSize = collection.getNucleusCount();

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

	    logger.log("Prefiltered values found");
//	    IJ.append("Prefiltered:\r\n", this.getDebugFile().getAbsolutePath());
	    exportFilterStats(collection);

	    for(int i=0;i<collection.getNucleusCount();i++){
	      INuclearFunctions n = collection.getNucleus(i);
	      
	      if(n.getArea() > maxArea || n.getArea() < minArea ){
	        n.updateFailureCode(FAILURE_AREA);
	        area++;
	      }
	      if(n.getPerimeter() > maxPerim || n.getPerimeter() < minPerim ){
	        n.updateFailureCode(FAILURE_PERIM);
	        perim++;
	      }
	      if(n.getPathLength() > maxPathLength){ // only filter for values too big here - wibbliness detector
	        n.updateFailureCode(FAILURE_THRESHOLD);
	        pathlength++;
	      }
	      if(n.getLength() > medianArrayLength * maxDifferenceFromMedian || n.getLength() < medianArrayLength / maxDifferenceFromMedian ){
	        n.updateFailureCode(FAILURE_ARRAY);
	         arraylength++;
	      }

	      if(n.getFeret() < minFeret){
	        n.updateFailureCode(FAILURE_FERET);
	        feretlength++;
	      }
	      
	      if(n.getFailureCode() > 0){
	        failCollection.addNucleus(n);
	      }
	    }

	    for( INuclearFunctions f : failCollection.getNuclei()){ // should be safer than the i-- above
	    	collection.getNuclei().remove(f);
	    }
	      

	    medianArea = collection.getMedianNuclearArea();
	    medianPerimeter = collection.getMedianNuclearPerimeter();
	    medianPathLength = collection.getMedianPathLength();
	    medianArrayLength = collection.getMedianArrayLength();
	    medianFeretLength = collection.getMedianFeretLength();

	    int afterSize = collection.getNucleusCount();
	    int removed = beforeSize - afterSize;

	    logger.log("Postfiltered values found");
	    exportFilterStats(collection);
	    logger.log("Removed due to size or length issues: "+removed+" nuclei");
	    logger.log("Due to area outside bounds "+(int)minArea+"-"+(int)maxArea+": "+area+" nuclei");
	    logger.log("Due to perimeter outside bounds "+(int)minPerim+"-"+(int)maxPerim+": "+perim+" nuclei");
	    logger.log("Due to wibbliness >"+(int)maxPathLength+" : "+(int)pathlength+" nuclei");
	    logger.log("Due to array length: "+arraylength+" nuclei");
	    logger.log("Due to feret length: "+feretlength+" nuclei");
	    logger.log("Remaining: "+collection.getNucleusCount()+" nuclei");
	    
	  }
	
	public static void exportFilterStats(INuclearCollection collection){

	    double medianArea = collection.getMedianNuclearArea();
	    double medianPerimeter = collection.getMedianNuclearPerimeter();
	    double medianPathLength = collection.getMedianPathLength();
	    double medianArrayLength = collection.getMedianArrayLength();
	    double medianFeretLength = collection.getMedianFeretLength();

	    logger.log("Area: "        +(int)medianArea);
	    logger.log("Perimeter: "   +(int)medianPerimeter);
	    logger.log("Path length: " +(int)medianPathLength);
	    logger.log("Array length: "+(int)medianArrayLength);
	    logger.log("Feret length: "+(int)medianFeretLength);
	    
	  }
}
