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
										
					MainWindow frame = new MainWindow();
					frame.setVisible(true);
				}
			});

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		


		//		AnalysisCreator analysisCreator = new AnalysisCreator();
		//		analysisCreator.run();
	}

}

