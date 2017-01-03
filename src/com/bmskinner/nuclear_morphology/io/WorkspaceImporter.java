package com.bmskinner.nuclear_morphology.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import com.bmskinner.nuclear_morphology.analysis.DefaultWorkspace;
import com.bmskinner.nuclear_morphology.analysis.IWorkspace;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Load a workspace
 * @author ben
 * @since 1.13.3
 *
 */
public class WorkspaceImporter implements Loggable, Importer {
	
	private final File file;
	public final String CHARSET = "ISO-8859-1";
	
	/**
	 * Construct with a file to import. 
	 * @param f the file
	 * @throws IllegalArgumentException if the file is null, a folder, or otherwise not a valid file
	 */
	public WorkspaceImporter(final File f){
		if( ! Importer.isSuitableImportFile(f)){
			throw new IllegalArgumentException(INVALID_FILE_ERROR);
		}

		file = f;
	}
	
	/**
	 * Import the workspace described by this importer.
	 * @return a workspace
	 */
	public IWorkspace importWorkspace(){
		
		IWorkspace w = new DefaultWorkspace(file);
		try {
			
			FileInputStream fstream = new FileInputStream(file);
			BufferedReader br = new BufferedReader(
					new InputStreamReader(fstream, Charset.forName(CHARSET)));

			int i = 0;
			String strLine;
			while (( strLine = br.readLine()) != null) {
				i++; 
				
				File f = new File(strLine);
				
				if(f.exists()){
					w.add(f);
				}

			}
			fstream.close();
		}
		catch (Exception e) {
			error("Error parsing workspace file", e);
		}
		
		return w;
	}

}
