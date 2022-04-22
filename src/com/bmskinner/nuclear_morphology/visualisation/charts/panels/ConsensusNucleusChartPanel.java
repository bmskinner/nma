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
package com.bmskinner.nuclear_morphology.visualisation.charts.panels;

import java.awt.Color;
import java.awt.Paint;
import java.io.File;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;

import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.measure.MeasurementScale;
import com.bmskinner.nuclear_morphology.core.DatasetListManager;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nuclear_morphology.gui.DefaultInputSupplier;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;
import com.bmskinner.nuclear_morphology.gui.events.revamp.UserActionController;
import com.bmskinner.nuclear_morphology.io.Io;
import com.bmskinner.nuclear_morphology.io.SVGWriter;
import com.bmskinner.nuclear_morphology.visualisation.charts.overlays.ShapeOverlay;
import com.bmskinner.nuclear_morphology.visualisation.charts.overlays.ShapeOverlayObject;
import com.bmskinner.nuclear_morphology.visualisation.datasets.ComponentOutlineDataset;

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

	private ShapeOverlay consensusOverlay = null;

	private UserActionController uac = UserActionController.getInstance();

	public ConsensusNucleusChartPanel(JFreeChart chart) {
		super(chart);

		JPopupMenu popup = createPopupMenu();
		this.setPopupMenu(popup);
		this.validate();
		this.setFixedAspectRatio(true);
		consensusOverlay = new ShapeOverlay();
		this.addOverlay(consensusOverlay);

	}

	/**
	 * Provide an override to the GlobalOptions for this panel. If this is false,
	 * the consensus will never be filled. If this is true, the consensus will be
	 * filled then the GlobalOptions is also true
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

			// Per panel override in case this panel should always be outline only
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
//					consensusOverlay.addShape(o, n);
					consensusOverlay.addShape(o);
				}
			}

		}

	}

	private JPopupMenu createPopupMenu() {
		JPopupMenu popup = this.getPopupMenu();
		popup.addSeparator();

		JMenuItem rotateItem = new JMenuItem(ROTATE_BY_LBL);
		rotateItem.addActionListener(e -> rotateConsensusNucleus());

		JMenuItem resetItem = new JMenuItem(RESET_ROTATION_LBL);
		resetItem.addActionListener(
				e -> uac.consensusRotationResetReceived(DatasetListManager.getInstance().getActiveDataset()));

		JMenuItem offsetItem = new JMenuItem(OFFSET_LBL);
		offsetItem.addActionListener(e -> offsetConsensusNucleus());

		JMenuItem resetOffsetItem = new JMenuItem(RESET_OFFSET_LBL);
		resetOffsetItem.addActionListener(
				e -> uac.consensusTranslationResetReceived(DatasetListManager.getInstance().getActiveDataset()));

		JMenuItem exportSvgItem = new JMenuItem(EXPORT_SVG_LBL);
		exportSvgItem.addActionListener(e -> exportConsensusNuclei());

		popup.add(rotateItem);
		popup.add(resetItem);
		popup.addSeparator();
		popup.add(offsetItem);
		popup.add(resetOffsetItem);
		popup.addSeparator();
		popup.add(exportSvgItem);

		return popup;
	}

	private void rotateConsensusNucleus() {

		IAnalysisDataset d = DatasetListManager.getInstance().getActiveDataset();
		if (d == null)
			return;
		if (!d.getCollection().hasConsensus())
			return;

		try {
			double angle = new DefaultInputSupplier().requestDouble("Choose the amount to rotate", 0, -360, 360, 1.0);
			uac.consensusRotationUpdateReceived(d, angle);
		} catch (RequestCancelledException e) {
		}
	}

	private void offsetConsensusNucleus() {
		IAnalysisDataset d = DatasetListManager.getInstance().getActiveDataset();
		if (d == null)
			return;
		if (!d.getCollection().hasConsensus())
			return;

		// get the x and y offset
		SpinnerNumberModel xModel = new SpinnerNumberModel(0, -100, 100, 0.1);
		SpinnerNumberModel yModel = new SpinnerNumberModel(0, -100, 100, 0.1);

		JSpinner xSpinner = new JSpinner(xModel);
		JSpinner ySpinner = new JSpinner(yModel);

		JSpinner[] spinners = { xSpinner, ySpinner };

		int option = JOptionPane.showOptionDialog(null, spinners, "Choose the amount to offset x and y",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
		if (option == JOptionPane.CANCEL_OPTION) {
			// user hit cancel
		} else if (option == JOptionPane.OK_OPTION) {
			double x = (Double) xSpinner.getModel().getValue();
			double y = (Double) ySpinner.getModel().getValue();

			uac.consensusTranslationUpdateReceived(d, x, y);
		}
	}

	private void exportConsensusNuclei() {
		IAnalysisDataset d = DatasetListManager.getInstance().getActiveDataset();
		if (d == null)
			return;
		if (!d.getCollection().hasConsensus())
			return;

		String defaultFileName = DatasetListManager.getInstance().isMultipleSelectedDatasets() ? "Outlines"
				: d.getName();
		File defaultFolder = IAnalysisDataset.commonPathOfFiles(DatasetListManager.getInstance().getSelectedDatasets());

		try {
			File exportFile = new DefaultInputSupplier().requestFileSave(defaultFolder, defaultFileName,
					Io.SVG_FILE_EXTENSION_NODOT);

			// If the file exists, confirm before overwriting
			if (exportFile.exists()) {
				if (!new DefaultInputSupplier().requestApproval("Overwrite existing file?", "Confirm overwrite"))
					return;
			}

			SVGWriter wr = new SVGWriter(exportFile);

			String[] scaleChoices = new String[] { MeasurementScale.MICRONS.toString(),
					MeasurementScale.PIXELS.toString() };

			int scaleChoice = new DefaultInputSupplier().requestOption(scaleChoices, "Choose scale");

			MeasurementScale scale = scaleChoice == 0 ? MeasurementScale.MICRONS : MeasurementScale.PIXELS;
			wr.exportConsensusOutlines(DatasetListManager.getInstance().getSelectedDatasets(), scale);
		} catch (RequestCancelledException e) {
		}
	}
}
