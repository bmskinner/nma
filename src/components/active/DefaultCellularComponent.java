package components.active;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import analysis.detection.BooleanMask;
import analysis.detection.Mask;
import analysis.image.ImageConverter;
import analysis.profiles.Taggable;
import components.CellularComponent;
import components.active.generic.FloatPoint;
import components.generic.IMutablePoint;
import components.generic.IPoint;
import components.generic.MeasurementScale;
import components.nuclear.IBorderPoint;
import components.nuclei.Nucleus;
import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import io.ImageImporter;
import io.UnloadableImageException;
import io.ImageImporter.ImageImportException;
import stats.PlottableStatistic;
import stats.Quartile;
import utility.AngleTools;
import utility.Constants;

/**
 * An abstract implementation of {@link CellularComponent}, which is extended
 * for concrete cellular components. This handles the border, centre of mass and
 * statistics of a component. 
 * @author ben
 * @since 1.13.3
 *
 */
public abstract class DefaultCellularComponent implements CellularComponent {
	
	private static final long serialVersionUID = 1L;
	private final UUID id;
	
	/**
	 * The original position in the source image of the component.
	 * Values are stored at the indexes in {@link CellularComponent.X_BASE}, 
	 * {@link CellularComponent.Y_BASE}, {@link CellularComponent.WIDTH} 
	 * and {@link CellularComponent.HEIGHT}
	 * @see AbstractCellularComponent#getPosition()
	 */
	private final int[] position;
	
	/**
	 * The current centre of the object.
	 */
	private IMutablePoint centreOfMass;
	
	/**
	 * The original centre of the object in its source image.
	 */
	private final IPoint originalCentreOfMass;
	
	/**
	 * The statistical values stored for this object, which should
	 * be an enum implementing {@link PlottableStatistic}
	 */
	private Map<PlottableStatistic, Double> statistics = new HashMap<PlottableStatistic, Double>();
			
	
	/**
	 * The source file the component was detected in. This is detected
	 * on dataset loading as a relative path from the .nmd
	 * e.g. C:\MyImageFolder\MyImage.tiff
	 */
	private File sourceFile;
	
	/**
	 * The RGB channel in which this component was detected
	 */
	private int channel;
	
	/**
	 * The length of a micron in pixels. Allows conversion between pixels and SI units. 
	 * Set to 1 by default. 
	 * @see AbstractCellularComponent#setScale()
	 */
	private double scale = 1; 
	
	/**
	 * The points within the Roi from which the object was detected.
	 */
	private int[] xpoints, ypoints;
	
	/*
	 * TRANSIENT FILEDS
	 */
	
	
	/**
	 * The complete border list, offset to an appropriate position for the object
	 */
	private transient List<IBorderPoint> borderList    = new ArrayList<IBorderPoint>(0);
	
	private transient SoftReference<ImageProcessor> imageRef = new SoftReference<ImageProcessor>(null); // allow caching of images while memory is available
	
	
	/**
	 * Construct with an ROI, a source image and channel, and the original position in the source image.
	 * It sets the immutable original centre of mass, and the mutable current centre of mass. 
	 * It also assigns a random ID to the component.
	 * @param roi the roi of the object
	 * @param centerOfMass the original centre of mass of the component
	 * @param source the image file the component was found in
	 * @param channel the RGB channel the component was found in 
	 * @param position the bounding position of the component in the original image
	 */
	public DefaultCellularComponent(Roi roi, IPoint centreOfMass, File source, int channel, int[] position){
		this.originalCentreOfMass = IPoint.makeNew(centreOfMass);
		this.centreOfMass         = IMutablePoint.makeNew(centreOfMass);
		this.id                   = java.util.UUID.randomUUID();
		this.sourceFile           = source;
		this.channel              = channel;
		this.position             = position;
		
		if(roi==null){
			throw new IllegalArgumentException("Roi cannot be null");
		}
		
		if(centreOfMass==null){
			throw new IllegalArgumentException("Centre of mass cannot be null");
		}
		
		
		// Store the original points. From these, the smooth polygon can be reconstructed.
		Polygon polygon = roi.getPolygon();
		
		
		if( ! polygon.getBounds().contains(centreOfMass.getX(), centreOfMass.getY())){
			
			int minX = (int) polygon.getBounds().getX();
			int maxX = (int) (minX + polygon.getBounds().getWidth());
			int minY = (int) polygon.getBounds().getY();
			int maxY = (int) (minY + polygon.getBounds().getHeight());
			
			throw new IllegalArgumentException("The centre of mass ("
					+centreOfMass.toString()
					+") must be within the roi bounds (x = "
					+minX+"-"+maxX+
					"), (y = "
					+minY+"-"+maxY+")");
		}
		
		if( ! polygon.contains(centreOfMass.getX(), centreOfMass.getY())){
			fine("Centre of mass is not inside the object. You may have a doughnut.");
		}
		
		
		this.xpoints = new int[polygon.npoints];
		this.ypoints = new int[polygon.npoints];

		// Discard empty indices left in polygon array
		for(int i=0; i<polygon.npoints; i++){
			this.xpoints[i] = polygon.xpoints[i];
			this.ypoints[i] = polygon.ypoints[i];
//			log("\tPoint at "+i+": "+this.xpoints[i]+", "+this.ypoints[i]);
		}
						
		// convert the roi positions to a list of nucleus border points
		// Only smooth the points for large objects like nuclei
//		log("Int array in constructor : "+this.xpoints[0]+", "+this.ypoints[0]);
		makeBorderList();		
		
	}
		

	
	/**
	 * Create the border list from the stored int[] points.
	 * Move the centre of mass to any stored position.
	 * @param roi
	 */
	private void makeBorderList(){

		// Make a copy of the int[] points otherwise creating a polygon roi
		// will reset them to 0,0 coordinates
		int[] xcopy = Arrays.copyOf( xpoints, xpoints.length);
		int[] ycopy = Arrays.copyOf( ypoints, ypoints.length);
		PolygonRoi roi = new PolygonRoi(xcopy, ycopy, xpoints.length, Roi.TRACED_ROI);
		

		// Creating the border list will set everything to the original image position.
		// Move the border list back over the CoM if needed.
		IPoint oldCoM = IPoint.makeNew(centreOfMass);
		centreOfMass = IMutablePoint.makeNew(originalCentreOfMass);
		
		borderList    = new ArrayList<IBorderPoint>(0);
		
		// convert the roi positions to a list of border points
		// Each object decides whether it should be smoothed.
		boolean isSmooth = isSmoothByDefault();
		FloatPolygon smoothed = roi.getInterpolatedPolygon(1, isSmooth);
		
		for(int i=0; i<smoothed.npoints; i++){
			IBorderPoint point = IBorderPoint.makeNew( smoothed.xpoints[i], smoothed.ypoints[i]);

			if(i>0){
				point.setPrevPoint(borderList.get(i-1));
				point.prevPoint().setNextPoint(point);
			}
			borderList.add(point);
		}
		// link endpoints
		borderList.get(borderList.size()-1).setNextPoint(borderList.get(0));
		borderList.get(0).setPrevPoint(borderList.get(borderList.size()-1));
		
		moveCentreOfMass(oldCoM);
		
	}
	
	
	/**
	 * Duplicate a component. The ID is kept consistent.
	 * @param a the template component
	 */
	protected DefaultCellularComponent(CellularComponent a){
		this.id                   = a.getID();
		this.position             = a.getPosition();
		this.originalCentreOfMass = a.getOriginalCentreOfMass();
		this.centreOfMass         = IMutablePoint.makeNew(a.getCentreOfMass());
		this.sourceFile           = a.getSourceFile();
		this.channel              = a.getChannel();
		this.scale 			      = a.getScale();
				
		for(PlottableStatistic stat : a.getStatistics() ){
			try {
				this.setStatistic(stat, a.getStatistic(stat, MeasurementScale.PIXELS));
			} catch (Exception e) {
				stack("Error getting "+stat+" from template", e);
				this.setStatistic(stat, 0);
			}
		}


		
		if(a instanceof DefaultCellularComponent){
			
			DefaultCellularComponent comp = (DefaultCellularComponent) a;
			
			this.xpoints = Arrays.copyOf( comp.xpoints, comp.xpoints.length);
			this.ypoints = Arrays.copyOf( comp.ypoints, comp.ypoints.length);
			makeBorderList();

		} else {
			duplicateBorderList(a);
		}
		finest("Created border list");
		
	}
	
	private void duplicateBorderList(CellularComponent c){
		// Duplicate the border points
		this.borderList = new ArrayList<IBorderPoint>(c.getBorderLength());

		for(IBorderPoint p : c.getBorderList()){
			borderList.add( IBorderPoint.makeNew(p));
		}

		// Link points

		for(int i=0; i<borderList.size(); i++){
			IBorderPoint point = borderList.get(i);

			if(i>0 && i<borderList.size()-1){
				point.setNextPoint(borderList.get(i+1));
				point.setPrevPoint(borderList.get(i-1));
			}
		}

		// Set first and last
		IBorderPoint first = borderList.get(0);
		first.setNextPoint(borderList.get(1));
		first.setPrevPoint(borderList.get(borderList.size()-1));

		IBorderPoint last = borderList.get(borderList.size()-1);
		last.setNextPoint(borderList.get(0));
		last.setPrevPoint(borderList.get(borderList.size()-2));
	}
	
	public boolean isSmoothByDefault(){
		return true;
	}
	
	public UUID getID() {
		return this.id;
	}
	

	public int[] getPosition() {
		return this.position;
	}
	
	public Rectangle getBounds() {
		return this.toShape().getBounds();
	}
	
	/**
	 * Get the source folder for images
	 * @return
	 */
	public File getSourceFolder(){
		return this.sourceFile.getParentFile();
	}
	

	public void updateSourceFolder(File newFolder) {
		File oldFile = sourceFile;
		String oldName = oldFile.getName();
		File newFile = new File(newFolder+File.separator+oldName);
		if(newFile.exists()){
			this.setSourceFile(newFile);
		} else {
			throw new IllegalArgumentException("Cannot find file "+oldName+" in folder "+newFolder.getAbsolutePath());
		}
		
	}
	
	/**
	 * Get the absolute path to the source image on the current
	 * computer. Merges the dynamic image folder with the image name
	 * @return
	 */
	@Override
	public File getSourceFile(){
		return sourceFile;
	}
	
	@Override
	public String getSourceFileName(){
		return sourceFile.getName();
	}
	
	@Override
	public String getSourceFileNameWithoutExtension(){

		String trimmed = "";

		int i = getSourceFileName().lastIndexOf('.');
		if (i > 0) {
				trimmed   = getSourceFileName().substring(0,i);
		}
		return trimmed;
	}
	
	public ImageProcessor getImage() throws UnloadableImageException{
		
		ImageProcessor ip = imageRef.get();
		if(ip !=null){
			return ip;
		}
		

		if(getSourceFile().exists()){
			
			// Get the stack, make greyscale and invert
			int stack = Constants.rgbToStack(getChannel());
			
			try {
				ImageStack imageStack = new ImageImporter(getSourceFile()).importImage();
				ip = new ImageConverter(imageStack).convertToGreyscale(stack).toProcessor();
				ip.invert();	
				
				imageRef = new SoftReference<ImageProcessor>(ip);
				
				return ip;
				
			} catch (ImageImportException e) {
				stack("Error importing source image "+this.getSourceFile().getAbsolutePath(), e);
				throw new UnloadableImageException("Source image is not available");
			}
			
		} else {
			throw new UnloadableImageException("Source image is not available");
		}
	}
	

	public ImageProcessor getComponentImage() throws UnloadableImageException{
		ImageProcessor ip = getImage();

		if(ip==null){	
			return null;	
		}
		
		int[] positions = getPosition();
		
		int padding = CellularComponent.COMPONENT_BUFFER; // a border of pixels beyond the cell boundary
		int wideW = (int) (positions[CellularComponent.WIDTH]+(padding*2));
		int wideH = (int) (positions[CellularComponent.HEIGHT]+(padding*2));
		int wideX = (int) (positions[CellularComponent.X_BASE]-padding);
		int wideY = (int) (positions[CellularComponent.Y_BASE]-padding);

		wideX = wideX<0 ? 0 : wideX;
		wideY = wideY<0 ? 0 : wideY;

		ip.setRoi(wideX, wideY, wideW, wideH);
		ip = ip.crop();

		return ip;
	}
	
//	public void setSourceFileName(String name) {
//		this.sourceFileName = name;
//	}
	
	public void setSourceFolder(File sourceFolder) {
		
		File newFile = new File(sourceFolder+File.separator+sourceFile.getName());
		
		this.sourceFile = newFile;
	}
	
	public void setSourceFile(File sourceFile){
		this.sourceFile = sourceFile;
	}
	


	public int getChannel() {
		return channel;
	}


	public void setChannel(int channel) {
		this.channel = channel;
	}
	
	public double getScale(){
		return this.scale;
	}
	
	public void setScale(double scale){
		this.scale = scale;
	}


	@Override
	public boolean equals(CellularComponent c) {
		return false;
	}
	
	public synchronized boolean hasStatistic(PlottableStatistic stat){
		return this.statistics.containsKey(stat);
	}

	@Override
	public synchronized double getStatistic(PlottableStatistic stat, MeasurementScale scale) {
		if(this.statistics.containsKey(stat)){
			
			double result = statistics.get(stat);	
		
			result = stat.convert(result, this.scale, scale);
			return result;
		} else {

			double result = calculateStatistic(stat);
//
			setStatistic(stat, result);
			return result;
		}
	}

	
	// For subclasses to override
		protected double calculateStatistic(PlottableStatistic stat){
			return 0;
		}
		
		@Override
		public synchronized double getStatistic(PlottableStatistic stat) {
			return this.getStatistic(stat, MeasurementScale.PIXELS);
		}


		@Override
		public synchronized void setStatistic(PlottableStatistic stat, double d) {
			this.statistics.put(stat, d);
		}


		@Override
		public PlottableStatistic[] getStatistics() {
			return this.statistics.keySet().toArray(new PlottableStatistic[0]);
		}
		
		/**
		 * If any stats are listed as uncalcualted, attempt to calculate them
		 */
		public void updateDependentStats(){
			
			for(PlottableStatistic stat : this.getStatistics()){
				
				if(this.getStatistic(stat)==STAT_NOT_CALCULATED){
					this.setStatistic(stat, calculateStatistic(stat));
				}
			}
			
		}
		
		public IPoint getCentreOfMass() {
			return centreOfMass;
		}
		
		public IPoint getOriginalCentreOfMass() {
			return IPoint.makeNew( originalCentreOfMass);
		}
		
		public int getBorderLength(){
			return this.borderList.size();
		}


		public IBorderPoint getBorderPoint(int i){
			return this.borderList.get(i);
		}
		
		public IBorderPoint getOriginalBorderPoint(int i){
			IBorderPoint p = getBorderPoint(i);
			
			double diffX = p.getX() - centreOfMass.getX();
			double diffY = p.getY() - centreOfMass.getY();

			// Offset to the original position
			IBorderPoint ip = IBorderPoint.makeNew(originalCentreOfMass.getX()+diffX, originalCentreOfMass.getY()+diffY);

			return ip;
		}
		
		public int getBorderIndex(IBorderPoint p){
			int i = 0;
			for(IBorderPoint n : borderList){
				if( n.getX()==p.getX() && n.getY()==p.getY()){
					return i;
				}
				i++;
			}
			return -1; // default if no match found
		}
		
		public void updateBorderPoint(int i, IPoint p){
			this.updateBorderPoint(i, p.getX(), p.getY());
		}
		
		public void updateBorderPoint(int i, double x, double y){
			borderList.get(i).setX(x);
			borderList.get(i).setY(y);
		}

		public List<IBorderPoint> getBorderList(){
			return this.borderList;
		}
		
		public List<IBorderPoint> getOriginalBorderList(){
			List<IBorderPoint> result = new ArrayList<IBorderPoint>(borderList.size());
			
						
			double diffX = originalCentreOfMass.getX() - centreOfMass.getX();
			double diffY = originalCentreOfMass.getY() - centreOfMass.getY();

			
			// Offset to the original position
			for(IBorderPoint p : borderList){
				result.add( IBorderPoint.makeNew(p.getX()+diffX, p.getY()+diffY));
			}
			return result;
		}
		
		
		/**
		 * Check if a given point lies within the nucleus
		 * @param p
		 * @return
		 */
		public boolean containsPoint(IPoint p){
			
			// Fast check - is the point within the bounding rectangle?
			if( ! this.getBounds().contains(p.toPoint2D())){
				return false;			
			} 
			
			// Check detailed position
			if(this.createPolygon().contains( (float)p.getX(), (float)p.getY() ) ){
				return true;
			} else { 
				return false;
			}
			
			
		}
		
		/**
		 * Check if a given point lies within the nucleus
		 * @param p
		 * @return
		 */
		public boolean containsPoint(int x, int y){
			// Check detailed position
			
			return this.toShape().contains(x, y);

			
		}
		
		/**
		 * Check if a given point in the original source image lies within the nucleus
		 * @param p
		 * @return
		 */
		public boolean containsOriginalPoint(IPoint p){

			// Check detailed position
			return this.createOriginalPolygon().contains( (float)p.getX(), (float)p.getY() );
		}
		
		/**
		 * Check if a given point lies within the nucleus
		 * @param p
		 * @return
		 */
		public boolean containsOriginalPoint(int x, int y){
			
			// Fast check - is the point within the bounding rectangle moved to the original position?
			Rectangle r = new Rectangle(position[X_BASE], position[Y_BASE], position[WIDTH], position[HEIGHT]);

			if( ! r.contains(x, y)){
				return false;			
			} 
			
			// Check detailed position
			
			return this.toOriginalShape().contains(x, y);		
		}
		
		
		/*
		 * 
		 * GET MAX AND MIN BORDER POSITIONS
		 * 
		 */
		
		public double getMaxX(){
			
			return this.toShape().getBounds().getMaxX();
		}

		public double getMinX(){
			return this.toShape().getBounds().getMinX();
		}

		public double getMaxY(){
			return this.toShape().getBounds().getMaxY();
		}

		public double getMinY(){
			return this.toShape().getBounds().getMinY();
		}
		

		/* (non-Javadoc)
		 * @see components.CellularComponent#flipXAroundPoint(components.generic.IPoint)
		 */
		public void flipXAroundPoint(IPoint p){

			double xCentre = p.getX();

			for(IBorderPoint n : borderList){
				double dx = xCentre - n.getX();
				double xNew = xCentre + dx;
				n.setX(xNew);
			}
			

		}

		public double getMedianDistanceBetweenPoints(){
			double[] distances = new double[this.borderList.size()];
			for(int i=0;i<this.borderList.size();i++){
				IBorderPoint p = borderList.get(i);
				IBorderPoint next = borderList.get( wrapIndex(i+1 ));
				distances[i] = p.getLengthTo(next);
			}
			return new Quartile(distances, Quartile.MEDIAN).doubleValue();
		}
		
		
		/**
		 * Translate the XY coordinates of each border point so that
		 * the nuclear centre of mass is at the given point
		 * @param point the new centre of mass
		 */
		public void moveCentreOfMass(IPoint point){
			
			// get the difference between the x and y positions 
			// of the points as offsets to apply
			double xOffset = point.getX() - centreOfMass.getX();
			double yOffset = point.getY() - centreOfMass.getY();		

			offset(xOffset, yOffset);
		}
		
		/**
		 * Translate the XY coordinates of the object
		 * @param xOffset the amount to move in the x-axis
		 * @param yOffset the amount to move in the y-axis
		 */
		public void offset(double xOffset, double yOffset){

			/// update each border point
			for(int i=0; i<borderList.size(); i++){
				IBorderPoint p = borderList.get(i);
				p.offset(xOffset, yOffset);
			}

			this.centreOfMass.offset(xOffset,  yOffset);

		}
		
		public void reverse(){
			
			int[] newXpoints = new int[xpoints.length], newYpoints = new int[xpoints.length];
			
			for(int i=xpoints.length-1, j=0; j<xpoints.length; i--, j++){
				newXpoints[j] = xpoints[i];
				newYpoints[j] = ypoints[i];
			}
			xpoints = newXpoints;
			ypoints = newYpoints;

			
			makeBorderList();	// Recreate the border list from the new key points
		}
		
		/**
		 * Turn the list of border points into a polygon. The points are at the original
		 * position in a source image.
		 * @see Nucleus.getPosition
		 * @return
		 */
		public FloatPolygon createOriginalPolygon(){
			
			double minX = this.getBounds().getX();
			double minY = this.getBounds().getY();
			
			double diffX = position[CellularComponent.X_BASE] - minX;
			double diffY = position[CellularComponent.Y_BASE] - minY;

			return createOffsetPolygon(  (float) diffX,
					                     (float) diffY);
		}

		/**
		 * Turn the list of border points into a closed polygon. 
		 * @return
		 */
		public FloatPolygon createPolygon(){
			return createOffsetPolygon(0, 0);
		}
		
		public Shape toShape(){
			
			// Converts whatever coordinates are in the border
			// to a shape
			return toOffsetShape(0, 0);
			
		}
		
		public Shape toOriginalShape(){
						
			// Calculate the difference between the original CoM and the new CoM
			// and apply this offset
					
			double diffX = originalCentreOfMass.getX() - centreOfMass.getX();
			double diffY = originalCentreOfMass.getY() - centreOfMass.getY();
			
			return toOffsetShape(diffX, diffY);
		}
		
		public List<IPoint> getPixelsAsPoints(){
			
			// Get a list of all the points within the ROI
			List<IPoint> result = new ArrayList<IPoint>(0);
			
			Rectangle b = this.toShape().getBounds();
			
			// get the bounding box of the roi
			// make a list of all the pixels in the roi
			int minX = (int) b.getMinX();
			int maxX = (int) (minX + b.getWidth());
			
			int minY = (int) b.getMinY();
			int maxY = minY + (int) b.getHeight();
			
			for(int x=minX; x<=maxX; x++){
				for(int y=minY; y<=maxY; y++){
					
					if(this.containsPoint(x, y)){

						result.add( IPoint.makeNew(x, y));
					}
				}
			}
			
			if(result.isEmpty()){
//				IJ.log("    Roi has no pixels");
				warn("No points found in roi");
				fine("X base: "+minX
						+"  Y base: "+minY
						+"  X max: "+maxX
						+"  Y max: "+maxY);
			}
			
			return result;
		}
		
		private Shape toOffsetShape(double xOffset, double yOffset){
			Path2D.Double path = new Path2D.Double();
			
			if(borderList.size()==0 || borderList.size()==1){
				throw new IllegalArgumentException("Border list is empty or single entry");
			}
			
			IBorderPoint first = borderList.get(0);
			path.moveTo(first.getX()+xOffset, first.getY()+yOffset);
			
			for(IBorderPoint b : this.borderList){
				path.lineTo(b.getX()+xOffset, b.getY()+yOffset);
			}
			path.closePath();

			return path;
		}

		/**
		 * Make an offset polygon from the border list of this object
		 * @return
		 */
		private FloatPolygon createOffsetPolygon(float xOffset, float yOffset){
			float[] xpoints = new float[borderList.size()+1];
			float[] ypoints = new float[borderList.size()+1];

			for(int i=0;i<borderList.size();i++){
				IBorderPoint p = borderList.get(i);
				xpoints[i] = (float) p.getX() + xOffset;
				ypoints[i] = (float) p.getY() + yOffset;
			}

			// Ensure the polygon is closed
			xpoints[borderList.size()] = (float) borderList.get(0).getX() + xOffset;
			ypoints[borderList.size()] = (float) borderList.get(0).getY() + yOffset;

			return new FloatPolygon(xpoints, ypoints);
		}
		
		public int wrapIndex(int i){
			return wrapIndex(i, this.getBorderLength());
		}
		
		
		 /**
		  * Wrap arrays. If an index falls of the end, it is returned to the start and vice versa
		 * @param i the index
		 * @param length the array length
		 * @return the index within the array
		 */
		public static int wrapIndex(int i, int length){
			 if(i<0){
				 // if the inputs are (-336, 330), this will return -6. Recurse until positive
				 i = length+i;
				 return wrapIndex(i, length);
			 }
			 
			 if(i<length){ // if not wrapping
				 return i;
			 }
			 
			 return i%length;
		 }
		
		/**
		 * Wrap arrays for doubles. This is used in interpolation. 
		 * If an index falls of the end, it is returned to the start and vice versa
		 * @param i the index
		 * @param length the array length
		 * @return the index within the array
		 */
		public static double wrapIndex(double i, int length){
			 if(i<0){
				 i = length+i;
				 return wrapIndex(i, length); // correct for values multiple profile lengths below zero
			 }
			 
			 if(i<length){ // if not wrapping
				 return i;
			 }
			 
			// i is greater than array length
			 
			 return i % length;

		 }
		
		/**
		 * Create a boolean mask, in which 1 is within the nucleus and 0 is outside
		 * the nucleus, for an image centred on the nuclear centre of mass, of the
		 * given size
		 * @param height
		 * @param width
		 * @return
		 */
		public Mask getBooleanMask(int height, int width){
			
			int halfX = width >> 1;
			int halfY = height >> 1;	
				
			boolean[][] result = new boolean[height][width];
						
			for(int x= -halfX, aX=0; aX<width; x++, aX++ ){

				for(int y=-halfY, aY=0; aY<height; y++, aY++ ){

					result[aY][aX] = this.containsPoint( new FloatPoint(x, y) );

				}
				
			}
				
			return new BooleanMask(result);
		}
		
		
		/*
		For two NucleusBorderPoints in a Nucleus, find the point that lies halfway between them
		Used for obtaining a consensus between potential tail positions. Ensure we choose the
		smaller distance
		 */
		public int getPositionBetween(IBorderPoint pointA, IBorderPoint pointB){

			int a = 0;
			int b = 0;
			// find the indices that correspond on the array
			for(int i = 0; i<this.getBorderLength(); i++){
				if(this.getBorderPoint(i).overlaps(pointA)){
					a = i;
				}
				if(this.getBorderPoint(i).overlaps(pointB)){
					b = i;
				}
			}

			// find the higher and lower index of a and b
			int maxIndex = a > b ? a : b;
			int minIndex = a > b ? b : a;

			// there are two midpoints between any points on a ring; we want to take the 
			// midpoint that is in the smaller segment.

			int difference1 = maxIndex - minIndex;
			int difference2 = this.getBorderLength() - difference1;

			// get the midpoint
			int mid1 = this.wrapIndex( (int)Math.floor( (difference1/2)+minIndex ) );
			int mid2 = this.wrapIndex( (int)Math.floor( (difference2/2)+maxIndex ));

			return difference1 < difference2 ? mid1 : mid2;
		}

		public IBorderPoint findOppositeBorder(IBorderPoint p){

			int minDeltaYIndex = 0;
			double minAngle = 180;

			for(int i = 0; i<this.getBorderLength();i++){
				
				double angle = this.getCentreOfMass().findAngle(p, this.getBorderPoint(i));

				if(Math.abs(180 - angle) < minAngle){
					minDeltaYIndex = i;
					minAngle = 180 - angle;
				}
			}
			return this.getBorderPoint(minDeltaYIndex);
		}

		/*
			From the point given, create a line to the CoM. Measure angles from all 
			other points. Pick the point closest to 90 degrees. Can then get opposite
			point. Defaults to input point if unable to find point.
		*/
		public IBorderPoint findOrthogonalBorderPoint(IBorderPoint a){

			IBorderPoint orthgonalPoint = a;
			double bestAngle = 0;

			for(int i=0;i<this.getBorderLength();i++){

				IBorderPoint p = this.getBorderPoint(i);
				double angle = this.getCentreOfMass().findAngle(a, p); 
				if(Math.abs(90-angle)< Math.abs(90-bestAngle)){
					bestAngle = angle;
					orthgonalPoint = p;
				}
			}
			return orthgonalPoint;
		}
		
		
		
		public IBorderPoint findClosestBorderPoint(IPoint p){
			
			double minDist = Double.MAX_VALUE;
			IBorderPoint result = null;
			
			for(IBorderPoint bp : this.borderList){
				
				if(bp.getLengthTo(p)< minDist){
					minDist = bp.getLengthTo(p);
					result = bp;
				}
			}
			return result;
		}
		
		/*
		 * 
		 * SERIALIZATION METHODS
		 * 
		 */
		
		private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
//			finest("\tReading abstract cellular component");
			
			in.defaultReadObject();
			
			
			
			// Fill the transient fields
			imageRef = new SoftReference<ImageProcessor>(null);
			
			// needs to be traced to allow interpolation into the border list
//			PolygonRoi roi = new PolygonRoi(xpoints, ypoints, xpoints.length, Roi.TRACED_ROI);
						
			makeBorderList(); // This will update the border to the original CoM saved
//			this.moveCentreOfMass(getCentreOfMass()); // update border to the  saved CoM
			
		}

		private void writeObject(java.io.ObjectOutputStream out) throws IOException {

			out.defaultWriteObject();
//			log("CoM : "+this.getCentreOfMass().toString());
//			log("B0  : "+xpoints[0]+", "+ypoints[0]);
//			log("Pos : "+this.getPosition()[0]+", "+this.getPosition()[1]);
		}
		
		
		/*
		 * #############################################
		 * Methods implementing the Rotatable interface
		 * #############################################
		 */
		
		/**
		 * Rotate the nucleus so that the given point is directly 
		 * below the centre of mass
		 * @param bottomPoint
		 */
		public void rotatePointToBottom(IPoint bottomPoint){
			this.alignPointsOnVertical(centreOfMass, bottomPoint);
		}
		
		public abstract void alignVertically();
		
		@Override
		public void rotate(double angle) {
			if(angle!=0){

				for(IMutablePoint p : borderList){
//					log(p.toString());
					IPoint newPoint = getPositionAfterRotation(p, angle);
					p.set(newPoint);
//					log(p.toString());
				}
			}
					
		}
		
		/**
		 * Get the position of the given point in this object after the object
		 * has been rotated by the given amount
		 * @param p the point to move
		 * @param angle the angle in degrees
		 * @return
		 */
		protected IPoint getPositionAfterRotation(IPoint p, double angle){
			
			// get the distance from the point to the centre of mass
			double distance = p.getLengthTo(centreOfMass);

			// get the angle between the centre of mass (C), the point (P) and a
			// point directly under the centre of mass (V)

			/*
			 *      C
			 *      |\  
			 *      V P
			 * 
			 */
			double oldAngle = centreOfMass.findAngle( p,
					IPoint.makeNew(centreOfMass.getX(),-10));


			if(p.getX()<centreOfMass.getX()){
				oldAngle = 360-oldAngle;
			}

			double newAngle = oldAngle + angle;
			double newX = new AngleTools().getXComponentOfAngle(distance, newAngle) + centreOfMass.getX();
			double newY = new AngleTools().getYComponentOfAngle(distance, newAngle) + centreOfMass.getY();
			return IPoint.makeNew(newX, newY);
		}
}
