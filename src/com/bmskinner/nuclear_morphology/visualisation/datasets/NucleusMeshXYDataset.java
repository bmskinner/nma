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
package com.bmskinner.nuclear_morphology.visualisation.datasets;

import java.util.HashMap;
import java.util.Map;

import org.jfree.data.xy.DefaultXYDataset;

@SuppressWarnings("serial")
public class NucleusMeshXYDataset extends DefaultXYDataset {

    private Map<Comparable<?>, Double> ratioMap = new HashMap<Comparable<?>, Double>();

    public NucleusMeshXYDataset() {
        super();
    }

    public double getRatio(Comparable<?> seriesKey) {
        return ratioMap.get(seriesKey);
    }

    public void setRatio(Comparable<?> seriesKey, double ratio) {
        this.ratioMap.put(seriesKey, ratio);
    }

    @Override
	public String toString() {

        StringBuilder b = new StringBuilder();
        for (int series = 0; series < this.getSeriesCount(); series++) {

            for (int item = 0; item < this.getItemCount(series); item++) {

                double x = this.getXValue(series, item);
                double y = this.getYValue(series, item);

                b.append("Series " + series + " - Item " + item + ": " + x + "   " + y + "\n");
            }
        }
        return b.toString();
    }

}
