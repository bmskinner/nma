/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
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
package io;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import stats.NucleusStatistic;
import analysis.AnalysisDataset;
import components.CellCollection;
import components.generic.BorderTag;
import components.generic.MeasurementScale;
import components.generic.Profile;
import components.generic.ProfileCollection;
import components.generic.ProfileType;
import components.nuclear.NucleusBorderSegment;
import components.nuclei.Nucleus;

public class StatsExporter {
	
	private static Logger logger;

	public static boolean run(AnalysisDataset dataset){

		CellCollection collection = dataset.getCollection();
		logger = Logger.getLogger(StatsExporter.class.getName());
		try {
			logger.addHandler(dataset.getLogHandler());
		} catch (SecurityException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		

		try{
			exportNuclearStats(collection, "log.stats");
			exportImagePaths(collection, "log.imagePaths");
			exportSegmentStats(collection, "log.segmentStats");
//			exportAngleProfiles(collection);
//			exportSegmentProfiles(collection);
			exportMediansOfProfile(collection, "log.medians");
		} catch (Exception e){
			logger.log(Level.SEVERE, "Error in stats export", e);
			return false;
		} finally {
			for(Handler h : logger.getHandlers()){
				h.close();
				logger.removeHandler(h);
			}
		}
		return true;
	}
	
	public static void exportNuclearStats(CellCollection collection, String filename) throws Exception {

		TableExporter nuclearStats = new TableExporter(collection.getFolder()+File.separator+collection.getOutputFolderName());
		nuclearStats.addColumn("AREA",                       collection.getNuclearStatistics(NucleusStatistic.AREA, MeasurementScale.PIXELS));
		nuclearStats.addColumn("PERIMETER",                  collection.getNuclearStatistics(NucleusStatistic.PERIMETER, MeasurementScale.PIXELS));
		nuclearStats.addColumn("FERET",                      collection.getNuclearStatistics(NucleusStatistic.MAX_FERET, MeasurementScale.PIXELS));
		nuclearStats.addColumn("PATH_LENGTH",                collection.getPathLengths());
		nuclearStats.addColumn("MEDIAN_DIST_BETWEEN_POINTS", collection.getMedianDistanceBetweenPoints());
		nuclearStats.addColumn("MIN_FERET",                  collection.getNuclearStatistics(NucleusStatistic.MIN_DIAMETER, MeasurementScale.PIXELS));
//		nuclearStats.addColumn("NORM_TAIL_INDEX",            collection.getBorderIndex(BorderTag.ORIENTATION_POINT));
		nuclearStats.addColumn("DIFFERENCE_TO_MEDIAN",       collection.getDifferencesToMedianFromPoint(BorderTag.ORIENTATION_POINT));
		nuclearStats.addColumn("PATH",                       collection.getNucleusImagePaths());
		nuclearStats.export(filename+"."+collection.getType());
	}

	public static void exportImagePaths(CellCollection collection, String filename){
		TableExporter logger = new TableExporter(collection.getFolder()+File.separator+collection.getOutputFolderName());
		logger.addColumn("PATH",     collection.getCleanNucleusPaths());
//		logger.addColumn("POSITION", collection.getPositions());
		logger.export(filename+"."+collection.getType());
	}
	
//	public static void exportAngleProfiles(CellCollection collection){
//		try{
//			for(Nucleus n : collection.getNuclei()){ // for each roi
//				n.exportAngleProfile();
//			}
//		}catch (Exception e){
//			logger.error("Error in angle profile export", e);
//		}
//
//	}
	
//	public static void exportSegmentProfiles(CellCollection collection){
//		try {
//			for(Nucleus n : collection.getNuclei()){ // for each roi
//				n.exportSegments();
//			}
//		} catch (Exception e){
//			logger.error("Error in segment export", e);
//		}
//	}
	
	public static void exportSegmentStats(CellCollection collection, String filename){
		try {
			TableExporter logger = new TableExporter(collection.getFolder()+File.separator+collection.getOutputFolderName());

			ProfileCollection pc = collection.getProfileCollection(ProfileType.REGULAR);
			List<NucleusBorderSegment> segs = pc.getSegments(BorderTag.ORIENTATION_POINT);
			for(NucleusBorderSegment segment : segs){
				String s = segment.getName();

				List<Integer> list = new ArrayList<Integer>(0);
				for(Nucleus n : collection.getNuclei()){
					NucleusBorderSegment seg = n.getAngleProfile().getSegment(s);
					list.add(seg.length());
				}
				logger.addColumn(s, list.toArray(new Integer[0]));
			}


			logger.export(filename+"."+collection.getType());

		} catch (Exception e){
			logger.log(Level.SEVERE, "Error in segment stats export", e);
		}
	}

	public static void exportMediansOfProfile(CellCollection collection, String filename){
		
		try {

			Profile normalisedMedian = collection.getProfileCollection(ProfileType.REGULAR).getProfile(BorderTag.ORIENTATION_POINT, 50);
			Profile interpolatedMedian = normalisedMedian.interpolate( (int)collection.getMedianStatistic(NucleusStatistic.PERIMETER, MeasurementScale.PIXELS));

			TableExporter logger = new TableExporter(collection.getFolder()+File.separator+collection.getOutputFolderName());
			logger.addColumn("X_POSITION",   interpolatedMedian.getPositions(interpolatedMedian.size()).asArray());
			logger.addColumn("ANGLE_MEDIAN", interpolatedMedian.asArray());
			logger.export(filename+"."+collection.getType());
		}catch (Exception e){
			logger.log(Level.SEVERE, "Error in median stats export", e);
		}
	}

}
