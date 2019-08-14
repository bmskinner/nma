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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;

import com.bmskinner.nuclear_morphology.gui.main.DockableMainWindow;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.ImagesTabPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.cells_detail.CellsListPanel;
import com.bmskinner.nuclear_morphology.io.Io;

/**
 * Take screenshots of a detail panel and all the sub-panels
 * @author ben
 * @since 1.14.0
 *
 */
public class DetailPanelScreenshotter {
	
	private Robot robot;
	private DockableMainWindow mw;
	
	/**
	 * Create with a main window to control, and a robot to take screenshots
	 * @param mw
	 * @param robot
	 */
	public DetailPanelScreenshotter(DockableMainWindow mw, Robot robot) {
		this.robot = robot;		
		this.mw = mw;
	}
	
	/**
	 * Adjust the crop of images to fit components
	 * @param c the display component to crop
	 * @return
	 */
	private Rectangle makeCroppedBounds(Component c) {
		Point p = c.getLocationOnScreen();
		int topCrop = 0;
		int btmCrop = 0;
		int leftCrop = 0;
		int rightCrop = 0;
		return new Rectangle(p.x+leftCrop, p.y+topCrop, c.getWidth()-(rightCrop+leftCrop), c.getHeight()-(topCrop+btmCrop));
	}
	
	/**
	 * Take screenshots of the given panel
	 * @param panel the panel to image
	 * @param folder the location for saved images
	 * @param title the title for the image
	 * @throws IOException
	 */
	public void takeScreenShots(DetailPanel panel, File folder, String title) throws IOException {
		if(panel instanceof ImagesTabPanel)
			selectImage((ImagesTabPanel) panel);
		takeAnnotatedScreenShot(panel, folder, title);
		takeScreenShot(folder, title);

		for(Component c : panel.getComponents()) {
			takeScreenShots(c, folder, title+"_"+panel.getPanelTitle());
		}
	}
	
	/**
	 * Take screenshots of an AWT component
	 * @param panel the component
	 * @param folder the location for saved images
	 * @param title the title for the image
	 * @throws IOException
	 */
	private void takeScreenShots(Component panel, File folder, String title) throws IOException {
		if(panel instanceof DetailPanel) {
			DetailPanel dp = (DetailPanel)panel;
			if(dp instanceof CellsListPanel)
				selectSingleCell((CellsListPanel) dp);
			
			
			
			takeAnnotatedScreenShot(dp, folder, title+"_"+dp.getPanelTitle());
			takeScreenShots(dp, folder, title+"_"+dp.getPanelTitle());
			for(Component c : dp.getComponents()) {
				takeScreenShots(c, folder, title+"_"+dp.getPanelTitle());
			}
			return;
		}
		
		if(panel instanceof JTabbedPane) {
			JTabbedPane pane = (JTabbedPane)panel;
			for(int i=0; i<pane.getTabCount(); i++) {
				pane.setSelectedIndex(i);
				Component t = pane.getSelectedComponent();
				takeScreenShots(t, folder, title+"_"+i);
			}
			return;
		}
		
		if(panel instanceof JComponent) {
			JComponent jc = (JComponent)panel;
			for(Component c : jc.getComponents()) {
				takeScreenShots(c, folder, title+"_"+jc.getClass().getSimpleName());
			}
		}
	}
	
	/**
	 * Select a single cell in the cells list panel
	 * @param dp
	 */
	private void selectSingleCell(CellsListPanel dp) {
		Point listPos = dp.getLocationOnScreen();
		robot.mouseMove(listPos.x+60, listPos.y+120);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		try {
			Thread.sleep(20);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
	}
	
	/**
	 * Select a single image in the images tab panel
	 * @param dp
	 */
	private void selectImage(ImagesTabPanel dp) {
		Point listPos = dp.getLocationOnScreen();
		robot.mouseMove(listPos.x+90, listPos.y+90);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		try {
			Thread.sleep(20);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		try {
			Thread.sleep(100); // give time for image to load
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Take a screenshot and draw a border around the given panel
	 * @param panel the panel to highlight
	 * @param folder the location for saved images
	 * @param title the title for the image
	 * @throws IOException
	 */
	private void takeAnnotatedScreenShot(DetailPanel panel, File folder, String title) throws IOException {
		Point topLeft = mw.getLocationOnScreen();
		BufferedImage img = robot.createScreenCapture(makeCroppedBounds(mw));
		Graphics2D g2 =img.createGraphics();
		g2.setStroke(new BasicStroke(3));
		g2.setColor(Color.RED);
		Point componentTopLeft = panel.getLocationOnScreen();
		int x = componentTopLeft.x-topLeft.x;
		int y = componentTopLeft.y-topLeft.y;
		g2.drawRect(x, y, panel.getWidth(), panel.getHeight());
		File outputfile = new File(folder, title+"_annotated"+Io.PNG_FILE_EXTENSION);
		if(outputfile.exists())
			return;
		ImageIO.write(img, "png", outputfile);
	}
	
	/**
	 * Take a screenshot of the main window.
	 * @param folder the location for saved images
	 * @param title the title for the image
	 * @throws IOException
	 */
	private void takeScreenShot(File folder, String title) throws IOException {
		BufferedImage img = robot.createScreenCapture(makeCroppedBounds(mw));
		File outputfile = new File(folder, title+Io.PNG_FILE_EXTENSION);
		if(outputfile.exists())
			return;
		ImageIO.write(img, "png", outputfile);
	}

}
