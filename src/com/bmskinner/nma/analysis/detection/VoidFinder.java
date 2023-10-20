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
import java.util.Collection;
import java.util.logging.Logger;

import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.io.ImageImporter;
import com.bmskinner.nma.io.ImageImporter.ImageImportException;
import com.bmskinner.nma.logging.Loggable;

/**
 * An implementation of the Finder for analyses that don't return values. For
 * example, FISH remapping and signal assignment
 * 
 * @author ben
 * @since 1.13.5
 *
 */
public abstract class VoidFinder extends AbstractFinder<Void> {

	private static final Logger LOGGER = Logger.getLogger(VoidFinder.class.getName());

	public VoidFinder(IAnalysisOptions op) {
		super(op);
	}

	@Override
	public Collection<Void> findInFolder(File folder) throws ImageImportException {

		File[] files = folder.listFiles();
		if (files == null)
			return null;

		for (File f : files) {
			if (ImageImporter.isFileImportable(f)) {
				try {
					findInImage(f);
				} catch (ImageImportException e) {
					LOGGER.log(Loggable.STACK, "Error searching image", e);
				}
			}

		}
		return null;
	}

}
