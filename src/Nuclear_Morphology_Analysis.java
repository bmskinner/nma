/*
  -------------------------------------------------
  NUCLEAR MORPHOLOGY IMAGEJ PLUGIN
  -------------------------------------------------
  Copyright (C) Ben Skinner 2015

  This plugin allows for automated detection of FISH
  signals in a mouse sperm nucleus, and measurement of
  the signal position relative to the nuclear centre of
  mass (CoM) and sperm tip. Works with both red and green channels.
  It also generates a profile of the nuclear shape, allowing
  morphology comparisons
 */

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import ij.IJ;
import ij.plugin.PlugIn;
import no.gui.MainWindow;

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

