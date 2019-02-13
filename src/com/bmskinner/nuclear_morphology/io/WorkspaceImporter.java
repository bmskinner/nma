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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import com.bmskinner.nuclear_morphology.components.workspaces.DefaultWorkspace;
import com.bmskinner.nuclear_morphology.components.workspaces.IWorkspace;
import com.bmskinner.nuclear_morphology.components.workspaces.IWorkspace.BioSample;
import com.bmskinner.nuclear_morphology.components.workspaces.WorkspaceFactory;
import com.bmskinner.nuclear_morphology.io.Io.Importer;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Load a workspace
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public abstract class WorkspaceImporter implements Loggable, Importer {
	
	private static final String VERSION_1_13_x = "1.13.x";
	private static final String VERSION_1_14_0 = "1.14.0";
	
	public abstract IWorkspace importWorkspace();
    
    /**
     * Create an importer to open the given file. The importer will be
     * created based on the workspace version of the file.
     * @param f the workspace file to open
     * @return
     */
    public static WorkspaceImporter createImporter(@NonNull final File f) {
    	String fileVersion = getWorkspaceFileVersion(f);
    	switch(fileVersion) {
    	
    		case VERSION_1_13_x: return new v_1_13_WorkspaceImporter(f);
    		case VERSION_1_14_0: return new v_1_14_0_WorkspaceImporter(f);
    		default: return new v_1_14_0_WorkspaceImporter(f);
    	}
    	
    }

    /**
     * Determine the workspace file version based on the content of the given file
     * @param f
     * @return
     */
    private static String getWorkspaceFileVersion(@NonNull final File f){


    	try( FileInputStream fstream = new FileInputStream(f);
    			BufferedReader br = new BufferedReader(new InputStreamReader(fstream));){

    		String firstLine = br.readLine();
    		if(firstLine.startsWith("<?xml version"))
    			return VERSION_1_14_0;

    	} catch (Exception e) {
    		e.printStackTrace();
    	}

    	return VERSION_1_13_x;
    }
    
    
    /**
     * Open the workspace format created by software from version 1.14.0.
     * This is based on XML encoding to allow extra fields such as BioSamples
     * to be included.
     * @author bms41
     * @since 1.14.0
     *
     */
    private static class v_1_14_0_WorkspaceImporter extends WorkspaceImporter {
    	
    	private final File  file;
    	
    	private v_1_14_0_WorkspaceImporter(@NonNull final File f) {
    		file=f;
    	}

		@Override
		public IWorkspace importWorkspace() {
			
			IWorkspace w = WorkspaceFactory.createWorkspace(file);
			
		    try {
		         SAXBuilder saxBuilder = new SAXBuilder();
		         Document document = saxBuilder.build(file);
		         
		         Element workspaceElement = document.getRootElement();
		         String workspaceName =  workspaceElement.getChild(IWorkspace.WORKSPACE_NAME).getText();
		         w.setName(workspaceName);
		         
		         Element datasetElement =  workspaceElement.getChild(IWorkspace.DATASETS_ELEMENT);
		         Element sampleElement  =  workspaceElement.getChild(IWorkspace.BIOSAMPLES_ELEMENT);
		         
		         List<Element> datasets = datasetElement.getChildren();


		         for(Element dataset : datasets) {
		        	 String path = dataset.getChild(IWorkspace.DATASET_PATH).getText();
		        	 File f = new File(path);
		        	 w.add(f);
		         }
		         

		         
		         List<Element> biosamples = sampleElement.getChildren();
		         for(Element sample : biosamples) {
		        	 String name = sample.getAttributeValue(IWorkspace.BIOSAMPLES_NAME_KEY);

		        	 w.addBioSample(name);
		        	 List<Element> sampleDatasets = sample.getChildren(IWorkspace.BIOSAMPLES_DATASET_KEY);
		        	 for(Element d : sampleDatasets) {
		        		 String datasetId =  d.getValue();
		        		 File f = new File(datasetId);
		        		 BioSample bs = w.getBioSample(name);
		        		 if(bs!=null)
		        			 bs.addDataset(f);
		        	 }
		         }
		         		         
		      } catch(JDOMException e) {
		         e.printStackTrace();
		      } catch(IOException ioe) {
		         ioe.printStackTrace();
		      }
			return w;
		}
    	
    }
    
    
    /**
     * Open the workspace format created by software up till and including version 1.13.8
     * @author bms41
     * @since 1.14.0
     *
     */
    private static class v_1_13_WorkspaceImporter extends WorkspaceImporter {
    	
    	private final File  file;
        private final IWorkspace w;
        private final String CHARSET = "ISO-8859-1";
    	
    	public v_1_13_WorkspaceImporter(@NonNull final File f) {

           file = f;
           w = new DefaultWorkspace(file);    
        }
    	
    	 /**
         * Import the workspace described by this importer.
         * Applies to the workspace format in versions 1.13.x.
         * For v1.14.0 and beyond, use the {@link importWorkspace} method.
         * 
         * @return a workspace
         */
    	@Override
        public IWorkspace importWorkspace() {

            
            try {

                FileInputStream fstream = new FileInputStream(file);
                BufferedReader br = new BufferedReader(new InputStreamReader(fstream, Charset.forName(CHARSET)));

                int i = 0;
                String strLine;
                while ((strLine = br.readLine()) != null) {
                    i++;
                    parseLine(strLine);
                }
                fstream.close();
            } catch (Exception e) {
                error("Error parsing workspace file", e);
            }

            return w;
        }
        
        private void parseLine(String line){
            
            // Check line format
            // Pre 1.13.8 have just the dataset file name.
            
            File f = null;
            
            if(line.endsWith(Importer.SAVE_FILE_EXTENSION)){
                // old format
                f = new File(line);
                
            } else {
                
                String[] arr = line.split(TAB);
                f = new File(arr[0]);
                
                String group = arr[1];
                
                w.addBioSample(group);
                BioSample bs = w.getBioSample(group);
                if(bs!=null)
                	bs.addDataset(f);
            }

            if (f.exists()) {
                w.add(f);
            }
        }

    	
    }

   
}
