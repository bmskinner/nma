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

package com.bmskinner.nuclear_morphology.samples.dummy;

import java.io.File;
import java.util.Map;
import java.util.UUID;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalCollection;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.sperm.DefaultRodentSpermNucleus;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;

import ij.gui.PolygonRoi;
import ij.gui.Roi;

/**
 * A rodent sperm component taken from a testing dataset
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class DummyRodentSpermNucleus extends DummyCellularComponent implements Nucleus {
	
	private final String name;
	
	private static final float[] X_POINTS = { 97, 98, 99, 100, 101, 102, 102, 103, 104, 105, 105, 106, 107, 108, 109, 109, 110, 110, 111, 111, 112, 112, 112, 113, 113, 113, 113, 113, 113, 113, 113, 113, 113, 113, 112, 112, 112, 112, 112, 111, 111, 111, 111, 110, 110, 110, 110, 109, 109, 109, 109, 108, 108, 107, 107, 107, 107, 106, 106, 105, 105, 104, 104, 103, 103, 102, 102, 101, 101, 100, 99, 99, 98, 97, 97, 96, 96, 95, 94, 94, 93, 93, 92, 91, 90, 90, 89, 88, 87, 87, 86, 85, 84, 83, 83, 82, 81, 80, 79, 78, 77, 76, 76, 75, 74, 73, 72, 71, 70, 70, 69, 68, 67, 66, 65, 64, 63, 62, 61, 60, 59, 59, 58, 57, 56, 55, 54, 53, 52, 51, 50, 49, 48, 47, 46, 45, 44, 43, 42, 41, 40, 39, 38, 37, 36, 35, 34, 33, 32, 31, 30, 29, 28, 27, 26, 25, 24, 23, 22, 21, 20, 19, 18, 17, 16, 15, 14, 13, 12, 11, 10, 10, 9, 8, 7, 6, 5, 4, 3, 2, 2, 1, 0, 0, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 26, 27, 28, 29, 30, 31, 31, 32, 33, 34, 34, 35, 36, 37, 38, 38, 39, 39, 40, 40, 41, 41, 41, 41, 42, 42, 42, 42, 43, 43, 43, 44, 44, 45, 45, 45, 46, 46, 47, 47, 47, 48, 48, 49, 49, 49, 50, 50, 50, 51, 51, 51, 52, 52, 53, 53, 53, 54, 54, 54, 55, 55, 55, 56, 56, 57, 57, 58, 58, 59, 59, 60, 60, 61, 61, 62, 63, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 77, 78, 78, 79, 80, 80, 81, 81, 82, 83, 83, 84, 85, 86, 87, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96 };
	private static final float[] Y_POINTS = { 0, 1, 1, 1, 2, 2, 3, 4, 4, 5, 6, 6, 7, 7, 8, 9, 10, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 52, 53, 54, 55, 56, 57, 57, 58, 59, 60, 61, 61, 62, 63, 64, 65, 65, 66, 67, 68, 69, 69, 70, 70, 71, 72, 72, 73, 74, 74, 75, 75, 76, 76, 77, 77, 78, 78, 79, 80, 80, 80, 81, 82, 82, 83, 83, 84, 84, 84, 85, 85, 86, 86, 86, 87, 87, 88, 88, 88, 89, 89, 89, 90, 90, 90, 91, 91, 91, 92, 92, 92, 92, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 92, 92, 92, 92, 92, 91, 91, 91, 90, 90, 90, 89, 89, 88, 88, 88, 87, 86, 86, 85, 84, 83, 82, 81, 81, 81, 81, 81, 81, 81, 81, 81, 82, 82, 82, 82, 82, 82, 82, 82, 82, 82, 82, 82, 81, 81, 81, 80, 80, 79, 79, 78, 78, 77, 77, 76, 75, 75, 74, 73, 73, 72, 72, 71, 70, 70, 69, 68, 67, 66, 65, 64, 63, 62, 61, 60, 59, 58, 57, 56, 55, 54, 54, 53, 52, 51, 50, 49, 48, 47, 46, 45, 44, 43, 43, 42, 41, 40, 39, 38, 37, 36, 35, 34, 33, 32, 31, 30, 29, 29, 28, 27, 26, 25, 24, 23, 22, 21, 20, 20, 19, 18, 17, 16, 16, 15, 14, 14, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 12, 12, 12, 11, 10, 9, 8, 7, 7, 6, 5, 4, 3, 3, 2, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	private static final double PROFILE_WINDOW = 0.05;
	private static final Roi ROI = new PolygonRoi(X_POINTS, Y_POINTS, Roi.TRACED_ROI);
	private static final int component_NUMBER = 0;
	private static final int IMAGE_CHANNEL = 0;
	private static final int[] POSITION = {105, 34};
	private static final File IMAGE_FILE = new File("samples/images/Testing/s60.tiff"); // This component is from Testing\s60\0
	private static final IPoint COM = IPoint.makeNew(74, 46);
		
	Nucleus nucleus = (Nucleus) component;
	
	
	public DummyRodentSpermNucleus() throws ComponentCreationException{
	    this("default");
	}
	
	public DummyRodentSpermNucleus(String name) throws ComponentCreationException{
		component = new DefaultRodentSpermNucleus(ROI, COM, IMAGE_FILE, IMAGE_CHANNEL,  
	            POSITION , component_NUMBER);
        initialise(PROFILE_WINDOW);	    
        setStatistic(PlottableStatistic.AREA,     4827.00);
        setStatistic(PlottableStatistic.MAX_FERET, 134.27);
        setStatistic(PlottableStatistic.PERIMETER, 347.02);
        setStatistic(PlottableStatistic.MIN_DIAMETER, 53.14);
		this.name = name;		
	}
	
	@Override
	public void findPointsAroundBorder() throws ComponentCreationException {
		nucleus.findPointsAroundBorder();
		
	}

	@Override
	public void initialise(double angleWindowProportion) throws ComponentCreationException {
		nucleus.initialise(angleWindowProportion);
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
		return nucleus.getNameAndNumber();
	}

	@Override
	public int getNucleusNumber() {
		return nucleus.getNucleusNumber();
	}

	@Override
	public String getPathAndNumber() {
		return nucleus.getPathAndNumber();
	}


	@Override
	public void calculateSignalAnglesFromPoint(IBorderPoint p) {
		nucleus.calculateSignalAnglesFromPoint(p);
		
	}

	@Override
	public String dumpInfo(int type) {
		return nucleus.dumpInfo(type);
	}

	@Override
	public ISignalCollection getSignalCollection() {
		return nucleus.getSignalCollection();
	}

	@Override
	public Nucleus getVerticallyRotatedNucleus() {
		return nucleus.getVerticallyRotatedNucleus();
	}

	@Override
	public void updateVerticallyRotatedNucleus() {
		nucleus.updateVerticallyRotatedNucleus();
	}

	@Override
	public boolean isClockwiseRP() {
		return nucleus.isClockwiseRP();
	}

	

	@Override
	public boolean equals(CellularComponent c) {
		return nucleus.equals(c);
	}

	@Override
	public boolean hasProfile(ProfileType type) {
		return nucleus.hasProfile(type);
	}

	@Override
	public ISegmentedProfile getProfile(ProfileType type) throws UnavailableProfileTypeException {
		return nucleus.getProfile(type);
	}

	@Override
	public void setProfile(ProfileType type, ISegmentedProfile profile) {
		nucleus.setProfile(type, profile);
		
	}

	@Override
	public int getWindowSize(ProfileType type) {
		return nucleus.getWindowSize(type);
	}

	@Override
	public double getWindowProportion(ProfileType type) {
		return nucleus.getWindowProportion(type);
	}

	@Override
	public void setWindowProportion(ProfileType type, double d) {
		nucleus.setWindowProportion(type, d);
	}

	@Override
	public boolean isLocked() {
		return nucleus.isLocked();
	}

	@Override
	public void setLocked(boolean b) {
		nucleus.setLocked(b);
	}

	@Override
	public void setSegmentStartLock(boolean lock, UUID segID) {
		nucleus.setSegmentStartLock(lock, segID);
	}

	

	@Override
	public void calculateProfiles() throws ProfileException {
		nucleus.calculateProfiles();
	}


	@Override
	public int getBorderIndex(Tag tag) {
	    return nucleus.getBorderIndex(tag);
	}

	@Override
	public Tag getBorderTag(Tag tag, int index) {
		return nucleus.getBorderTag(tag, index);
	}

	@Override
	public Tag getBorderTag(int index) {
	    return nucleus.getBorderTag(index);
	}

	@Override
	public IBorderPoint getBorderTag(Tag tag) throws UnavailableBorderTagException {
		return nucleus.getBorderTag(tag);
	}

	@Override
	public boolean hasBorderTag(Tag tag) {
		return nucleus.hasBorderTag(tag);
	}

	@Override
	public boolean hasBorderTag(Tag tag, int i) {
		return nucleus.hasBorderTag(tag, i);
	}

	@Override
	public boolean hasBorderTag(int index) {
		return nucleus.hasBorderTag(index);
	}

	@Override
	public void setBorderTag(Tag tag, int i) {
		nucleus.setBorderTag(tag, i);
	}

	@Override
	public void setBorderTag(Tag reference, Tag tag,
			int i) {
		nucleus.setBorderTag(reference, tag, i);		
	}

	@Override
	public ISegmentedProfile getProfile(ProfileType type, Tag tag) throws UnavailableBorderTagException, UnavailableProfileTypeException, ProfileException {
		return nucleus.getProfile(type, tag);
	}

	@Override
	public void setProfile(ProfileType type, Tag tag,
			ISegmentedProfile profile) {
		setProfile(type, profile);
		
	}

	@Override
	public Map<Tag, Integer> getBorderTags() {
		return nucleus.getBorderTags();
	}

	@Override
	public IBorderPoint getBorderPoint(Tag tag) throws UnavailableBorderTagException {
		return nucleus.getBorderPoint(tag);
	}

	@Override
	public int getOffsetBorderIndex(Tag reference, int index) {
		return nucleus.getOffsetBorderIndex(reference, index);
	}

	@Override
	public void replaceBorderTags(Map<Tag, Integer> tagMap) {
		nucleus.replaceBorderTags(tagMap);
	}

	
	@Override
	public double getDistanceFromCoMToBorderAtAngle(double angle) {
		return nucleus.getDistanceFromCoMToBorderAtAngle(angle);
	}

	@Override
	public int compareTo(Nucleus o) {
		return nucleus.compareTo(o);
	}

	

	

	

}
