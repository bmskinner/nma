package no.export;

import java.io.File;

import no.collections.INuclearCollection;
import no.components.Profile;
import no.nuclei.INuclearFunctions;
import no.utility.Logger;

public class StatsExporter {
	
	private static Logger logger;

	public static boolean run(INuclearCollection collection){

		logger = new Logger(collection.getDebugFile(), "StatsExporter");
		try{
			exportNuclearStats(collection, "log.stats");
			exportImagePaths(collection, "log.imagePaths");
			exportAngleProfiles(collection);
			exportMediansOfProfile(collection, "log.medians");
		} catch (Exception e){
			logger.log("Error in stats export: "+e.getMessage(), Logger.ERROR);
			return false;
		}
		return true;
	}
	
	public static void exportNuclearStats(INuclearCollection collection, String filename){

		TableExporter nuclearStats = new TableExporter(collection.getFolder()+File.separator+collection.getOutputFolderName());
		nuclearStats.addColumn("AREA",                       collection.getAreas());
		nuclearStats.addColumn("PERIMETER",                  collection.getPerimeters());
		nuclearStats.addColumn("FERET",                      collection.getFerets());
		nuclearStats.addColumn("PATH_LENGTH",                collection.getPathLengths());
		nuclearStats.addColumn("MEDIAN_DIST_BETWEEN_POINTS", collection.getMedianDistanceBetweenPoints());
		nuclearStats.addColumn("MIN_FERET",                  collection.getMinFerets());
		nuclearStats.addColumn("NORM_TAIL_INDEX",            collection.getPointIndexes("tail"));
		nuclearStats.addColumn("DIFFERENCE_TO_MEDIAN",       collection.getDifferencesToMedianFromPoint("tail"));
		nuclearStats.addColumn("PATH",                       collection.getNucleusPaths());
		nuclearStats.export(filename+"."+collection.getType());
	}

	public static void exportImagePaths(INuclearCollection collection, String filename){
		TableExporter logger = new TableExporter(collection.getFolder()+File.separator+collection.getOutputFolderName());
		logger.addColumn("PATH",     collection.getCleanNucleusPaths());
		logger.addColumn("POSITION", collection.getPositions());
		logger.export(filename+"."+collection.getType());
	}
	
	public static void exportAngleProfiles(INuclearCollection collection){
		for(INuclearFunctions n : collection.getNuclei()){ // for each roi
			n.exportAngleProfile();
		}
	}

	public static void exportMediansOfProfile(INuclearCollection collection, String filename){

		Profile normalisedMedian = collection.getProfileCollection().getProfile("tail");
		Profile interpolatedMedian = normalisedMedian.interpolate((int)collection.getMedianNuclearPerimeter());

		TableExporter logger = new TableExporter(collection.getFolder()+File.separator+collection.getOutputFolderName());
		logger.addColumn("X_POSITION",   interpolatedMedian.getPositions(interpolatedMedian.size()).asArray());
		logger.addColumn("ANGLE_MEDIAN", interpolatedMedian.asArray());
		logger.export(filename+"."+collection.getType());
	}

}
