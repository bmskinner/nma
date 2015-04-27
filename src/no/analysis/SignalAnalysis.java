package no.analysis;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import no.collections.AsymmetricNucleusCollection;
import no.collections.INuclearCollection;
import no.components.NuclearSignal;
import no.components.XYPoint;
import no.export.Logger;
import no.imports.ImageImporter;
import no.nuclei.INuclearFunctions;
import no.utility.Utils;

public class SignalAnalysis {


	public static void run(INuclearCollection collection){

		if(collection.getSignalCount()==0){
			return;
		}

		for(INuclearFunctions n : collection.getNuclei()){
			n.exportSignalDistanceMatrix();
			
			// if asymmetric, calculate the angle from the tail
			if(AsymmetricNucleusCollection.class.isAssignableFrom(collection.getClass())){
				n.calculateSignalAnglesFromPoint(n.getBorderTag("tail"));
			}
			

		}
		exportSignalStats(collection);
		exportDistancesBetweenSingleSignals(collection);
		addSignalsToProfileCharts(collection);

		collection.getProfileCollection().exportProfilePlots(collection.getFolder()+
				File.separator+
				collection.getOutputFolderName(), collection.getType());
	}

	private static void exportSignalStats(INuclearCollection collection){

		for(int channel : collection.getSignalChannels()){
			Logger logger = new Logger(collection.getFolder()+File.separator+collection.getOutputFolderName());
			logger.addColumnHeading("SIGNAL_AREA");
			logger.addColumnHeading("SIGNAL_ANGLE");
			logger.addColumnHeading("SIGNAL_FERET");
			logger.addColumnHeading("SIGNAL_DISTANCE");
			logger.addColumnHeading("FRACT_DISTANCE");
			logger.addColumnHeading("SIGNAL_PERIM.");
			logger.addColumnHeading("SIGNAL_RADIUS");
			logger.addColumnHeading("CLOSEST_BORDER_INDEX");
			logger.addColumnHeading("SOURCE");

			for(NuclearSignal s : collection.getSignals(channel)){
				logger.addRow("SIGNAL_AREA"         , s.getArea());
				logger.addRow("SIGNAL_ANGLE"        , s.getAngle());
				logger.addRow("SIGNAL_FERET"        , s.getFeret());
				logger.addRow("SIGNAL_DISTANCE"     , s.getDistanceFromCoM());
				logger.addRow("FRACT_DISTANCE"     , s.getFractionalDistanceFromCoM());
				logger.addRow("SIGNAL_PERIM."       , s.getPerimeter());
				logger.addRow("SIGNAL_RADIUS"       , s.getRadius());
				logger.addRow("CLOSEST_BORDER_INDEX", s.getClosestBorderPoint());
				logger.addRow("SOURCE"              , s.getOrigin());
			}
			logger.export("log.signals.Channel_"+channel+"."+collection.getType()); // TODO: get channel names
		}
	}

	public static void exportDistancesBetweenSingleSignals(INuclearCollection collection){

		Logger logger = new Logger(collection.getFolder()+File.separator+collection.getOutputFolderName());
		logger.addColumnHeading("DISTANCE_BETWEEN_SIGNALS");
		logger.addColumnHeading("RED_DISTANCE_TO_COM");
		logger.addColumnHeading("GREEN_DISTANCE_TO_COM");
		logger.addColumnHeading("NUCLEAR_FERET");
		logger.addColumnHeading("RED_FRACTION_OF_FERET");
		logger.addColumnHeading("GREEN_FRACTION_OF_FERET");
		logger.addColumnHeading("DIST_BETWEEN_SIGNALS_FRACT_FERET");
		logger.addColumnHeading("NORMALISED_DISTANCE");
		logger.addColumnHeading("PATH");

		for(INuclearFunctions n : collection.getNuclei()){

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

				logger.addRow("DISTANCE_BETWEEN_SIGNALS"		, distanceBetween);
				logger.addRow("RED_DISTANCE_TO_COM"				, rDistanceToCoM);
				logger.addRow("GREEN_DISTANCE_TO_COM"			, gDistanceToCoM);
				logger.addRow("NUCLEAR_FERET"					, nFeret);
				logger.addRow("RED_FRACTION_OF_FERET"			, rFractionOfFeret);
				logger.addRow("GREEN_FRACTION_OF_FERET"			, gFractionOfFeret);
				logger.addRow("DIST_BETWEEN_SIGNALS_FRACT_FERET", distanceFractionOfFeret);
				logger.addRow("NORMALISED_DISTANCE"    			, normalisedPosition);
				logger.addRow("PATH"                			, n.getPath());
			}
		}
		if(logger.length()>0){
			logger.export("log.SingleSignalDistances");
		}
	}

	private static void addSignalsToProfileCharts(INuclearCollection collection){

		Set<String> headings = collection.getProfileCollection().getPlotKeys();

		for( String pointType : headings ){
			//      Plot normPlot = this.profileCollection.getPlots(pointType).get("norm");
			addSignalsToProfileChartFromPoint(collection, pointType);

		}    
	}

	private static void addSignalsToProfileChartFromPoint(INuclearCollection collection, String pointType){
		// for each signal in each nucleus, find index of point. Draw dot

		List<List<XYPoint>> points = new ArrayList<List<XYPoint>>(0);
		for(int channel : collection.getSignalChannels()){
			points.add(new ArrayList<XYPoint>(0)); // hold signal positions in chart
		}

		for(INuclearFunctions n : collection.getNuclei()){

			int profileIndex = n.getBorderIndex(pointType); 
			List<List<NuclearSignal>> signals = n.getSignals();

			int channel = 0;
			for( List<NuclearSignal> channelSignals : signals ){

				if(!channelSignals.isEmpty()){

					List<XYPoint> channelPoints = points.get(channel);

					for(NuclearSignal s : channelSignals){

						// get the index of the point closest to the signal
						int borderIndex = s.getClosestBorderPoint();

						// offset the index relative to the current profile type, and normalise
						int offsetIndex = Utils.wrapIndex( borderIndex - profileIndex , n.getLength() );
						double normIndex = (  (double) offsetIndex / (double) n.getLength()  ) * 100;

						double yPosition = INuclearCollection.CHART_SIGNAL_Y_LINE_MIN + ( s.getFractionalDistanceFromCoM() * ( INuclearCollection.CHART_SIGNAL_Y_LINE_MAX - INuclearCollection.CHART_SIGNAL_Y_LINE_MIN) ); // 

						// make a point, and add to the appropriate list
						channelPoints.add(new XYPoint(normIndex, yPosition));
					}
				}
				channel++;
			}
		}

		// all points are assigned to lists
		// draw the lists
		int channel = 0;
		for( List<XYPoint> channelPoints : points ){
			Color colour 	= channel == 0 
					? Color.RED 
							: channel == 1 
							? Color.GREEN
									: Color.LIGHT_GRAY;
			collection.getProfileCollection().addSignalsToProfileChart(pointType, channelPoints, colour);
			channel++;
		}
	}

}
