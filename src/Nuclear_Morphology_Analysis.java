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

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import gui.MainWindow;
import ij.IJ;
import ij.plugin.PlugIn;

public class Nuclear_Morphology_Analysis
implements PlugIn

{
	

	
	
	/* 
    The first method to be run when the plugin starts.
	 */
	public void run(String paramString)  {

		try {
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

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

