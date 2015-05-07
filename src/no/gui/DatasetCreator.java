package no.gui;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import no.analysis.CurveRefolder;
import no.collections.NucleusCollection;
import no.components.NuclearSignal;
import no.components.NucleusBorderPoint;
import no.components.NucleusBorderSegment;
import no.components.Profile;
import no.components.ShellResult;
import no.components.XYPoint;
import no.nuclei.Nucleus;
import no.utility.Equation;
import no.utility.Utils;

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class DatasetCreator {
	
	private static XYDataset addSegmentsFromProfile(List<NucleusBorderSegment> segments, Profile profile, DefaultXYDataset ds){
		
		Profile xpoints = profile.getPositions(100);
		for(NucleusBorderSegment seg : segments){

			if(seg.getStartIndex()>seg.getEndIndex()){ // case when array wraps

				// beginning of array
				Profile subProfileA = profile.getSubregion(0, seg.getEndIndex());
				Profile subPointsA = xpoints.getSubregion(0, seg.getEndIndex());
				double[][] dataA = { subPointsA.asArray(), subProfileA.asArray() };
				ds.addSeries(seg.getSegmentType()+"_A", dataA);

				// end of array
				Profile subProfileB = profile.getSubregion(seg.getStartIndex(), profile.size()-1);
				Profile subPointsB = xpoints.getSubregion(seg.getStartIndex(), profile.size()-1);
				double[][] dataB = { subPointsB.asArray(), subProfileB.asArray() };
				ds.addSeries(seg.getSegmentType()+"_B", dataB);
				continue;
			} 
			Profile subProfile = profile.getSubregion(seg.getStartIndex(), seg.getEndIndex());
			Profile subPoints  = xpoints.getSubregion(seg.getStartIndex(), seg.getEndIndex());
			double[][] data = { subPoints.asArray(), subProfile.asArray() };
			ds.addSeries(seg.getSegmentType(), data);
		}
		return ds;
	}

	public static XYDataset createSegmentDataset(NucleusCollection collection){
		DefaultXYDataset ds = new DefaultXYDataset();
		Profile profile = collection.getProfileCollection().getProfile(collection.getOrientationPoint());
		Profile xpoints = profile.getPositions(100);
		
		// rendering order will be first on top
		
		// add the segments
		List<NucleusBorderSegment> segments = collection.getProfileCollection().getSegments(collection.getOrientationPoint());
		addSegmentsFromProfile(segments, profile, ds);

		// make the IQR
		Profile profile25 = collection.getProfileCollection().getProfile(collection.getOrientationPoint()+"25");
		Profile profile75 = collection.getProfileCollection().getProfile(collection.getOrientationPoint()+"75");
		double[][] data25 = { xpoints.asArray(), profile25.asArray() };
		ds.addSeries("Q25", data25);
		double[][] data75 = { xpoints.asArray(), profile75.asArray() };
		ds.addSeries("Q75", data75);

		// add the individual nuclei
		for(Nucleus n : collection.getNuclei()){
			Profile angles = n.getAngleProfile(collection.getOrientationPoint()).interpolate(profile.size());
			double[][] ndata = { xpoints.asArray(), angles.asArray() };
			ds.addSeries("Nucleus_"+n.getImageName()+"-"+n.getNucleusNumber(), ndata);
		}
		return ds;
	}
	
	public static DefaultXYDataset createMultiProfileDataset(List<NucleusCollection> list){
		DefaultXYDataset ds = new DefaultXYDataset();

		int i=0;
		for(NucleusCollection collection : list){
			Profile profile = collection.getProfileCollection().getProfile(collection.getOrientationPoint());
			Profile xpoints = profile.getPositions(100);
			double[][] data = { xpoints.asArray(), profile.asArray() };
			ds.addSeries("Profile_"+i, data);
			i++;
		}
		return ds;
	}
	
	public static List<XYSeriesCollection> createMultiProfileIQRDataset(List<NucleusCollection> list){

		List<XYSeriesCollection> result = new ArrayList<XYSeriesCollection>(0);

		int i=0;
		for(NucleusCollection collection : list){
			Profile profile = collection.getProfileCollection().getProfile(collection.getOrientationPoint());
			Profile xpoints = profile.getPositions(100);

			// rendering order will be first on top

			// make the IQR
			Profile profile25 = collection.getProfileCollection().getProfile(collection.getOrientationPoint()+"25");
			Profile profile75 = collection.getProfileCollection().getProfile(collection.getOrientationPoint()+"75");
			
			XYSeries series25 = new XYSeries("Q25_"+i);
			for(int j=0; j<profile25.size();j++){
				series25.add(xpoints.get(j), profile25.get(j));
			}
			
			XYSeries series75 = new XYSeries("Q75_"+i);
			for(int j=0; j<profile75.size();j++){
				series75.add(xpoints.get(j), profile75.get(j));
			}
			
			XYSeriesCollection xsc = new XYSeriesCollection();
		    xsc.addSeries(series25);
		    xsc.addSeries(series75);
		    result.add(xsc);
		    i++;
		}
		return result;
	}
	
	public static XYDataset createIQRVariabilityDataset(List<NucleusCollection> list){

		if(list.size()==1){
			NucleusCollection collection = list.get(0);
			Profile profile = collection.getProfileCollection().getIQRProfile(collection.getOrientationPoint());
			List<NucleusBorderSegment> segments = collection.getProfileCollection().getSegments(collection.getOrientationPoint());
			XYDataset ds = addSegmentsFromProfile(segments, profile, new DefaultXYDataset());	
			return ds;
		} else {
			int i = 0;
			DefaultXYDataset ds = new DefaultXYDataset();
			for(NucleusCollection collection : list){
				Profile profile = collection.getProfileCollection().getIQRProfile(collection.getOrientationPoint());
				Profile xpoints = profile.getPositions(100);
				double[][] data = { xpoints.asArray(), profile.asArray() };
				ds.addSeries("Profile_"+i+"_"+collection.getName(), data);
				i++;
			}
			return ds;
		}
		
	}
		
	public static XYDataset createFrankenSegmentDataset(NucleusCollection collection){
		DefaultXYDataset ds = new DefaultXYDataset();
		Profile profile = collection.getFrankenCollection().getProfile(collection.getOrientationPoint());
		Profile xpoints = profile.getPositions(100);

		// rendering order will be first on top

		// add the segments

		List<NucleusBorderSegment> segments = collection.getFrankenCollection().getSegments(collection.getOrientationPoint());

		for(NucleusBorderSegment seg : segments){

			if(seg.getStartIndex()>seg.getEndIndex()){ // case when array wraps

				// beginning of array
				Profile subProfileA = profile.getSubregion(0, seg.getEndIndex());
				Profile subPointsA = xpoints.getSubregion(0, seg.getEndIndex());
				double[][] dataA = { subPointsA.asArray(), subProfileA.asArray() };
				ds.addSeries(seg.getSegmentType()+"_1", dataA);

				// end of array
				Profile subProfileB = profile.getSubregion(seg.getStartIndex(), profile.size()-1);
				Profile subPointsB = xpoints.getSubregion(seg.getStartIndex(), profile.size()-1);
				double[][] dataB = { subPointsB.asArray(), subProfileB.asArray() };
				ds.addSeries(seg.getSegmentType()+"_2", dataB);
				continue;
			} 
			Profile subProfile = profile.getSubregion(seg.getStartIndex(), seg.getEndIndex());
			Profile subPoints  = xpoints.getSubregion(seg.getStartIndex(), seg.getEndIndex());
			double[][] data = { subPoints.asArray(), subProfile.asArray() };
			ds.addSeries(seg.getSegmentType(), data);
		}

		// make the IQR
		//		Profile profile25 = collection.getFrankenCollection().getProfile(collection.getOrientationPoint()+"25");
		//		Profile profile75 = collection.getFrankenCollection().getProfile(collection.getOrientationPoint()+"75");
		//		double[][] data25 = { xpoints.asArray(), profile25.asArray() };
		//		ds.addSeries("Q25", data25);
		//		double[][] data75 = { xpoints.asArray(), profile75.asArray() };
		//		ds.addSeries("Q75", data75);
		//
		//		// add the individual nuclei
		//		for(Nucleus n : collection.getNuclei()){
		//			Profile angles = n.getAngleProfile(collection.getOrientationPoint()).interpolate(profile.size());
		//			double[][] ndata = { xpoints.asArray(), angles.asArray() };
		//			ds.addSeries("Nucleus_"+n.getImageName()+"-"+n.getNucleusNumber(), ndata);
		//		}
		return ds;
	}

	/**
	 * Create a table model of basic stats from a nucleus collection.
	 * If null parameter is passed, will create an empty table
	 * @param collection
	 * @return
	 */
	public static TableModel createStatsTable(List<NucleusCollection> list){

		DefaultTableModel model = new DefaultTableModel();

		Object[] columnData = {
				"Nuclei", 
				"Median area",
				"Median perimeter",
				"Median feret",
				"Signal channels",
				"Profile window",
				"Nucleus threshold",
				"Nucleus min size",
				"Nucleus max size",
				"Nucleus min circ",
				"Nucleus max circ",
				"Signal threshold",
				"Signal min size",
				"Signal max fraction",
				"Consensus folded",
				"Refold mode",
				"Number of signals",
				"Signals per nucleus",
				"Shell analysis run",
				"Run date",
				"Run time",
				"Type"};
		model.addColumn("Field", columnData);
		
		if(list==null){
			model.addColumn("No data loaded");
		} else {

			// format the numbers and make into a tablemodel
			DecimalFormat df = new DecimalFormat("#0.00"); 

			for(NucleusCollection collection : list){
				
				// only display refold mode if nucleus was refolded
				String refoldMode = collection.getAnalysisOptions().refoldNucleus() 
									? collection.getAnalysisOptions().getRefoldMode()
									: "N/A";
									
				String[] times = collection.getOutputFolderName().split("_");
				String date = times[0];
				String time = times[1];
				
				double signalPerNucleus = (double) collection.getSignalCount()/  (double) collection.getNucleusCount();

				Object[] collectionData = {
						collection.getNucleusCount(),
						df.format(collection.getMedianNuclearArea()),
						df.format(collection.getMedianNuclearPerimeter()),
						df.format(collection.getMedianFeretLength()),
						collection.getSignalChannels().size(),
						collection.getAnalysisOptions().getAngleProfileWindowSize(),
						collection.getAnalysisOptions().getNucleusThreshold(),
						collection.getAnalysisOptions().getMinNucleusSize(),
						collection.getAnalysisOptions().getMaxNucleusSize(),
						collection.getAnalysisOptions().getMinNucleusCirc(),
						collection.getAnalysisOptions().getMaxNucleusCirc(),
						collection.getAnalysisOptions().getSignalThreshold(),
						collection.getAnalysisOptions().getMinSignalSize(),
						collection.getAnalysisOptions().getMaxSignalFraction(),
						collection.getAnalysisOptions().refoldNucleus(),
						refoldMode,
						collection.getSignalCount(),
						df.format(signalPerNucleus),
						collection.hasShellResult(),
						date,
						time,
						collection.getAnalysisOptions().getNucleusClass().getSimpleName()				
				};

				model.addColumn(collection.getName(), collectionData);
			}
		}
		return model;	
	}
				
	public static BoxAndWhiskerCategoryDataset createAreaBoxplotDataset(List<NucleusCollection> collections) {
                
		DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();

		for (int i=0; i < collections.size(); i++) {
			NucleusCollection c = collections.get(i);

			List<Double> list = new ArrayList<Double>();

			for (double d : c.getAreas()) {
				list.add(new Double(d));
			}
			dataset.add(list, c.getType()+"_"+i, "Area");
		}

		return dataset;
	}
	
	public static BoxAndWhiskerCategoryDataset createPerimBoxplotDataset(List<NucleusCollection> collections) {

		DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();

		for (int i=0; i < collections.size(); i++) {
			NucleusCollection c = collections.get(i);

			List<Double> list = new ArrayList<Double>();

			
			for (double d : c.getPerimeters()) {
				list.add(new Double(d));
			}
			dataset.add(list, c.getType()+"_"+i, "Perimeter");
		}

		return dataset;
	}
	
	public static BoxAndWhiskerCategoryDataset createMinFeretBoxplotDataset(List<NucleusCollection> collections) {

		DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();

		for (int i=0; i < collections.size(); i++) {
			NucleusCollection c = collections.get(i);

			List<Double> list = new ArrayList<Double>();

			for (double d : c.getMinFerets()) {
				list.add(new Double(d));
			}
			dataset.add(list, c.getType()+"_"+i, "Min feret");
		}

		return dataset;
	}
	
	public static BoxAndWhiskerCategoryDataset createMaxFeretBoxplotDataset(List<NucleusCollection> collections) {

		DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();

		for (int i=0; i < collections.size(); i++) {
			NucleusCollection c = collections.get(i);

			List<Double> list = new ArrayList<Double>();

			for (double d : c.getFerets()) {
				list.add(new Double(d));
			}
			dataset.add(list, c.getType()+"_"+i, "Max feret");
		}

		return dataset;
	}

	public static BoxAndWhiskerCategoryDataset createDifferenceBoxplotDataset(List<NucleusCollection> collections) {

		DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();

		for (int i=0; i < collections.size(); i++) {
			NucleusCollection c = collections.get(i);

			List<Double> list = new ArrayList<Double>();

			for (double d : c.getDifferencesToMedianFromPoint(c.getOrientationPoint())) {
				list.add(new Double(d));
			}
			dataset.add(list, c.getType()+"_"+i, "Difference to median");
		}

		return dataset;
	}
	
	public static XYDataset createNucleusOutline(NucleusCollection collection){
		DefaultXYDataset ds = new DefaultXYDataset();
		Nucleus n = collection.getConsensusNucleus();
		Profile q25 = collection.getProfileCollection().getProfile(collection.getOrientationPoint()+"25");
		Profile q75 = collection.getProfileCollection().getProfile(collection.getOrientationPoint()+"75");
		

		// Add lines to show the IQR of the angle profile at each point
		double[] innerIQRX = new double[n.getLength()+1];
		double[] innerIQRY = new double[n.getLength()+1];
		double[] outerIQRX = new double[n.getLength()+1];
		double[] outerIQRY = new double[n.getLength()+1];

		// find the maximum difference between IQRs
		double maxIQR = 0;
		for(int i=0; i<n.getLength(); i++){
			if(q75.get(i) - q25.get(i)>maxIQR){
				maxIQR = q75.get(i) - q25.get(i);
			}
		}

		// get the maximum values from nuclear diameters
		// get the limits  for the plot  	
		double min = Math.min(n.getMinX(), n.getMinY());
		double max = Math.max(n.getMaxX(), n.getMaxY());
		double scale = Math.min(Math.abs(min), Math.abs(max));
		
		Profile iqrRange = q75.subtract(q25);
		Profile scaledRange = iqrRange.divide(iqrRange.getMax()); // iqr as fraction of total variability
		scaledRange = scaledRange.multiply(scale/10); // set to 10% min radius


		// rendering order will be first on top
		
		// add the segments
		List<NucleusBorderSegment> segmentList = n.getSegments();
		if(!segmentList.isEmpty()){ // only draw if there are segments
			for(int i=0;i<segmentList.size();i++){

				NucleusBorderSegment seg = n.getSegmentTag("Seg_"+i);

				double[] xpoints = new double[seg.length(n.getLength())+1];
				double[] ypoints = new double[seg.length(n.getLength())+1];
				for(int j=0; j<=seg.length(n.getLength());j++){
					int k = Utils.wrapIndex(seg.getStartIndex()+j, n.getLength());
					NucleusBorderPoint p = n.getBorderPoint(k); // get the border points in the segment
					xpoints[j] = p.getX();
					ypoints[j] = p.getY();
				}

				double[][] data = { xpoints, ypoints };
				ds.addSeries("Seg_"+i, data);
			}
		}

		// add the IQR
		// error here - the nucleus is not starting from the same point as the iqr
		// perhaps a reverse order issue? No. Still offset.
		// Also a problem with order. IQR is doubling back towards tail; 
			// the final point is correct (tailindex-1), but the rest are off by ~8
			// moved back to innerIQRX[i] from innerIQRX[index]; everything joins up again. 
		// Confirms the issue is with the XYPoint positions assigned to each IQR 
//		scaledRange.reverse();
//		int offset = -n.getBorderIndex(collection.getOrientationPoint())-20;
//		scaledRange = scaledRange.offset(offset);
//		IJ.log("Offset: "+offset);
//		int ref = n.getBorderIndex(collection.getReferencePoint());
//		IJ.log("Ref point: "+ref);
		
		
		// what we need to do is match the profile positions to the borderpoints
		
		int tailIndex = n.getBorderIndex(collection.getOrientationPoint());

		for(int i=0; i<n.getLength(); i++){

			int index = Utils.wrapIndex(i + tailIndex, n.getLength()); // start from the orientation point

			int prevIndex = Utils.wrapIndex(index-3, n.getLength());
			int nextIndex = Utils.wrapIndex(index+3, n.getLength());

			XYPoint p = n.getBorderPoint( index  );

			
			// decide the angle at which to place the iqr points
			// make a line between points 3 ahead and behind. 
			// get the orthogonal line, running through the XYPoint
			Equation eq = new Equation(n.getPoint( prevIndex  ), n.getPoint( nextIndex  ));
			// move the line to the index point, and find the orthogonal line
			Equation perp = eq.translate(p).getPerpendicular(p);
			
			XYPoint aPoint = perp.getPointOnLine(p, (0-scaledRange.get(i)));
			XYPoint bPoint = perp.getPointOnLine(p, scaledRange.get(i));

			XYPoint innerPoint = Utils.createPolygon(n).contains(  (float) aPoint.getX(), (float) aPoint.getY() ) ? aPoint : bPoint;
			XYPoint outerPoint = Utils.createPolygon(n).contains(  (float) bPoint.getX(), (float) bPoint.getY() ) ? aPoint : bPoint;

			innerIQRX[i] = innerPoint.getX();
			innerIQRY[i] = innerPoint.getY();
			outerIQRX[i] = outerPoint.getX();
			outerIQRY[i] = outerPoint.getY();

		}
		innerIQRX[n.getLength()] = innerIQRX[0];
		innerIQRY[n.getLength()] = innerIQRY[0];
		outerIQRX[n.getLength()] = outerIQRX[0];
		outerIQRY[n.getLength()] = outerIQRY[0];
		
		double[][] inner = { innerIQRX, innerIQRY };
		ds.addSeries("Q25", inner);
		double[][] outer = { outerIQRX, outerIQRY };
		ds.addSeries("Q75", outer);
		return ds;
	}

	public static XYDataset createMultiNucleusOutline(List<NucleusCollection> list){

		DefaultXYDataset ds = new DefaultXYDataset();

		int i=0;
		for(NucleusCollection collection : list){
			if(collection.hasConsensusNucleus()){
				Nucleus n = collection.getConsensusNucleus();

				double[] xpoints = new double[n.getLength()];
				double[] ypoints = new double[n.getLength()];

				int j =0;

				for(NucleusBorderPoint p : n.getBorderList()){
					xpoints[j] = p.getX();
					ypoints[j] = p.getY();
					j++;
				}
				double[][] data = { xpoints, ypoints };
				ds.addSeries("Nucleus_"+i+"_"+collection.getName(), data);
			}
			i++;

		}
		return ds;
	}
	
	

	public static XYPoint getXYCoordinatesForSignal(NuclearSignal n, Nucleus outline){
		double angle = n.getAngle();

		double fractionalDistance = n.getFractionalDistanceFromCoM();

		// determine the total distance to the border at this angle
		double distanceToBorder = CurveRefolder.getDistanceFromAngle(angle, outline);

		// convert to fractional distance to signal
		double signalDistance = distanceToBorder * fractionalDistance;

		// adjust X and Y because we are now counting angles from the vertical axis
		double signalX = Utils.getXComponentOfAngle(signalDistance, angle-90);
		double signalY = Utils.getYComponentOfAngle(signalDistance, angle-90);
		return new XYPoint(signalX, signalY);
	}
	
	public static XYDataset createSignalCoMDataset(NucleusCollection collection){
		DefaultXYDataset ds = new DefaultXYDataset();
		
		if(collection.getSignalCount()>0){

			for(int channel : collection.getSignalChannels()){

				double[] xpoints = new double[collection.getSignals(channel).size()];
				double[] ypoints = new double[collection.getSignals(channel).size()];

				int signalCount = 0;
				for(NuclearSignal n : collection.getSignals(channel)){

					XYPoint p = getXYCoordinatesForSignal(n, collection.getConsensusNucleus());
					
//					IJ.log("A: "+angle+"  D: "+signalDistance+"  X: "+signalX+"  Y: "+signalY);
					xpoints[signalCount] = p.getX();
					ypoints[signalCount] = p.getY();
					signalCount++;
					
				}
				double[][] data = { xpoints, ypoints };
				ds.addSeries("Channel_"+channel, data);
			}
		}
		return ds;
	}
	
	public static List<Shape> createSignalRadiusDataset(NucleusCollection collection, int channel){

		List<Shape> result = new ArrayList<Shape>(0);
		if(collection.getSignalCount()>0){

			for(NuclearSignal n : collection.getSignals(channel)){
				XYPoint p = getXYCoordinatesForSignal(n, collection.getConsensusNucleus());
				
				// ellipses are drawn starting from x y at upper left. Provide an offset from the centre
				double offset = n.getRadius(); 

				result.add(new Ellipse2D.Double(p.getX()-offset, p.getY()-offset, n.getRadius()*2, n.getRadius()*2));
			}

		}
		return result;
	}
	
	public static TableModel createSignalStatsTable(List<NucleusCollection> list){

		DefaultTableModel model = new DefaultTableModel();
		
		List<Object> fieldNames = new ArrayList<Object>(0);
		
		// find the collection with the most channels
		// this defines  the number of rows

		if(list==null){
			model.addColumn("No data loaded");
			
		} else {
			
			int maxChannels = 0;
			for(NucleusCollection collection : list){
				maxChannels = Math.max(collection.getSignalChannels().size(), maxChannels);
			}
			
			// create the row names
			fieldNames.add("Number of channels");
			
			for(int i=0;i<maxChannels;i++){
				fieldNames.add("");
				fieldNames.add("Channel");
				fieldNames.add("Signals");
				fieldNames.add("Signals per nucleus");
				fieldNames.add("Median area");
				fieldNames.add("Median angle");
				fieldNames.add("Median feret");
				fieldNames.add("Median distance from CoM");
			}
			model.addColumn("", fieldNames.toArray(new Object[0])); // separate row block for each channel
			
//			IJ.log("Added headers");
				
			// format the numbers and make into a tablemodel
			DecimalFormat df = new DecimalFormat("#0.00"); 

			// make a new column for each collection
			for(NucleusCollection collection : list){
				
//				IJ.log("Adding collection");
				List<Object> rowData = new ArrayList<Object>(0);
				rowData.add(collection.getSignalChannels().size());

				for(int channel : collection.getSignalChannels()){
					if(collection.getSignalCount(channel)>0){
						rowData.add("");
						rowData.add(channel);
						rowData.add(collection.getSignalCount(channel));
						double signalPerNucleus = (double) collection.getSignalCount(channel)/  (double) collection.getNucleiWithSignals(channel).size();
						rowData.add(df.format(signalPerNucleus));
						rowData.add(df.format(collection.getMedianSignalArea(channel)));
						rowData.add(df.format(collection.getMedianSignalAngle(channel)));
						rowData.add(df.format(collection.getMedianSignalFeret(channel)));
						rowData.add(df.format(collection.getMedianSignalDistance(channel)));
					} else {
						rowData.add("");
						rowData.add("");
						rowData.add("");
						rowData.add("");
						rowData.add("");
						rowData.add("");
						rowData.add("");
						rowData.add("");
					}
				}
				model.addColumn(collection.getName(), rowData.toArray(new Object[0])); // separate row block for each channel
			}
		}
//		IJ.log("Created model");
		return model;	
	}
	
	public static HistogramDataset createSignalAngleHistogramDataset(List<NucleusCollection> list){
		HistogramDataset ds = new HistogramDataset();
		for(NucleusCollection collection : list){
			
			for(int channel : collection.getSignalChannels()){

				if(collection.getSignalCount(channel)>0){

					List<Double> angles = new ArrayList<Double>(0);

					for(Nucleus n : collection.getNuclei()){
						angles.addAll(n.getSignalCollection().getAngles(channel));
					}
					double[] values = Utils.getdoubleFromDouble(angles.toArray(new Double[0]));
					ds.addSeries("Channel_"+channel+"_"+collection.getName(), values, 12);
				}
			}
			
		}
		return ds;
	}
	
	public static HistogramDataset createSignalDistanceHistogramDataset(List<NucleusCollection> list){
		HistogramDataset ds = new HistogramDataset();
		for(NucleusCollection collection : list){

			for(int channel : collection.getSignalChannels()){

				if(collection.getSignalCount(channel)>0){

					List<Double> angles = new ArrayList<Double>(0);

					for(Nucleus n : collection.getNuclei()){
						angles.addAll(n.getSignalCollection().getDistances(channel));
					}
					double[] values = Utils.getdoubleFromDouble(angles.toArray(new Double[0]));
					ds.addSeries("Channel_"+channel+"_"+collection.getName(), values, 12);
				}
			}

		}
		return ds;
	}
	
	public static CategoryDataset createShellBarChartDataset(List<NucleusCollection> list){
		DefaultStatisticalCategoryDataset ds = new DefaultStatisticalCategoryDataset();
		for(NucleusCollection collection : list){

			for(int channel : collection.getSignalChannels()){
				
				if(collection.hasSignals(channel)){
					ShellResult r = collection.getShellResult(channel);

					for(int shell = 0; shell<r.getNumberOfShells();shell++){
						Double d = r.getMeans().get(shell);
						Double std = r.getStandardErrors().get(shell);
						ds.add(d*100, std.doubleValue()*100, "Channel_"+channel+"_"+collection.getName(), String.valueOf(shell)); 
						// we need the string value for shell otherwise we get error
						// "the method addValue(Number, Comparable, Comparable) is ambiguous for the type DefaultCategoryDataset"
						// ditto the doublevalue for std

					}
				}
			}
		}
		return ds;
	}
}
