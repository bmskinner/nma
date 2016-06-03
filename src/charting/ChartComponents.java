/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package charting;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Shape;

import org.jfree.chart.plot.ValueMarker;
import org.jfree.util.ShapeUtilities;

public class ChartComponents {
	public static final BasicStroke SEGMENT_STROKE = new BasicStroke(3);
	public static final BasicStroke MARKER_STROKE = new BasicStroke(2);
	public static final BasicStroke PROFILE_STROKE = new BasicStroke(1);
	public static final BasicStroke QUARTILE_STROKE = new BasicStroke(1);
	
	public static final ValueMarker DEGREE_LINE_180 = new ValueMarker(180, Color.BLACK, MARKER_STROKE);
	
	public static final Shape DEFAULT_POINT_SHAPE = ShapeUtilities.createDiamond(5);
}
