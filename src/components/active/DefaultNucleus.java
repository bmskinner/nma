package components.active;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import analysis.profiles.ProfileException;
import analysis.profiles.ProfileIndexFinder;
import analysis.profiles.RuleSet;
import analysis.signals.SignalAnalyser;
import components.active.generic.DefaultBorderPoint;
import components.active.generic.DefaultSignalCollection;
import components.active.generic.UnavailableBorderTagException;
import components.active.generic.UnavailableProfileTypeException;
import components.active.generic.UnprofilableObjectException;
import components.generic.Equation;
import components.generic.IPoint;
import components.generic.IProfile;
import components.generic.MeasurementScale;
import components.generic.ProfileType;
import components.generic.Tag;
import components.nuclear.IBorderPoint;
import components.nuclear.IBorderSegment;
import components.nuclear.INuclearSignal;
import components.nuclear.ISignalCollection;
import components.nuclei.Nucleus;
import ij.gui.Roi;
import stats.NucleusStatistic;
import stats.PlottableStatistic;
import stats.SignalStatistic;

/**
 * The standard round nucleus, implementing {@link Nucleus}. All 
 * non-round nuclei extend this.
 * @author ben
 * @since 1.13.3
 *
 */
public class DefaultNucleus 
	extends ProfileableCellularComponent
	implements Nucleus {
	
	
	private static final long serialVersionUID = 1L;
	
	protected int nucleusNumber; // the number of the nucleus in the current image

	protected ISignalCollection signalCollection = new DefaultSignalCollection();
	
	protected transient Nucleus verticalNucleus = null; // cache the vertically rotated nucleus
	
	protected transient boolean canReverse = true;
	
	/**
	 * Construct with an ROI, a source image and channel, and the original position in the source image
	 * @param roi
	 * @param f
	 * @param channel
	 * @param position
	 * @param centreOfMass
	 */
	public DefaultNucleus(Roi roi, IPoint centreOfMass, File f, int channel, int[] position, int number){
		super(roi, centreOfMass, f, channel, position  );
		this.nucleusNumber   = number;
	}
		
	protected DefaultNucleus(Nucleus n) throws UnprofilableObjectException {
		super( n);
		nucleusNumber = n.getNucleusNumber();
		signalCollection = new DefaultSignalCollection(n.getSignalCollection());
		finest("Created new nucleus");	
	}
	
	@Override
	public Nucleus duplicate(){
		try {
			return new DefaultNucleus(this);
		} catch (UnprofilableObjectException e) {
			warn("Duplication failed");
			stack("Error duplicating nucleus", e);
		}	
		return null;
	}
	
	/*
	* Finds the key points of interest around the border
	* of the Nucleus. Can use several different methods, and 
	* take a best-fit, or just use one. The default in a round 
	* nucleus is to get the longest diameter and set this as
	*  the head/tail axis.
	*/
	@Override
	public void findPointsAroundBorder(){

		try {

			RuleSet rpSet = RuleSet.roundRPRuleSet();
			IProfile p = this.getProfile(rpSet.getType());
			ProfileIndexFinder f = new ProfileIndexFinder();
			int rpIndex = f.identifyIndex(p, rpSet);

			setBorderTag(Tag.REFERENCE_POINT, rpIndex);		
			setBorderTag(Tag.ORIENTATION_POINT, rpIndex);

			if(!this.isProfileOrientationOK() && canReverse){
				fine("Reversing profile");
				this.reverse();

				// the number of border points can change when reversing
				// due to float interpolation from different starting positions
				// so do the whole thing again
				initialise(this.getWindowProportion(ProfileType.ANGLE));
				canReverse = false;
				findPointsAroundBorder();
			}  

		} catch(UnavailableProfileTypeException e){
			stack("Error getting profile type", e);
		}
	}
	
	@Override
	public void initialise(double proportion) {

		super.initialise(proportion);
		
		SignalAnalyser s = new SignalAnalyser();
		s.calculateSignalDistancesFromCoM(this);
		s.calculateFractionalSignalDistancesFromCoM(this); 
	}
	
	@Override
	public int getNucleusNumber(){
		return this.nucleusNumber;
	}
	
	
	@Override
	public String getNameAndNumber(){
		return this.getSourceFileName()+"-"+this.getNucleusNumber();
	}

	@Override
	public String getPathAndNumber(){
		return this.getSourceFile()+File.separator+this.nucleusNumber;
	}
	
	@Override
	protected double calculateStatistic(PlottableStatistic stat) {
		
		if(stat instanceof NucleusStatistic){
			return calculateStatistic( (NucleusStatistic) stat);
		} else {
			throw new IllegalArgumentException("Statistic type inappropriate for nucleus: "+stat.getClass().getName());
		}
		
	}

	protected double calculateStatistic(NucleusStatistic stat) {
		
		double result = 0;
		switch(stat){
		
		case AREA:
			result = this.getStatistic(stat);
			break;
		case ASPECT:
			result = this.getAspectRatio();
			break;
		case CIRCULARITY:
			result = this.getCircularity();
			break;
		case MAX_FERET:
			result = this.getStatistic(stat);
			break;
		case MIN_DIAMETER:
			result = this.getNarrowestDiameter();
			break;
		case PERIMETER:
			result = this.getStatistic(stat);
			break;
		case VARIABILITY:
			break;
		case BOUNDING_HEIGHT:
			result = this.getVerticallyRotatedNucleus().getBounds().getHeight();
			break;
		case BOUNDING_WIDTH:
			result = this.getVerticallyRotatedNucleus().getBounds().getWidth();
			break;
		case OP_RP_ANGLE:
			try {
				result = this.getCentreOfMass().findAngle(this.getBorderTag(Tag.REFERENCE_POINT), this.getBorderTag(Tag.ORIENTATION_POINT));
			} catch (UnavailableBorderTagException e) {
				stack("Cannot get border tag", e);
				result = 0;
			}
			break;
		default: // result stays zero
			break;
	
		}
		return result;
	}
	
	private double getCircularity() {
		double perim2 = Math.pow(this.getStatistic(NucleusStatistic.PERIMETER, MeasurementScale.PIXELS), 2);
		return (4 * Math.PI) * (this.getStatistic(NucleusStatistic.AREA, MeasurementScale.PIXELS) / perim2);
	}
	
	private double getAspectRatio() {
		double h = this.getVerticallyRotatedNucleus().getBounds().getHeight();
		double w = this.getVerticallyRotatedNucleus().getBounds().getWidth();

		return h / w;
	}
		
	protected void setSignals(ISignalCollection collection){
		this.signalCollection = collection;
	}
	
	@Override
	public boolean isClockwiseRP(){
		return false;
	}

	
	public ISignalCollection getSignalCollection(){
		return this.signalCollection;
	}


	public void updateSignalAngle(UUID channel, int signal, double angle){
		signalCollection.getSignals(channel).get(signal).setStatistic(SignalStatistic.ANGLE, angle);
	}

	// do not move this into SignalCollection - it is overridden in RodentSpermNucleus
	public void calculateSignalAnglesFromPoint(IBorderPoint p) {

		for( UUID signalGroup : signalCollection.getSignalGroupIDs()){
			
			if(signalCollection.hasSignal(signalGroup)){
				
			
				List<INuclearSignal> signals = signalCollection.getSignals(signalGroup);

				for(INuclearSignal s : signals){

					double angle = this.getCentreOfMass().findAngle(p, s.getCentreOfMass());
					s.setStatistic(SignalStatistic.ANGLE, angle);

				}
			}
		}
	}

	/*
	Get a readout of the state of the nucleus
	Used only for debugging
	 */
	public String dumpInfo(int type){
		String result = "";
		result += "Dumping nucleus info: "+this.getNameAndNumber()+"\n";
		result += "    Border length: "+this.getBorderLength()+"\n";
		result += "    CoM: "+this.getCentreOfMass().toString()+"\n";
		if(type==ALL_POINTS || type==BORDER_POINTS){
			result += "    Border:\n";
			for(int i=0; i<this.getBorderLength(); i++){
				IBorderPoint p = this.getBorderPoint(i);
				result += "      Index "+i+": "+p.getX()+"\t"+p.getY()+"\t"+this.getBorderTag(i)+"\n";
			}
		}
		if(type==ALL_POINTS || type==BORDER_TAGS){
			result += "    Points of interest:\n";
			Map<Tag, Integer> pointHash = this.getBorderTags();

			for(Tag s : pointHash.keySet()){
				IBorderPoint p = getBorderPoint(pointHash.get(s));
				result += "    "+s+": "+p.getX()+"    "+p.getY()+" at index "+pointHash.get(s)+"\n";
			}
		}
		return result;
	}
	
	/**
	 * Checks if the smoothed array nuclear shape profile has the appropriate
	 * orientation.Counts the number of points above 180 degrees
	 * in each half of the array.
	 * @return 
	 */
	@Override
	public boolean isProfileOrientationOK(){
		int frontPoints = 0;
		int rearPoints  = 0;

		IProfile profile;
		try {
			profile = this.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
		} catch (ProfileException | UnavailableBorderTagException | UnavailableProfileTypeException e) {
			fine("Error getting profile",e);
			return false;
		}

		int midPoint = this.getBorderLength()>>1 ;
		for(int i=0; i<this.getBorderLength();i++){ // integrate points over 180

			if(i<midPoint){
				frontPoints += profile.get(i);
			}
			if(i>midPoint){
				rearPoints  += profile.get(i);
			}
		}

		if(frontPoints > rearPoints){ // if the maxIndex is closer to the end than the beginning
			return true;
		} else{ 
			return false;
		}
	}
	
	@Override
	public void updateVerticallyRotatedNucleus(){

		verticalNucleus = null;
		verticalNucleus = this.getVerticallyRotatedNucleus();
	}
	
	@Override
	public Nucleus getVerticallyRotatedNucleus(){
		fine("Getting vertically rotated nucleus");

		if(verticalNucleus!=null){
			fine("Vertical nucleus not null, not creating");
			return verticalNucleus;
		}

		// Make an exact copy of the nucleus
		verticalNucleus = this.duplicate();
		
		// At this point the new nucleus was created at the original image coordinates 
		// of the template nucleus, then moved to the current CoM.
		// Now align the nucleus on vertical.
		
		fine("Creating new vertical nucleus: "+verticalNucleus.getClass().getSimpleName());

		verticalNucleus.alignVertically();	


		this.setStatistic(NucleusStatistic.BOUNDING_HEIGHT, verticalNucleus.getBounds().getHeight());
		this.setStatistic(NucleusStatistic.BOUNDING_WIDTH,  verticalNucleus.getBounds().getWidth());

		double aspect = verticalNucleus.getBounds().getHeight() / verticalNucleus.getBounds().getWidth();
		this.setStatistic(NucleusStatistic.ASPECT,  aspect);

		this.setStatistic(NucleusStatistic.BODY_WIDTH, STAT_NOT_CALCULATED);
		this.setStatistic(NucleusStatistic.HOOK_LENGTH, STAT_NOT_CALCULATED);

		return verticalNucleus;
	}
	
	@Override
	public void moveCentreOfMass(IPoint point){
		
		double diffX = point.getX() - this.getCentreOfMass().getX();
		double diffY = point.getY() - this.getCentreOfMass().getY();
		
		offset(diffX, diffY);
	}
	
	@Override
	public void offset(double xOffset, double yOffset){
		
		super.offset(xOffset, yOffset);
		
		// Move signals within the nucleus
		if(signalCollection!=null){
			for(INuclearSignal s : this.signalCollection.getAllSignals()){
				s.offset(xOffset, yOffset);
			}
		}
	}
	
	/*
	 * #############################################
	 * Methods implementing the Rotatable interface
	 * #############################################
	 */
	
	
	@Override
	public void alignVertically(){
		
		boolean useTVandBV = true;
		
		if(this.hasBorderTag(Tag.TOP_VERTICAL) && this.hasBorderTag(Tag.BOTTOM_VERTICAL)){
			
			fine(this.getNameAndNumber()+": TV and BV are present");
			int topPoint    = getBorderIndex(Tag.TOP_VERTICAL);
			int bottomPoint = getBorderIndex(Tag.BOTTOM_VERTICAL);
			
			if( topPoint == BORDER_INDEX_NOT_FOUND){ // check if the point was set but not found
				fine(this.getNameAndNumber()+": TV index not found");
				useTVandBV = false;
			}
			
			if( bottomPoint == BORDER_INDEX_NOT_FOUND){
				fine(this.getNameAndNumber()+": BV index not found");
				useTVandBV = false;
			}
			
			if(topPoint==bottomPoint){ // Situation when something went very wrong
				fine(this.getNameAndNumber()+": TV index == BV index");
				useTVandBV = false;
			}

		} else {
			
			fine(this.getNameAndNumber()+": TV and BV are not present");
			useTVandBV = false;

		}
		
		
		
		
		if(useTVandBV){
			
			IBorderPoint[] points;
			try {
				
				points = getBorderPointsForVerticalAlignment();
				alignPointsOnVertical(points[0], points[1] );
				
			} catch (UnavailableBorderTagException | UnavailableProfileTypeException e) {
				stack("Cannot get border tag or profile", e);
				try {
					rotatePointToBottom(getBorderPoint(Tag.ORIENTATION_POINT));
				} catch (UnavailableBorderTagException e1) {
					stack("Cannot get border tag", e1);
				}
			}
			
		} else {
			
			// Default if top and bottom vertical points have not been specified
			
			fine(this.getNameAndNumber()+": Rotating OP to bottom");
			try {
				rotatePointToBottom(getBorderPoint(Tag.ORIENTATION_POINT));
			} catch (UnavailableBorderTagException e) {
				stack("Cannot get border tag", e);
			}
		}
		
	}
	
	/**
	 * Detect the points that can be used for vertical alignment.These are based on the
	 * BorderTags TOP_VERTICAL and BOTTOM_VETICAL. The actual points returned are not
	 * necessarily on the border of the nucleus; a bibble correction is performed on the
	 * line drawn between the two border points, minimising the sum-of-squares to each border
	 * point within the region covered by the line. 
	 * @return
	 * @throws UnavailableBorderTagException 
	 * @throws UnavailableProfileTypeException 
	 */	
	private IBorderPoint[] getBorderPointsForVerticalAlignment() throws UnavailableBorderTagException, UnavailableProfileTypeException{
		
		
		IBorderPoint topPoint;
		IBorderPoint bottomPoint;
			
		topPoint    = this.getBorderTag(Tag.TOP_VERTICAL);
		bottomPoint = this.getBorderTag(Tag.BOTTOM_VERTICAL);

		
		
		// Find the border points between the top and bottom verticals
		List<IBorderPoint> pointsInRegion = new ArrayList<IBorderPoint>();
		
		int topIndex  = this.getBorderIndex(Tag.TOP_VERTICAL);
		int btmIndex  = this.getBorderIndex(Tag.BOTTOM_VERTICAL);
		int totalSize = this.getProfile(ProfileType.ANGLE).size();
		
		// A segment has built in methods for iterating through just the points it contains
		IBorderSegment region = IBorderSegment.newSegment(topIndex, btmIndex, totalSize );

		int index = topIndex;
		
		Iterator<Integer> it = region.iterator();
		
		while(it.hasNext()){
			index = it.next();
			pointsInRegion.add(this.getBorderPoint(index));
		}
		
		// As an anti-bibble defence, get a best fit line acrosss the region
		// Use the line of best fit to find appropriate top and bottom vertical points
		Equation eq = Equation.calculateBestFitLine(pointsInRegion);
		
		
		// Take values along the best fit line that are close to the original TV and BV
		
		// What about when the TV or BV are in the bibble? TODO
		
		IBorderPoint top = new DefaultBorderPoint(topPoint.getX(), eq.getY(topPoint.getX()));
		IBorderPoint btm = new DefaultBorderPoint(eq.getX(bottomPoint.getY()), bottomPoint.getY());
		
		return new IBorderPoint[] {top, btm};
		
	}
	
	@Override
	public void flipXAroundPoint(IPoint p){
		super.flipXAroundPoint(p);
		
		for(UUID id : signalCollection.getSignalGroupIDs()){
			
			signalCollection.getSignals(id).parallelStream().forEach( s -> {
				
				s.flipXAroundPoint(p);				
			});
							
		}
		
	}
		
	@Override
	public void rotate(double angle){
		
		super.rotate(angle);
		
		if(angle!=0){
						
			for(UUID id : signalCollection.getSignalGroupIDs()){
				
				signalCollection.getSignals(id).parallelStream().forEach( s -> {
					
					s.rotate(angle);
										
					// get the new signal centre of mass based on the nucleus rotation
					IPoint p = getPositionAfterRotation(s.getCentreOfMass(), angle);				
					s.moveCentreOfMass(p);					
				});
								
			}
		}
	}
	
	
	/*
	 * #############################################
	 * Object methods
	 * #############################################
	 */
	
	
		
	/**
	 * Describes the nucleus state
	 * @return
	 */
	public String toString(){
		String newLine = System.getProperty("line.separator");
		StringBuilder b = new StringBuilder();
		
		b.append(this.getNameAndNumber());
		b.append(newLine);
		b.append(this.getSignalCollection().toString());
		b.append(newLine);
		return b.toString();
	}
		
	@Override
	public int compareTo(Nucleus n) {

		int number  = this.getNucleusNumber();
		String name = this.getSourceFileNameWithoutExtension();
		
		// Compare on image name.
		// If that is equal, compare on nucleus number

		int byName = name.compareTo(n.getSourceFileNameWithoutExtension());
		
		if(byName==0){
			
			if(number < n.getNucleusNumber()){
				return -1;
			} else if(number > n.getNucleusNumber()){
				return 1;
			} else {
				return 0;
			}

		} else {
			return byName;
		}

	}
		

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		
		result = prime * result + nucleusNumber;
			
		result = prime
				* result
				+ ((signalCollection == null) ? 0 : signalCollection.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DefaultNucleus other = (DefaultNucleus) obj;
		if (nucleusNumber != other.nucleusNumber)
			return false;

		if (signalCollection == null) {
			if (other.signalCollection != null)
				return false;
		} else if (!signalCollection.equals(other.signalCollection))
			return false;
		return true;
	}
	
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		
		in.defaultReadObject();
		
		
	    this.verticalNucleus    = null;
	}
}
