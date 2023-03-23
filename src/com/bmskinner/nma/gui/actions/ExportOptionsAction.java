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

import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.components.FileSelector;
import com.bmskinner.nma.io.DatasetOptionsExportMethod;
import com.bmskinner.nma.utility.FileUtils;

/**
 * Export the options stored in a dataset
 * 
 * @author ben
 * @since 1.14.0
 *
 */
public class ExportOptionsAction extends MultiDatasetResultAction {

	private static final Logger LOGGER = Logger.getLogger(ExportOptionsAction.class.getName());

	private static final String PROGRESS_LBL = "Exporting options";

	public ExportOptionsAction(@NonNull List<IAnalysisDataset> datasets,
			@NonNull final ProgressBarAcceptor acceptor) {
		super(datasets, PROGRESS_LBL, acceptor);
	}

	@Override
	public void run() {
		setProgressBarIndeterminate();

		try {
			File file = datasets.size() == 1 ? FileSelector.chooseOptionsExportFile(datasets.get(0))
					: is.requestFolder(FileUtils.commonPathOfDatasets(datasets));

			Runnable r = () -> {
				try {
					new DatasetOptionsExportMethod(datasets, file).call();
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE, "Error exporting options", e);
				}
				finished();
			};

			ThreadManager.getInstance().submit(r);
		} catch (RequestCancelledException e) {
			// user cancelled, no action
		}

	}

}
