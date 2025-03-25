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
package com.bmskinner.nma.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Document;

import com.bmskinner.nma.components.workspaces.IWorkspace;
import com.bmskinner.nma.core.DatasetListManager;

/**
 * Saves a workspace to a *.wrk file
 * 
 * @author Ben Skinner
 * @since 1.13.3
 *
 */
public class WorkspaceExporter extends XMLWriter implements Io {

	private static final Logger LOGGER = Logger.getLogger(WorkspaceExporter.class.getName());

	public static void exportWorkspace(@NonNull final IWorkspace w) {

		try {

			File exportFile = w.getSaveFile();
			if (exportFile == null)
				return;
			if (exportFile.exists())
				Files.delete(exportFile.toPath());

			Document doc = new Document(w.toXmlElement());

			writeXML(doc, exportFile);

			// Confirm save
			DatasetListManager.getInstance().updateHashCode(w);
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Unable to export workspace", e);
		}

	}

}
