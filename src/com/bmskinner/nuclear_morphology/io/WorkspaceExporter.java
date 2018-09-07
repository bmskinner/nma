/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import com.bmskinner.nuclear_morphology.components.workspaces.IWorkspace;
import com.bmskinner.nuclear_morphology.components.workspaces.IWorkspace.BioSample;
import com.bmskinner.nuclear_morphology.io.Io.Exporter;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Saves a workspace to a *.wrk file
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public abstract class WorkspaceExporter extends XMLWriter implements Exporter {
	
	private static final String VERSION_1_13_x = "1.13.x";
	private static final String VERSION_1_14_0 = "1.14.0";

	public abstract void exportWorkspace(@NonNull IWorkspace w);
	
	 /**
     * Create an exporter to save to file.
     * @return
     */
    public static WorkspaceExporter createExporter() {
    	String fileVersion = VERSION_1_14_0;
    	switch(fileVersion) {
    	
    		case VERSION_1_13_x: return new v_1_13_WorkspaceExporter();
    		case VERSION_1_14_0: return new v_1_14_0_WorkspaceExporter();
    		default: return new v_1_14_0_WorkspaceExporter();
    	}
    }
	
	
	/**
	 * The original workspace exporter, for compatibility only
	 * @author bms41
	 *
	 */
	private static class v_1_13_WorkspaceExporter extends WorkspaceExporter {
		
		private static final String NEWLINE = System.getProperty("line.separator");
		
	    @Override
		public void exportWorkspace(final @NonNull IWorkspace w) {

	        File exportFile = w.getSaveFile();
	        if (exportFile.exists())
	            exportFile.delete();

	        StringBuilder builder = new StringBuilder();

	        for (File f : w.getFiles()) 
	            builder.append(f.getAbsolutePath()+NEWLINE);

	        try(PrintWriter out = new PrintWriter(exportFile)) {
	            out.print(builder.toString());
	        } catch (FileNotFoundException e) {
	            warn("Cannot export workspace file");
	            fine("Error writing file", e);
	        }
	    }
	}
	
	/**
	 * The current workspace exporter
	 * @author bms41
	 * @since 1.14.0
	 *
	 */
	private static class v_1_14_0_WorkspaceExporter extends WorkspaceExporter {
		
	    @Override
		public void exportWorkspace(@NonNull final IWorkspace w) {

	        File exportFile = w.getSaveFile();
	        if (exportFile.exists())
	            exportFile.delete();
	        
	        try {
	            //root element
	            Element rootElement = new Element(IWorkspace.WORKSPACE_ELEMENT);
	            
	            Element workspaceName = new Element(IWorkspace.WORKSPACE_NAME);
	            workspaceName.setText(w.getName());
	            rootElement.addContent(workspaceName);
	            
	            // Add datasets
	            Element datasetsElement = new Element(IWorkspace.DATASETS_ELEMENT);
	            for(File f : w.getFiles()) {
	            	Element dataset = new Element("dataset");
	            	Element datasetPath = new Element(IWorkspace.DATASET_PATH);

	            	datasetPath.setText(f.getAbsolutePath());
	            	dataset.addContent(datasetPath);
	            	datasetsElement.addContent(dataset);
	            }
	            rootElement.addContent(datasetsElement);
	            
	            // Add biosamples
	            Element biosamplesElement = new Element(IWorkspace.BIOSAMPLES_ELEMENT);
	            for(BioSample b : w.getBioSamples()) {
	            	Element sampleElement = new Element("biosample");
	            	sampleElement.setAttribute(new Attribute(IWorkspace.BIOSAMPLES_NAME_KEY, b.getName()));
	            	
	            	for(File f : b.getDatasets()) {
	            		Element pathElement = new Element(IWorkspace.BIOSAMPLES_DATASET_KEY);
	            		pathElement.setText(f.getAbsolutePath());
	            		sampleElement.addContent(pathElement);
	            	}
	            	biosamplesElement.addContent(sampleElement);
	            }
	            
	            rootElement.addContent(biosamplesElement);
	            
	            Document doc = new Document(rootElement);

	            writeXML(doc, exportFile);
	         } catch(IOException e) {
	            e.printStackTrace();
	         }

	    }
	}


   

}
