/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
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

package samples.dummy;

import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;

import java.awt.Rectangle;
import java.awt.Shape;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.bmskinner.nuclear_morphology.analysis.detection.Mask;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.generic.BorderTagObject;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.generic.XYPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.BorderPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalCollection;
import com.bmskinner.nuclear_morphology.components.nuclear.SignalCollection;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.sperm.DefaultRodentSpermNucleus;
import com.bmskinner.nuclear_morphology.components.stats.NucleusStatistic;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;

/**
 * A rodent sperm nucleus taken from a testing dataset
 * @author bms41
 *
 */
public class DummyRodentSpermNucleus implements Nucleus {
	
	private final String name;
	
	private static final float[] X_POINTS = { 97, 98, 99, 100, 101, 102, 102, 103, 104, 105, 105, 106, 107, 108, 109, 109, 110, 110, 111, 111, 112, 112, 112, 113, 113, 113, 113, 113, 113, 113, 113, 113, 113, 113, 112, 112, 112, 112, 112, 111, 111, 111, 111, 110, 110, 110, 110, 109, 109, 109, 109, 108, 108, 107, 107, 107, 107, 106, 106, 105, 105, 104, 104, 103, 103, 102, 102, 101, 101, 100, 99, 99, 98, 97, 97, 96, 96, 95, 94, 94, 93, 93, 92, 91, 90, 90, 89, 88, 87, 87, 86, 85, 84, 83, 83, 82, 81, 80, 79, 78, 77, 76, 76, 75, 74, 73, 72, 71, 70, 70, 69, 68, 67, 66, 65, 64, 63, 62, 61, 60, 59, 59, 58, 57, 56, 55, 54, 53, 52, 51, 50, 49, 48, 47, 46, 45, 44, 43, 42, 41, 40, 39, 38, 37, 36, 35, 34, 33, 32, 31, 30, 29, 28, 27, 26, 25, 24, 23, 22, 21, 20, 19, 18, 17, 16, 15, 14, 13, 12, 11, 10, 10, 9, 8, 7, 6, 5, 4, 3, 2, 2, 1, 0, 0, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 26, 27, 28, 29, 30, 31, 31, 32, 33, 34, 34, 35, 36, 37, 38, 38, 39, 39, 40, 40, 41, 41, 41, 41, 42, 42, 42, 42, 43, 43, 43, 44, 44, 45, 45, 45, 46, 46, 47, 47, 47, 48, 48, 49, 49, 49, 50, 50, 50, 51, 51, 51, 52, 52, 53, 53, 53, 54, 54, 54, 55, 55, 55, 56, 56, 57, 57, 58, 58, 59, 59, 60, 60, 61, 61, 62, 63, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 77, 78, 78, 79, 80, 80, 81, 81, 82, 83, 83, 84, 85, 86, 87, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96 };
	private static final float[] Y_POINTS = { 0, 1, 1, 1, 2, 2, 3, 4, 4, 5, 6, 6, 7, 7, 8, 9, 10, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 52, 53, 54, 55, 56, 57, 57, 58, 59, 60, 61, 61, 62, 63, 64, 65, 65, 66, 67, 68, 69, 69, 70, 70, 71, 72, 72, 73, 74, 74, 75, 75, 76, 76, 77, 77, 78, 78, 79, 80, 80, 80, 81, 82, 82, 83, 83, 84, 84, 84, 85, 85, 86, 86, 86, 87, 87, 88, 88, 88, 89, 89, 89, 90, 90, 90, 91, 91, 91, 92, 92, 92, 92, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 92, 92, 92, 92, 92, 91, 91, 91, 90, 90, 90, 89, 89, 88, 88, 88, 87, 86, 86, 85, 84, 83, 82, 81, 81, 81, 81, 81, 81, 81, 81, 81, 82, 82, 82, 82, 82, 82, 82, 82, 82, 82, 82, 82, 81, 81, 81, 80, 80, 79, 79, 78, 78, 77, 77, 76, 75, 75, 74, 73, 73, 72, 72, 71, 70, 70, 69, 68, 67, 66, 65, 64, 63, 62, 61, 60, 59, 58, 57, 56, 55, 54, 54, 53, 52, 51, 50, 49, 48, 47, 46, 45, 44, 43, 43, 42, 41, 40, 39, 38, 37, 36, 35, 34, 33, 32, 31, 30, 29, 29, 28, 27, 26, 25, 24, 23, 22, 21, 20, 20, 19, 18, 17, 16, 16, 15, 14, 14, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 12, 12, 12, 11, 10, 9, 8, 7, 7, 6, 5, 4, 3, 3, 2, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	private static final double PROFILE_WINDOW = 0.05;
	private static final Roi ROI = new PolygonRoi(X_POINTS, Y_POINTS, Roi.TRACED_ROI);
	private static final int NUCLEUS_NUMBER = 0;
	private static final int IMAGE_CHANNEL = 0;
	private static final int[] POSITION = {105, 34};
	private static final File IMAGE_FILE = new File("samples/images/Testing/s60.tiff"); // This nucleus is from Testing\s60\0
	private static final IPoint COM = IPoint.makeNew(74, 46);
	
	private static final Nucleus NUCLEUS = new DefaultRodentSpermNucleus(ROI, COM, IMAGE_FILE, IMAGE_CHANNEL,  
            POSITION , NUCLEUS_NUMBER);
	
	
	public DummyRodentSpermNucleus() throws ComponentCreationException{
	    this("default");
	}
	
	public DummyRodentSpermNucleus(String name) throws ComponentCreationException{
        initialise(PROFILE_WINDOW);	    
        setStatistic(PlottableStatistic.AREA,     4827.00);
        setStatistic(PlottableStatistic.MAX_FERET, 134.27);
        setStatistic(PlottableStatistic.PERIMETER, 347.02);
        setStatistic(PlottableStatistic.MIN_DIAMETER, 53.14);
		this.name = name;		
	}
	
	
		
	public String toString(){
	    return NUCLEUS.toString();
	}

	@Override
	public void findPointsAroundBorder() throws ComponentCreationException {
	    NUCLEUS.findPointsAroundBorder();
		
	}

	@Override
	public void initialise(double angleWindowProportion) throws ComponentCreationException {
		NUCLEUS.initialise(angleWindowProportion);
	}

	@Override
	public Nucleus duplicate() {
		try {
            return new DummyRodentSpermNucleus(name);
        } catch (ComponentCreationException e) {
            e.printStackTrace();
            return null;
        }
	}

	@Override
	public String getNameAndNumber() {
		return NUCLEUS.getNameAndNumber();
	}

	@Override
	public int getNucleusNumber() {
		return NUCLEUS.getNucleusNumber();
	}

	@Override
	public String getPathAndNumber() {
		return NUCLEUS.getPathAndNumber();
	}


	@Override
	public void calculateSignalAnglesFromPoint(IBorderPoint p) {
	    NUCLEUS.calculateSignalAnglesFromPoint(p);
		
	}

	@Override
	public String dumpInfo(int type) {
		return NUCLEUS.dumpInfo(type);
	}

	@Override
	public ISignalCollection getSignalCollection() {
		return NUCLEUS.getSignalCollection();
	}


	@Override
	public void updateSourceFolder(File newFolder) {
	    NUCLEUS.updateSourceFolder(newFolder);
		
	}

	@Override
	public Nucleus getVerticallyRotatedNucleus() {
		return NUCLEUS.getVerticallyRotatedNucleus();
	}

	@Override
	public void updateVerticallyRotatedNucleus() {
	    NUCLEUS.updateVerticallyRotatedNucleus();
	}

	@Override
	public boolean isClockwiseRP() {
		return NUCLEUS.isClockwiseRP();
	}

	@Override
	public UUID getID() {
		return NUCLEUS.getID();
	}

	@Override
	public boolean equals(CellularComponent c) {
		return NUCLEUS.equals(c);
	}

	@Override
	public double getStatistic(PlottableStatistic stat, MeasurementScale scale) {
		return NUCLEUS.getStatistic(stat, scale);
	}

	@Override
	public double getStatistic(PlottableStatistic stat) {
	    return NUCLEUS.getStatistic(stat);
	}

	@Override
	public void setStatistic(PlottableStatistic stat, double d) {
		NUCLEUS.setStatistic(stat, d);
	}

	@Override
	public PlottableStatistic[] getStatistics() {
		return NUCLEUS.getStatistics();
	}

	@Override
	public double getScale() {
		return NUCLEUS.getScale();
	}

	@Override
	public void setScale(double scale) {
	    NUCLEUS.setScale(scale);
	}

	@Override
	public IPoint getCentreOfMass() {
		return NUCLEUS.getCentreOfMass();
	}

	@Override
	public BorderPoint getBorderPoint(int i) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IBorderPoint getOriginalBorderPoint(int i) {
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
		return 345;
	}

	@Override
	public List<IBorderPoint> getBorderList() {
		// TODO Auto-generated method stub
		return null;
	}

//	@Override
//	public void setBorderList(List<IBorderPoint> list) {
//		// TODO Auto-generated method stub
//		
//	}

	@Override
	public List<IBorderPoint> getOriginalBorderList() {
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
	public Shape toOriginalShape() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FloatPolygon toPolygon() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Mask getBooleanMask(int height, int width) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getPositionBetween(IBorderPoint pointA, IBorderPoint pointB) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public BorderPoint findOppositeBorder(IBorderPoint p) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IBorderPoint findOrthogonalBorderPoint(IBorderPoint a) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IBorderPoint findClosestBorderPoint(IPoint p) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int[] getPosition() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public File getSourceFile() {
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
	public ImageProcessor getComponentImage() throws UnloadableImageException {
		// TODO Auto-generated method stub
		return null;
	}

//	@Override
//	public List<IPoint> getPixelsAsPoints() {
//		// TODO Auto-generated method stub
//		return null;
//	}

	@Override
	public Rectangle getBounds() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public File getSourceFolder() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSourceFileName() {
		// TODO Auto-generated method stub
		return null;
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
	public boolean hasProfile(ProfileType type) {
		return NUCLEUS.hasProfile(type);
	}

	@Override
	public ISegmentedProfile getProfile(ProfileType type) throws UnavailableProfileTypeException {
		return NUCLEUS.getProfile(type);
	}

	@Override
	public void setProfile(ProfileType type, ISegmentedProfile profile) {
	    NUCLEUS.setProfile(type, profile);
		
	}

	@Override
	public int getWindowSize(ProfileType type) {
		return NUCLEUS.getWindowSize(type);
	}

	@Override
	public double getWindowProportion(ProfileType type) {
		return NUCLEUS.getWindowProportion(type);
	}

	@Override
	public void setWindowProportion(ProfileType type, double d) {
	    NUCLEUS.setWindowProportion(type, d);
	}

	@Override
	public boolean isLocked() {
		return NUCLEUS.isLocked();
	}

	@Override
	public void setLocked(boolean b) {
	    NUCLEUS.setLocked(b);
	}

	@Override
	public void setSegmentStartLock(boolean lock, UUID segID) {
	    NUCLEUS.setSegmentStartLock(lock, segID);
	}

	@Override
	public void reverse() {
	    NUCLEUS.reverse();
	}

	@Override
	public void calculateProfiles() throws ProfileException {
	    NUCLEUS.calculateProfiles();
	}


	@Override
	public int getBorderIndex(Tag tag) {
	    return NUCLEUS.getBorderIndex(tag);
	}

	@Override
	public Tag getBorderTag(Tag tag, int index) {
		return null;
	}

	@Override
	public Tag getBorderTag(int index) {
	    return NUCLEUS.getBorderTag(index);
	}

	@Override
	public IBorderPoint getBorderTag(Tag tag) throws UnavailableBorderTagException {
		// TODO Auto-generated method stub
		return NUCLEUS.getBorderTag(tag);
	}

	@Override
	public boolean hasBorderTag(Tag tag) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasBorderTag(Tag tag, int i) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasBorderTag(int index) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setBorderTag(Tag tag, int i) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setBorderTag(Tag reference, Tag tag,
			int i) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ISegmentedProfile getProfile(ProfileType type, Tag tag) throws UnavailableBorderTagException, UnavailableProfileTypeException, ProfileException {
		return NUCLEUS.getProfile(type, tag);
	}

	@Override
	public void setProfile(ProfileType type, Tag tag,
			ISegmentedProfile profile) {
		setProfile(type, profile);
		
	}

	@Override
	public Map<Tag, Integer> getBorderTags() {

		return null;
	}

	@Override
	public BorderPoint getBorderPoint(Tag tag) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getOffsetBorderIndex(Tag reference, int index) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void replaceBorderTags(Map<Tag, Integer> tagMap) {
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
	public IPoint getOriginalCentreOfMass() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int compareTo(Nucleus o) {
		return 0;
	}

	@Override
	public boolean isSmoothByDefault() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getSourceFileNameWithoutExtension() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateDependentStats() {
	    NUCLEUS.updateDependentStats();
	}

	@Override
	public double getDistanceFromCoMToBorderAtAngle(double angle) {
		return NUCLEUS.getDistanceFromCoMToBorderAtAngle(angle);
	}

	@Override
	public ImageProcessor getRGBImage() throws UnloadableImageException {
		return NUCLEUS.getRGBImage();
	}

	@Override
	public IPoint getOriginalBase() {
		return NUCLEUS.getOriginalBase();
	}

	@Override
	public ImageProcessor getComponentRGBImage()
			throws UnloadableImageException {
		return NUCLEUS.getComponentRGBImage();
	}

	@Override
	public Mask getSourceBooleanMask() {
		return NUCLEUS.getSourceBooleanMask();
	}

	@Override
	public boolean hasStatistic(PlottableStatistic stat) {
		return NUCLEUS.hasStatistic(stat);
	}

	@Override
	public Shape toShape(MeasurementScale scale) {
		return NUCLEUS.toShape(scale);
	}

	@Override
	public Roi toRoi() {
		return NUCLEUS.toRoi();
	}

	@Override
	public Roi toOriginalRoi() {
		return NUCLEUS.toOriginalRoi();
	}

	@Override
	public IPoint getBase() {
		return NUCLEUS.getBase();
	}

}
