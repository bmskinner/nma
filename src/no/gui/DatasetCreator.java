package no.gui;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import no.collections.NucleusCollection;
import no.components.NucleusBorderPoint;
import no.components.NucleusBorderSegment;
import no.components.Profile;
import no.components.XYPoint;
import no.nuclei.Nucleus;
import no.utility.Equation;
import no.utility.Utils;

import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

public class DatasetCreator {
	
	private static XYDataset addSegmentsFromProfile(List<NucleusBorderSegment> segments, Profile profile, DefaultXYDataset ds){
		
		Profile xpoints = profile.getPositions(100);
		for(NucleusBorderSegment seg : segments){

			if(seg.getStartIndex()>seg.getEndIndex()){ // case when array wraps

				// beginning of array
				Profile subProfileA = profile.getSubregion(0, seg.getEndIndex());
				Profile subPointsA = xpoints.getSubregion(0, seg.getEndIndex());
				double[][] dataA = { subPointsA.asArray(), subProfileA.asArray() };
				ds.addSeries(seg.getSegmentType(), dataA);

				// end of array
				Profile subProfileB = profile.getSubregion(seg.getStartIndex(), profile.size()-1);
				Profile subPointsB = xpoints.getSubregion(seg.getStartIndex(), profile.size()-1);
				double[][] dataB = { subPointsB.asArray(), subProfileB.asArray() };
				ds.addSeries(seg.getSegmentType(), dataB);
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
	
	public static XYDataset createMultiProfileDataset(List<NucleusCollection> list){
		DefaultXYDataset ds = new DefaultXYDataset();

		int i=0;
		for(NucleusCollection collection : list){
			Profile profile = collection.getProfileCollection().getProfile(collection.getOrientationPoint());
			Profile xpoints = profile.getPositions(100);
			double[][] data = { xpoints.asArray(), profile.asArray() };
			ds.addSeries("Profile_"+i, data);

			// rendering order will be first on top

			// make the IQR
			Profile profile25 = collection.getProfileCollection().getProfile(collection.getOrientationPoint()+"25");
			Profile profile75 = collection.getProfileCollection().getProfile(collection.getOrientationPoint()+"75");
			double[][] data25 = { xpoints.asArray(), profile25.asArray() };
			ds.addSeries("Q25_"+i, data25);
			double[][] data75 = { xpoints.asArray(), profile75.asArray() };
			ds.addSeries("Q75_"+i, data75);
			i++;
		}
		return ds;
	}
	
	public static XYDataset createIQRVariabilityDataset(NucleusCollection collection){
		Profile profile = collection.getProfileCollection().getIQRProfile(collection.getOrientationPoint());
		List<NucleusBorderSegment> segments = collection.getProfileCollection().getSegments(collection.getOrientationPoint());
		XYDataset ds = addSegmentsFromProfile(segments, profile, new DefaultXYDataset());		
		return ds;
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
	public static TableModel createStatsTable(NucleusCollection collection){
		
		String[] columnNames = {"Field", "Value"};
		
		if(collection==null){
			Object[][] data = {
					{"Nuclei", null},
					{"Median area", null},
					{"Median perimeter", null},
					{"Median feret", null},
					{"Signal channels", null},
					{"Profile window", null},
					{"Nucleus threshold", null},
					{"Nucleus min size", null},
					{"Nucleus max size", null},
					{"Nucleus min circ", null},
					{"Nucleus max circ", null},
					{"Signal threshold", null},
					{"Signal min size", null},
					{"Signal max fraction", null},
					{"Consensus folded", null},
					{"Refold mode", null},
					{"Ran", null},
					{"Type", null}
			};
			return new DefaultTableModel(data, columnNames);
		}
		// format the numbers and make into a tablemodel
		DecimalFormat df = new DecimalFormat("#.00"); 
		Object[][] data = {
				{"Nuclei", collection.getNucleusCount()},
				{"Median area", df.format(collection.getMedianNuclearArea())},
				{"Median perimeter", df.format(collection.getMedianNuclearPerimeter())},
				{"Median feret", df.format(collection.getMedianFeretLength())},
				{"Signal channels", collection.getSignalChannels().size()},
				{"Profile window", collection.getAnalysisOptions().getAngleProfileWindowSize()},
				{"Nucleus threshold", collection.getAnalysisOptions().getNucleusThreshold()},
				{"Nucleus min size", collection.getAnalysisOptions().getMinNucleusSize()},
				{"Nucleus max size", collection.getAnalysisOptions().getMaxNucleusSize()},
				{"Nucleus min circ", collection.getAnalysisOptions().getMinNucleusCirc()},
				{"Nucleus max circ", collection.getAnalysisOptions().getMaxNucleusCirc()},
				{"Signal threshold", collection.getAnalysisOptions().getSignalThreshold()},
				{"Signal min size", collection.getAnalysisOptions().getMinSignalSize()},
				{"Signal max fraction", collection.getAnalysisOptions().getMaxSignalFraction()},
				{"Consensus folded", collection.getAnalysisOptions().refoldNucleus()},
				{"Refold mode", collection.getAnalysisOptions().getRefoldMode()},
				{"Ran", collection.getOutputFolderName()},
				{"Type", collection.getAnalysisOptions().getNucleusClass().getSimpleName()}
		};

		return new DefaultTableModel(data, columnNames);
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
			ds.addSeries("Nucleus_"+i, data);
			i++;
		}
		return ds;
	}
}
