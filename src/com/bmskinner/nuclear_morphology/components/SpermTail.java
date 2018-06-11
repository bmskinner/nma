/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.components;

import ij.gui.Roi;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;

/**
 * The sperm tail is a specialised type of flagellum. It is anchored at the tail
 * end of the sperm nucleus, and contains a midpiece (with mitochondria
 * attached) and a long thin tail. Cytoplasmic droplets may be present. Imaged
 * tails often overlap themselves and other tails. Common stain - anti-tubulin.
 * 
 * @author bms41
 *
 */
@Deprecated
public class SpermTail extends AbstractCellularComponent implements Serializable, Flagellum {

    private static final long serialVersionUID = 1L;

    protected double length; // the length of the skeleton

    protected IPoint nucleusIntersection; // the position where the tail
                                          // intersects the nucleus

    protected List<IPoint> skeletonPoints = new ArrayList<IPoint>(0);
    protected List<IPoint> borderPoints   = new ArrayList<IPoint>(0);

    public SpermTail(File source, int channel, Roi skeleton, Roi border) {
        super();

        this.setSourceFolder(source.getParentFile());
        this.setSourceFileName(source.getName());
        this.setChannel(channel);

        // this.setPosition( new int[] { (int)
        // border.getPolygon().getBounds().getMinX(),
        // (int) border.getPolygon().getBounds().getMinY(),
        // (int) border.getPolygon().getBounds().getWidth(),
        // (int) border.getPolygon().getBounds().getHeight()});

        FloatPolygon skeletonPolygon = skeleton.getInterpolatedPolygon(1, true);
        for (int i = 0; i < skeletonPolygon.npoints; i++) {
            skeletonPoints.add(IPoint.makeNew(skeletonPolygon.xpoints[i], skeletonPolygon.ypoints[i]));
        }

        FloatPolygon borderPolygon = border.getInterpolatedPolygon(1, true);
        for (int i = 0; i < borderPolygon.npoints; i++) {
            borderPoints.add(IPoint.makeNew(borderPolygon.xpoints[i], borderPolygon.ypoints[i]));
        }

        this.length = skeleton.getLength();

    }

    public SpermTail(final SpermTail t) {
        super(t);
        this.borderPoints = t.getBorder();
        this.skeletonPoints = t.getSkeleton();

        this.length = t.getLength();
    }

    public List<IPoint> getSkeleton() {
        return this.skeletonPoints;
    }

    /**
     * Fetch the skeleton offset to zero
     * 
     * @return
     */
    public List<IPoint> getOffsetSkeleton() {
        List<IPoint> result = new ArrayList<IPoint>(0);
        for (IPoint p : skeletonPoints) {
            result.add(IPoint.makeNew(p.getX() - this.getPosition()[X_BASE], p.getY() - this.getPosition()[Y_BASE]));
        }
        return result;
    }

    public List<IPoint> getBorder() {
        return this.borderPoints;
    }

    // positions are offset by the bounding rectangle for easier plotting
    public List<IPoint> getOffsetBorder() {
        List<IPoint> result = new ArrayList<IPoint>(0);
        for (IPoint p : borderPoints) {
            result.add(IPoint.makeNew(p.getX() - this.getPosition()[X_BASE], p.getY() - this.getPosition()[Y_BASE]));
        }
        return result;
    }

    public double getLength() {
        return this.length;
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        finest("\tWriting sperm tail");
        out.defaultWriteObject();
        finest("\tWrote sperm tail");
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        finest("\tReading sperm tail");
        in.defaultReadObject();
        finest("\tRead sperm tail");
    }

    @Override
    public void alignVertically() {
        // TODO Auto-generated method stub

    }

    @Override
    public Flagellum duplicate() {
        return new SpermTail(this);
    }

    @Override
    public String getSourceFileNameWithoutExtension() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isSmoothByDefault() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public ImageProcessor getRGBImage() throws UnloadableImageException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ImageProcessor getComponentRGBImage() throws UnloadableImageException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateSourceFolder(File newFolder) {
        // TODO Auto-generated method stub

    }

	@Override
	public IPoint getBase() {
		// TODO Auto-generated method stub
		return null;
	}

}
