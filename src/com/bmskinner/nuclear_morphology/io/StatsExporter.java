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
package com.bmskinner.nuclear_morphology.io;

public class StatsExporter implements Exporter {
//	
//	private static Logger logger;
//
//	public static boolean run(IAnalysisDataset dataset){
//
//		ICellCollection collection = dataset.getCollection();
//		logger = Logger.getLogger(StatsExporter.class.getName());
//		try {
//			logger.addHandler(dataset.getLogHandler());
//		} catch (SecurityException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		} catch (Exception e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//		
//
//		try{
//			exportNuclearStats(collection, "log.stats");
//			exportImagePaths(collection, "log.imagePaths");
//			exportSegmentStats(collection, "log.segmentStats");
////			exportAngleProfiles(collection);
////			exportSegmentProfiles(collection);
//			exportMediansOfProfile(collection, "log.medians");
//		} catch (Exception e){
//			logger.log(Level.SEVERE, "Error in stats export", e);
//			return false;
//		} finally {
//			for(Handler h : logger.getHandlers()){
//				h.close();
//				logger.removeHandler(h);
//			}
//		}
//		return true;
//	}
//	
//	public static void exportNuclearStats(ICellCollection collection, String filename) throws Exception {
//
//		TableExporter nuclearStats = new TableExporter(collection.getFolder()+File.separator+collection.getOutputFolderName());
//		nuclearStats.addColumn("AREA",                       collection.getMedianStatistics(NucleusStatistic.AREA, MeasurementScale.PIXELS));
//		nuclearStats.addColumn("PERIMETER",                  collection.getMedianStatistics(NucleusStatistic.PERIMETER, MeasurementScale.PIXELS));
//		nuclearStats.addColumn("FERET",                      collection.getMedianStatistics(NucleusStatistic.MAX_FERET, MeasurementScale.PIXELS));
//		nuclearStats.addColumn("PATH_LENGTH",                collection.getPathLengths());
//		nuclearStats.addColumn("MEDIAN_DIST_BETWEEN_POINTS", collection.getMedianDistanceBetweenPoints());
//		nuclearStats.addColumn("MIN_FERET",                  collection.getMedianStatistics(NucleusStatistic.MIN_DIAMETER, MeasurementScale.PIXELS));
////		nuclearStats.addColumn("NORM_TAIL_INDEX",            collection.getBorderIndex(BorderTagObject.ORIENTATION_POINT));
//		nuclearStats.addColumn("DIFFERENCE_TO_MEDIAN",       collection.getDifferencesToMedianFromPoint(Tag.ORIENTATION_POINT));
//		nuclearStats.addColumn("PATH",                       collection.getNucleusImagePaths());
//		nuclearStats.export(filename+"."+collection.getName());
//	}
//
//	public static void exportImagePaths(CellCollection collection, String filename){
//		TableExporter logger = new TableExporter(collection.getFolder()+File.separator+collection.getOutputFolderName());
//		logger.addColumn("PATH",     collection.getNucleusImagePaths());
////		logger.addColumn("POSITION", collection.getPositions());
//		logger.export(filename+"."+collection.getName());
//	}
//	
////	public static void exportAngleProfiles(CellCollection collection){
////		try{
////			for(Nucleus n : collection.getNuclei()){ // for each roi
////				n.exportAngleProfile();
////			}
////		}catch (Exception e){
////			logger.error("Error in angle profile export", e);
////		}
////
////	}
//	
////	public static void exportSegmentProfiles(CellCollection collection){
////		try {
////			for(Nucleus n : collection.getNuclei()){ // for each roi
////				n.exportSegments();
////			}
////		} catch (Exception e){
////			logger.error("Error in segment export", e);
////		}
////	}
//	
//	public static void exportSegmentStats(ICellCollection collection, String filename){
//		try {
//			TableExporter logger = new TableExporter(collection.getFolder()+File.separator+collection.getOutputFolderName());
//
//			IProfileCollection pc = collection.getProfileCollection(ProfileType.ANGLE);
//			List<NucleusBorderSegment> segs = pc.getSegments(Tag.ORIENTATION_POINT);
//			for(NucleusBorderSegment segment : segs){
//				String s = segment.getName();
//
//				List<Integer> list = new ArrayList<Integer>(0);
//				for(Nucleus n : collection.getNuclei()){
//					NucleusBorderSegment seg = n.getProfile(ProfileType.ANGLE).getSegment(s);
//					list.add(seg.length());
//				}
//				logger.addColumn(s, list.toArray(new Integer[0]));
//			}
//
//
//			logger.export(filename+"."+collection.getName());
//
//		} catch (Exception e){
//			logger.log(Level.SEVERE, "Error in segment stats export", e);
//		}
//	}
//
//	public static void exportMediansOfProfile(ICellCollection collection, String filename){
//		
//		try {
//
//			IProfile normalisedMedian = collection.getProfileCollection(ProfileType.ANGLE).getProfile(Tag.ORIENTATION_POINT, 50);
//			IProfile interpolatedMedian = normalisedMedian.interpolate( (int)collection.getMedianStatistic(NucleusStatistic.PERIMETER, MeasurementScale.PIXELS));
//
//			TableExporter logger = new TableExporter(collection.getFolder()+File.separator+collection.getOutputFolderName());
//			logger.addColumn("X_POSITION",   interpolatedMedian.getPositions(interpolatedMedian.size()).asArray());
//			logger.addColumn("ANGLE_MEDIAN", interpolatedMedian.asArray());
//			logger.export(filename+"."+collection.getName());
//		}catch (Exception e){
//			logger.log(Level.SEVERE, "Error in median stats export", e);
//		}
//	}
//
}
