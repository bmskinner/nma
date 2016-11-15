package components.active;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import analysis.profiles.ProfileIndexFinder;
import analysis.profiles.RuleSet;
import components.active.generic.DefaultBorderPoint;
import components.active.generic.FloatPoint;
import components.active.generic.UnavailableBorderTagException;
import components.active.generic.UnprofilableObjectException;
import components.generic.BooleanProfile;
import components.generic.Equation;
import components.generic.IPoint;
import components.generic.IProfile;
import components.generic.ProfileType;
import components.generic.Tag;
import components.nuclear.IBorderPoint;
import components.nuclear.INuclearSignal;
import components.nuclei.Nucleus;
import ij.gui.Roi;
import ij.process.FloatPolygon;
import stats.NucleusStatistic;
import stats.SignalStatistic;

/**
 * The standard rodent sperm nucleus
 * @author ben
 * @since 1.13.3
 *
 */
public class DefaultRodentSpermNucleus extends AbstractAsymmetricNucleus {

	private static final long serialVersionUID = 1L;

//	public DefaultRodentSpermNucleus(Roi roi, File f, int channel, int[] position, int number) {
//		super(roi, f, channel, position, number);
//
//	}

	/**
	 * Construct with an ROI, a source image and channel, and the original position in the source image
	 * @param roi
	 * @param f
	 * @param channel
	 * @param position
	 * @param centreOfMass
	 */
	public DefaultRodentSpermNucleus(Roi roi, IPoint centreOfMass, File f, int channel, int[] position, int number){
		super(roi, centreOfMass, f, channel, position, number );
	}

	protected DefaultRodentSpermNucleus(Nucleus n) throws UnprofilableObjectException {
		super(n);
	}

	@Override
	public Nucleus duplicate(){			
		try {
			return new DefaultRodentSpermNucleus(this);
		} catch (UnprofilableObjectException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	protected double calculateStatistic(NucleusStatistic stat){
		double result = super.calculateStatistic(stat);
		//		finest("Calculating stat in rodent sperm nucleus: "+stat);
		switch(stat){

		case HOOK_LENGTH:
			result = getHookOrBodyLength(true);
			break;
		case BODY_WIDTH:
			result = getHookOrBodyLength(false);
			break;
		default:
			return result;

		}
		//		finest("Calculated stat in rodent sperm nucleus: "+stat);
		return result;

	}

	@Override
	public void setBorderTag(Tag tag, int i){
		super.setBorderTag(tag, i);


		// If the flat region moved, update the cached lengths 
		if( this.hasBorderTag(Tag.TOP_VERTICAL) && this.hasBorderTag(Tag.BOTTOM_VERTICAL)){

			if(tag.equals(Tag.TOP_VERTICAL) || tag.equals(Tag.BOTTOM_VERTICAL)){

				calculateHookAndBodyLength();
			}
		}

	}

	private double getHookOrBodyLength(boolean useHook) {

		// check stat is present before calling a getStatistic
		if(hasStatistic(NucleusStatistic.HOOK_LENGTH) || hasStatistic(NucleusStatistic.BODY_WIDTH)){

			if(getStatistic(NucleusStatistic.HOOK_LENGTH) == ERROR_CALCULATING_STAT 
					|| getStatistic(NucleusStatistic.BODY_WIDTH) == ERROR_CALCULATING_STAT){
				calculateHookAndBodyLength();
			}



		} else {
			calculateHookAndBodyLength();
		}

		double stat = useHook 
				? getStatistic(NucleusStatistic.HOOK_LENGTH) 
				: getStatistic(NucleusStatistic.BODY_WIDTH);

		stat = stat == BORDER_POINT_NOT_PRESENT ? 0 : stat; // -2 is the error code when TV and BV are not present. Using -1 will cause infinite loop.

		return stat;


	}

	private void calculateHookAndBodyLength() {

		// Copy the nucleus
		finest("Calculating hook and body length");
		//		RodentSpermNucleus testNucleus = new RodentSpermNucleus( this); //.duplicate();

		// Start with the vertically rotated nucleus
		Nucleus testNucleus = getVerticallyRotatedNucleus();

		// Only proceed if the verticals have been set
		if(testNucleus!=null && testNucleus.hasBorderTag(Tag.TOP_VERTICAL) 
				&& testNucleus.hasBorderTag(Tag.BOTTOM_VERTICAL)){

			finer("Nucleus "+this.getNameAndNumber());
			
			/*
			 * Get the X position of the top vertical
			 */
			double vertX;
			try {
				vertX = testNucleus.getBorderTag(Tag.TOP_VERTICAL).getX();
			} catch (UnavailableBorderTagException e) {
				fine("Cannot get border tag", e);
				setStatistic(NucleusStatistic.HOOK_LENGTH, ERROR_CALCULATING_STAT);
				setStatistic(NucleusStatistic.BODY_WIDTH,  ERROR_CALCULATING_STAT);
				return;
				
			}


			/*
			 * Find the x values in the bounding box of the 
			 * vertical nucleus.
			 */
			double maxBoundingX = testNucleus.createPolygon().getBounds().getMaxX();
			double minBoundingX = testNucleus.createPolygon().getBounds().getMinX();

			if(vertX < minBoundingX || vertX > maxBoundingX ){
				finer("Error calculating hook and body: vertical is out of bounds" );
				setStatistic(NucleusStatistic.HOOK_LENGTH, ERROR_CALCULATING_STAT);
				setStatistic(NucleusStatistic.BODY_WIDTH,  ERROR_CALCULATING_STAT);
				return;
			}



			/*
			 * Find the distance from the vertical X position to the min and max points of the 
			 * bounding box. VertX must lie between these points.
			 */


			double distanceLower  = vertX - minBoundingX;
			double distanceHigher = maxBoundingX - vertX;

			/*
			 * To determine if the point is hook or hump, take
			 * the X position of the tip. This must lie on the
			 * hook side of the vertX
			 */

			double distanceHook = 0;
			double distanceHump = 0;
			double referenceX;
			try {
				referenceX = testNucleus.getBorderTag(Tag.REFERENCE_POINT).getX();
			} catch (UnavailableBorderTagException e) {
				fine("Cannot get border tag", e);
				setStatistic(NucleusStatistic.HOOK_LENGTH, ERROR_CALCULATING_STAT);
				setStatistic(NucleusStatistic.BODY_WIDTH,  ERROR_CALCULATING_STAT);
				return;
			}

			finer("TV is at "+vertX);
			finer("Max bounding x is "+ maxBoundingX);
			finer("Min bounding x is "+ minBoundingX);
			finer("RP x is "+ referenceX);
			finer("Distance lower is "+ distanceLower);
			finer("Distance higher is "+ distanceHigher);

			if(referenceX < vertX){
				distanceHook = distanceLower;
				distanceHump = distanceHigher;
			} else {
				distanceHook = distanceHigher;
				distanceHump = distanceLower;
			}

			setStatistic(NucleusStatistic.HOOK_LENGTH, distanceHook);
			setStatistic(NucleusStatistic.BODY_WIDTH,  distanceHump);

			finer("Hook length is "+ distanceHook);
			finer("Body width is "+ distanceHump);

			finest("Hook length and body width calculated");
		} else {
			finest("Top and bottom vertical not assigned, skipping");

			setStatistic(NucleusStatistic.HOOK_LENGTH, BORDER_POINT_NOT_PRESENT);
			setStatistic(NucleusStatistic.BODY_WIDTH,  BORDER_POINT_NOT_PRESENT);
		}
		testNucleus = null;
	}

	/**
	 * Get a copy of the points in the hook roi
	 * @return
	 */
	public List<IBorderPoint> getHookRoi(){	

		List<IBorderPoint> result = new ArrayList<IBorderPoint>(0);
		
		IBorderPoint testPoint;
		IBorderPoint referencePoint;
		IBorderPoint interSectionPoint;
		IBorderPoint orientationPoint;
		
		try {
			
			testPoint         = this.getBorderTag(Tag.REFERENCE_POINT);
			referencePoint    = this.getBorderTag(Tag.REFERENCE_POINT);
			interSectionPoint = this.getBorderTag(Tag.INTERSECTION_POINT);
			orientationPoint = this.getBorderTag(Tag.ORIENTATION_POINT);
			
		} catch (UnavailableBorderTagException e) {
			fine("Cannot get border tag", e);
			return result;
		}



		/*
		 * Go from the reference point. We hit either the IP or
		 * the OP depending on direction. On hitting one,
		 * move to the other and continue until we're back at the RP
		 */

		//		boolean hasHitPoint = false;
		int i=0;
		IBorderPoint continuePoint = null;

		while(testPoint.hasNextPoint()){
			result.add(testPoint);

			//			IJ.log("Test point :"+testPoint.toString());
			if( testPoint.overlapsPerfectly(interSectionPoint) ){
				continuePoint = orientationPoint;
				//				IJ.log("Hit IP :"+testPoint.toString());
				break;
			}

			if( testPoint.overlapsPerfectly(orientationPoint) ){
				continuePoint = interSectionPoint;
				//				IJ.log("Hit OP :"+testPoint.toString());
				break;
			}

			testPoint = testPoint.nextPoint();

			/*
			 * Only allow the loop to go around the nucleus once
			 */
			if( testPoint.overlapsPerfectly(referencePoint) ){
				//				IJ.log("Hit RP :"+testPoint.toString());
				break;
			}


			i++;
			if(i>1000){
				warn("Forced break");
				break;
			}
		}

		if(continuePoint==null){
			warn("Error getting roi - IP and OP not found");
			return result;
		}

		/*
		 * Continue until we're back at the RP
		 */
		while(continuePoint.hasNextPoint()){
			result.add(continuePoint);
			//			IJ.log("Continue point :"+continuePoint.toString());
			if( continuePoint.overlapsPerfectly(referencePoint.prevPoint()) ){
				break;
			}

			continuePoint = continuePoint.nextPoint();
			i++;
			if(i>2000){
				warn("Forced break for continue point");
				break;
			}
		}
		return result;

	}



	/*
    Identify key points: tip, estimated tail position
	 */
	@Override
	public void findPointsAroundBorder() {


		RuleSet rpSet = RuleSet.mouseSpermRPRuleSet();
		IProfile p     = this.getProfile(rpSet.getType());
		ProfileIndexFinder f = new ProfileIndexFinder();
		int tipIndex = f.identifyIndex(p, rpSet);


		// find tip - use the least angle method
		//		int tipIndex = identifyBorderTagIndex(BorderTag.REFERENCE_POINT);
		setBorderTag(Tag.REFERENCE_POINT, tipIndex);

		// decide if the profile is right or left handed; flip if needed
		if(!this.isProfileOrientationOK() && canReverse){
			this.reverse(); // reverses all profiles, border array and tagged points
			
			// the number of border points can change when reversing
			// due to float interpolation from different starting positions
			// so do the whole thing again
			initialise(this.getWindowProportion(ProfileType.ANGLE));
			canReverse = false;
			findPointsAroundBorder();
		}  


		/*
      Find the tail point using multiple independent methods. 
      Find a consensus point

      Method 1: Use the list of local minima to detect the tail corner
                This is the corner furthest from the tip.
                Can be confused as to which side of the sperm head is chosen
		 */  
		IBorderPoint spermTail2;
		try {
			spermTail2 = findTailPointFromMinima();
			this.addTailEstimatePosition(spermTail2);

			/*    
		      Method 3: Find the narrowest diameter around the nuclear CoM
		                Draw a line orthogonal, and pick the intersecting border points
		                The border furthest from the tip is the tail
			 */  
			IBorderPoint spermTail1;
			try {
				spermTail1 = this.findTailByNarrowestWidthMethod();
				this.addTailEstimatePosition(spermTail1);

				/*
				      Given distinct methods for finding a tail,
				      take a position between them on roi
				 */
				int consensusTailIndex = this.getPositionBetween(spermTail2, spermTail1);
				IBorderPoint consensusTail = this.getBorderPoint(consensusTailIndex);


				setBorderTag(Tag.ORIENTATION_POINT, consensusTailIndex);

				setBorderTag(Tag.INTERSECTION_POINT, this.getBorderIndex(this.findOppositeBorder(consensusTail)));

			} catch (UnavailableBorderTagException e) {
				fine("Cannot get border tag", e);
			}

		} catch (UnavailableBorderTagException e) {
			fine("Error gettting tail position",e);
		}

	
	}


	/**
	 * @param list
	 * @return
	 */
	private FloatPolygon createRoiPolygon(List<IBorderPoint> list){
		float[] xpoints = new float[list.size()+1];
		float[] ypoints = new float[list.size()+1];
		

		for(int i=0;i<list.size();i++){
			IBorderPoint p = list.get(i);
			xpoints[i] = (float) p.getX();
			ypoints[i] = (float) p.getY();
		}

		// Ensure the polygon is closed
		xpoints[list.size()] = (float) list.get(0).getX();
		ypoints[list.size()] = (float) list.get(0).getY();

		return new FloatPolygon(xpoints, ypoints);
	}

	/**
	 * Check if the given point is in the hook side of the nucleus
	 * @param p
	 * @return
	 */
	public boolean isHookSide(IPoint p){
		if(containsPoint(p)){

			/*
			 * Find out which side has been captured. The hook side
			 * has the reference point
			 */

			FloatPolygon poly = createRoiPolygon(getHookRoi());

			if(poly.contains((float)p.getX(), (float)p.getY() )){
				return true;

			} else {
				return false;
			}

		} else {
			throw new IllegalArgumentException("Requested point is not in the nucleus: "+p.toString());
		}
	}


	@Override
	public Nucleus getVerticallyRotatedNucleus(){

		/*
		 * Ensure the nucleus is cached
		 */
		super.getVerticallyRotatedNucleus();
		finest("Fetched vertical nucleus from round nucleus");
		if(verticalNucleus==null){
			warn("Unknown error creating vertical nucleus");
		}

		/*
		 * Get the X position of the reference point
		 */
		double vertX;
		try {
			vertX = verticalNucleus.getBorderTag(Tag.REFERENCE_POINT).getX();
		} catch (UnavailableBorderTagException e) {
			fine("Cannot get RP from vertical nucleus. Not checking horizontal orientation");
			return verticalNucleus;
		}

		/*
		 * If the reference point is left of the centre of mass, 
		 * the nucleus is pointing left. If not, flip thw nucleus
		 */

		if(vertX > verticalNucleus.getCentreOfMass().getX() ){
			clockwiseRP = true; // this is only set to true, as the default is false, and will become false after the nucleus is flipped
			verticalNucleus.flipXAroundPoint(verticalNucleus.getCentreOfMass());
			verticalNucleus.moveCentreOfMass( IPoint.makeNew(0,0));
		} 

		return verticalNucleus;
	}

	/*
    -----------------------
    Methods for detecting the tail
    -----------------------
	 */

	/*
    Detect the tail based on a list of local minima in an NucleusBorderPoint array.
    The putative tail is the point furthest from the sum of the distances from the CoM and the tip
	 */
	public IBorderPoint findTailPointFromMinima() throws UnavailableBorderTagException{

		// we cannot be sure that the greatest distance between two points will be the endpoints
		// because the hook may begin to curve back on itself. We supplement this basic distance with
		// the distances of each point from the centre of mass. The points with the combined greatest
		// distance are both far from each other and far from the centre, and are a more robust estimate
		// of the true ends of the signal
		double tipToCoMDistance;
		try {
			tipToCoMDistance = this.getBorderTag(Tag.REFERENCE_POINT).getLengthTo(this.getCentreOfMass());
		} catch (UnavailableBorderTagException e) {
			fine("Cannot get border tag", e);
			throw new UnavailableBorderTagException("Cannot get RP", e);
		}
		BooleanProfile array = this.getProfile(ProfileType.ANGLE).getLocalMinima(5);

		double maxDistance = 0;
		IBorderPoint tail;

		tail = this.getBorderTag(Tag.REFERENCE_POINT);
		// start at tip, move round

		for(int i=0; i<array.size();i++){
			if(array.get(i)==true){

				double distanceAcrossCoM = tipToCoMDistance + this.getCentreOfMass().getLengthTo(getBorderPoint(i));
				double distanceBetweenEnds;
				distanceBetweenEnds = this.getBorderTag(Tag.REFERENCE_POINT).getLengthTo(getBorderPoint(i));


				double totalDistance = distanceAcrossCoM + distanceBetweenEnds;

				if(totalDistance > maxDistance){
					maxDistance = totalDistance;
					tail = getBorderPoint(i);
				}
			}
		}
		return tail;
	}


	/*
    This is a method for finding a tail point independent of local minima:
      Find the narrowest diameter around the nuclear CoM
      Draw a line orthogonal, and pick the intersecting border points
      The border furthest from the tip is the tail
	 */
	public IBorderPoint findTailByNarrowestWidthMethod() throws UnavailableBorderTagException{

		// Find the narrowest point around the CoM
		// For a position in teh roi, draw a line through the CoM to the intersection point
		// Measure the length; if < min length..., store equation and border(s)

		double minDistance = this.getStatistic(NucleusStatistic.MAX_FERET);
		IBorderPoint reference;

		reference = this.getBorderTag(Tag.REFERENCE_POINT);


		for(int i=0;i<this.getBorderLength();i++){

			IBorderPoint p = this.getBorderPoint(i);
			IBorderPoint opp = this.findOppositeBorder(p);
			double distance = p.getLengthTo(opp);

			if(distance<minDistance){
				minDistance = distance;
				reference = p;
			}
		}
		//    this.minFeretPoint1 = reference;
		//    this.minFeretPoint2 = this.findOppositeBorder(reference);

		// Using the point, draw a line from teh CoM to the border. Measure the angle to an intersection point
		// if close to 90, and the distance to the tip > CoM-tip, keep the point
		// return the best point
		double difference = 90;
		IBorderPoint tail = new DefaultBorderPoint(0,0);
		for(int i=0;i<this.getBorderLength();i++){

			IBorderPoint p = this.getBorderPoint(i);
			double angle = this.getCentreOfMass().findAngle(reference, p);

			if(  Math.abs(90-angle)<difference && 
					p.getLengthTo(this.getBorderTag(Tag.REFERENCE_POINT)) > this.getCentreOfMass().getLengthTo( this.getBorderTag(Tag.REFERENCE_POINT) ) ){
				difference = 90-angle;
				tail = p;
			}

		}
		return tail;
	}

	/*
    -----------------------
    Methods for dividing the nucleus to hook
    and hump sides
    -----------------------
	 */

	/*
    In order to split the nuclear roi into hook and hump sides,
    we need to get an intersection point of the line through the 
    tail and centre of mass with the opposite border of the nucleus.
	 */
	private int findIntersectionPointForNuclearSplit() throws UnavailableBorderTagException {
		// test if each point from the tail intersects the splitting line
		// determine the coordinates of the point intersected as int
		// for each xvalue of each point in array, get the line y value
		// at the point the yvalues are closest and not the tail point is the intersesction
		Equation lineEquation = new Equation(this.getCentreOfMass(), this.getBorderTag(Tag.ORIENTATION_POINT));

		double minDeltaY = 100;
		int minDeltaYIndex = 0;

		for(int i = 0; i<this.getBorderLength();i++){
			double x = this.getBorderPoint(i).getX();
			double y = this.getBorderPoint(i).getY();
			double yOnLine = lineEquation.getY(x);

			double distanceToTail = this.getBorderPoint(i).getLengthTo(this.getBorderTag(Tag.ORIENTATION_POINT));


			double deltaY = Math.abs(y - yOnLine);
			if(deltaY < minDeltaY && distanceToTail > this.getStatistic(NucleusStatistic.MAX_FERET)/2){ // exclude points too close to the tail
				minDeltaY = deltaY;
				minDeltaYIndex = i;
			}
		}
		return minDeltaYIndex;
	}

	public void splitNucleusToHeadAndHump() {

		if(!this.hasBorderTag(Tag.INTERSECTION_POINT)){
			int index;
			try {
				index = findIntersectionPointForNuclearSplit();
			} catch (UnavailableBorderTagException e) {
				fine("Cannot get border tag", e);
				return;
			}
			this.setBorderTag(Tag.INTERSECTION_POINT, index );
		} 
	}

	/*
    -----------------------
    Methods for measuring signal positions
    -----------------------
	 */

	// needs to override AsymmetricNucleus version because hook/hump
	@Override
	public void calculateSignalAnglesFromPoint(IBorderPoint p) {


		super.calculateSignalAnglesFromPoint(p);

		if(this.getSignalCollection().hasSignal()){

			//		  IJ.log(this.dumpInfo(BORDER_TAGS));

			// update signal angles with hook or hump side
			for( UUID i : signalCollection.getSignalGroupIDs()){

				if(signalCollection.hasSignal(i)){

					List<INuclearSignal> signals = signalCollection.getSignals(i);

					for(INuclearSignal n : signals){

						/*
						 * Angle begins from the orientation point 
						 */

						double angle = n.getStatistic(SignalStatistic.ANGLE);

						try{
							// This com is offset, not original
							IPoint com = n.getCentreOfMass();

							// These rois are offset, not original
							if( this.isHookSide(com) ){ 
								angle = 360 - angle;

							} 

						} catch(Exception e){
							// IJ.log(this.getNameAndNumber()+": Error detected: falling back on default angle: "+e.getMessage());
						} finally {

							n.setStatistic(SignalStatistic.ANGLE, angle);

						}
					}
				}
			}
		}
	}

	@Override
	public void rotate(double angle){

		if(angle!=0){

			super.rotate(angle);
		}
	}


	@Override
	public String dumpInfo(int type){
		String result = super.dumpInfo(type);

		return result;

	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		//	  finest("\tReading rodent sperm nucleus");
		in.defaultReadObject();
		calculateHookAndBodyLength();
		//	  finest("\tRead rodent sperm nucleus");
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		//	  finest("\tWriting rodent sperm nucleus");
		out.defaultWriteObject();
		//	  finest("\tWrote rodent sperm nucleus");
	}
}
