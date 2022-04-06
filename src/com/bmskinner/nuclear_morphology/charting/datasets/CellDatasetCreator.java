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
package com.bmskinner.nuclear_morphology.charting.datasets;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.components.MissingComponentException;
import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
import com.bmskinner.nuclear_morphology.components.cells.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.cells.Nucleus;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileSegment;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;

public class CellDatasetCreator extends AbstractDatasetCreator<ChartOptions> {
	
	private static final Logger LOGGER = Logger.getLogger(CellDatasetCreator.class.getName());

    public CellDatasetCreator(@NonNull final ChartOptions options) {
        super(options);
    }

    /**
     * Create an XY dataset for the offset xy positions of the start positions
     * of a segment
     * 
     * @param options
     *            the chart options
     * @return a chart
     */
    public XYDataset createPositionFeatureDataset() {

        XYDataset ds = null;

        if (options.isSingleDataset()) {
            LOGGER.finest( "Creating single dataset position dataset");

            ds = createSinglePositionFeatureDataset();

        }

        if (options.isMultipleDatasets()) {

            LOGGER.finest( "Creating multiple dataset position dataset");

            if (IProfileSegment.segmentCountsMatch(options.getDatasets())) {

                ds = createMultiPositionFeatureDataset();
            } else {
                LOGGER.fine("Unable to create multiple chart: segment counts do not match");
            }
        }

        return ds;
    }

    /**
     * Create an XYDataset of segment start positions for a single dataset
     * 
     * @param options
     * @return
     * @throws Exception
     */
    private XYDataset createSinglePositionFeatureDataset() {

        DefaultXYDataset ds = new DefaultXYDataset();

        LOGGER.finest( "Fetching segment position list");

        List<IPoint> offsetPoints = createAbsolutePositionFeatureList(options.firstDataset(), options.getSegID());

        double[] xPoints = new double[offsetPoints.size()];
        double[] yPoints = new double[offsetPoints.size()];

        for (int i = 0; i < offsetPoints.size(); i++) {

            xPoints[i] = offsetPoints.get(i).getX();
            yPoints[i] = offsetPoints.get(i).getY();

        }

        double[][] data = { xPoints, yPoints };

        ds.addSeries("Segment_" + options.getSegID() + "_" + options.firstDataset().getName(), data);
        LOGGER.finest( "Created segment position dataset for segment " + options.getSegID());
        return ds;
    }

    /**
     * Create an XYDataset of segment start positions for multiple datasets
     * 
     * @param options
     * @return
     * @throws Exception
     */
    private XYDataset createMultiPositionFeatureDataset() {

        DefaultXYDataset ds = new DefaultXYDataset();

        for (IAnalysisDataset dataset : options.getDatasets()) {

            /*
             * We need to convert the seg position into a seg id
             */
            try {
                UUID segID = dataset.getCollection().getProfileCollection()
                        .getSegmentAt(Landmark.REFERENCE_POINT, options.getSegPosition()).getID();

                List<IPoint> offsetPoints = createAbsolutePositionFeatureList(dataset, segID);

                double[] xPoints = new double[offsetPoints.size()];
                double[] yPoints = new double[offsetPoints.size()];

                for (int i = 0; i < offsetPoints.size(); i++) {

                    xPoints[i] = offsetPoints.get(i).getX();
                    yPoints[i] = offsetPoints.get(i).getY();

                }

                double[][] data = { xPoints, yPoints };

                ds.addSeries("Segment_" + segID + "_" + dataset.getName(), data);

            } catch (MissingLandmarkException | ProfileException e) {
                LOGGER.warning("Missing segment from " + dataset.getName());
            }

        }

        return ds;
    }

    /**
     * Create a list of points corresponding to the start index of the segment
     * with the given id
     * 
     * @param dataset
     * @param segmentID
     * @return
     * @throws Exception
     */
    public List<IPoint> createAbsolutePositionFeatureList(IAnalysisDataset dataset, UUID segmentID) {

        if (dataset == null) {
            throw new IllegalArgumentException("Dataset is null");
        }

        if (segmentID == null) {
            throw new IllegalArgumentException("Segment id is null");
        }

        List<IPoint> result = new ArrayList<>();

        /*
         * Fetch the cells from the dataset, and rotate the nuclei appropriately
         */
        LOGGER.finest( "Fetching segment position for each nucleus");
        for (Nucleus nucleus : dataset.getCollection().getNuclei()) {

        	try {
        		Nucleus verticalNucleus = nucleus.getOrientedNucleus();
        		LOGGER.finest( "Fetched vertical nucleus");

        		// Get the segment start position XY coordinates
        		if (!verticalNucleus.getProfile(ProfileType.ANGLE).hasSegment(segmentID)) {
        			LOGGER.fine("Segment " + segmentID.toString() + " not found in vertical nucleus for "
        					+ nucleus.getNameAndNumber());
        			continue;

        		}
        		IProfileSegment segment = verticalNucleus.getProfile(ProfileType.ANGLE).getSegment(segmentID);
        		LOGGER.finest( "Fetched segment " + segmentID.toString());

        		int start = segment.getStartIndex();
        		LOGGER.finest( "Getting start point at index " + start);
        		IPoint point = verticalNucleus.getBorderPoint(start);
        		result.add(point);
        	} catch (MissingComponentException | ProfileException | ComponentCreationException e) {
        		LOGGER.warning("Cannot get angle profile for nucleus");

        	}

        }
        LOGGER.finest( "Fetched segment position for each nucleus");
        return result;
    }
}
