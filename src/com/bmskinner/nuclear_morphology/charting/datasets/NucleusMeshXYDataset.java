package com.bmskinner.nuclear_morphology.charting.datasets;

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
