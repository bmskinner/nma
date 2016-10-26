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

package components;

import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import io.UnloadableImageException;

import java.awt.Rectangle;
import java.awt.Shape;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import stats.NucleusStatistic;
import stats.PlottableStatistic;
import components.generic.BorderTagObject;
import components.generic.IPoint;
import components.generic.ISegmentedProfile;
import components.generic.MeasurementScale;
import components.generic.Profile;
import components.generic.ProfileType;
import components.generic.SegmentedProfile;
import components.generic.Tag;
import components.generic.XYPoint;
import components.nuclear.BorderPoint;
import components.nuclear.IBorderPoint;
import components.nuclear.SignalCollection;
import components.nuclei.Nucleus;

/**
 * A rodent sperm nucleus that has only a profile, but no shape.
 * @author bms41
 *
 */
public class DummyRodentSpermNucleus extends DummyNucleus {
	
	private static final double DEFAULT_AREA      = 2000;
	private static final double DEFAULT_PERIMETER = 400;
	
	private Map<ProfileType, ISegmentedProfile> profiles = new HashMap<ProfileType, ISegmentedProfile>();
	
	private double area;
	private double perimeter;
	
	private final String name;
	
	public DummyRodentSpermNucleus(String name, ISegmentedProfile p){
		this.name = name;
		
		try {
			
			this.setProfile(ProfileType.ANGLE, p);
			this.setProfile(ProfileType.DIAMETER, p);
			this.setProfile(ProfileType.RADIUS, p);
			this.setProfile(ProfileType.FRANKEN, p);
			
			
			double random = (0.5 - Math.random()) * 20; // up to 200 pixels variation from default value for area
			this.area = DEFAULT_AREA + random;
			
			random = (0.5 - Math.random()) * 5; // up to 5 pixels variation from default value for area
			
			this.perimeter = DEFAULT_PERIMETER + random;
			
		} catch (Exception e) {
			
		}
		
	}
	
	public DummyRodentSpermNucleus(String name){
		this(name, new DummyMouseProfile());		
	}
	
	public String toString(){
		String out = "";
		for(ProfileType t : ProfileType.values()){
			int size = 0;
			if(profiles.containsKey(t)){
			 size = profiles.get(t).size();
			}
			out += t+" : "+size+"\n";
		}
		return out;
	}

	@Override
	public void findPointsAroundBorder() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void intitialiseNucleus(double angleWindowProportion)
			throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Nucleus duplicate() {
		return new DummyRodentSpermNucleus(name, profiles.get(ProfileType.ANGLE));
	}

	@Override
	public String getNameAndNumber() {
		return name;
	}

	@Override
	public String getImageNameWithoutExtension() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public File getOutputFolder() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPathWithoutExtension() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getNucleusNumber() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getPathAndNumber() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setOutputFolder(String f) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void calculateSignalAnglesFromPoint(IBorderPoint p) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String dumpInfo(int type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SignalCollection getSignalCollection() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getOutputFolderName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateSourceFolder(File newFolder) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Nucleus getVerticallyRotatedNucleus() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateVerticallyRotatedNucleus() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isClockwiseRP() {
		// TODO Auto-generated method stub
		return false;
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
	public double getStatistic(PlottableStatistic stat, MeasurementScale scale) {
		
		if( ! ( stat instanceof NucleusStatistic )){
			return 0;
		}
		
		switch( (NucleusStatistic) stat){
			case AREA:
				return area;
			case PERIMETER:
				return perimeter;
			default:
				return 0;
		}

	}

	@Override
	public double getSafeStatistic(PlottableStatistic stat,
			MeasurementScale scale) {
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
	public double getScale() {
		return 1;
	}

	@Override
	public void setScale(double scale) {}

	@Override
	public XYPoint getCentreOfMass() {
		// TODO Auto-generated method stub
		return null;
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

	@Override
	public void setBorderList(List<IBorderPoint> list) {
		// TODO Auto-generated method stub
		
	}

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
	public FloatPolygon createOriginalPolygon() {
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
	public FloatPolygon createPolygon() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean[][] getBooleanMask(int height, int width) {
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
	public double[] getPosition() {
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

	@Override
	public List<IPoint> getPixelsAsPoints() {
		// TODO Auto-generated method stub
		return null;
	}

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
	public void setPosition(double[] position) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setBoundingRectangle(Rectangle boundingRectangle) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setSourceFileName(String name) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setSourceFolder(File sourceFolder) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean hasProfile(ProfileType type) {
		return profiles.containsKey(type);
	}

	@Override
	public ISegmentedProfile getProfile(ProfileType type) {
		return profiles.get(type);
	}

	@Override
	public void setProfile(ProfileType type, ISegmentedProfile profile) {
		profiles.put(type, profile);
		
	}

	@Override
	public int getWindowSize(ProfileType type) {
		return 15;
	}

	@Override
	public double getWindowProportion(ProfileType type) {
		return 0.05;
	}

	@Override
	public void setWindowProportion(ProfileType type, double d) {}

	@Override
	public boolean isLocked() {
		return false;
	}

	@Override
	public void setLocked(boolean b) {	}

	@Override
	public void setSegmentStartLock(boolean lock, UUID segID) {

	}

	@Override
	public void reverse() {

	}

	@Override
	public void calculateProfiles() {

	}

	@Override
	public double getPathLength(ProfileType type) {
		// TODO Auto-generated method stub
		return 400;
	}

	@Override
	public int getBorderIndex(Tag tag) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Tag getBorderTag(Tag tag, int index) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BorderTagObject getBorderTag(int index) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BorderPoint getBorderTag(Tag tag) {
		// TODO Auto-generated method stub
		return null;
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
	public ISegmentedProfile getProfile(ProfileType type, Tag tag) {
		return profiles.get(type);
	}

	@Override
	public void setProfile(ProfileType type, Tag tag,
			ISegmentedProfile profile) throws Exception {
		setProfile(type, profile);
		
	}

	@Override
	public Map<BorderTagObject, Integer> getBorderTags() {

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
	public void replaceBorderTags(Map<BorderTagObject, Integer> tagMap) {
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

}
