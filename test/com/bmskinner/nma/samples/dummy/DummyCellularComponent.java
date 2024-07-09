package com.bmskinner.nma.samples.dummy;

import java.awt.Shape;
import java.io.File;
import java.util.List;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Element;

import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.UnavailableBorderPointException;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.measure.MeasurementScale;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;

import ij.gui.Roi;
import ij.process.FloatPolygon;

/**
 * Provide methods for a dummy component
 * 
 * @author ben
 *
 */
@SuppressWarnings("serial")
public class DummyCellularComponent implements CellularComponent {

	protected CellularComponent component;

	@Override
	public void rotatePointToBottom(@NonNull IPoint bottomPoint) {
		component.rotatePointToBottom(bottomPoint);
	}

	@Override
	public void rotate(double angle) {
		component.rotate(angle);
	}

	@Override
	public void rotate(double angle, IPoint anchor) {
		component.rotate(angle, anchor);
	}

	@Override
	public IPoint getOriginalCentreOfMass() {
		return component.getOriginalCentreOfMass();
	}

	@Override
	public String getSourceFileNameWithoutExtension() {
		return component.getSourceFileNameWithoutExtension();
	}

	@Override
	public IPoint getOriginalBase() {
		return component.getOriginalBase();
	}

//	@Override
//	public Mask getSourceBooleanMask() {
//		return component.getSourceBooleanMask();
//	}

	@Override
	public Shape toShape(MeasurementScale scale) {
		return component.toShape(scale);
	}

	@Override
	public Roi toRoi() {
		return component.toRoi();
	}

	@Override
	public Roi toOriginalRoi() {
		return component.toOriginalRoi();
	}

	@Override
	public IPoint getBase() {
		return component.getBase();
	}

	@Override
	public int getXBase() {
		return component.getXBase();
	}

	@Override
	public int getYBase() {
		return component.getYBase();
	}

	@Override
	public double getWidth() {
		return component.getWidth();
	}

	@Override
	public double getHeight() {
		return component.getHeight();
	}

	@Override
	public @NonNull UUID getId() {
		return component.getId();
	}

	@Override
	public double getMeasurement(@NonNull Measurement stat, @NonNull MeasurementScale scale)
			throws MissingDataException, ComponentCreationException, SegmentUpdateException {
		return component.getMeasurement(stat, scale);
	}

	@Override
	public double getMeasurement(@NonNull Measurement stat)
			throws MissingDataException, ComponentCreationException, SegmentUpdateException {
		return component.getMeasurement(stat);
	}

	@Override
	public void setMeasurement(@NonNull Measurement stat, double d) {
		component.setMeasurement(stat, d);
	}

	@Override
	public List<Measurement> getMeasurements() {
		return component.getMeasurements();
	}

	@Override
	public double getScale() {
		return component.getScale();
	}

	@Override
	public void setScale(double scale) {
		component.setScale(scale);
	}

	@Override
	public IPoint getCentreOfMass() {
		return component.getCentreOfMass();
	}

	@Override
	public IPoint getBorderPoint(int i) throws UnavailableBorderPointException {
		return component.getBorderPoint(i);
	}

	@Override
	public IPoint getOriginalBorderPoint(int i) throws UnavailableBorderPointException {
		return component.getOriginalBorderPoint(i);
	}

	@Override
	public int getBorderIndex(@NonNull IPoint p) {
		return component.getBorderIndex(p);
	}

	@Override
	public int getBorderLength() {
		return component.getBorderLength();
	}

	@Override
	public List<IPoint> getBorderList() {
		return component.getBorderList();
	}

	@Override
	public List<IPoint> getOriginalBorderList() throws UnavailableBorderPointException {
		return component.getOriginalBorderList();
	}

	@Override
	public boolean containsPoint(IPoint p) {
		return component.containsPoint(p);
	}

	@Override
	public boolean containsPoint(int x, int y) {
		return component.containsPoint(x, y);
	}

	@Override
	public boolean containsOriginalPoint(IPoint p) {
		return component.containsOriginalPoint(p);
	}

	@Override
	public double getMaxX() {
		return component.getMaxX();
	}

	@Override
	public double getMinX() {
		return component.getMinX();
	}

	@Override
	public double getMaxY() {
		return component.getMaxY();
	}

	@Override
	public double getMinY() {
		return component.getMinY();
	}

	@Override
	public void flipHorizontal() {
		component.flipHorizontal(component.getCentreOfMass());
	}

	@Override
	public void flipHorizontal(@NonNull IPoint p) {
		component.flipHorizontal(p);
	}

	@Override
	public void moveCentreOfMass(@NonNull IPoint point) {
		component.moveCentreOfMass(point);
	}

	@Override
	public void offset(double xOffset, double yOffset) {
		component.offset(xOffset, yOffset);
	}

	@Override
	public int wrapIndex(int i) {
		return component.wrapIndex(i);
	}

	@Override
	public FloatPolygon toPolygon() {
		return component.toPolygon();
	}

	@Override
	public FloatPolygon toOriginalPolygon() {
		return component.toOriginalPolygon();
	}

	@Override
	public Shape toShape() {
		return component.toShape();
	}

	@Override
	public Shape toOriginalShape() {
		return component.toOriginalShape();
	}

//	@Override
//	public Mask getBooleanMask(int height, int width) {
//		return component.getBooleanMask(height, width);
//	}

	@Override
	public int getPositionBetween(@NonNull IPoint pointA, @NonNull IPoint pointB) {
		return component.getPositionBetween(pointA, pointB);
	}

	@Override
	public IPoint findOppositeBorder(@NonNull IPoint p) throws UnavailableBorderPointException {
		return component.findOppositeBorder(p);
	}

	@Override
	public IPoint findOrthogonalBorderPoint(@NonNull IPoint a)
			throws UnavailableBorderPointException {
		return component.findOrthogonalBorderPoint(a);
	}

	@Override
	public IPoint findClosestBorderPoint(@NonNull IPoint p) throws UnavailableBorderPointException {
		return component.findClosestBorderPoint(p);
	}

	@Override
	public File getSourceFile() {
		return component.getSourceFile();
	}

	@Override
	public int getChannel() {
		return component.getChannel();
	}

	@Override
	public File getSourceFolder() {
		return component.getSourceFolder();
	}

	@Override
	public String getSourceFileName() {
		return component.getSourceFileName();
	}

	@Override
	public void setSourceFile(@NonNull File sourceFile) {
		component.setSourceFile(sourceFile);
	}

	@Override
	public void setSourceFolder(@NonNull File sourceFolder) {
		component.setSourceFolder(sourceFolder);
	}

	@Override
	public CellularComponent duplicate() {
		return component.duplicate();
	}

	@Override
	public void reverse()
			throws MissingDataException, SegmentUpdateException, ComponentCreationException {
		component.reverse();
	}

	@Override
	public String toString() {
		return component.toString();
	}

	@Override
	public double wrapIndex(double d) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void clearMeasurement(@NonNull Measurement stat) {
		component.clearMeasurement(stat);
	}

	@Override
	public Element toXmlElement() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void rotatePointToLeft(IPoint leftPoint) {
		// TODO Auto-generated method stub

	}

	@Override
	public void flipVertical(@NonNull IPoint centre) {
		// TODO Auto-generated method stub

	}

	@Override
	public void flipVertical() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isReversed() {
		return component.isReversed();
	}

	@Override
	public void clearMeasurements() {
		component.clearMeasurements();

	}

	@Override
	public boolean hasMeasurement(@NonNull Measurement measurement) {
		return component.hasMeasurement(measurement);
	}

}
