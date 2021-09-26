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

import java.awt.Color;
import java.awt.Paint;
import java.util.Iterator;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;

import com.bmskinner.nuclear_morphology.charting.charts.overlays.ComponentOverlay;
import com.bmskinner.nuclear_morphology.charting.charts.overlays.ShapeOverlayObject;
import com.bmskinner.nuclear_morphology.charting.datasets.ComponentOutlineDataset;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.measure.MeasurementScale;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;
import com.bmskinner.nuclear_morphology.gui.events.EventListener;
import com.bmskinner.nuclear_morphology.gui.events.SignalChangeEvent;

@SuppressWarnings("serial")
public class ConsensusNucleusChartPanel extends ExportableChartPanel {

    private static final String RESET_OFFSET_LBL = "Reset offset to zero";

	private static final String OFFSET_LBL = "Offset...";

	private static final String RESET_ROTATION_LBL = "Reset rotation to tail";

	private static final String ROTATE_BY_LBL = "Rotate by...";

	private static final String ALIGN_VERTICAL_LBL = "Align vertical";

	public static final String SOURCE_COMPONENT = "ConsensusNucleusChartPanel";

    public static final String EXPORT_SVG_LBL = "Export SVG";

    private boolean fillConsensus = true;

    private ComponentOverlay consensusOverlay = null;

    public ConsensusNucleusChartPanel(JFreeChart chart) {
        super(chart);

        JPopupMenu popup = createPopupMenu();
        this.setPopupMenu(popup);
        this.validate();
        this.setFixedAspectRatio(true);
        consensusOverlay = new ComponentOverlay();
        this.addOverlay(consensusOverlay);

    }

    /**
     * Provide an override to the GlobalOptions for this panel. If this is
     * false, the consensus will never be filled. If this is true, the consensus
     * will be filled then the GlobalOptions is also true
     * 
     * @return
     */
    public void setFillConsensus(boolean b) {
        fillConsensus = b;
    }

    /**
     * Check if this panel is overriding the global options
     * 
     * @return
     */
    public boolean isFillConsensus() {
        return fillConsensus;
    }

    @Override
    public synchronized void setChart(JFreeChart chart) {

        super.setChart(chart);

        MeasurementScale scale = GlobalOptions.getInstance().getScale();

        // Clear the overlay
        if (consensusOverlay != null) {
            consensusOverlay.clearShapes();

            if (!GlobalOptions.getInstance().isFillConsensus())
                return;

            if (!fillConsensus)
                return;

            if (!(chart.getPlot() instanceof XYPlot))
                return;

            if (!(chart.getXYPlot().getDataset() instanceof ComponentOutlineDataset))
                return;

            ComponentOutlineDataset ds = (ComponentOutlineDataset) chart.getXYPlot().getDataset();

            for (int series = 0; series < ds.getSeriesCount(); series++) {

                Comparable seriesKey = ds.getSeriesKey(series);

                CellularComponent n = ds.getComponent(seriesKey);

                Paint c = chart.getXYPlot().getRenderer().getSeriesPaint(series);

                if (n != null) {
                    c = ColourSelecter.getTransparentColour((Color) c, true, 128);
                    ShapeOverlayObject o = new ShapeOverlayObject(n.toShape(scale), null, null, c);
                    consensusOverlay.addShape(o, n);
                }
            }

        }

    }

    private JPopupMenu createPopupMenu() {
        JPopupMenu popup = this.getPopupMenu();
        popup.addSeparator();

        JMenuItem alignItem = new JMenuItem(ALIGN_VERTICAL_LBL);
        alignItem.addActionListener(e-> fireSignalChangeEvent("AlignVertical"));
        alignItem.setEnabled(true);

        JMenuItem rotateItem = new JMenuItem(ROTATE_BY_LBL);
        rotateItem.addActionListener(e->fireSignalChangeEvent("RotateConsensus"));

        JMenuItem resetItem = new JMenuItem(RESET_ROTATION_LBL);
        resetItem.addActionListener(e->fireSignalChangeEvent("RotateReset"));

        JMenuItem offsetItem = new JMenuItem(OFFSET_LBL);
        offsetItem.addActionListener(e->fireSignalChangeEvent("OffsetAction"));

        JMenuItem resetOffsetItem = new JMenuItem(RESET_OFFSET_LBL);
        resetOffsetItem.addActionListener(e->fireSignalChangeEvent("OffsetReset"));
        
        JMenuItem exportSvgItem = new JMenuItem(EXPORT_SVG_LBL);
        exportSvgItem.addActionListener(e->fireSignalChangeEvent(EXPORT_SVG_LBL));

        popup.add(alignItem);
        popup.add(rotateItem);
        popup.add(resetItem);
        popup.addSeparator();
        popup.add(offsetItem);
        popup.add(resetOffsetItem);
        popup.addSeparator();
        popup.add(exportSvgItem);

        return popup;
    }

    public synchronized void addSignalChangeListener(EventListener l) {
        listeners.add(l);
    }

    public synchronized void removeSignalChangeListener(EventListener l) {
        listeners.remove(l);
    }

    private synchronized void fireSignalChangeEvent(String message) {
        SignalChangeEvent event = new SignalChangeEvent(this, message, SOURCE_COMPONENT);
        Iterator<Object> iterator = listeners.iterator();
        while (iterator.hasNext()) {
            ((EventListener) iterator.next()).eventReceived(event);
        }
    }

}
