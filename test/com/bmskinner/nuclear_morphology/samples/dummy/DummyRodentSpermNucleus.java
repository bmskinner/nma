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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
import com.bmskinner.nuclear_morphology.components.cells.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.generic.FloatPoint;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.nuclei.DefaultNucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileSegment;
import com.bmskinner.nuclear_morphology.components.profiles.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.MissingProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.components.rules.OrientationMark;
import com.bmskinner.nuclear_morphology.components.rules.PriorityAxis;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;
import com.bmskinner.nuclear_morphology.components.signals.ISignalCollection;

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
	private static final File IMAGE_FILE = new File("test/samples/images/Testing/s60.tiff"); // This component is from Testing\s60\0
	private static final IPoint COM = new FloatPoint(74, 46);
	
	
	public static final double AREA         = 4827.00;
	public static final double MAX_FERET    = 134.27;
	public static final double PERIMETER    = 347.02;
	public static final double MIN_DIAMETER = 53.14;
		
	Nucleus nucleus = (Nucleus) component;
	
	
	public DummyRodentSpermNucleus() throws ComponentCreationException{
	    this("default");
	}
	
	public DummyRodentSpermNucleus(String name) throws ComponentCreationException{
		component = new DefaultNucleus(ROI, COM, IMAGE_FILE, IMAGE_CHANNEL,  
	            105, 35, component_NUMBER, RuleSetCollection.mouseSpermRuleSetCollection());
		nucleus = (Nucleus) component;
		nucleus.offset(COM.getX(), COM.getY());
        initialise(PROFILE_WINDOW);	    
        setMeasurement(Measurement.AREA,     AREA);
        setMeasurement(Measurement.PERIMETER, PERIMETER);
        setMeasurement(Measurement.MIN_DIAMETER, MIN_DIAMETER);
		this.name = name;		
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
	public void calculateSignalAnglesFromPoint(@NonNull IPoint p) {
		nucleus.calculateSignalAnglesFromPoint(p);
		
	}

	@Override
	public ISignalCollection getSignalCollection() {
		return nucleus.getSignalCollection();
	}

	@Override
	public Nucleus getOrientedNucleus() throws MissingLandmarkException {
		return nucleus.getOrientedNucleus();
	}

	@Override
	public boolean hasProfile(@NonNull ProfileType type) {
		return nucleus.hasProfile(type);
	}

	@Override
	public ISegmentedProfile getProfile(@NonNull ProfileType type) throws MissingProfileException, ProfileException, MissingLandmarkException {
		return nucleus.getProfile(type);
	}

	@Override
	public int getWindowSize() {
		return nucleus.getWindowSize();
	}

	@Override
	public double getWindowProportion() {
		return nucleus.getWindowProportion();
	}

	@Override
	public void setWindowProportion(double d) {
		nucleus.setWindowProportion(d);
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
	public void setSegmentStartLock(boolean lock, @NonNull UUID segID) {
		nucleus.setSegmentStartLock(lock, segID);
	}

	

//	@Override
//	public void calculateProfiles() throws ProfileException {
//		nucleus.calculateProfiles();
//	}


	@Override
	public int getBorderIndex(@NonNull Landmark tag) throws MissingLandmarkException {
	    return nucleus.getBorderIndex(tag);
	}

	@Override
	public Landmark getBorderTag(int index) {
	    return nucleus.getBorderTag(index);
	}

	@Override
	public boolean hasLandmark(@NonNull Landmark tag) {
		return nucleus.hasLandmark(tag);
	}

	@Override
	public void setLandmark(@NonNull Landmark tag, int i) throws IndexOutOfBoundsException, MissingProfileException, MissingLandmarkException, ProfileException {
		nucleus.setLandmark(tag, i);
	}

	@Override
	public ISegmentedProfile getProfile(@NonNull ProfileType type, @NonNull Landmark tag) throws MissingLandmarkException, MissingProfileException, ProfileException {
		return nucleus.getProfile(type, tag);
	}

	@Override
	public void setSegments(@NonNull List<IProfileSegment> segs) throws MissingLandmarkException, ProfileException {
		nucleus.setSegments(segs);
		
	}

	@Override
	public Map<Landmark, Integer> getLandmarks() {
		return nucleus.getLandmarks();
	}

	@Override
	public IPoint getBorderPoint(@NonNull Landmark tag) throws MissingLandmarkException {
		return nucleus.getBorderPoint(tag);
	}

	@Override
	public int getIndexRelativeTo(@NonNull Landmark reference, int index) throws MissingLandmarkException {
		return nucleus.getIndexRelativeTo(reference, index);
	}

	@Override
	public int compareTo(Nucleus o) {
		return nucleus.compareTo(o);
	}

	@Override
	public List<OrientationMark> getOrientationMarks() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public @Nullable Landmark getLandmark(OrientationMark landmark) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public @Nullable PriorityAxis getPriorityAxis() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void orient() {
		// TODO Auto-generated method stub
		
	}

	

	

	

}
