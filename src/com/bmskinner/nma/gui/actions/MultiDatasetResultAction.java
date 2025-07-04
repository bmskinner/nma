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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.gui.ProgressBarAcceptor;

/**
 * An action class that allows multiple datasets to be operated on
 * simultaneously
 * 
 * @author bms41
 * @since 1.13.6
 *
 */
public abstract class MultiDatasetResultAction extends VoidResultAction {

	protected final @NonNull List<IAnalysisDataset> datasets = new ArrayList<>();

	protected MultiDatasetResultAction(@NonNull final List<IAnalysisDataset> datasets, @NonNull final String barMessage,
			@NonNull final ProgressBarAcceptor acceptor) {
		super(barMessage, acceptor);
		this.datasets.addAll(datasets);
	}

}
