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
package com.bmskinner.nuclear_morphology.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Document;
import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.components.workspaces.IWorkspace;
import com.bmskinner.nuclear_morphology.core.DatasetListManager;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Saves a workspace to a *.wrk file
 * 
 * @author ben
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

			// root element
			Element rootElement = new Element(IWorkspace.WORKSPACE_ELEMENT)
					.setAttribute(IWorkspace.WORKSPACE_NAME, w.getName());

			// Add datasets
			Element datasetsElement = new Element(IWorkspace.DATASETS_ELEMENT);
			for (File f : w.getFiles()) {
				Element dataset = new Element("dataset");
				Element datasetPath = new Element(IWorkspace.DATASET_PATH);

				datasetPath.setText(f.getAbsolutePath());
				dataset.addContent(datasetPath);
				datasetsElement.addContent(dataset);
			}
			rootElement.addContent(datasetsElement);

			Document doc = new Document(rootElement);

			writeXML(doc, exportFile);

			// Confirm save
			DatasetListManager.getInstance().updateHashCode(w);
		} catch (IOException e) {
			LOGGER.log(Loggable.STACK, "Unable to export workspace", e);
		}

	}

}
