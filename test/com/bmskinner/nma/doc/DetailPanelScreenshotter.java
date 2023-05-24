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
package com.bmskinner.nma.doc;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.JTabbedPane;

import com.bmskinner.nma.gui.main.DockableMainWindow;
import com.bmskinner.nma.gui.tabs.DetailPanel;
import com.bmskinner.nma.io.Io;

/**
 * Take screenshots of a detail panel and all the sub-panels
 * 
 * @author ben
 * @since 1.14.0
 *
 */
public class DetailPanelScreenshotter {

	private static final Logger LOGGER = Logger.getLogger(DetailPanelScreenshotter.class.getName());

	private Robot robot;
	private DockableMainWindow mw;

	/**
	 * Create with a main window to control, and a robot to take screenshots
	 * 
	 * @param mw
	 * @param robot
	 */
	public DetailPanelScreenshotter(DockableMainWindow mw, Robot robot) {
		this.robot = robot;
		this.mw = mw;
	}

	/**
	 * Adjust the crop of images to fit components
	 * 
	 * @param c the display component to crop
	 * @return
	 */
	private Rectangle makeCroppedBounds(Component c) {
		Point p = c.getLocationOnScreen();
		int topCrop = 0;
		int btmCrop = 7;
		int leftCrop = 7;
		int rightCrop = 7;
		return new Rectangle(p.x + leftCrop, p.y + topCrop, c.getWidth() - (rightCrop + leftCrop),
				c.getHeight() - (topCrop + btmCrop));
	}

	/**
	 * Create a rectangle encompassing the given component
	 * 
	 * @param c
	 * @return
	 */
	private Rectangle makeBounds(Component c) {
		Point p = c.getLocationOnScreen();
		return new Rectangle(p.x, p.y, c.getWidth(), c.getHeight());
	}

	/**
	 * Take screenshots of the given panel
	 * 
	 * @param panel  the panel to image
	 * @param folder the location for saved images
	 * @param title  the title for the image
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void takeScreenShots(DetailPanel panel, File folder, String title)
			throws IOException, InterruptedException {

		Thread.sleep(100); // give time for panel to load

		boolean hasTabPane = Arrays.stream(panel.getComponents())
				.anyMatch(c -> c instanceof JTabbedPane);

		if (!hasTabPane) {
			if (panel.getPanelTitle().equals("Images")) {
				clickInPanel(panel, 90, 90);
			}

			if (panel.getPanelTitle().equals("Cells")) {
				clickInPanel(panel, 30, 90);
			}

			if (panel.getPanelTitle().equals("Warping")) {
				clickInPanel(panel, 30, 80);
			}

			takeScreenShot(folder, title + panel.getPanelTitle());

		} else {
			for (Component c : panel.getComponents()) {
				if (c instanceof JTabbedPane pane) {
					for (int i = 0; i < pane.getTabCount(); i++) {
						pane.setSelectedIndex(i);
						Component t = pane.getSelectedComponent();
						if (t instanceof DetailPanel d)
							takeScreenShots(d, folder, panel.getPanelTitle() + "_");
					}
				}
			}
		}

	}

	private void clickInPanel(DetailPanel dp, int x, int y) throws InterruptedException {
		Point listPos = dp.getLocationOnScreen();
		robot.mouseMove(listPos.x + x, listPos.y + y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);

		Thread.sleep(20);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

		Thread.sleep(500); // give time for image to load
	}

	/**
	 * Take a screenshot of the main window.
	 * 
	 * @param folder the location for saved images
	 * @param title  the title for the image
	 * @throws IOException
	 */
	private void takeScreenShot(File folder, String title) throws IOException {

		String platform = System.getProperty("os.name");

		// On Windows, bounds include some background pixels. Crop them out
		Rectangle regionToCapture = null;
		if (platform.startsWith("Windows")) {
			regionToCapture = makeCroppedBounds(mw);
		} else {
			regionToCapture = makeBounds(mw);
		}

		BufferedImage img = robot.createScreenCapture(regionToCapture);
		File outputfile = new File(folder, title + Io.PNG_FILE_EXTENSION);
		ImageIO.write(img, "png", outputfile);
	}

}
