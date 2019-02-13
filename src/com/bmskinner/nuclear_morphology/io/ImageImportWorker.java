/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nuclear_morphology.io;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;
import javax.swing.table.TableModel;

import com.bmskinner.nuclear_morphology.analysis.IAnalysisWorker;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.gui.components.SelectableCellIcon;
import com.bmskinner.nuclear_morphology.gui.dialogs.collections.CellCollectionOverviewDialog;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.process.Blitter;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

/**
 * Handles the import of all images within a given AnalysisDataset and sizing
 * for display
 * 
 * @author ben
 *
 */
public abstract class ImageImportWorker extends SwingWorker<Boolean, SelectableCellIcon> implements Loggable {

	protected final IAnalysisDataset dataset;
	protected final TableModel       model;
	protected static final int       COLUMN_COUNT = CellCollectionOverviewDialog.COLUMN_COUNT;
	protected int                    loaded       = 0;
	protected boolean                rotate;

    public ImageImportWorker(IAnalysisDataset dataset, TableModel model, boolean rotate) {
        super();
        this.dataset = dataset;
        this.model = model;
        this.rotate = rotate;
    }

    @Override
    protected Boolean doInBackground() throws Exception {

    	List<ICell> cells = new ArrayList<>(dataset.getCollection().getCells());
    	
    	cells.sort((c1, c2)-> c1.getNucleus().getNameAndNumber().compareTo(c2.getNucleus().getNameAndNumber()));
        for (ICell c : cells) {
        	if(isCancelled())
        		return false;
            try {
            	SelectableCellIcon ic = importCellImage(c);
                publish(ic);
            } catch (Exception e) {
                stack("Error importing cell image", e);
            }
        }
        return true;
    }

    @Override
    public void done() {

        if(isCancelled()) {
    		firePropertyChange(IAnalysisWorker.CANCELLED_MSG, getProgress(), IAnalysisWorker.CANCELLED);
    		return;
    	}
        try {
            if (this.get())
                firePropertyChange(IAnalysisWorker.FINISHED_MSG, getProgress(), IAnalysisWorker.FINISHED);
            else
                firePropertyChange(IAnalysisWorker.ERROR_MSG, getProgress(), IAnalysisWorker.ERROR);
        } catch (InterruptedException e) {
            stack("Interruption error in worker", e);
            firePropertyChange(IAnalysisWorker.ERROR_MSG, getProgress(), IAnalysisWorker.ERROR);
        } catch (ExecutionException e) {
        	stack("Execution error in worker", e);
            firePropertyChange(IAnalysisWorker.ERROR_MSG, getProgress(), IAnalysisWorker.ERROR);
        }
    }
    protected abstract SelectableCellIcon importCellImage(ICell c);
    
    protected ImageProcessor rotateToVertical(ICell c, ImageProcessor ip) throws UnavailableBorderTagException {
        // Calculate angle for vertical rotation
        Nucleus n = c.getNucleus();

        IPoint topPoint;
        IPoint btmPoint;

        if (!n.hasBorderTag(Tag.TOP_VERTICAL) || !n.hasBorderTag(Tag.BOTTOM_VERTICAL)) {
            topPoint = n.getCentreOfMass();
            btmPoint = n.getBorderPoint(Tag.ORIENTATION_POINT);

        } else {

            topPoint = n.getBorderPoint(Tag.TOP_VERTICAL);
            btmPoint = n.getBorderPoint(Tag.BOTTOM_VERTICAL);

            // Sometimes the points have been set to overlap in older datasets
            if (topPoint.overlapsPerfectly(btmPoint)) {
                topPoint = n.getCentreOfMass();
                btmPoint = n.getBorderPoint(Tag.ORIENTATION_POINT);
            }
        }
        
        double angle = findVerticalRotationAngle(topPoint, btmPoint);

        // Increase the canvas size so rotation does not crop the nucleus
        finer("Input: " + n.getNameAndNumber() + " - " + ip.getWidth() + " x " + ip.getHeight());
        ImageProcessor newIp = createEnlargedProcessor(ip, angle);

        newIp.rotate(angle);
        return newIp;
    }
    
    private double findVerticalRotationAngle(IPoint top, IPoint bottom) {
        // Find which point is higher in the image
        IPoint comp = IPoint.makeNew(bottom.getX(), top.getY());

        /*
         * LA RA RB LB
         * 
         * T C C T B C C B \ | | / \ | | / B B T T
         * 
         * When Ux<Lx, angle describes the clockwise rotation around L needed to
         * move U above it. When Ux>Lx, angle describes the anticlockwise
         * rotation needed to move U above it.
         * 
         * If L is supposed to be on top, the clockwise rotation must be 180+a
         * 
         * However, the image coordinates have a reversed Y axis
         */

        double angleFromVertical = bottom.findSmallestAngle(top, comp);

        double angle = 0;
        if (top.isLeftOf(bottom) && top.isAbove(bottom)) {
            angle = 360 - angleFromVertical;
            // log("LA: "+angleFromVertical+" to "+angle); // Tested working
        }

        if (top.isRightOf(bottom) && top.isAbove(bottom)) {
            angle = angleFromVertical;
            // log("RA: "+angleFromVertical+" to "+angle); // Tested working
        }

        if (top.isLeftOf(bottom) && top.isBelow(bottom)) {
            angle = angleFromVertical + 180;
            // angle = 180-angleFromVertical;
            // log("LB: "+angleFromVertical+" to "+angle); // Tested working
        }

        if (top.isRightOf(bottom) && top.isBelow(bottom)) {
            // angle = angleFromVertical+180;
            angle = 180 - angleFromVertical;
            // log("RB: "+angleFromVertical+" to "+angle); // Tested working
        }
        return angle;
      }

    protected ImageProcessor createEnlargedProcessor(ImageProcessor ip, double degrees) {

        double rad = Math.toRadians(degrees);

        // Calculate the new width and height of the canvas
        // new width is h sin(a) + w cos(a) and vice versa for height
        double newWidth = Math.abs(Math.sin(rad) * ip.getHeight()) + Math.abs(Math.cos(rad) * ip.getWidth());
        double newHeight = Math.abs(Math.sin(rad) * ip.getWidth()) + Math.abs(Math.cos(rad) * ip.getHeight());

        int w = (int) Math.ceil(newWidth);
        int h = (int) Math.ceil(newHeight);

        // The new image may be narrower or shorter following rotation.
        // To avoid clipping, ensure the image never gets smaller in either
        // dimension.
        w = w < ip.getWidth() ? ip.getWidth() : w;
        h = h < ip.getHeight() ? ip.getHeight() : h;

        // paste old image to centre of enlarged canvas
        int xBase = (w - ip.getWidth()) >> 1;
        int yBase = (h - ip.getHeight()) >> 1;

        finer(String.format("New image %sx%s from %sx%s : Rot: %s", w, h, ip.getWidth(), ip.getHeight(), degrees));

        finest("Copy starting at " + xBase + ", " + yBase);

        ImageProcessor newIp = new ColorProcessor(w, h);

        newIp.setColor(Color.WHITE); // fill current space with white
        newIp.fill();

        newIp.setBackgroundValue(16777215); // fill on rotate is RGB int white
        newIp.copyBits(ip, xBase, yBase, Blitter.COPY);
        return newIp;
    }

    @Override
    protected void process(List<SelectableCellIcon> chunks) {

        for (SelectableCellIcon im : chunks) {

            int row = loaded / COLUMN_COUNT;
            int col = loaded % COLUMN_COUNT;
            // log("Image: "+loaded+" - Row "+row+" col "+col);

            model.setValueAt(im, row, col);

            loaded++;
        }

        int percent = (int) ((double) loaded / (double) dataset.getCollection().size() * 100);

        if (percent >= 0 && percent <= 100) {
            setProgress(percent); // the integer representation of the percent
        }
    }

}
