package com.bmskinner.nuclear_morphology.documentation;

import java.awt.AWTException;
import java.awt.Component;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

import javax.imageio.ImageIO;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;

import com.bmskinner.nuclear_morphology.core.EventHandler;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.gui.DefaultInputSupplier;
import com.bmskinner.nuclear_morphology.gui.events.SignalChangeEvent;
import com.bmskinner.nuclear_morphology.gui.main.DockableMainWindow;
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
	
	private static final String SINGLE_DATASET = "test/samples/datasets/Testing_round_1_13_8.nmd";
	
	/**
	 * Launch the screenshotter
	 * @param args
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws InterruptedException {
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
	
	private void run() throws InterruptedException {
		InputSupplier is = new DefaultInputSupplier();
		EventHandler eh = new EventHandler(is);
		mw = new DockableMainWindow(true,eh);
		mw.setVisible(true);
		
		Thread.sleep(1000);
		takeScreens("Blank");
		
		eh.eventReceived(new SignalChangeEvent(this, SignalChangeEvent.IMPORT_DATASET_PREFIX+SINGLE_DATASET, this.getClass().getSimpleName()));
		
		Thread.sleep(5000);
		takeScreens("Single");

		mw.dispose();
	}

	private void takeScreens(String prefix) {

		try {
			Robot r = new Robot();
			BufferedImage img = r.createScreenCapture(mw.getBounds());
			
			File outputfile = new File("screens/");
			outputfile.mkdirs();
			
			Field tabDock = mw.getClass().getDeclaredField("tabDock");
			tabDock.setAccessible(true);
			TabDock dock = (TabDock) tabDock.get(mw);
			
			int docks = dock.getDockableCount();
			for(int i=0; i<docks; i++) {
				dock.setSelectedDockable(dock.getDockable(i));
				Thread.sleep(500);
				img = r.createScreenCapture(mw.getBounds());
				outputfile = new File("screens/"+prefix+"_"+i+"_"+dock.getDockable(i).getTitle()+".png");
				ImageIO.write(img, "png", outputfile);
				
				// Child tabs
				Component c = dock.getDockable(i).getContent();
				if(!(c instanceof DetailPanel)) {
					System.out.println(c.getClass().getName());
					continue;
				}
				
				DetailPanel d = (DetailPanel)c;
				exportDetailPanel(d, prefix+"_"+i+"_"+dock.getDockable(i).getTitle());
						
			}
			
		} catch (AWTException | IOException | InterruptedException | NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
		}
	}
	
	private void exportDetailPanel(DetailPanel d, String fileNamePrefix) throws AWTException, IllegalArgumentException, IllegalAccessException, InterruptedException, IOException {
		Robot r = new Robot();
		Field subPanelField = null;
		Field[] fields = d.getClass().getDeclaredFields();
		
		for(Field f : fields) {
			f.setAccessible(true);
			
			if(f.get(d) instanceof DetailPanel)
				exportDetailPanel((DetailPanel) f.get(d), fileNamePrefix );
			
			if(f.get(d) instanceof JTabbedPane) 
				subPanelField = f;
		}
		
		if(subPanelField==null)
			return;
		System.out.println(d.getClass().getSimpleName());
		subPanelField.setAccessible(true);
		JTabbedPane subPanel = (JTabbedPane) subPanelField.get(d);
		int tabs = subPanel.getTabCount();
		for(int j=0; j<tabs; j++) {
			subPanel.setSelectedIndex(j);
			Thread.sleep(500);
			BufferedImage img = r.createScreenCapture(mw.getBounds());
			File outputfile = new File("screens/"+fileNamePrefix+"_"+j+"_"+subPanel.getTitleAt(j)+".png");
			ImageIO.write(img, "png", outputfile);
		}		
	}

}
