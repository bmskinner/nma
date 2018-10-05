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
package com.bmskinner.nuclear_morphology.documentation;

import java.awt.AWTException;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;

import com.bmskinner.nuclear_morphology.components.generic.Version;
import com.bmskinner.nuclear_morphology.core.EventHandler;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.gui.DefaultInputSupplier;
import com.bmskinner.nuclear_morphology.gui.events.SignalChangeEvent;
import com.bmskinner.nuclear_morphology.gui.main.DockableMainWindow;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.TabPanel;
import com.bmskinner.nuclear_morphology.io.Io;

import ij.IJ;

/**
 * Walk through the UI, taking screenshots of each window
 * for documentation
 * @author ben
 * @since 1.14.0
 *
 */
public class Screenshotter {
	
	private DockableMainWindow mw;
	
	/** Sleep time after switching tabs */
	private static final int SLEEP_TIME_MILLIS = 150;
	
	/** Sleep time after loading a dataset */
	private static final int LOAD_TIME_MILLIS = 5000;
	
	private static final String SCREENSHOT_FOLDER = "screens/";
	private static final String NULL_DATASET_FOLDER = "Blank";
	private static final String SINGLE_ROUND_DATASET_FOLDER = "Single_round";
	private static final String SINGLE_MOUSE_DATASET_FOLDER = "Single_mouse";
	private static final String MULTI_DATASET_FOLDER = "Multi";
	
	private static final String TEST_MOUSE_DATASET = "test/samples/images/Mouse/UnitTest_"+Version.currentVersion()+"/Mouse.nmd";
	private static final String TEST_ROUND_DATASET = "test/samples/images/Round_with_signals/UnitTest_"+Version.currentVersion()+"/Round_with_signals.nmd";
	
	private final Robot robot;
	
	private Screenshotter() throws AWTException {
		robot = new Robot();
	}
	
	/**
	 * Launch the screenshotter
	 * @param args
	 * @throws InterruptedException 
	 * @throws AWTException 
	 */
	public static void main(String[] args) throws InterruptedException, AWTException {
		 IJ.setBackgroundColor(0, 0, 0);  // default background is black
         try {
             UIManager.setLookAndFeel(
                     UIManager.getSystemLookAndFeelClassName());
         } catch (Exception e) {
         	System.err.println("Error setting UI look and feel");
             e.printStackTrace();
         }
		Screenshotter s = new Screenshotter();
		s.run();
	}
	
	private void deleteFiles(File folder) {
		for(File f : folder.listFiles()) {
			if(f.isDirectory())
				deleteFiles(f);
			f.delete();
		}
	}
	
	private void run() throws InterruptedException {
		
		// clear previous runs
		File rootFolder = new File(SCREENSHOT_FOLDER);
		deleteFiles(rootFolder);
		
		InputSupplier is = new DefaultInputSupplier();
		EventHandler eh = new EventHandler(is);
		mw = new DockableMainWindow(true,eh);
		mw.setVisible(true);

		Thread.sleep(LOAD_TIME_MILLIS);
		takeScreens(rootFolder, NULL_DATASET_FOLDER);

		eh.eventReceived(new SignalChangeEvent(this, SignalChangeEvent.IMPORT_DATASET_PREFIX+new File(TEST_ROUND_DATASET).getAbsolutePath(), this.getClass().getSimpleName()));

		Thread.sleep(LOAD_TIME_MILLIS);

		takeScreens(rootFolder, SINGLE_ROUND_DATASET_FOLDER);
		
		eh.eventReceived(new SignalChangeEvent(this, SignalChangeEvent.IMPORT_DATASET_PREFIX+new File(TEST_MOUSE_DATASET).getAbsolutePath(), this.getClass().getSimpleName()));
		Thread.sleep(LOAD_TIME_MILLIS);
		takeScreens(rootFolder, SINGLE_MOUSE_DATASET_FOLDER);
		
		System.out.println("Select multiple datasets");
		for(int i=5; i>=0; i--) {
			System.out.println("Remaining: "+i+"...");
			Thread.sleep(1000);
		}
		
		// TODO - make populations panel repaint properly
//		mw.datasetSelectionEventReceived(new DatasetSelectionEvent(this, DatasetListManager.getInstance().getAllDatasets()));
		takeScreens(rootFolder, MULTI_DATASET_FOLDER);
		
		mw.dispose();
	}
	
	/**
	 * Remove the 10 pixel border from mw.getBounds()
	 * @param c
	 * @return
	 */
	private Rectangle makeCroppedBounds(Component c) {
//		Rectangle r = c.getBounds();
		Point p = c.getLocationOnScreen();
		int topCrop = 0;
		int btmCrop = 0;
		int leftCrop = 0;
		int rightCrop = 0;
		return new Rectangle(p.x+leftCrop, p.y+topCrop, c.getWidth()-(rightCrop+leftCrop), c.getHeight()-(topCrop+btmCrop));
	}

	private void takeScreens(File rootFolder, String prefix) {
		System.out.println("Imaging "+prefix);
		try {
			
			File outputFolder = new File(rootFolder, prefix+"/");
			outputFolder.mkdirs();
			DetailPanelScreenshotter dps = new DetailPanelScreenshotter(mw, robot);
			
			Field detailPanels = getDeclaredPrivateField(mw.getClass(),"detailPanels");
			List<TabPanel> panels = (List<TabPanel>) detailPanels.get(mw);
			for(TabPanel p : panels) {
				if(((Component)p).isShowing()) {
					Thread.sleep(SLEEP_TIME_MILLIS);
					dps.takeScreenShots((DetailPanel)p, outputFolder, prefix+"_"+p.getClass().getSimpleName()+"_annotated");
				}
			}
			
			
			
			TabPanelSwitcher s = new TabPanelSwitcher(mw);
			while(s.hasNext()) {
				DetailPanel d = s.nextTab();
				Thread.sleep(SLEEP_TIME_MILLIS);
				dps.takeScreenShots(d, outputFolder, prefix+"_"+d.getPanelTitle());				
			}

		} catch (IOException | InterruptedException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
		}
	}
	
	/**
	 * Get all the private fields for the class, including superclass fields
	 * @param type
	 * @return
	 */
	private List<Field> getInheritedPrivateFields(Class<?> type) {
	    List<Field> result = new ArrayList<>();

	    Class<?> i = type;
	    while (i != null && i != Object.class) {
	        Collections.addAll(result, i.getDeclaredFields());
	        i = i.getSuperclass();
	    }
	    return result;
	}
	
	/**
	 * Get all the private fields for the class, including superclass fields
	 * @param type
	 * @return
	 */
	private Field getDeclaredPrivateField(Class<?> type, String name) {
	    List<Field> result = getInheritedPrivateFields(type);

	    for(Field f : result)
	    	if(f.getName().equals(name)) {
	    		f.setAccessible(true);
	    		return f;
	    	}
	    return null;
	}

}
