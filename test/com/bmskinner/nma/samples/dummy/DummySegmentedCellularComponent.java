package com.bmskinner.nma.samples.dummy;

import java.io.File;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.ProfileableCellularComponent;
import com.bmskinner.nma.components.generic.FloatPoint;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.profiles.IProfile;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.profiles.ISegmentedProfile;
import com.bmskinner.nma.components.profiles.Landmark;
import com.bmskinner.nma.components.profiles.MissingLandmarkException;
import com.bmskinner.nma.components.profiles.MissingProfileException;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.components.rules.RuleSetCollection;

import ij.gui.PolygonRoi;
import ij.gui.Roi;

public class DummySegmentedCellularComponent extends ProfileableCellularComponent {

	private final String name;

	private static final float[] X_POINTS = { 97, 98, 99, 100, 101, 102, 102, 103, 104, 105, 105,
			106, 107, 108, 109,
			109, 110, 110, 111, 111, 112, 112, 112, 113, 113, 113, 113, 113, 113, 113, 113, 113,
			113, 113, 112, 112,
			112, 112, 112, 111, 111, 111, 111, 110, 110, 110, 110, 109, 109, 109, 109, 108, 108,
			107, 107, 107, 107,
			106, 106, 105, 105, 104, 104, 103, 103, 102, 102, 101, 101, 100, 99, 99, 98, 97, 97, 96,
			96, 95, 94, 94, 93,
			93, 92, 91, 90, 90, 89, 88, 87, 87, 86, 85, 84, 83, 83, 82, 81, 80, 79, 78, 77, 76, 76,
			75, 74, 73, 72, 71,
			70, 70, 69, 68, 67, 66, 65, 64, 63, 62, 61, 60, 59, 59, 58, 57, 56, 55, 54, 53, 52, 51,
			50, 49, 48, 47, 46,
			45, 44, 43, 42, 41, 40, 39, 38, 37, 36, 35, 34, 33, 32, 31, 30, 29, 28, 27, 26, 25, 24,
			23, 22, 21, 20, 19,
			18, 17, 16, 15, 14, 13, 12, 11, 10, 10, 9, 8, 7, 6, 5, 4, 3, 2, 2, 1, 0, 0, 0, 0, 1, 2,
			3, 4, 5, 6, 7, 8, 9,
			10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 26, 27, 28, 29, 30,
			31, 31, 32, 33, 34,
			34, 35, 36, 37, 38, 38, 39, 39, 40, 40, 41, 41, 41, 41, 42, 42, 42, 42, 43, 43, 43, 44,
			44, 45, 45, 45, 46,
			46, 47, 47, 47, 48, 48, 49, 49, 49, 50, 50, 50, 51, 51, 51, 52, 52, 53, 53, 53, 54, 54,
			54, 55, 55, 55, 56,
			56, 57, 57, 58, 58, 59, 59, 60, 60, 61, 61, 62, 63, 63, 64, 65, 66, 67, 68, 69, 70, 71,
			72, 73, 74, 75, 76,
			77, 77, 78, 78, 79, 80, 80, 81, 81, 82, 83, 83, 84, 85, 86, 87, 87, 88, 89, 90, 91, 92,
			93, 94, 95, 96 };
	private static final float[] Y_POINTS = { 0, 1, 1, 1, 2, 2, 3, 4, 4, 5, 6, 6, 7, 7, 8, 9, 10,
			10, 11, 12, 13, 14,
			15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36,
			37, 38, 39, 39, 40,
			41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 52, 53, 54, 55, 56, 57, 57, 58, 59, 60,
			61, 61, 62, 63, 64,
			65, 65, 66, 67, 68, 69, 69, 70, 70, 71, 72, 72, 73, 74, 74, 75, 75, 76, 76, 77, 77, 78,
			78, 79, 80, 80, 80,
			81, 82, 82, 83, 83, 84, 84, 84, 85, 85, 86, 86, 86, 87, 87, 88, 88, 88, 89, 89, 89, 90,
			90, 90, 91, 91, 91,
			92, 92, 92, 92, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93,
			93, 93, 93, 93, 93,
			93, 93, 93, 93, 93, 93, 92, 92, 92, 92, 92, 91, 91, 91, 90, 90, 90, 89, 89, 88, 88, 88,
			87, 86, 86, 85, 84,
			83, 82, 81, 81, 81, 81, 81, 81, 81, 81, 81, 82, 82, 82, 82, 82, 82, 82, 82, 82, 82, 82,
			82, 81, 81, 81, 80,
			80, 79, 79, 78, 78, 77, 77, 76, 75, 75, 74, 73, 73, 72, 72, 71, 70, 70, 69, 68, 67, 66,
			65, 64, 63, 62, 61,
			60, 59, 58, 57, 56, 55, 54, 54, 53, 52, 51, 50, 49, 48, 47, 46, 45, 44, 43, 43, 42, 41,
			40, 39, 38, 37, 36,
			35, 34, 33, 32, 31, 30, 29, 29, 28, 27, 26, 25, 24, 23, 22, 21, 20, 20, 19, 18, 17, 16,
			16, 15, 14, 14, 13,
			13, 13, 13, 13, 13, 13, 13, 13, 13, 12, 12, 12, 11, 10, 9, 8, 7, 7, 6, 5, 4, 3, 3, 2, 2,
			1, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0 };
	private static final double PROFILE_WINDOW = 0.05;
	private static final Roi ROI = new PolygonRoi(X_POINTS, Y_POINTS, Roi.TRACED_ROI);
	private static final int IMAGE_CHANNEL = 0;
	private static final File IMAGE_FILE = new File(
			"test/com/bmskinner/nuclear_morphology/samples/images/Testing/s60.tiff"); // This
																						// component
																						// is from
																						// Testing\s60\0
	private static final IPoint COM = new FloatPoint(74, 46);

	public static final double AREA = 4827.00;
	public static final double MAX_FERET = 134.27;
	public static final double PERIMETER = 347.02;
	public static final double MIN_DIAMETER = 53.14;

	public DummySegmentedCellularComponent() throws ComponentCreationException {
		this("default");
	}

	public DummySegmentedCellularComponent(String name) throws ComponentCreationException {
		super(ROI, COM, IMAGE_FILE, IMAGE_CHANNEL,
				RuleSetCollection.roundRuleSetCollection());
		setMeasurement(Measurement.AREA, AREA);
		setMeasurement(Measurement.PERIMETER, PERIMETER);
		setMeasurement(Measurement.MIN_DIAMETER, MIN_DIAMETER);
		try {
			createProfiles(PROFILE_WINDOW);
		} catch (ComponentCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		this.name = name;
	}

	@Override
	public CellularComponent duplicate() {
		try {
			return new DummySegmentedCellularComponent(name);
		} catch (ComponentCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public @NonNull ISegmentedProfile getProfile(@NonNull ProfileType type,
			@NonNull OrientationMark om)
			throws SegmentUpdateException, MissingDataException {
		return super.getProfile(type, om);
	}

	@Override
	public IProfile getUnsegmentedProfile(@NonNull ProfileType type, @NonNull OrientationMark om)
			throws MissingLandmarkException, MissingProfileException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Landmark getLandmark(OrientationMark om) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<OrientationMark> getOrientationMarks() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void orient() throws MissingLandmarkException {
		// TODO Auto-generated method stub

	}

	@Override
	public List<Landmark> getLandmarks() {
		// TODO Auto-generated method stub
		return null;
	}

}
