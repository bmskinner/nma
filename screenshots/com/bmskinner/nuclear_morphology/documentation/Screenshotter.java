package com.bmskinner.nuclear_morphology.documentation;

import java.awt.AWTException;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;

import com.bmskinner.nuclear_morphology.core.DatasetListManager;
import com.bmskinner.nuclear_morphology.core.EventHandler;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.gui.DefaultInputSupplier;
import com.bmskinner.nuclear_morphology.gui.events.SignalChangeEvent;
import com.bmskinner.nuclear_morphology.gui.main.DockableMainWindow;
import com.bmskinner.nuclear_morphology.gui.tabs.DatasetSelectionListener.DatasetSelectionEvent;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;
import com.javadocking.dock.TabDock;

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
	private static final String SINGLE_DATASET_FOLDER = "Single";
	private static final String MULTI_DATASET_FOLDER = "Multi";
	
	private static final String SINGLE_DATASET = "test/samples/images/Testing_round/UnitTest_1.14.0/UnitTest_1.14.0.nmd";
	
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
		mw.setExtendedState( mw.getExtendedState()|JFrame.MAXIMIZED_BOTH);
		mw.setVisible(true);

		Thread.sleep(LOAD_TIME_MILLIS);
		takeScreens(rootFolder, NULL_DATASET_FOLDER);

		eh.eventReceived(new SignalChangeEvent(this, SignalChangeEvent.IMPORT_DATASET_PREFIX+SINGLE_DATASET, this.getClass().getSimpleName()));

		Thread.sleep(LOAD_TIME_MILLIS);

		takeScreens(rootFolder, SINGLE_DATASET_FOLDER);
		
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
		Rectangle r = c.getBounds();
		int crop = 9;
		return new Rectangle(r.x+crop, r.y+crop, r.width-(crop*2), r.height-(crop*2));
	}

	private void takeScreens(File rootFolder, String prefix) {
		System.out.println("Imaging "+prefix);
		try {
			
			File outputFolder = new File(rootFolder, prefix+"/");
			outputFolder.mkdirs();
			
			Field tabDock = mw.getClass().getDeclaredField("tabDock");
			tabDock.setAccessible(true);
			TabDock dock = (TabDock) tabDock.get(mw);
			
			int docks = dock.getDockableCount();
			for(int i=0; i<docks; i++) {
				dock.setSelectedDockable(dock.getDockable(i));
				Thread.sleep(SLEEP_TIME_MILLIS);
				takeScreenShot(outputFolder, prefix+"_"+i+"_"+dock.getDockable(i).getTitle());
				
				// Child tabs
				Component c = dock.getDockable(i).getContent();
				if(!(c instanceof DetailPanel)) {
					System.out.println(c.getClass().getName());
					continue;
				}
				
				DetailPanel d = (DetailPanel)c;
				exportDetailPanel(d, outputFolder, prefix+"_"+i+"_"+dock.getDockable(i).getTitle());
			}
			
		} catch (AWTException | IOException | InterruptedException | NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
		}
	}
	
	private void exportDetailPanel(DetailPanel d, File folder, String fileNamePrefix) throws AWTException, IllegalArgumentException, IllegalAccessException, InterruptedException, IOException {
		
		Field subPanelField = null;
		Field[] fields = d.getClass().getDeclaredFields();
		
		for(Field f : fields) {
			f.setAccessible(true);
			
			if(f.get(d) instanceof DetailPanel)
				exportDetailPanel((DetailPanel) f.get(d), folder, fileNamePrefix );
			
			if(f.get(d) instanceof JTabbedPane) 
				subPanelField = f;
		}
		
		if(subPanelField==null)
			return;

		subPanelField.setAccessible(true);
		JTabbedPane subPanel = (JTabbedPane) subPanelField.get(d);
		int tabs = subPanel.getTabCount();
		for(int j=0; j<tabs; j++) {
			subPanel.setSelectedIndex(j);
			Thread.sleep(SLEEP_TIME_MILLIS);
			takeScreenShot(folder, fileNamePrefix+"_"+j+"_"+subPanel.getTitleAt(j));
		}		
	}
	
	private void takeScreenShot(File folder, String title) throws IOException {
		BufferedImage img = robot.createScreenCapture(makeCroppedBounds(mw));
		File outputfile = new File(folder, title+".png");
		ImageIO.write(img, "png", outputfile);
	}

}
