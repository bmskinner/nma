package com.bmskinner.nuclear_morphology.charting;

import ij.ImagePlus;
import ij.gui.ImageWindow;
import ij.process.ImageProcessor;

public class ImageViewer {
	
	public static void showImage(ImageProcessor ip, String title) throws InterruptedException {
		ImagePlus img = new ImagePlus(title, ip);
		ImageWindow win = new ImageWindow(img);
		win.setVisible(true);
		while(win.isVisible()) {
			Thread.sleep(1000);
		}
	}

}
