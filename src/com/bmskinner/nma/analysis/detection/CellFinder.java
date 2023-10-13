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
package com.bmskinner.nma.analysis.detection;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.core.GlobalOptions;
import com.bmskinner.nma.io.ImageImporter;
import com.bmskinner.nma.io.ImageImporter.ImageImportException;
import com.bmskinner.nma.logging.Loggable;

/**
 * An implementation of the Finder for cells
 * 
 * @author ben
 * @since 1.13.5
 *
 */
public abstract class CellFinder extends AbstractFinder<ICell> {

	private static final Logger LOGGER = Logger.getLogger(CellFinder.class.getName());

	/**
	 * Construct the finder using an options
	 * 
	 * @param op the options for cell detection
	 */
	protected CellFinder(@NonNull final IAnalysisOptions op) {
		super(op);

	}

	@Override
	public Collection<ICell> findInFolder(@NonNull final File folder) throws ImageImportException {

		final Queue<ICell> list = new ConcurrentLinkedQueue<>();
		File[] arr = folder.listFiles();
		if (arr == null)
			return list;

		if (GlobalOptions.getInstance().getBoolean(GlobalOptions.IS_SINGLE_THREADED_DETECTION))
			// single threaded for use in testing
			return singleThreaded(arr);

		// Submitted to the FJP::commonPool, which is thread limited by the ThreadManger
		return multiThreaded(arr);
	}

	/**
	 * Detect cells in images using a single thread
	 * 
	 * @param arr
	 * @return
	 */
	private Collection<ICell> singleThreaded(File[] arr) {
		final List<ICell> list = new ArrayList<>();

		for (File f : arr) {

			// Check we are good to use this file
			if (Thread.interrupted() || f.isDirectory() || !ImageImporter.isFileImportable(f))
				continue;

			try {
				list.addAll(findInImage(f));
				LOGGER.finer(() -> "Found images in %s".formatted(f.getName()));
			} catch (ImageImportException e) {
				LOGGER.log(Loggable.STACK, "Error searching image", e);
			}
		}

		return list;
	}

	/**
	 * Detect cells in images using a multiple threads. Files are submitted to the
	 * FJP::commonPool, which is thread limited by the ThreadManger
	 * 
	 * @param arr
	 * @return
	 */
	private Collection<ICell> multiThreaded(File[] arr) {

		final Queue<ICell> list = new ConcurrentLinkedQueue<>();

		Stream.of(arr).parallel().forEach(f -> {

			if (Thread.interrupted())
				return;
			if (f.isDirectory())
				return;
			if (!ImageImporter.isFileImportable(f))
				return;
			try {
				list.addAll(findInImage(f));
				LOGGER.finer(() -> "Found images in %s".formatted(f.getName()));
			} catch (ImageImportException e) {
				LOGGER.log(Loggable.STACK, "Error searching image", e);
			} catch (Exception e) {
				LOGGER.log(Loggable.STACK, "Error detecting cell", e);
				throw (e);
			}

		});
		return list;
	}
}
