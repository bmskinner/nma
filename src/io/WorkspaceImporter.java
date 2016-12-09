package io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import analysis.DefaultWorkspace;
import analysis.IWorkspace;
import logging.Loggable;

/**
 * Load a workspace
 * @author ben
 * @since 1.13.3
 *
 */
public class WorkspaceImporter implements Loggable {
	
	private final File file;
	public final String CHARSET = "ISO-8859-1";
	
	/**
	 * Construct with a file to import. 
	 * @param f the file
	 * @throws IllegalArgumentException if the file is null, a folder, or otherwise not a valid file
	 */
	public WorkspaceImporter(final File f){
		if(f==null){
			throw new IllegalArgumentException("File cannot be null");
		}
		
		if( ! f.exists()){
			throw new IllegalArgumentException("File does not exist");
		}
		
		if( f.isDirectory()){
			throw new IllegalArgumentException("File is a directory");
		}
		
		if( ! f.isFile()){
			throw new IllegalArgumentException("File has non-normal attributes or is not a file");
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
