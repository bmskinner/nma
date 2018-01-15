package samples.dummy;

import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.bmskinner.nuclear_morphology.analysis.detection.Mask;
import com.bmskinner.nuclear_morphology.components.AbstractCellularComponent;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.generic.IMutablePoint;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderPointException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderPoint;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;

import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;

public class DummyCellularComponent implements CellularComponent {
	
	private final UUID        id;

    /**
     * The original position in the source image of the component. Values are
     * stored at the indexes in {@link CellularComponent.X_BASE},
     * {@link CellularComponent.Y_BASE}, {@link CellularComponent.WIDTH} and
     * {@link CellularComponent.HEIGHT}
     * 
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
     * The statistical values stored for this object
     */
    private Map<PlottableStatistic, Double> statistics = new HashMap<PlottableStatistic, Double>();

    /**
     * The RGB channel in which this component was detected
     */
    private int channel = 0;
    private double scale = 1;

    /**
     * The points within the Roi from which the object was detected.
     */
    private int[] xpoints, ypoints;

    /**
     * The complete border list, offset to an appropriate position for the
     * object
     */
    private transient List<IBorderPoint> borderList = new ArrayList<>(0);                                                                                         // while
                                                                                                        // memory                                                                                                    // is
//	public DummyCellularComponent(List<IPoint> border, IPoint centreOfMass){
//		
//		
//		if (centreOfMass == null) {
//            throw new IllegalArgumentException("Centre of mass cannot be null");
//        }
//
//        if (roi == null) {
//            throw new IllegalArgumentException("Roi cannot be null");
//        }
//
//        this.originalCentreOfMass = IPoint.makeNew(centreOfMass);
//        this.centreOfMass = IMutablePoint.makeNew(centreOfMass);
//        this.id = java.util.UUID.randomUUID();
//
//        // Store the original points. From these, the smooth polygon can be
//        // reconstructed.
//        double epsilon = 1;
//        Polygon polygon = roi.getPolygon();
//        Rectangle2D bounds = polygon.getBounds().getFrame();
//
//        // // since small signals can have imprecision on the CoM that puts them
//        // on the border of the
//        // // object, add a small border to consider OK
//
//        double minX = bounds.getX();
//        double maxX = minX + bounds.getWidth();
//
//        minX -= epsilon;
//        maxX += epsilon;
//
//        if (centreOfMass.getX() < minX || centreOfMass.getX() > maxX) {
//            throw new IllegalArgumentException("The centre of mass X (" + centreOfMass.getX() + ")"
//                    + ") must be within the roi bounds (x = " + minX + "-" + maxX + ")");
//        }
//
//        double minY = bounds.getY();
//        double maxY = minY + bounds.getHeight();
//        minY -= epsilon;
//        maxY += epsilon;
//
//        if (centreOfMass.getY() < minY || centreOfMass.getY() > maxY) {
//            throw new IllegalArgumentException("The centre of mass Y (" + centreOfMass.getY() + ")"
//                    + ") must be within the roi bounds (y = " + minY + "-" + maxY + ")");
//        }
//
//        if (!polygon.contains(centreOfMass.getX(), centreOfMass.getY())) {
//            fine("Centre of mass is not inside the object. You may have a doughnut.");
//        }
//
//        this.xpoints = new int[polygon.npoints];
//        this.ypoints = new int[polygon.npoints];
//
//        // Discard empty indices left in polygon array
//        for (int i = 0; i < polygon.npoints; i++) {
//            this.xpoints[i] = polygon.xpoints[i];
//            this.ypoints[i] = polygon.ypoints[i];
//            // log("\tPoint at "+i+": "+this.xpoints[i]+", "+this.ypoints[i]);
//        }
//
//        // convert the roi positions to a list of nucleus border points
//        // Only smooth the points for large objects like nuclei
//        // log("Int array in constructor : "+this.xpoints[0]+",
//        // "+this.ypoints[0]);
//        makeBorderList();
//
//    }

    /**
     * Create the border list from the stored int[] points. Move the centre of
     * mass to any stored position.
     * 
     * @param roi
     */
    private void makeBorderList() {

        // Make a copy of the int[] points otherwise creating a polygon roi
        // will reset them to 0,0 coordinates
        int[] xcopy = Arrays.copyOf(xpoints, xpoints.length);
        int[] ycopy = Arrays.copyOf(ypoints, ypoints.length);
        PolygonRoi roi = new PolygonRoi(xcopy, ycopy, xpoints.length, Roi.TRACED_ROI);

        // Creating the border list will set everything to the original image
        // position.
        // Move the border list back over the CoM if needed.
        IPoint oldCoM = IPoint.makeNew(centreOfMass);
        centreOfMass = IMutablePoint.makeNew(originalCentreOfMass);

        borderList = new ArrayList<>(0);

        // convert the roi positions to a list of border points
        // Each object decides whether it should be smoothed.
        boolean isSmooth = isSmoothByDefault();
        FloatPolygon smoothed = roi.getInterpolatedPolygon(1, isSmooth);

        for (int i = 0; i < smoothed.npoints; i++) {
            IBorderPoint point = IBorderPoint.makeNew(smoothed.xpoints[i], smoothed.ypoints[i]);

            if (i > 0) {
                point.setPrevPoint(borderList.get(i - 1));
                point.prevPoint().setNextPoint(point);
            }
            borderList.add(point);
        }
        // link endpoints
        borderList.get(borderList.size() - 1).setNextPoint(borderList.get(0));
        borderList.get(0).setPrevPoint(borderList.get(borderList.size() - 1));

        moveCentreOfMass(oldCoM);

    }
    

	@Override
	public int[] getPosition() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IPoint getOriginalBase() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IPoint getBase() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getChannel() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ImageProcessor getImage() throws UnloadableImageException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ImageProcessor getRGBImage() throws UnloadableImageException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ImageProcessor getComponentImage() throws UnloadableImageException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ImageProcessor getComponentRGBImage() throws UnloadableImageException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Rectangle2D getBounds() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public File getSourceFolder() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public File getSourceFile() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSourceFileName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSourceFileNameWithoutExtension() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateSourceFolder(File newFolder) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setSourceFile(File sourceFile) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setChannel(int channel) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setSourceFolder(File sourceFolder) {
		// TODO Auto-generated method stub

	}

	@Override
	public void alignVertically() {
		// TODO Auto-generated method stub

	}

	@Override
	public void rotatePointToBottom(IPoint bottomPoint) {
		// TODO Auto-generated method stub

	}

	@Override
	public void rotate(double angle) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean hasStatistic(PlottableStatistic stat) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public double getStatistic(PlottableStatistic stat, MeasurementScale scale) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getStatistic(PlottableStatistic stat) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setStatistic(PlottableStatistic stat, double d) {
		// TODO Auto-generated method stub

	}

	@Override
	public PlottableStatistic[] getStatistics() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UUID getID() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean equals(CellularComponent c) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public CellularComponent duplicate() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isSmoothByDefault() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void updateDependentStats() {
		// TODO Auto-generated method stub

	}

	@Override
	public double getScale() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setScale(double scale) {
		// TODO Auto-generated method stub

	}

	@Override
	public IPoint getCentreOfMass() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IPoint getOriginalCentreOfMass() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IBorderPoint getBorderPoint(int i) throws UnavailableBorderPointException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IBorderPoint getOriginalBorderPoint(int i) throws UnavailableBorderPointException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getBorderIndex(IBorderPoint p) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void updateBorderPoint(int i, double x, double y) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateBorderPoint(int i, IPoint p) {
		// TODO Auto-generated method stub

	}

	@Override
	public int getBorderLength() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<IBorderPoint> getBorderList() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<IBorderPoint> getOriginalBorderList() throws UnavailableBorderPointException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean containsPoint(IPoint p) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean containsPoint(int x, int y) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean containsOriginalPoint(IPoint p) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public double getMaxX() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getMinX() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getMaxY() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getMinY() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void flipXAroundPoint(IPoint p) {
		// TODO Auto-generated method stub

	}

	@Override
	public double getMedianDistanceBetweenPoints() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void moveCentreOfMass(IPoint point) {
		// TODO Auto-generated method stub

	}

	@Override
	public void offset(double xOffset, double yOffset) {
		// TODO Auto-generated method stub

	}

	@Override
	public int wrapIndex(int i) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public FloatPolygon toPolygon() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FloatPolygon toOriginalPolygon() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Shape toShape() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Shape toShape(MeasurementScale scale) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Shape toOriginalShape() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Roi toRoi() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Roi toOriginalRoi() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Mask getBooleanMask(int height, int width) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Mask getSourceBooleanMask() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getPositionBetween(IBorderPoint pointA, IBorderPoint pointB) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public IBorderPoint findOppositeBorder(IBorderPoint p) throws UnavailableBorderPointException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IBorderPoint findOrthogonalBorderPoint(IBorderPoint a) throws UnavailableBorderPointException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IBorderPoint findClosestBorderPoint(IPoint p) throws UnavailableBorderPointException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void reverse() {
		// TODO Auto-generated method stub

	}

}
