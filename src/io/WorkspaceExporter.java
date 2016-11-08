package io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import components.active.IWorkspace;
import logging.Loggable;

/**
 * Saves a workspace to a *.wrk file 
 * @author ben
 * @since 1.13.3
 *
 */
public class WorkspaceExporter implements Loggable {
	
	private static final String NEWLINE = System.getProperty("line.separator"); 
	final IWorkspace w;
	
	public WorkspaceExporter(final IWorkspace w){
		
		this.w = w;
	}
	
	public void export(){
		
		File exportFile = w.getSaveFile();

		
		if(exportFile.exists()){
			exportFile.delete();
		}

		
		StringBuilder builder = new StringBuilder();
		
		/*
		 * Add the save paths of nmds
		 */
		for(File f : w.getFiles()){
			builder.append(f.getAbsolutePath());
			builder.append(NEWLINE);
		}
		

		try {
			
			PrintWriter out;
			out = new PrintWriter(exportFile);
			out.print(builder.toString());
			out.close();
			
		} catch (FileNotFoundException e) {
			warn("Cannot export workspace file");
			fine("Error writing file", e);
		}
	}

}
