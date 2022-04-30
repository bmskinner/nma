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
import java.util.List;
import java.util.logging.Logger;

import javax.xml.XMLConstants;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import com.bmskinner.nuclear_morphology.components.workspaces.DefaultWorkspace;
import com.bmskinner.nuclear_morphology.components.workspaces.IWorkspace;
import com.bmskinner.nuclear_morphology.io.Io.Importer;

/**
 * Load a workspace
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public class WorkspaceImporter implements Importer {

	private static final Logger LOGGER = Logger.getLogger(WorkspaceImporter.class.getName());

	/**
	 * Open the workspace format created by software from version 1.14.0. This is
	 * based on XML encoding to allow extra fields such as BioSamples to be
	 * included.
	 * 
	 * @author bms41
	 * @throws IOException
	 * @throws JDOMException
	 * @since 1.14.0
	 *
	 */

	public static IWorkspace importWorkspace(File file) throws JDOMException, IOException {

		LOGGER.fine("Attempting to read workspace file: " + file.getAbsolutePath());

		SAXBuilder saxBuilder = new SAXBuilder();
		saxBuilder.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		saxBuilder.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

		Document document = saxBuilder.build(file);

		Element workspaceElement = document.getRootElement();
		String name = workspaceElement.getAttributeValue(IWorkspace.WORKSPACE_NAME);

		IWorkspace w = new DefaultWorkspace(file, name);

		Element datasetElement = workspaceElement.getChild(IWorkspace.DATASETS_ELEMENT);
		Element sampleElement = workspaceElement.getChild(IWorkspace.BIOSAMPLES_ELEMENT);

		List<Element> datasets = datasetElement.getChildren();

		for (Element dataset : datasets) {
			String path = dataset.getChild(IWorkspace.DATASET_PATH).getText();
			File f = new File(path);
			LOGGER.fine("Workspace has dataset file: " + f.getAbsolutePath());
			w.add(f);
		}

		return w;
	}

}
