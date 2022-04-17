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
package com.bmskinner.nuclear_morphology.visualisation;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Shape;

import org.jfree.chart.event.MarkerChangeListener;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.util.ShapeUtils;

/**
 * Define standard components for charts
 * 
 * @author bs19022
 *
 */
public class ChartComponents {

	/**
	 * Stroke for landmarks (width 8)
	 */
	public static final BasicStroke LANDMARK_STROKE = new BasicStroke(8);

	/**
	 * Stroke for segments (width 3)
	 */
	public static final BasicStroke SEGMENT_STROKE = new BasicStroke(3);

	/**
	 * Stroke for markers (width 2)
	 */
	public static final BasicStroke MARKER_STROKE = new BasicStroke(2);

	/**
	 * Stroke for profiles (width 1)
	 */
	public static final BasicStroke PROFILE_STROKE = new BasicStroke(1);

	/**
	 * Stroke for quartiles (width 1)
	 */
	public static final BasicStroke QUARTILE_STROKE = new BasicStroke(1);

	/**
	 * A horizontal black line at y=180 with width 2. The marker does not signal to
	 * marker change listeners.
	 */
	public static final ValueMarker DEGREE_LINE_180 = new ValueMarker(180, Color.BLACK, MARKER_STROKE) {

		@Override
		public void addChangeListener(MarkerChangeListener listener) {
		}
	};

	/**
	 * A horizontal grey line at y=0 with width 1. The marker does not signal to
	 * marker change listeners.
	 */
	public static final ValueMarker CONSENSUS_ZERO_MARKER = new ValueMarker(0, Color.LIGHT_GRAY,
			new BasicStroke(1.0f)) {

		@Override
		public void addChangeListener(MarkerChangeListener listener) {
		}
	};

	/**
	 * A horizontal black line at y=0 with width 2. The marker does not signal to
	 * marker change listeners.
	 */
	public static final ValueMarker ZERO_MARKER = new ValueMarker(0, Color.BLACK, MARKER_STROKE) {

		@Override
		public void addChangeListener(MarkerChangeListener listener) {
		}
	};

	public static final Shape DEFAULT_POINT_SHAPE = ShapeUtils.createDiamond(5);
}
