package no.analysis;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import no.collections.AsymmetricNucleusCollection;
import no.collections.NucleusCollection;
import no.components.NuclearSignal;
import no.components.XYPoint;
import no.export.TableExporter;
import no.imports.ImageImporter;
import no.nuclei.Nucleus;
import no.utility.Logger;
import no.utility.Utils;

public class SignalAnalysis {

	private static Logger logger;

	public static boolean run(NucleusCollection collection){
		
		logger = new Logger(collection.getDebugFile(), "SignalAnalysis");

		if(collection.getSignalCount()==0){
			logger.log("No signals found in collection", Logger.DEBUG);
			return true;
		}

		try {
			logger.log("Exporting distance matrix...");
			for(Nucleus n : collection.getNuclei()){
				n.exportSignalDistanceMatrix();
				
				// if asymmetric, calculate the angle from the tail
				if(AsymmetricNucleusCollection.class.isAssignableFrom(collection.getClass())){
					n.calculateSignalAnglesFromPoint(n.getBorderTag("tail"));
				}
			}
			logger.log("Distance matrix exported");
			
			exportSignalStats(collection);
			exportDistancesBetweenSingleSignals(collection);
//			addSignalsToProfileCharts(collection);

//			collection.getProfileCollection().exportProfilePlots(collection.getFolder()+
//					File.separator+
//					collection.getOutputFolderName(), collection.getType());
		} catch (Exception e) {
			logger.log("Error in signal analysis: "+e.getMessage(), Logger.ERROR);
			for(StackTraceElement el : e.getStackTrace()){
				logger.log(el.toString(), Logger.STACK);
			}
			return false;
		}
		return true;
	}

	private static void exportSignalStats(NucleusCollection collection){

		logger.log("Exporting signal stats...");
		for(int channel : collection.getSignalChannels()){
			TableExporter tableExporter = new TableExporter(collection.getFolder()+File.separator+collection.getOutputFolderName());
			tableExporter.addColumnHeading("SIGNAL_AREA");
			tableExporter.addColumnHeading("SIGNAL_ANGLE");
			tableExporter.addColumnHeading("SIGNAL_FERET");
			tableExporter.addColumnHeading("SIGNAL_DISTANCE");
			tableExporter.addColumnHeading("FRACT_DISTANCE");
			tableExporter.addColumnHeading("SIGNAL_PERIM.");
			tableExporter.addColumnHeading("SIGNAL_RADIUS");
			tableExporter.addColumnHeading("CLOSEST_BORDER_INDEX");
			tableExporter.addColumnHeading("SOURCE");

			for(NuclearSignal s : collection.getSignals(channel)){
				tableExporter.addRow("SIGNAL_AREA"         , s.getArea());
				tableExporter.addRow("SIGNAL_ANGLE"        , s.getAngle());
				tableExporter.addRow("SIGNAL_FERET"        , s.getFeret());
				tableExporter.addRow("SIGNAL_DISTANCE"     , s.getDistanceFromCoM());
				tableExporter.addRow("FRACT_DISTANCE"     , s.getFractionalDistanceFromCoM());
				tableExporter.addRow("SIGNAL_PERIM."       , s.getPerimeter());
				tableExporter.addRow("SIGNAL_RADIUS"       , s.getRadius());
				tableExporter.addRow("CLOSEST_BORDER_INDEX", s.getClosestBorderPoint());
				tableExporter.addRow("SOURCE"              , s.getOrigin());
			}
			tableExporter.export("log.signals.Channel_"+channel+"."+collection.getType()); // TODO: get channel names
		}
		logger.log("Signal stats exported");
	}

	public static void exportDistancesBetweenSingleSignals(NucleusCollection collection){

		logger.log("Exporting distance between signals...");
		TableExporter tableExporter = new TableExporter(collection.getFolder()+File.separator+collection.getOutputFolderName());
		tableExporter.addColumnHeading("DISTANCE_BETWEEN_SIGNALS");
		tableExporter.addColumnHeading("RED_DISTANCE_TO_COM");
		tableExporter.addColumnHeading("GREEN_DISTANCE_TO_COM");
		tableExporter.addColumnHeading("NUCLEAR_FERET");
		tableExporter.addColumnHeading("RED_FRACTION_OF_FERET");
		tableExporter.addColumnHeading("GREEN_FRACTION_OF_FERET");
		tableExporter.addColumnHeading("DIST_BETWEEN_SIGNALS_FRACT_FERET");
		tableExporter.addColumnHeading("NORMALISED_DISTANCE");
		tableExporter.addColumnHeading("PATH");

		for(Nucleus n : collection.getNuclei()){

			if(	n.getSignalCount(ImageImporter.FIRST_SIGNAL_CHANNEL)  ==1 && 
					n.getSignalCount(ImageImporter.FIRST_SIGNAL_CHANNEL+1)==1){

				NuclearSignal r = n.getSignals(ImageImporter.FIRST_SIGNAL_CHANNEL).get(0);
				NuclearSignal g = n.getSignals(ImageImporter.FIRST_SIGNAL_CHANNEL+1).get(0);

				XYPoint rCoM = r.getCentreOfMass();
				XYPoint gCoM = g.getCentreOfMass();

				double distanceBetween = rCoM.getLengthTo(gCoM);    
				double rDistanceToCoM = rCoM.getLengthTo(n.getCentreOfMass());
				double gDistanceToCoM = gCoM.getLengthTo(n.getCentreOfMass());
				double nFeret = n.getFeret();

				double rFractionOfFeret = rDistanceToCoM / nFeret;
				double gFractionOfFeret = gDistanceToCoM / nFeret;
				double distanceFractionOfFeret = distanceBetween / nFeret;
				double normalisedPosition = distanceFractionOfFeret / rFractionOfFeret / gFractionOfFeret;

				tableExporter.addRow("DISTANCE_BETWEEN_SIGNALS"		, distanceBetween);
				tableExporter.addRow("RED_DISTANCE_TO_COM"				, rDistanceToCoM);
				tableExporter.addRow("GREEN_DISTANCE_TO_COM"			, gDistanceToCoM);
				tableExporter.addRow("NUCLEAR_FERET"					, nFeret);
				tableExporter.addRow("RED_FRACTION_OF_FERET"			, rFractionOfFeret);
				tableExporter.addRow("GREEN_FRACTION_OF_FERET"			, gFractionOfFeret);
				tableExporter.addRow("DIST_BETWEEN_SIGNALS_FRACT_FERET", distanceFractionOfFeret);
				tableExporter.addRow("NORMALISED_DISTANCE"    			, normalisedPosition);
				tableExporter.addRow("PATH"                			, n.getPath());
			}
		}
		if(tableExporter.length()>0){
			tableExporter.export("log.SingleSignalDistances");
		} else {
			logger.log("No single signal pairs found", Logger.DEBUG);
		}
		logger.log("Signal distances exported");
	}

//	private static void addSignalsToProfileCharts(INuclearCollection collection){
//
//		Set<String> headings = collection.getProfileCollection().getPlotKeys();
//
//		for( String pointType : headings ){
//			//      Plot normPlot = this.profileCollection.getPlots(pointType).get("norm");
//			addSignalsToProfileChartFromPoint(collection, pointType);
//
//		}    
//	}

//	private static void addSignalsToProfileChartFromPoint(INuclearCollection collection, String pointType){
//		// for each signal in each nucleus, find index of point. Draw dot
//
//		List<List<XYPoint>> points = new ArrayList<List<XYPoint>>(0);
//		for(int channel : collection.getSignalChannels()){
//			points.add(new ArrayList<XYPoint>(0)); // hold signal positions in chart
//		}
//
//		for(INuclearFunctions n : collection.getNuclei()){
//
//			int profileIndex = n.getBorderIndex(pointType); 
//			List<List<NuclearSignal>> signals = n.getSignals();
//
//			int channel = 0;
//			for( List<NuclearSignal> channelSignals : signals ){
//
//				if(!channelSignals.isEmpty()){
//
//					List<XYPoint> channelPoints = points.get(channel);
//
//					for(NuclearSignal s : channelSignals){
//
//						// get the index of the point closest to the signal
//						int borderIndex = s.getClosestBorderPoint();
//
//						// offset the index relative to the current profile type, and normalise
//						int offsetIndex = Utils.wrapIndex( borderIndex - profileIndex , n.getLength() );
//						double normIndex = (  (double) offsetIndex / (double) n.getLength()  ) * 100;
//
//						double yPosition = INuclearCollection.CHART_SIGNAL_Y_LINE_MIN + ( s.getFractionalDistanceFromCoM() * ( INuclearCollection.CHART_SIGNAL_Y_LINE_MAX - INuclearCollection.CHART_SIGNAL_Y_LINE_MIN) ); // 
//
//						// make a point, and add to the appropriate list
//						channelPoints.add(new XYPoint(normIndex, yPosition));
//					}
//				}
//				channel++;
//			}
//		}
//
//		// all points are assigned to lists
//		// draw the lists
//		int channel = 0;
//		for( List<XYPoint> channelPoints : points ){
//			Color colour 	= channel == 0 
//					? Color.RED 
//							: channel == 1 
//							? Color.GREEN
//									: Color.LIGHT_GRAY;
//			collection.getProfileCollection().addSignalsToProfileChart(pointType, channelPoints, colour);
//			channel++;
//		}
//	}

}
