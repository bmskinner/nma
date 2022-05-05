package com.bmskinner.nma.visualisation.datasets;

import java.awt.Color;

import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultXYDataset;

import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.visualisation.ChartComponents;

public class PointDataset extends DefaultXYDataset {

	public PointDataset() {

	}

	public void addPoint(String name, IPoint com) {
		double[][] data = new double[2][1];
		data[0][0] = com.getX();
		data[1][0] = com.getY();
		addSeries(name, data);
	}

	/**
	 * Get the default renderer for this dataset type
	 * 
	 * @return
	 */
	public XYItemRenderer getRenderer(Color pointColour) {
		XYLineAndShapeRenderer lmRend = new XYLineAndShapeRenderer();
		for (int lmSeries = 0; lmSeries < getSeriesCount(); lmSeries++) {
			lmRend.setSeriesLinesVisible(lmSeries, false);
			lmRend.setSeriesShapesVisible(lmSeries, true);
			lmRend.setSeriesVisibleInLegend(lmSeries, Boolean.FALSE);
			lmRend.setSeriesPaint(lmSeries, pointColour);
			lmRend.setSeriesShape(lmSeries, ChartComponents.DEFAULT_POINT_SHAPE);
		}
		return lmRend;
	}
}
