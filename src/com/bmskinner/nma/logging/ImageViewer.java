package com.bmskinner.nma.logging;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.ImageWindow;
import ij.gui.StackWindow;
import ij.process.ImageProcessor;

/**
 * Allow images to be displayed during testing
 * @author ben
 *
 */
public class ImageViewer {
	
	/**
	 * Display the given image and halt execution until closed.
	 * @param ip
	 * @param title
	 * @throws InterruptedException
	 */
	public static void showImage(ImageProcessor ip, String title) throws InterruptedException {
		ImagePlus img = new ImagePlus(title, ip);
		ImageWindow win = new ImageWindow(img);
		win.setVisible(true);
		while(win.isVisible()) {
			Thread.sleep(1000);
		}
	}
	
	/**
	 * Display the given image stack and halt execution until closed.
	 * @param ip
	 * @param title
	 * @throws InterruptedException
	 */
	public static void showImage(ImageStack st, String title) throws InterruptedException {
		ImagePlus img = new ImagePlus(title, st);
		StackWindow win = new StackWindow(img);
		win.setVisible(true);
		while(win.isVisible()) {
			Thread.sleep(1000);
		}
	}

}
