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
package com.bmskinner.nuclear_morphology.charting.charts.panels;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.data.Range;

import com.bmskinner.ViolinPlots.ViolinCategoryDataset;

@SuppressWarnings("serial")
public class ViolinChartPanel extends ExportableChartPanel {

    public ViolinChartPanel(JFreeChart chart) {
        super(chart);
    }

    @Override
    public void restoreAutoBounds() {

        Plot p = this.getChart().getPlot();

        if (!(p instanceof CategoryPlot)) {
            super.restoreAutoBounds();
            return;
        }

        CategoryPlot plot = (CategoryPlot) p;

        if (!(plot.getDataset(0) instanceof ViolinCategoryDataset)) {
            super.restoreAutoBounds();
            return;
        }

        ViolinCategoryDataset dataset = (ViolinCategoryDataset) plot.getDataset(0);

        if (!dataset.hasProbabilities()) {
            super.restoreAutoBounds();
            return;
        }

        Range result = dataset.getProbabiltyRange();

        for (int i = 1; i < plot.getDatasetCount(); i++) {
            ViolinCategoryDataset ds = (ViolinCategoryDataset) plot.getDataset(i);
            Range r = ds.getProbabiltyRange();
            result = Range.combine(result, r);
        }
        plot.getRangeAxis().setRange(result);

    }

}
