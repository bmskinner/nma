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
package com.bmskinner.nma.gui.components.panels;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.jfree.chart.JFreeChart;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.core.DatasetListManager;
import com.bmskinner.nma.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nma.gui.DefaultInputSupplier;
import com.bmskinner.nma.gui.events.UserActionController;

@SuppressWarnings("serial")
public class ConsensusNucleusChartPanel extends ExportableChartPanel {

	private static final String RESET_OFFSET_LBL = "Reset offset to zero";

	private static final String OFFSET_LBL = "Offset...";

	private static final String RESET_ROTATION_LBL = "Reset rotation to tail";

	private static final String ROTATE_BY_LBL = "Rotate by...";

	private static final String ALIGN_VERTICAL_LBL = "Align vertical";

	public static final String SOURCE_COMPONENT = "ConsensusNucleusChartPanel";

	public static final String EXPORT_SVG_LBL = "Export SVG";

	private UserActionController uac = UserActionController.getInstance();

	public ConsensusNucleusChartPanel(JFreeChart chart) {
		super(chart);

		JPopupMenu popup = createPopupMenu();
		this.setPopupMenu(popup);
		this.validate();
		this.setFixedAspectRatio(true);
	}

	private JPopupMenu createPopupMenu() {
		JPopupMenu popup = this.getPopupMenu();
		popup.addSeparator();

		JMenuItem rotateItem = new JMenuItem(ROTATE_BY_LBL);
		rotateItem.addActionListener(e -> rotateConsensusNucleus());

		JMenuItem resetItem = new JMenuItem(RESET_ROTATION_LBL);
		resetItem.addActionListener(
				e -> uac.consensusRotationResetReceived(
						DatasetListManager.getInstance().getActiveDataset()));

		JMenuItem offsetItem = new JMenuItem(OFFSET_LBL);
		offsetItem.addActionListener(e -> offsetConsensusNucleus());

		JMenuItem resetOffsetItem = new JMenuItem(RESET_OFFSET_LBL);
		resetOffsetItem.addActionListener(
				e -> uac.consensusTranslationResetReceived(
						DatasetListManager.getInstance().getActiveDataset()));

		popup.add(rotateItem);
		popup.add(resetItem);
		popup.addSeparator();
		popup.add(offsetItem);
		popup.add(resetOffsetItem);
		return popup;
	}

	private void rotateConsensusNucleus() {

		IAnalysisDataset d = DatasetListManager.getInstance().getActiveDataset();
		if (d == null)
			return;
		if (!d.getCollection().hasConsensus())
			return;

		try {
			double angle = new DefaultInputSupplier().requestDouble("Choose the amount to rotate",
					0, -360, 360, 1.0);
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

		int option = JOptionPane.showOptionDialog(null, spinners,
				"Choose the amount to offset x and y",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
		if (option == JOptionPane.CANCEL_OPTION) {
			// user hit cancel
		} else if (option == JOptionPane.OK_OPTION) {
			double x = (Double) xSpinner.getModel().getValue();
			double y = (Double) ySpinner.getModel().getValue();

			uac.consensusTranslationUpdateReceived(d, x, y);
		}
	}
}
