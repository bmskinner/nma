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

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import analysis.AnalysisDataset;
import components.CellCollection;
import components.generic.BorderTag;
import components.nuclei.AsymmetricNucleus;
import components.nuclei.Nucleus;

public class SignalAnalysis {

	private static Logger logger;

	public static boolean run(AnalysisDataset dataset){
		
		CellCollection collection = dataset.getCollection();
		logger = Logger.getLogger(SignalAnalysis.class.getName());
		logger.addHandler(dataset.getLogHandler());
//		logger = new Logger(collection.getDebugFile(), "SignalAnalysis");

		if(collection.getSignalCount()==0){
			logger.log(Level.FINE, "No signals found in collection");
			return true;
		} else {
			List<Integer> groups = collection.getSignalGroups();
			logger.log(Level.FINE, "Collection has "+groups.size()+" signal groups");
			for(int signalGroup : groups){
				logger.log(Level.FINE, "Group "+signalGroup+": "+collection.getSignalCount(signalGroup)+" signals");
			}
		}

		try {
			logger.log(Level.INFO, "Exporting distance matrix...");
			for(Nucleus n : collection.getNuclei()){
				n.exportSignalDistanceMatrix();
				
				// if asymmetric, calculate the angle from the tail
				if(AsymmetricNucleus.class.isAssignableFrom(collection.getNucleusType().getNucleusClass())){
					n.calculateSignalAnglesFromPoint(n.getPoint(BorderTag.ORIENTATION_POINT));
				}
			}
			logger.log(Level.INFO, "Distance matrix exported");
			
//			exportSignalStats(collection);
//			exportDistancesBetweenSingleSignals(collection);
//			addSignalsToProfileCharts(collection);

//			collection.getProfileCollection().exportProfilePlots(collection.getFolder()+
//					File.separator+
//					collection.getOutputFolderName(), collection.getType());
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error in signal analysis: "+e.getMessage());
			return false;
		}
		return true;
	}

//	private static void exportSignalStats(CellCollection collection){
//
//		logger.log("Exporting signal stats...");
//		for(int channel : collection.getSignalGroups()){
//			TableExporter tableExporter = new TableExporter(collection.getFolder()+File.separator+collection.getOutputFolderName());
//			tableExporter.addColumnHeading("SIGNAL_AREA");
//			tableExporter.addColumnHeading("SIGNAL_ANGLE");
//			tableExporter.addColumnHeading("SIGNAL_FERET");
//			tableExporter.addColumnHeading("SIGNAL_DISTANCE");
//			tableExporter.addColumnHeading("FRACT_DISTANCE");
//			tableExporter.addColumnHeading("SIGNAL_PERIM.");
//			tableExporter.addColumnHeading("SIGNAL_RADIUS");
//			tableExporter.addColumnHeading("CLOSEST_BORDER_INDEX");
//			tableExporter.addColumnHeading("SOURCE");
//
//			for(NuclearSignal s : collection.getSignals(channel)){
//				tableExporter.addRow("SIGNAL_AREA"         , s.getArea());
//				tableExporter.addRow("SIGNAL_ANGLE"        , s.getAngle());
//				tableExporter.addRow("SIGNAL_FERET"        , s.getFeret());
//				tableExporter.addRow("SIGNAL_DISTANCE"     , s.getDistanceFromCoM());
//				tableExporter.addRow("FRACT_DISTANCE"     , s.getFractionalDistanceFromCoM());
//				tableExporter.addRow("SIGNAL_PERIM."       , s.getPerimeter());
//				tableExporter.addRow("SIGNAL_RADIUS"       , s.getRadius());
//				tableExporter.addRow("CLOSEST_BORDER_INDEX", s.getClosestBorderPoint());
//				tableExporter.addRow("SOURCE"              , s.getOrigin());
//			}
//			tableExporter.export("log.signals.Channel_"+channel+"."+collection.getType()); // TODO: get channel names
//		}
//		logger.log("Signal stats exported");
//	}
//
//	private static void exportDistancesBetweenSingleSignals(CellCollection collection){
//
//		logger.log("Exporting distance between signals...");
//		TableExporter tableExporter = new TableExporter(collection.getFolder()+File.separator+collection.getOutputFolderName());
//		tableExporter.addColumnHeading("DISTANCE_BETWEEN_SIGNALS");
//		tableExporter.addColumnHeading("RED_DISTANCE_TO_COM");
//		tableExporter.addColumnHeading("GREEN_DISTANCE_TO_COM");
//		tableExporter.addColumnHeading("NUCLEAR_FERET");
//		tableExporter.addColumnHeading("RED_FRACTION_OF_FERET");
//		tableExporter.addColumnHeading("GREEN_FRACTION_OF_FERET");
//		tableExporter.addColumnHeading("DIST_BETWEEN_SIGNALS_FRACT_FERET");
//		tableExporter.addColumnHeading("NORMALISED_DISTANCE");
//		tableExporter.addColumnHeading("PATH");
//
//		for(Nucleus n : collection.getNuclei()){
//
//			if(	n.getSignalCount(Constants.FIRST_SIGNAL_CHANNEL)  ==1 && 
//					n.getSignalCount(Constants.FIRST_SIGNAL_CHANNEL+1)==1){
//
//				NuclearSignal r = n.getSignals(Constants.FIRST_SIGNAL_CHANNEL).get(0);
//				NuclearSignal g = n.getSignals(Constants.FIRST_SIGNAL_CHANNEL+1).get(0);
//
//				XYPoint rCoM = r.getCentreOfMass();
//				XYPoint gCoM = g.getCentreOfMass();
//
//				double distanceBetween = rCoM.getLengthTo(gCoM);    
//				double rDistanceToCoM = rCoM.getLengthTo(n.getCentreOfMass());
//				double gDistanceToCoM = gCoM.getLengthTo(n.getCentreOfMass());
//				double nFeret = n.getFeret();
//
//				double rFractionOfFeret = rDistanceToCoM / nFeret;
//				double gFractionOfFeret = gDistanceToCoM / nFeret;
//				double distanceFractionOfFeret = distanceBetween / nFeret;
//				double normalisedPosition = distanceFractionOfFeret / rFractionOfFeret / gFractionOfFeret;
//
//				tableExporter.addRow("DISTANCE_BETWEEN_SIGNALS"		, distanceBetween);
//				tableExporter.addRow("RED_DISTANCE_TO_COM"				, rDistanceToCoM);
//				tableExporter.addRow("GREEN_DISTANCE_TO_COM"			, gDistanceToCoM);
//				tableExporter.addRow("NUCLEAR_FERET"					, nFeret);
//				tableExporter.addRow("RED_FRACTION_OF_FERET"			, rFractionOfFeret);
//				tableExporter.addRow("GREEN_FRACTION_OF_FERET"			, gFractionOfFeret);
//				tableExporter.addRow("DIST_BETWEEN_SIGNALS_FRACT_FERET", distanceFractionOfFeret);
//				tableExporter.addRow("NORMALISED_DISTANCE"    			, normalisedPosition);
//				tableExporter.addRow("PATH"                			, n.getPath());
//			}
//		}
//		if(tableExporter.length()>0){
//			tableExporter.export("log.SingleSignalDistances");
//		} else {
//			logger.log("No single signal pairs found", Logger.DEBUG);
//		}
//		logger.log("Signal distances exported");
//	}
}
