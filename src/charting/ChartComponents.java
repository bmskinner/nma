package charting;

import java.awt.BasicStroke;
import java.awt.Color;

import org.jfree.chart.plot.ValueMarker;

public class ChartComponents {
	public static final BasicStroke SEGMENT_STROKE = new BasicStroke(3);
	public static final BasicStroke MARKER_STROKE = new BasicStroke(2);
	public static final BasicStroke PROFILE_STROKE = new BasicStroke(1);
	public static final BasicStroke QUARTILE_STROKE = new BasicStroke(1);
	
	public static final ValueMarker DEGREE_LINE_180 = new ValueMarker(180, Color.BLACK, MARKER_STROKE);
}
