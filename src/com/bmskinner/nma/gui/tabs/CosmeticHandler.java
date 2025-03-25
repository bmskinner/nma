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
package com.bmskinner.nma.gui.tabs;

import java.awt.Color;
import java.awt.Paint;
import java.io.File;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.ICellCollection;
import com.bmskinner.nma.components.datasets.IClusterGroup;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.components.options.MissingOptionException;
import com.bmskinner.nma.components.signals.ISignalGroup;
import com.bmskinner.nma.components.workspaces.IWorkspace;
import com.bmskinner.nma.core.DatasetListManager;
import com.bmskinner.nma.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nma.gui.Labels;
import com.bmskinner.nma.gui.components.ColourSelecter;
import com.bmskinner.nma.gui.events.UIController;
import com.bmskinner.nma.gui.events.UserActionController;
import com.bmskinner.nma.gui.events.UserActionEvent;
import com.bmskinner.nma.logging.Loggable;
import com.bmskinner.nma.utility.FileUtils;

/**
 * Handle cosmetic changes in datasets. Generates the dialogs for confirmation.
 * 
 * @author Ben Skinner
 * @since 1.13.8
 *
 */
public class CosmeticHandler {

	private static final Logger LOGGER = Logger.getLogger(CosmeticHandler.class.getName());

	private static final String CHOOSE_A_NEW_NAME_LBL = "Choose a new name";
	private final TabPanel parent;

	/**
	 * Create the handler for a panel
	 * 
	 * @param p the panel to register the handler to
	 */
	public CosmeticHandler(@NonNull TabPanel p) {
		parent = p;
	}

	/**
	 * Choose a new scale for the dataset and apply it to all cells
	 * 
	 * @param dataset
	 * @param row
	 */
	public void changeDatasetScale(@NonNull IAnalysisDataset dataset) {

		try {
			double initialScale = 1;
			Optional<IAnalysisOptions> op = dataset.getAnalysisOptions();
			if (op.isPresent()) {
				Optional<HashOptions> nOp = op.get().getDetectionOptions(CellularComponent.NUCLEUS);
				if (nOp.isPresent())
					initialScale = nOp.get().getDouble(HashOptions.SCALE);
			}

			double scale = parent.getInputSupplier().requestDouble(
					Labels.Cells.CHOOSE_NEW_SCALE_LBL, initialScale, 1,
					100000, 1);
			dataset.setScale(scale);
		} catch (RequestCancelledException e) {
			// User cancelled, no action
		}
	}

	/**
	 * Make a JColorChooser for the given dataset, and set the color.
	 * 
	 * @param dataset
	 * @param row
	 */
	public void changeDatasetColour(@NonNull IAnalysisDataset dataset) {

		int row = DatasetListManager.getInstance().getSelectedDatasets().indexOf(dataset);
		Paint oldColour = dataset.getDatasetColour().orElse(ColourSelecter.getColor(row));

		try {
			Color newColor = parent.getInputSupplier().requestColor("Choose dataset colour",
					(Color) oldColour);
			dataset.setDatasetColour(newColor);
			UIController.getInstance().fireDatasetColourUpdated(dataset);

		} catch (RequestCancelledException e) {
			// User cancelled, no action
		}
	}

	/**
	 * Rename an existing dataset and update the population list.
	 * 
	 * @param dataset the dataset to rename
	 */
	public void renameDataset(@NonNull IAnalysisDataset dataset) {
		ICellCollection collection = dataset.getCollection();

		try {
			String newName = parent.getInputSupplier().requestString(CHOOSE_A_NEW_NAME_LBL,
					collection.getName());
			collection.setName(newName);
		} catch (RequestCancelledException e) {
			// User cancelled, no action
		}
	}

	/**
	 * Rename an existing group and update the population list.
	 * 
	 * @param group the group to rename
	 */
	public void renameClusterGroup(@NonNull IClusterGroup group) {

		try {
			String newName = parent.getInputSupplier().requestString(CHOOSE_A_NEW_NAME_LBL,
					group.getName());
			group.setName(newName);
		} catch (RequestCancelledException e) {
			// User cancelled, no action
		}
	}

	/**
	 * Rename an existing workspace and update the population list.
	 * 
	 * @param workspace the workspace to rename
	 */
	public void renameWorkspace(@NonNull IWorkspace workspace) {

		try {
			String newName = parent.getInputSupplier().requestString(CHOOSE_A_NEW_NAME_LBL,
					workspace.getName());
			workspace.setName(newName);

			// Automatically save workspace
			UserActionController.getInstance().userActionEventReceived(
					new UserActionEvent(this, UserActionEvent.SAVE_WORKSPACE));

		} catch (RequestCancelledException e) {
			// User cancelled, no action
		}
	}

	/**
	 * Update the colour of a signal group
	 * 
	 * @param d             the dataset
	 * @param oldColour     the old colour
	 * @param signalGroupId the signal group to change
	 * @return true if the colour was changed, false otherwise
	 */
	public boolean changeSignalColour(@NonNull IAnalysisDataset d, @NonNull UUID signalGroupId) {

		if (!d.getCollection().hasSignalGroup(signalGroupId))
			return false;
		try {

			Color oldColour = d.getCollection().getSignalGroup(signalGroupId).get().getGroupColour()
					.orElse(Color.YELLOW);
			Color newColor = parent.getInputSupplier()
					.requestColor(Labels.Signals.CHOOSE_SIGNAL_COLOUR, oldColour);

			d.getCollection().getSignalGroup(signalGroupId).get().setGroupColour(newColor);

			// If we updated a child signal colour, we need to refresh the parent
			// because they share the same signal group object
			if (!d.isRoot()) {
				d = DatasetListManager.getInstance().getRootParent(d);
			}

			UIController.getInstance().fireNuclearSignalUpdated(d);
		} catch (RequestCancelledException e) {
			return false;
		}
		return true;
	}

	/**
	 * Update the name of a signal group in the active dataset
	 * 
	 * @param signalGroup
	 */
	public void renameSignalGroup(@NonNull IAnalysisDataset d, @NonNull UUID signalGroup) {
		Optional<ISignalGroup> groupValue = d.getCollection().getSignalGroup(signalGroup);
		if (!groupValue.isPresent())
			return;
		ISignalGroup group = groupValue.get();
		String oldName = group.getGroupName();

		try {
			String newName = parent.getInputSupplier().requestString(CHOOSE_A_NEW_NAME_LBL,
					oldName);
			group.setGroupName(newName);
			UIController.getInstance().fireNuclearSignalUpdated(d);
		} catch (RequestCancelledException e) {
			// user cancelled, no action
		}
	}

	/**
	 * Update the source image folder for the given signal group
	 * 
	 * @param d
	 * @param signalGroup
	 */
	public void updateSignalSource(@NonNull IAnalysisDataset d, @NonNull UUID signalGroup) {

		try {

			File currentFolder = d.getAnalysisOptions().orElseThrow(MissingOptionException::new)
					.getNuclearSignalDetectionFolder(signalGroup)
					.orElseThrow(MissingOptionException::new);
			File newFolder = parent.getInputSupplier()
					.requestFolder(FileUtils.extantComponent(currentFolder));

			d.getCollection().getSignalManager().updateSignalSourceFolder(signalGroup,
					newFolder.getAbsoluteFile());
			UIController.getInstance().fireNuclearSignalUpdated(d);
		} catch (RequestCancelledException e) {
			// user cancelled, ignore
		} catch (MissingOptionException e) {
			LOGGER.log(Loggable.STACK, "Error updating signal source", e);
		}

	}

	/**
	 * Update the nucleus folder for nuclei in the given image
	 * 
	 * @param d     the dataset to update
	 * @param image the image to update cells within
	 */
	public void updateNucleusSource(@NonNull IAnalysisDataset d, File image) {

		try {
			File folder = parent.getInputSupplier()
					.requestFolder(FileUtils.extantComponent(image.getParentFile()));

			Set<ICell> cells = d.getCollection().getCells(image);

			for (ICell c : cells)
				for (Nucleus n : c.getNuclei())
					n.setSourceFolder(folder);
			UIController.getInstance().fireFilePathUpdated(d);
		} catch (RequestCancelledException e) {
			// User cancelled, no action
		}

	}

	/**
	 * Update the source image folder for the given signal group
	 * 
	 * @param d the dataset to update
	 */
	public void updateNucleusSource(@NonNull IAnalysisDataset d) {

		try {
			File currentFolder = d.getAnalysisOptions().get()
					.getNucleusDetectionFolder().get();
			File newFolder = parent.getInputSupplier()
					.requestFolder(FileUtils.extantComponent(currentFolder));

			d.getCollection().setSourceFolder(newFolder);
			UIController.getInstance().fireFilePathUpdated(d);
		} catch (RequestCancelledException e) {
			// User cancelled, no action
		}

	}
}
