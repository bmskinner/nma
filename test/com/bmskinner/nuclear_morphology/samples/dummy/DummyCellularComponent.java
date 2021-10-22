package com.bmskinner.nuclear_morphology.samples.dummy;

import java.awt.Rectangle;
import java.awt.Shape;
import java.io.File;
import java.util.List;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.analysis.detection.Mask;
import com.bmskinner.nuclear_morphology.components.UnavailableBorderPointException;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.measure.MeasurementScale;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;

import ij.gui.Roi;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;

/**
 * Provide methods for a dummy component
 * @author ben
 *
 */
@SuppressWarnings("serial")
public class DummyCellularComponent implements CellularComponent {
	
	protected CellularComponent component;

	@Override
	public void rotatePointToBottom(IPoint bottomPoint) {
		component.rotatePointToBottom(bottomPoint);
	}

	@Override
	public void rotate(double angle) {
		component.rotate(angle);
	}

	@Override
	public IPoint getOriginalCentreOfMass() {
		return component.getOriginalCentreOfMass();
	}
	
	@Override
	public boolean isSmoothByDefault() {
		return component.isSmoothByDefault();
	}

	@Override
	public String getSourceFileNameWithoutExtension() {
		return component.getSourceFileNameWithoutExtension();
	}
	
	@Override
    public ImageProcessor getGreyscaleImage() throws UnloadableImageException {
        return component.getGreyscaleImage();
    }
	
	@Override
	public ImageProcessor getRGBImage() throws UnloadableImageException {
		return component.getRGBImage();
	}

	@Override
	public IPoint getOriginalBase() {
		return component.getOriginalBase();
	}

	@Override
	public ImageProcessor getComponentRGBImage()
			throws UnloadableImageException {
		return component.getComponentRGBImage();
	}

	@Override
	public Mask getSourceBooleanMask() {
		return component.getSourceBooleanMask();
	}
	
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
	public @NonNull UUID getID() {
		return component.getID();
	}
	
	@Override
	public double getStatistic(Measurement stat, MeasurementScale scale) {
		return component.getStatistic(stat, scale);
	}

	@Override
	public double getStatistic(Measurement stat) {
	    return component.getStatistic(stat);
	}

	@Override
	public void setStatistic(Measurement stat, double d) {
		component.setStatistic(stat, d);
	}

	@Override
	public Measurement[] getStatistics() {
		return component.getStatistics();
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
	public int getBorderIndex(IPoint p) {
		return component.getBorderIndex(p);
	}

	@Override
	public void updateBorderPoint(int i, double x, double y) {
		component.updateBorderPoint(i, x, y);		
	}

	@Override
	public void updateBorderPoint(int i, IPoint p) {
		component.updateBorderPoint(i, p);
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
	public void flipHorizontal(IPoint p) {
		component.flipHorizontal(p);		
	}
	
	@Override
	public void moveCentreOfMass(IPoint point) {
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

	@Override
	public Mask getBooleanMask(int height, int width) {
		return component.getBooleanMask(height, width);
	}

	@Override
	public int getPositionBetween(IPoint pointA, IPoint pointB) {
		return component.getPositionBetween(pointA, pointB);
	}

	@Override
	public IPoint findOppositeBorder(IPoint p) throws UnavailableBorderPointException {
		return component.findOppositeBorder(p);
	}

	@Override
	public IPoint findOrthogonalBorderPoint(IPoint a) throws UnavailableBorderPointException {
		return component.findOrthogonalBorderPoint(a);
	}

	@Override
	public IPoint findClosestBorderPoint(IPoint p) throws UnavailableBorderPointException {
		return component.findClosestBorderPoint(p);
	}

	@Override
	public int[] getPosition() {
		return component.getPosition();
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
	public ImageProcessor getImage() throws UnloadableImageException {
		return component.getImage();
	}

	@Override
	public ImageProcessor getComponentImage() throws UnloadableImageException {
		return component.getComponentImage();
	}

	@Override
	public Rectangle getBounds() {
		return component.getBounds().getBounds();
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
	public void setSourceFile(File sourceFile) {
		component.setSourceFile(sourceFile);		
	}

	@Override
	public void setSourceFolder(File sourceFolder) {
		component.setSourceFolder(sourceFolder);
	}

	@Override
	public CellularComponent duplicate() {
		return component.duplicate();
	}

	@Override
	public void updateDependentStats() {
	    component.updateDependentStats();
	}

	
	@Override
	public boolean hasStatistic(Measurement stat) {
		return component.hasStatistic(stat);
	}

	@Override
	public double getMedianDistanceBetweenPoints() {
		return component.getMedianDistanceBetweenPoints();
	}


	@Override
	public void reverse() {
	    component.reverse();
	}
	
	public String toString(){
	    return component.toString();
	}

	@Override
	public double wrapIndex(double d) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void refreshBorderList(boolean useSplineFitting) {
		component.refreshBorderList(useSplineFitting);
	}

	@Override
	public int[][] getUnsmoothedBorderCoordinates() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void clearStatistic(Measurement stat) {
		component.clearStatistic(stat);
	}

	@Override
	public ImageProcessor getGreyscaleComponentImage() throws UnloadableImageException {
		return component.getGreyscaleComponentImage();
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

}
