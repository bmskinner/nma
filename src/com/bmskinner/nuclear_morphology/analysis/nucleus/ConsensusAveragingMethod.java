package com.bmskinner.nuclear_morphology.analysis.nucleus;

import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bmskinner.nuclear_morphology.analysis.AbstractAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.ComponentMeasurer;
import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.analysis.profiles.Profileable;
import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.generic.BorderTagObject;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderPointException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.generic.UnprofilableObjectException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclei.DefaultConsensusNucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.NucleusFactory;
import com.bmskinner.nuclear_morphology.components.options.MissingOptionException;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.stats.Quartile;
import com.bmskinner.nuclear_morphology.utility.CircleTools;

/**
 * This method refolds the consensus nucleus based on averaging the positions
 * of equally spaced points around the perimeter of each vertical nucleus in the dataset.
 * @author ben
 * @since 1.13.5
 *
 */
public class ConsensusAveragingMethod extends AbstractAnalysisMethod {
	
	private static final double PROFILE_LENGTH = 200d;
	
	public ConsensusAveragingMethod(IAnalysisDataset dataset) {
		super(dataset);
	}


	
	@Override
	public IAnalysisResult call() throws Exception {

		run();		
		IAnalysisResult r = new DefaultAnalysisResult(dataset);
		return r;
	}
		
	private void run() {
		
//		runFromPoint(3);
		try {
			List<IPoint> border = getPointAverage();
			
			Nucleus refoldNucleus = makeConsensus(border);

		
			dataset.getCollection().setConsensus(refoldNucleus);
			
			
		} catch (Exception e) {
			error("Error getting points", e);
		}
//		for(int i=0; i<16; i++){
//			runFromPoint(i);
//		}
		
	}
	
	
	private Nucleus makeConsensus(List<IPoint> list) throws UnprofilableObjectException, MissingOptionException, ComponentCreationException, UnavailableBorderTagException, ProfileException, UnavailableProfileTypeException{
				
		IPoint com = IPoint.makeNew(0, 0);
				
		NucleusFactory fact = new NucleusFactory(  dataset.getCollection().getNucleusType()  );
		Nucleus n = fact.buildInstance(list, new File("Empty"), 0, com);
		
		// Calculate the stats for the new consensus
		// Required for angle window size calculation
		double perim = ComponentMeasurer.calculatePerimeter(n);
		n.setStatistic(PlottableStatistic.PERIMETER, perim);
		n.initialise(Profileable.DEFAULT_PROFILE_WINDOW_PROPORTION);

		DefaultConsensusNucleus cons = new DefaultConsensusNucleus(n, dataset.getCollection().getNucleusType());

		for(Tag tag : BorderTagObject.values()){
			
			if(Tag.INTERSECTION_POINT.equals(tag)){ // not relevant here
				continue;
			}
			
			if(dataset.getCollection().getProfileCollection().hasBorderTag(tag)){
				IProfile median = dataset.getCollection()
						.getProfileCollection()
						.getProfile(ProfileType.ANGLE, tag, Quartile.MEDIAN);
				int newIndex  = cons.getProfile(ProfileType.ANGLE).getSlidingWindowOffset(median);
				cons.setBorderTag(tag, newIndex);
			}
			

		}
		
		// Check the profile generated
		
		IProfile median = dataset.getCollection()
				.getProfileCollection()
				.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Quartile.MEDIAN);
		IProfile nucProfile = cons.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
		double diff = median.absoluteSquareDifference(nucProfile);
		fine("Difference to median: "+diff);

		
		
//		// Adjust segments to fit size
		ISegmentedProfile profile = cons.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
		List<IBorderSegment> segs = dataset.getCollection().getProfileCollection().getSegments(Tag.REFERENCE_POINT);
		List<IBorderSegment> newSegs = IBorderSegment.scaleSegments(segs, profile.size());
		profile.setSegments(newSegs);
		cons.setProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, profile);
		
		if(cons.hasBorderTag(Tag.TOP_VERTICAL) && cons.hasBorderTag(Tag.BOTTOM_VERTICAL)){
			cons.alignPointsOnVertical(cons.getBorderTag(Tag.TOP_VERTICAL), cons.getBorderTag(Tag.BOTTOM_VERTICAL));

			if(cons.getBorderPoint(Tag.REFERENCE_POINT).getX()>cons.getCentreOfMass().getX()){
				// need to flip about the CoM
				cons.flipXAroundPoint(cons.getCentreOfMass());
			}

		}
		return cons;
		
	}
		
	private List<IPoint> getPointAverage() throws UnavailableBorderTagException, UnavailableProfileTypeException, ProfileException, UnavailableBorderPointException, MissingOptionException{
		
		List<IPoint> result = new ArrayList<>();
		
		final Map<Double, List<IPoint>> map = new HashMap<Double, List<IPoint>>();
		
		IPoint com = IPoint.makeNew(0, 0);
		dataset.getCollection().getNuclei().stream().forEach( n ->{
			try {
				Nucleus v = n.getVerticallyRotatedNucleus();
				v.moveCentreOfMass(com);
				IProfile p = v.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);


				for(int i=0; i<PROFILE_LENGTH; i++){

					double d = ((double) i) /PROFILE_LENGTH;

					if(map.get(d)==null){
						map.put(d, new ArrayList<IPoint>());
					}
					List<IPoint> list = map.get(d);


					int index = p.getIndexOfFraction(d);

					int offset = v.getOffsetBorderIndex(Tag.REFERENCE_POINT, index);

					IPoint point;
					point = v.getBorderPoint(offset);
					list.add(point);
				}
			} catch (Exception e1) {
				error("Error on nucleus "+n.getNameAndNumber(), e1);
			}

		});


		for(int i=0; i<PROFILE_LENGTH; i++){
			
			double d = ((double) i) /PROFILE_LENGTH;
			List<IPoint> list = map.get(d);
			result.add(calculateMedianPoint(list));
			fireProgressEvent();
		}
		
		return result;
	}
	
	private synchronized IPoint calculateMedianPoint(List<IPoint> list){
		double[] xpoints = new double[list.size()];
		double[] ypoints = new double[list.size()];
		
		for(int i=0; i<list.size(); i++){
			IPoint p = list.get(i);
			
			xpoints[i] = p.getX();
			ypoints[i] = p.getY();
		}
		
		double xMed = Quartile.quartile(xpoints, Quartile.MEDIAN);
		double yMed = Quartile.quartile(ypoints, Quartile.MEDIAN);
		
		IPoint avgRP = IPoint.makeNew(xMed, yMed);
		return avgRP;
	}
	
//	private void runFromPoint(int start) {
//		
//		try {
//		
//			IProfile angleProfile  = dataset.getCollection()
//					.getProfileCollection()
//					.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Quartile.MEDIAN);
//
//			IProfile radiusProfile = dataset.getCollection()
//					.getProfileCollection()
//					.getProfile(ProfileType.RADIUS, Tag.REFERENCE_POINT, Quartile.MEDIAN);
//
////			IProfile p2pProfile = dataset.getCollection()
////					.getProfileCollection()
////					.getProfile(ProfileType.P2P, Tag.REFERENCE_POINT, Quartile.MEDIAN);
//
//
//			angleProfile.reverse();
//			radiusProfile.reverse();
//			List<IPoint> list = new ArrayList<>();
//
//			IPoint com = IPoint.makeNew(0, 0);
//
//			// Make the first point on the x axis, radius from zero.
//			// This is the reference point
//			log("First point radius is "+radiusProfile.get(start));
//			IPoint first = IPoint.makeNew( 0, radiusProfile.get(start));
//
//			/*
//			 * The angle profile measures points at every windowsize intervals.
//			 * Therefore we need to measure the angles between points spaced
//			 * windowsize apart when building a skeleton outline
//			 * Move windowsize points along the profile and get the angle.
//			 * The interpolation of nucleus periperies has put points every ~1 pixel.
//			 * The rough distance to move to the next point is one pixel * the window size
//			 */
//
//			int totalPoints = angleProfile.size();
//			int window = (int) (dataset.getAnalysisOptions().getProfileWindowProportion() * totalPoints);
//			// Get the distance between points
//			double  distance = p2pProfile.get(start);
//
//			log("Setting distance between points to "+distance);
//
//			list.add(first);
//			log(first.toString());
//
//			// Now calculate the position of the next point, which is 
//			// radius away from CoM, and can be in any direction from 
//			// the first point
//
//			/*
//			 * Looking for the intersection of the circles
//			 * with radii described
//			 */
//			log("Second point radius is "+radiusProfile.get(window));
//			IPoint second = calculateSecondPoint(first, distance, com, radiusProfile.get(window));
//			log("Second point distance from CoM: "+second.getLengthTo(com));
//			list.add(second);
//			log("Second: "+second.toString());
//
//			/*
//			 * We can now use the angle from the previous point with the radius
//			 * to find a position.
//			 */
//			
//			Path2D outline = new Path2D.Double();
//			outline.moveTo(first.getX(), first.getY());
//			outline.lineTo( second.getX(), second.getY() );
//
////			int finalPoint = 0;
//			for(int i=start+window, k=totalPoints,  j=1; i<totalPoints; i+=window, j++, k--){
//
//				double r = radiusProfile.get(CellularComponent.wrapIndex(i+1, radiusProfile.size()));
//				double a = angleProfile.get( CellularComponent.wrapIndex(i+1, angleProfile.size()) );
//				distance = p2pProfile.get(   CellularComponent.wrapIndex(i+1, p2pProfile.size())   );
//
//				// In the  first iteration of the loop
//				// a is the angle from first to third via second
//				// r is the distance from com to third
//
//				// The next point must be one of the intersections between
//				// point two circle and the com circle
//
//				// Get the current and previous points to work from
//				IPoint prev    = list.get(j-1);
//				IPoint current = list.get(j);
//
//				// Find which circle intersection is closest to the desired angle
//				IPoint[] inters = CircleTools.findIntersections(com, r, current, distance);
//
//				// We have the two intersections of the circles. One of these will be closer 
//				// to the desired angle
//
//				IPoint i0 = inters[0];
//				IPoint i1 = inters[1];
//
//				double a1 = current.findAngle(prev, i0);
//				double a2 = current.findAngle(prev, i1);
//
//				// The angle should be the interior angle of the object being drawn
//				// If the desired angle is >180 degrees, correct
////
////				if(a>180){
////					a1+=180;
////					a2+=180;
////				}
//
//				// get the differences to the desired angle
//
//				double d1 = Math.abs(a1-a);
//				double d2 = Math.abs(a2-a);
//
//
//
//				IPoint next = d1 < d2 ? i0 : i1;
//
//				/*
//				 * If the angle is near 90 degrees, the wrong choice may be made.
//				 * We need to direct the point further.
//				 * 
//				 * Check if the chosen point-CoM line crosses the Current-PrevPoint line
//				 */
//
//				Line2D l1 = new Line2D.Double(current.toPoint2D(), prev.toPoint2D());
//				Line2D l2 = new Line2D.Double(next.toPoint2D(), com.toPoint2D());
//				if(l1.intersectsLine(l2)){
//					next = next==i0 ? i1 : i0;
//				}
//				
//				if(intersects(outline, l2)){ // swap points if the point would intersect the existing path
//					next = next==i0 ? i1 : i0;
//				}
//				
//
//
//				//			if(next.getLengthTo(current) > distance){
//				//				log("Error getting points");
//				//			}
//				outline.lineTo( next.getX(), next.getY() );
//				list.add(next);
////				finalPoint = i;
//			}
//
//			log("##################");
//			for(IPoint p : list){
//				log("\t"+p.getX()+"\t"+p.getY());
//			}
//			
//			
//			// Try to go further around the perimeter
//			
//			if(angleProfile.size()%window==0){
//				log("Exact multiple of window, cannot move to another point set");
//			} else{
//				
//			}
//		
//		} catch(Exception e){
//			error("Profile refold error", e);
//		}
//	}
	
	private static boolean intersects(Path2D path, Line2D line) {
	    double x1 = -1 ,y1 = -1 , x2= -1, y2 = -1;
	    for (PathIterator pi = path.getPathIterator(null); !pi.isDone(); pi.next()) 
	    {
	        double[] coordinates = new double[6];
	        switch (pi.currentSegment(coordinates))
	        {
	        case PathIterator.SEG_MOVETO:
	        case PathIterator.SEG_LINETO:
	        {
	            if(x1 == -1 && y1 == -1 )
	            {
	                x1= coordinates[0];
	                y1= coordinates[1];
	                break;
	            }               
	            if(x2 == -1 && y2 == -1)
	            {
	                x2= coordinates[0];             
	                y2= coordinates[1];
	                break;
	            }
	            break;
	        }
	        }
	        if(x1 != -1 && y1 != -1 && x2 != -1 && y2 != -1)
	        {
	            Line2D segment = new Line2D.Double(x1, y1, x2, y2);
	            if (segment.intersectsLine(line)) 
	            {
	                return true;
	            }
	            x1 = -1;
	            y1 = -1;
	            x2 = -1;
	            y2 = -1;
	        }
	    }
	    return false;
	} 
	
	/**
	 * Find the points at the intersection of two circles, and return one of them
	 * @param first
	 * @param r1
	 * @param com
	 * @param r2
	 * @return
	 */
	private IPoint calculateSecondPoint(IPoint first, double r1, IPoint com, double r2){
		
//		IPoint lastPoint; // needed to get the position of second point correct
		
		/*
		 * Looking for the intersection of the circles
		 * with radii described
		 */
		IPoint[] inters = CircleTools.findIntersections(first, r1, com, r2 );
		
		IPoint i0 = inters[0];
		IPoint i1 = inters[1];
		
		// Check the angle with the last point in the object
		
//		IPoint[] interesctions = null;
//		boolean loop = true;
//		while( loop && r1 > 1 && r1 < 1000){
//			try {
//				interesctions = CircleTools.findIntersections(first, r1, com, r2 );
//				loop=false;
//			} catch(IllegalArgumentException e){
//				log(e.getMessage());
//				r1++;
//				log("Increasing distance to "+r1);
//				
//			}
//		}
		
		return i0.getX()<0 ? i0 : i1;
		
//		log("Second point possible values:");
//		log(i0.toString());
//		log(i1.toString());
//		return i0;
	}

}
