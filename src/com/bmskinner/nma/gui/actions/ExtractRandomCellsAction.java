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
package com.bmskinner.nma.gui.actions;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.MissingLandmarkException;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.VirtualDataset;
import com.bmskinner.nma.components.profiles.MissingProfileException;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.dialogs.SettingsDialog;
import com.bmskinner.nma.gui.events.UIController;
import com.bmskinner.nma.logging.Loggable;

/**
 * Allow a random subset of cells to be extracted as a child of the given
 * dataset.
 * 
 * @author ben
 * @since 1.13.8
 *
 */
public class ExtractRandomCellsAction extends SingleDatasetResultAction {

	private static final Logger LOGGER = Logger.getLogger(ExtractRandomCellsAction.class.getName());

	private static final String PROGRESS_LBL = "Extract cells";

	public ExtractRandomCellsAction(IAnalysisDataset dataset,
			@NonNull final ProgressBarAcceptor acceptor) {
		super(dataset, PROGRESS_LBL, acceptor);
		this.setProgressBarIndeterminate();
	}

	@Override
	public void run() {
		ExtractNucleiSetupDialog dialog = new ExtractNucleiSetupDialog();

		if (dialog.isReadyToRun()) {
			List<ICell> cells = new ArrayList<>(dataset.getCollection().getCells());

			Collections.shuffle(cells);

			List<ICell> subList = cells.subList(0, dialog.getCellCount());

			if (!subList.isEmpty()) {

				try {
					IAnalysisDataset c = new VirtualDataset(dataset, "Random_selection", null,
							subList);

					IAnalysisDataset d = dataset.addChildDataset(c);

					// set shared counts
					c.getCollection().setSharedCount(dataset.getCollection(),
							c.getCollection().size());
					dataset.getCollection().setSharedCount(c.getCollection(),
							c.getCollection().size());

					UIController.getInstance().fireDatasetAdded(d);
				} catch (ProfileException | MissingProfileException | MissingLandmarkException e) {
					LOGGER.warning("Error copying collection offsets");
					LOGGER.log(Loggable.STACK, "Error in offsetting", e);
				}
			}

		} else {
			LOGGER.fine("User cancelled operation");
		}
		cancel();
	}

	private class ExtractNucleiSetupDialog extends SettingsDialog implements ActionListener {

		private JSpinner spinner;

		public ExtractNucleiSetupDialog() {
			super(true);

			this.setTitle("Extract cells options");
			setSize(450, 300);
			this.setLocationRelativeTo(null);
			createGUI();
			// this.pack();
			this.setVisible(true);
		}

		public int getCellCount() {
			return (int) spinner.getModel().getValue();
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			// TODO Auto-generated method stub

		}

		private void createGUI() {

			setLayout(new BorderLayout());

			JPanel panel = new JPanel();
			GridBagLayout layout = new GridBagLayout();
			panel.setLayout(layout);

			List<JLabel> labels = new ArrayList<>();
			List<Component> fields = new ArrayList<>();

			spinner = new JSpinner(
					new SpinnerNumberModel(1, 1, dataset.getCollection().getNucleusCount(), 1));
			labels.add(new JLabel("Number of cells"));
			fields.add(spinner);

			this.addLabelTextRows(labels, fields, layout, panel);

			JPanel header = new JPanel(new FlowLayout());
			header.add(new JLabel("Extract random cells from the dataset"));

			this.add(header, BorderLayout.NORTH);
			this.add(panel, BorderLayout.CENTER);

			this.add(createFooter(), BorderLayout.SOUTH);

		}
	}

}
