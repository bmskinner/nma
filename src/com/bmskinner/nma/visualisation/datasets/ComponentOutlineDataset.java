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
package com.bmskinner.nma.visualisation.datasets;

import java.util.List;

import org.jfree.data.xy.DefaultXYDataset;

import com.bmskinner.nma.components.MissingLandmarkException;
import com.bmskinner.nma.components.Taggable;
import com.bmskinner.nma.components.UnavailableBorderPointException;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.measure.MeasurementScale;
import com.bmskinner.nma.components.profiles.IProfileSegment;
import com.bmskinner.nma.components.profiles.MissingProfileException;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;

/**
 * Holds the outlines of cellular components
 * 
 * @author ben
 */
@SuppressWarnings("serial")
public class ComponentOutlineDataset extends DefaultXYDataset {

	private static final String UNABLE_TO_GET_BORDER_POINT_ERROR = "Unable to get border point";

	private final CellularComponent c;
	private final MeasurementScale scale;

	public ComponentOutlineDataset(CellularComponent c, boolean showSegmented,
			MeasurementScale scale)
			throws ChartDatasetCreationException {
		this.c = c;
		this.scale = scale;
		if (showSegmented) {
			createWithSegments();
		} else {
			createWithoutSegments();
		}

	}

	private void createWithSegments() throws ChartDatasetCreationException {
		if (!(c instanceof Taggable)) {
			createWithoutSegments();
			return;
		}

		Taggable t = (Taggable) c;
		try {
			List<IProfileSegment> segmentList = t
					.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE)
					.getSegments();

			if (!segmentList.isEmpty()) { // only draw if there are segments

				for (IProfileSegment seg : segmentList) {

					// If we make the array the length of the segment,
					// there will be a gap between the segment end and the
					// next segment start. Include a position for the next
					// segment start as well
					double[] xpoints = new double[seg.length() + 1];
					double[] ypoints = new double[seg.length() + 1];

					int segmentPosition = seg.getPosition();

					for (int j = 0; j <= seg.length(); j++) {
						int index = seg.getStartIndex() + j;
						int offsetIndex = t.getIndexRelativeTo(OrientationMark.REFERENCE, index);

						/*
						 * Note that the original border point is used here to avoid mismatches with
						 * the border tags drawn in other methods.
						 */
						IPoint p = t.getOriginalBorderPoint(offsetIndex);
						double x = p.getX();
						double y = p.getY();

						if (MeasurementScale.MICRONS.equals(scale)) {
							x = Measurement.lengthToMicrons(p.getX(), c.getScale());
							y = Measurement.lengthToMicrons(p.getY(), c.getScale());
						}
						xpoints[j] = x;
						ypoints[j] = y;
					}

					double[][] data = { xpoints, ypoints };

					String seriesKey = "Seg_" + segmentPosition + "_" + t.getID();
					addSeries(seriesKey, data);
				}
			} else {
				createWithoutSegments();
			}
		} catch (ProfileException | MissingLandmarkException | MissingProfileException
				| UnavailableBorderPointException e) {
			throw new ChartDatasetCreationException("Cannot get profile", e);
		}
	}

	private void createWithoutSegments() throws ChartDatasetCreationException {
		double[] xpoints = new double[c.getBorderLength() + 1];
		double[] ypoints = new double[c.getBorderLength() + 1];

		try {
			for (int i = 0; i < c.getBorderLength(); i++) {
				IPoint p = c.getBorderPoint(i);
				double x = p.getX();
				double y = p.getY();

				if (MeasurementScale.MICRONS.equals(scale)) {
					x = Measurement.lengthToMicrons(p.getX(), c.getScale());
					y = Measurement.lengthToMicrons(p.getY(), c.getScale());
				}
				xpoints[i] = x;
				ypoints[i] = y;
			}

			// complete the line
			xpoints[c.getBorderLength()] = xpoints[0];
			ypoints[c.getBorderLength()] = ypoints[0];

		} catch (UnavailableBorderPointException e) {
			throw new ChartDatasetCreationException(UNABLE_TO_GET_BORDER_POINT_ERROR, e);
		}
		addSeries(c.getID(), new double[][] { xpoints, ypoints });
	}

	public CellularComponent getComponent() {
		return c;
	}
}
