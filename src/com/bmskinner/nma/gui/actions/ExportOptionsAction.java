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
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.components.FileSelector;
import com.bmskinner.nma.io.Io;
import com.bmskinner.nma.io.XMLWriter;
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

		if (datasets.size() == 1) {
			File file = FileSelector.chooseOptionsExportFile(datasets.get(0));

			if (file == null) {
				cancel();
				return;
			}

			Runnable r = () -> {

				Optional<IAnalysisOptions> opt = datasets.get(0).getAnalysisOptions();
				if (opt.isEmpty())
					return;

				// Remove any folders from the export
				// The point of this is to make a reusable analysis,
				// not replicate existing datasets
				IAnalysisOptions op = opt.get().duplicate();
				for (String s : op.getDetectionOptionTypes()) {
					op.getDetectionOptions(s).get().remove(HashOptions.DETECTION_FOLDER);
				}

				// Also remove the analysis time so we don't use the same output folder
				op.clearAnalysisTime();

				try {
					XMLWriter.writeXML(op.toXmlElement(),
							file);
				} catch (IOException e) {
					LOGGER.warning("Unable to write options to file");
				}
				cancel();
			};
			ThreadManager.getInstance().submit(r);
		} else {

			// More than one dataset, choose folder only
			try {
				File folder = is.requestFolder(FileUtils.commonPathOfDatasets(datasets));
				Runnable r = () -> {
					for (IAnalysisDataset d : datasets) {
						File f = new File(folder, d.getName() + Io.XML_FILE_EXTENSION);
						try {
							XMLWriter.writeXML(d.getAnalysisOptions().get().toXmlElement(), f);
						} catch (IOException e) {
							LOGGER.warning("Unable to write options to file");
						}
						LOGGER.info(String.format("Exported %s options to %s", d.getName(),
								f.getAbsolutePath()));
					}
					cancel();
				};
				ThreadManager.getInstance().submit(r);
			} catch (RequestCancelledException e) {
				cancel();
			}

		}

	}

}
