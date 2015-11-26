/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

import gui.MainWindow;
import ij.IJ;
import ij.plugin.PlugIn;

import java.io.File;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class Nuclear_Morphology_Analysis
implements PlugIn

{
	
	String[] requiredFiles = {
			"commons-math3",
			"jcommon",
			"jdistlib",
			"jebl",
			"jfreechart",
			"swingx-all",
			"weka",
			"AnalyzeSkeleton",
			"Gray_Morphology",
			"Skeletonize3D"
	};
	
	
	/* 
    The first method to be run when the plugin starts.
	 */
	public void run(String paramString)  {

		try {

			if(checkPlugins()){

				java.awt.EventQueue.invokeLater(new Runnable() {
					public void run() {

						IJ.setBackgroundColor(0, 0, 0);	 // default background is black

						try {
							UIManager.setLookAndFeel(
									UIManager.getSystemLookAndFeelClassName());
						} catch (ClassNotFoundException e) {


							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (InstantiationException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IllegalAccessException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (UnsupportedLookAndFeelException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						MainWindow frame = new MainWindow();
						frame.setVisible(true);
					}
				});

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private boolean checkPlugins(){
		boolean result = true;
		String pluginDirName = IJ.getDirectory("plugins");
		File pluginDir = new File(pluginDirName);
		
		File jarDir = new File(pluginDirName+File.separator+"jars");
		
		boolean[] oklist = new boolean[requiredFiles.length];
		for(boolean ok : oklist){
			ok = false;
		}

		// check the plugins directory and the plugins/jars directory
		for(int i=0; i<requiredFiles.length; i++){
			String fileName = requiredFiles[i];
			for(File file : pluginDir.listFiles()){
				
				if(file.getName().startsWith(fileName)){
					oklist[i] = true;
				}
				
			}
			
			for(File file : jarDir.listFiles()){
				
				if(file.getName().startsWith(fileName)){
					oklist[i] = true;
				}
				
			}
		}
		
		// report missing jars
		for(int i=0; i<requiredFiles.length; i++){
			if(oklist[i]==false){
				IJ.log("Cannot find required plugin: "+requiredFiles[i]);
				result = false;
			}
		}
		
		if(result==false){
			IJ.log("Unable to launch Nuclear Morphology Analysis");
			IJ.log("Check the wiki for download links for missing plugins:");
			IJ.log("https://bitbucket.org/bmskinner/nuclear_morphology/wiki/Installation");
			
		}
		
		return result;
	}
}

