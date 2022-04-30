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
import java.util.concurrent.CountDownLatch;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.DefaultAnalysisWorker;
import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.analysis.nucleus.CellRelocationMethod;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.components.FileSelector;
import com.bmskinner.nma.gui.events.UIController;

/**
 * Creates child datasets from a .cell mapping file
 * 
 * @author ben
 *
 */
public class RelocateFromFileAction extends SingleDatasetResultAction {

	private static final @NonNull String PROGRESS_LBL = "Relocating cells";

	public RelocateFromFileAction(@NonNull IAnalysisDataset dataset, @NonNull final ProgressBarAcceptor acceptor,
			 CountDownLatch latch) {
		super(dataset, PROGRESS_LBL, acceptor);
		this.setLatch(latch);
		setProgressBarIndeterminate();
	}

	@Override
	public void run() {
		/*
		 * Get the file to search
		 */

		File file = FileSelector.chooseRemappingFile(dataset);
		if (file != null) {

			IAnalysisMethod m = new CellRelocationMethod(dataset, file);
			worker = new DefaultAnalysisWorker(m);

			worker.addPropertyChangeListener(this);

			this.setProgressMessage("Locating cells...");
			ThreadManager.getInstance().submit(worker);
		} else {
			cancel();
		}
	}

	@Override
	public void finished() {
		UIController.getInstance().fireDatasetAdded(dataset);
		this.countdownLatch();
		super.finished();
	}

}
