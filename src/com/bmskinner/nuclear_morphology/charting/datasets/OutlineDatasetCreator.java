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


package com.bmskinner.nuclear_morphology.charting.datasets;

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.Taggable;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderPointException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;

/**
 * The chart dataset creator for outlines of cellular components
 * 
 * @author bms41
 *
 */
public class OutlineDatasetCreator extends AbstractDatasetCreator<ChartOptions> {

    private final CellularComponent component;

    public OutlineDatasetCreator(@NonNull final ChartOptions options, @NonNull final CellularComponent c) {
        super(options);
        component = c;
    }

    /**
     * Create a dataset with the outline of the current object.
     * 
     * @param segmented should the outline be segmented (where segments are available)
     * @return an XYDataset with the outline. Segments will be as separate
     *         series if segmented is true.
     * @throws ChartDatasetCreationException
     */
    public OutlineDataset<CellularComponent> createOutline(boolean segmented) throws ChartDatasetCreationException {
        if (segmented)
            return createSegmentedOutline();
		return createNonSegmentedOutline();

    }

    /**
     * Add the outline of the current object to the given dataset.
     * 
     * @param ds the dataset to add an outline to
     * @param segmented should the outline be segmented (where segments are available)
     */
    public void addOutline(ComponentOutlineDataset<CellularComponent> ds, boolean segmented)
            throws ChartDatasetCreationException {

        String seriesKey = chooseSeriesKey();
        if (segmented)
            addSegmentedOutline(ds);
        else
            addNonSegmentedOutline(ds, seriesKey);
    }

    /**
     * Create a dataset with the outline of the current object.
     * 
     * @param ds the dataset the data should be added to
     * @param segmented should the outline be segmented (where segments are available)
     * @throws ChartDatasetCreationException
     */
    public void addOutline(ComponentOutlineDataset<CellularComponent> ds, Comparable seriesKey, boolean segmented)
            throws ChartDatasetCreationException {

        if (segmented) {
            addSegmentedOutline(ds);
        } else {
            addNonSegmentedOutline(ds, seriesKey);

        }
    }

    /**
     * Get the outline for a specific nucleus in a dataset. Sets the position to
     * the original coordinates in the image
     * 
     * @param cell
     * @param segmented
     *            true to include each segment separately, false for an
     *            unsegmented outline
     * @return
     * @throws ChartDatasetCreationException
     */
    private OutlineDataset<CellularComponent> createSegmentedOutline() throws ChartDatasetCreationException {
        ComponentOutlineDataset<CellularComponent> ds = new ComponentOutlineDataset<CellularComponent>();
        addSegmentedOutline(ds);
        return ds;
    }

    /**
     * Get the outline for the loaded component and add it to the given dataset.
     * Sets the position to the original coordinates in the image.
     * 
     * @param ds
     *            the chart dataset to add the data to
     * @return
     * @throws ChartDatasetCreationException
     */
    private void addSegmentedOutline(ComponentOutlineDataset<CellularComponent> ds)
            throws ChartDatasetCreationException {

        if (!(component instanceof Taggable))
            throw new ChartDatasetCreationException("Component is not segmentable");

        fine("Creating segmented outline");

        Taggable t = (Taggable) component;

        List<IBorderSegment> segmentList;
        try {
            segmentList = t.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT).getSegments();
        } catch (ProfileException | UnavailableBorderTagException | UnavailableProfileTypeException e) {
            fine("Cannot get profile from RP", e);
            throw new ChartDatasetCreationException("Cannot get profile", e);
        }

        if (!segmentList.isEmpty()) { // only draw if there are segments
//            finest("Component has " + segmentList.size() + " segments");

            for (IBorderSegment seg : segmentList) {

                // If we make the array the length of the segment,
                // there will be a gap between the segment end and the
                // next segment start. Include a position for the next
                // segment start as well
                double[] xpoints = new double[seg.length() + 1];
                double[] ypoints = new double[seg.length() + 1];

                int segmentPosition = seg.getPosition();

                for (int j = 0; j <= seg.length(); j++) {
                	try {
                		int index = seg.getStartIndex() + j;
                		int offsetIndex = t.getOffsetBorderIndex(Tag.REFERENCE_POINT, index);

                		/*
                		 * Note that the original border point is used here to
                		 * avoid mismatches with the border tags drawn in other
                		 * methods.
                		 */
                		// p = t.getBorderPoint(offsetIndex);
                		IBorderPoint p = t.getOriginalBorderPoint(offsetIndex);
                		xpoints[j] = p.getX() - 0.5;
                		ypoints[j] = p.getY() - 0.5;
                	} catch (UnavailableBorderPointException | UnavailableBorderTagException e) {
                		throw new ChartDatasetCreationException("Cannot get border point", e);
                	}
                }

                double[][] data = { xpoints, ypoints };

                String seriesKey = "Seg_" + segmentPosition + "_" + t.getID();
                ds.addSeries(seriesKey, data);
                ds.setComponent(seriesKey, component);
                finest("Added segment data to chart dataset");
            }
        } else {
            fine("Component does not have segments; falling back to bare outline");
            addNonSegmentedOutline(ds, chooseSeriesKey());
        }

    }

    /**
     * Create a non-segmented outline for the current cellular component and add
     * it to a new dataset
     * 
     * @return
     * @throws ChartDatasetCreationException
     */
    private OutlineDataset<CellularComponent> createNonSegmentedOutline() throws ChartDatasetCreationException {
        ComponentOutlineDataset<CellularComponent> ds = new ComponentOutlineDataset<CellularComponent>();

        String seriesKey = chooseSeriesKey();

        addNonSegmentedOutline(ds, seriesKey);

        return ds;
    }

    /**
     * Create a non-segmented outline and add it to the given dataset
     * 
     * @param ds
     * @return
     * @throws ChartDatasetCreationException
     */
    private OutlineDataset<CellularComponent> addNonSegmentedOutline(ComponentOutlineDataset<CellularComponent> ds,
            Comparable seriesKey) throws ChartDatasetCreationException {
        finest("Creating non-segmented outline from component");

        try {

            double[] xpoints = new double[component.getOriginalBorderList().size()];
            double[] ypoints = new double[component.getOriginalBorderList().size()];

            int i = 0;
            for (IPoint p : component.getOriginalBorderList()) {

                xpoints[i] = p.getX() - 0.5;
                ypoints[i] = p.getY() - 0.5;
                i++;
            }

            double[][] data = { xpoints, ypoints };

            finest("Adding series for component border centred on " + component.getCentreOfMass().toString());
            ds.addSeries(seriesKey, data);
            ds.setComponent(seriesKey, component);
            return ds;
        } catch (UnavailableBorderPointException e) {
            throw new ChartDatasetCreationException("Cannot get border point", e);

        }
    }

    /**
     * Choose the string to use a a series key for the component. This defaults
     * to the UUID unless the component is a nucleus, in which case the key will
     * be name and number.
     * 
     * @return
     */
    private String chooseSeriesKey() {
        String seriesKey = component.getID().toString();

        if (component instanceof Nucleus) {
            seriesKey = ((Nucleus) component).getNameAndNumber();
        }
        return seriesKey;
    }

}
