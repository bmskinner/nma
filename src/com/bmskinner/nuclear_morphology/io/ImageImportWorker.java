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


package com.bmskinner.nuclear_morphology.io;

import java.awt.Color;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.ImageIcon;
import javax.swing.SwingWorker;
import javax.swing.table.TableModel;

import com.bmskinner.nuclear_morphology.analysis.IAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.image.ImageAnnotator;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.nuclear.Lobe;
import com.bmskinner.nuclear_morphology.components.nuclei.LobedNucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.gui.dialogs.collections.CellCollectionOverviewDialog;
import com.bmskinner.nuclear_morphology.gui.tabs.cells_detail.LabelInfo;
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
public class ImageImportWorker extends SwingWorker<Boolean, LabelInfo> implements Loggable {

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

        for (ICell c : dataset.getCollection().getCells()) {

            try {

                ImageIcon ic = importCellImage(c);

                LabelInfo inf = new LabelInfo(ic, c);

                publish(inf);
            } catch (Exception e) {
                error("Error opening cell image", e);
            }

        }

        return true;
    }

    @Override
    public void done() {

        finest("Worker completed task");

        try {
            if (this.get()) {
                finest("Firing trigger for sucessful task");
                firePropertyChange("Finished", getProgress(), IAnalysisWorker.FINISHED);

            } else {
                finest("Firing trigger for failed task");
                firePropertyChange("Error", getProgress(), IAnalysisWorker.ERROR);
            }
        } catch (InterruptedException e) {
            error("Interruption error in worker", e);
            firePropertyChange("Error", getProgress(), IAnalysisWorker.ERROR);
        } catch (ExecutionException e) {
            error("Execution error in worker", e);
            firePropertyChange("Error", getProgress(), IAnalysisWorker.ERROR);
        }

    }

    protected ImageIcon importCellImage(ICell c) {
        ImageProcessor ip;

        try {
            if (c.hasCytoplasm()) {
                ip = c.getCytoplasm().getComponentRGBImage();
            } else {
                ip = c.getNucleus().getComponentImage();
            }

            // Nucleus n = c.getNucleus();
            // ip = n.getComponentImage();

        } catch (UnloadableImageException e) {
            stack("Cannot load image for component", e);
            return new ImageIcon();
        }

        ImageAnnotator an = new ImageAnnotator(ip);
        if (c.hasCytoplasm()) {

            an = an.annotateBorder(c.getCytoplasm(), c.getCytoplasm(), Color.CYAN);
            for (Nucleus n : c.getNuclei()) {
                an.annotateBorder(n, c.getCytoplasm(), Color.ORANGE);

                if (n instanceof LobedNucleus) {

                    for (Lobe l : ((LobedNucleus) n).getLobes()) {
                        an.annotateBorder(l, c.getCytoplasm(), Color.RED);
                        an.annotatePoint(l.getCentreOfMass(), c.getCytoplasm(), Color.GREEN);
                    }
                }
                // an = an.annotateSegments(n, n);
            }

        } else {

            for (Nucleus n : c.getNuclei()) {
                an = an.annotateSegments(n, n);
            }
        }

        ip = an.toProcessor();

        // drawNucleus(c, ip);

        if (rotate) {
            try {
                ip = rotateToVertical(c, ip);
            } catch (UnavailableBorderTagException e) {
                fine("Unable to rotate", e);
            }
            ip.flipVertical(); // Y axis needs inverting
        }
        // Rescale the resulting image
        ip = scaleImage(ip);

        ImageIcon ic = new ImageIcon(ip.getBufferedImage());
        return ic;
    }

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

        // Find which point is higher in the image
        IPoint upperPoint = topPoint.getY() > btmPoint.getY() ? topPoint : btmPoint;
        IPoint lowerPoint = upperPoint == topPoint ? btmPoint : topPoint;

        IPoint comp = IPoint.makeNew(lowerPoint.getX(), upperPoint.getY());

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

        double angleFromVertical = lowerPoint.findSmallestAngle(upperPoint, comp);

        double angle = 0;
        if (topPoint.isLeftOf(btmPoint) && topPoint.isAbove(btmPoint)) {
            angle = 360 - angleFromVertical;
            // log("LA: "+angleFromVertical+" to "+angle); // Tested working
        }

        if (topPoint.isRightOf(btmPoint) && topPoint.isAbove(btmPoint)) {
            angle = angleFromVertical;
            // log("RA: "+angleFromVertical+" to "+angle); // Tested working
        }

        if (topPoint.isLeftOf(btmPoint) && topPoint.isBelow(btmPoint)) {
            angle = angleFromVertical + 180;
            // angle = 180-angleFromVertical;
            // log("LB: "+angleFromVertical+" to "+angle); // Tested working
        }

        if (topPoint.isRightOf(btmPoint) && topPoint.isBelow(btmPoint)) {
            // angle = angleFromVertical+180;
            angle = 180 - angleFromVertical;
            // log("RB: "+angleFromVertical+" to "+angle); // Tested working
        }

        // Increase the canvas size so rotation does not crop the nucleus
        finer("Input: " + n.getNameAndNumber() + " - " + ip.getWidth() + " x " + ip.getHeight());
        ImageProcessor newIp = createEnlargedProcessor(ip, angle);

        newIp.rotate(angle);
        return newIp;
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

        finer("New image " + w + " x " + h + " from " + ip.getWidth() + " x " + ip.getHeight() + " : Rot: " + degrees);

        finest("Copy starting at " + xBase + ", " + yBase);

        ImageProcessor newIp = new ColorProcessor(w, h);

        newIp.setColor(Color.WHITE); // fill current space with white
        newIp.fill();

        newIp.setBackgroundValue(16777215); // fill on rotate is RGB int white
        newIp.copyBits(ip, xBase, yBase, Blitter.COPY);
        return newIp;
    }

    protected ImageProcessor scaleImage(ImageProcessor ip) {
        double aspect = (double) ip.getWidth() / (double) ip.getHeight();
        double finalWidth = 150 * aspect; // fix height
        finalWidth = finalWidth > 150 ? 150 : finalWidth; // but constrain width
                                                          // too

        ip = ip.resize((int) finalWidth);
        return ip;
    }

    @Override
    protected void process(List<LabelInfo> chunks) {

        for (LabelInfo im : chunks) {

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
