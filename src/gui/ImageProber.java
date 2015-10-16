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
package gui;

import ij.IJ;
import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import io.ImageExporter;
import io.ImageImporter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import utility.Logger;
import utility.Utils;
import analysis.AnalysisOptions;
import analysis.nucleus.NucleusDetector;
import analysis.nucleus.NucleusFinder;

import components.Cell;
import components.nuclei.Nucleus;

@SuppressWarnings("serial")
public class ImageProber extends JDialog {
	
	Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();

	private final JPanel contentPanel = new JPanel();
	private AnalysisOptions options; // the options to detect with
	private File openImage;			// the image currently open
//	private Logger logger;
	private java.util.logging.Logger programLogger;
	private JLabel imageLabel;		// the JLabel to hold the image

	private ImageIcon imageIcon = null;	// the icon with the image, for display in JLabel
	private JLabel headerLabel;		// the header text and loading gif
	
	private ImageIcon loadingGif = null; // the icon for the loading gif
	
	private boolean ok = false;
	
	private List<File> probableFiles;	// the list of image files
	private int index = 0; 				// the index of the open file

	/**
	 * Create the dialog.
	 */
	public ImageProber(AnalysisOptions options, java.util.logging.Logger logger) {
		
		if(options==null){
			throw new IllegalArgumentException("Options is null");
		} 
		this.setModal(true);
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		this.options = options;
		this.programLogger = logger;
//		this.logger = new Logger(logFile, "ImageProber");
		this.setTitle("Image Prober");
		
		int w = (int) (screenSize.getWidth() * 0.75);
		int h = (int) (screenSize.getHeight() * 0.75);
		
		setBounds(100, 100, w, h);
		
		try{
			String pathToGif = "res/ajax-loader.gif";	
			
			// Get current classloader
			ClassLoader cl = this.getClass().getClassLoader();
			URL urlToGif = cl.getResource(pathToGif);
//			URL urlToGif = this.getClass().getResource(pathToGif);
			
			if(urlToGif!=null){
				loadingGif = new ImageIcon(urlToGif);

				if(loadingGif==null){
//					logger.log(new LogRecord(Level.INFO, "Looking for: "+urlToGif.getFile()));
					logger.log(new LogRecord(Level.WARNING, "Unable to load gif"));
					//				IJ.log("Looking for: "+urlToGif.getFile());
					//				IJ.log("Unable to load gif");
				} else {
				}

			}
			
		} catch (Exception e){
			logger.log(new LogRecord(Level.WARNING, "Cannot load gif resource: "+e.getMessage()));
//			IJ.log("Cannot load gif resource: "+e.getMessage());
//			for(StackTraceElement e1 : e.getStackTrace()){
//				IJ.log(e1.toString());
//			}
		}

		getContentPane().setLayout(new BorderLayout());
		contentPanel.setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		
		
		JPanel header = this.createHeader();
		contentPanel.add(header, BorderLayout.NORTH);
		
		JPanel footer = this.createFooter();
		contentPanel.add(footer, BorderLayout.SOUTH);
		
		JPanel imagePane = createImagePanel();
		contentPanel.add(imagePane, BorderLayout.CENTER);
		
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		
		{
			JButton nextButton = new JButton("Next");
			nextButton.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent arg0) {

					Thread thr = new Thread(){
						public void run() {
							openImage = getNextImage();
							importAndDisplayImage(openImage);
						}
					};	
					thr.start();

				}
			});
			contentPanel.add(nextButton, BorderLayout.EAST);
			
			JButton prevButton = new JButton("Prev");
			prevButton.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent arg0) {

					Thread thr = new Thread(){
						public void run() {
							openImage = getPrevImage();
							importAndDisplayImage(openImage);
						}
					};	
					thr.start();

				}
			});
			contentPanel.add(prevButton, BorderLayout.WEST);
		}

		createFileList(options.getFolder());

		this.setVisible(true);
	}
	
	/**
	 * Make the header panel with status label
	 * @return
	 */
	private JPanel createHeader(){
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());

		headerLabel = new JLabel("Examining input folders...");
		headerLabel.setIcon(loadingGif);
		
		panel.add(new JLabel("Objects meeting nucleus parameters are outlined in yellow"), BorderLayout.NORTH);

		panel.add(headerLabel, BorderLayout.SOUTH);

		return panel;
	}
	


	/**
	 * Make the image panel 
	 * @return
	 */
	private JPanel createImagePanel(){
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());

		imageIcon = loadingGif;
		imageLabel = new JLabel("", imageIcon, JLabel.CENTER);
		panel.add(imageLabel, BorderLayout.CENTER);

		return panel;
	}


	/**
	 * Make the footer panel, with ok and cancel buttons
	 * @return
	 */
	private JPanel createFooter(){
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.RIGHT));

		JButton okButton = new JButton("Proceed with analysis");
		okButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				ImageProber.this.ok = true;
				ImageProber.this.setVisible(false);

			}
		});
		panel.add(okButton);

		getRootPane().setDefaultButton(okButton);

		JButton cancelButton = new JButton("Revise settings");
		cancelButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {

				ImageProber.this.ok = false;
				ImageProber.this.setVisible(false);

			}
		});
		panel.add(cancelButton);


		return panel;
	}

	
	public boolean getOK(){
		return this.ok;
	}

	/**
	 * Get the next image in the file list
	 * @return
	 */
	private File getNextImage(){

		if(index >= probableFiles.size()-1){
			index = probableFiles.size()-1;
		} else {
			index++;
		}
		File f =  probableFiles.get(index);
		return f;

	}
	
	/**
	 * Get the previous image in the file list
	 * @return
	 */
	private File getPrevImage(){
		
		if(index <= 0){
			index = 0;
		} else {
			index--;
		}

		File f =  probableFiles.get(index);
		return f;
	}
	
	
	/**
	 * Create a list of image files in the given folder
	 * @param folder
	 */
	private void createFileList(final File folder){
		
		probableFiles = new ArrayList<File>();
		
		Thread thr = new Thread(){
			public void run() {
				probableFiles = importImages(folder);
				openImage = probableFiles.get(index);
				
				importAndDisplayImage(openImage);
			}
		};	
		thr.start();
		
	}
		
	/**
	 * Check each file in the given folder for suitability
	 * If the folder contains folders, check recursively
	 * @param folder the folder to check
	 * @return a list of image files
	 */
	private List<File> importImages(File folder){

		List<File> files = new ArrayList<File>();

		for (File file :  folder.listFiles()) {

			boolean ok = NucleusDetector.checkFile(file); // check file extension

			if(ok){
				files.add(file);
			}
			
			if(file.isDirectory()){
				files.addAll(importImages(file));
			}
		}
		return files;
	}
	
	
	/**
	 * Import the given file as an image, detect nuclei and
	 * display the image with annotated nuclear outlines
	 * @param imageFile
	 */
	private void importAndDisplayImage(File imageFile){

		try {
//		    openImage = imageFile;
//			IJ.log("Displaying index "+index);
//			IJ.log("Displaying file "+imageFile.getAbsolutePath());
			headerLabel.setText("Probing image "+index+": "+imageFile.getAbsolutePath()+"...");
			if(loadingGif!=null){
				headerLabel.setIcon(loadingGif);
				headerLabel.repaint();

				imageLabel.setIcon(loadingGif);
				imageLabel.repaint();
			}
			
//			logger.log("Importing file: "+imageFile.getAbsolutePath(), Logger.DEBUG);
			ImageStack imageStack = ImageImporter.importImage(imageFile, programLogger);
			
			
			ImageProcessor openProcessor = ImageExporter.convert(imageStack).getProcessor();

			
//			programLogger.log(Level.INFO, "Searching image...");

			List<Cell> cells = NucleusFinder.getCells(imageStack, 
					options, 
					programLogger, 
					imageFile, 
					null);
		
			for(Cell cell : cells){

				Nucleus n = cell.getNucleus();
				// annotate the image processor with the nucleus outline
				
				if(checkNucleus(n)){
					openProcessor.setColor(Color.YELLOW);
				} else {
					openProcessor.setColor(Color.RED);
				}
				
				
				double[] positions = n.getPosition();
				FloatPolygon polygon = Utils.createPolygon(n.getBorderList());
				PolygonRoi roi = new PolygonRoi(polygon, PolygonRoi.POLYGON);
				roi.setLocation(positions[Nucleus.X_BASE], positions[Nucleus.Y_BASE]);
				openProcessor.setLineWidth(2);
				openProcessor.draw(roi);
			}
			
			programLogger.log(Level.INFO, "Displaying nuclei");

			if(imageIcon!=null){
				imageIcon.getImage().flush();
			}
			imageIcon = createViewableImage(openProcessor);

//			programLogger.log(Level.INFO, "Created icon");
			imageLabel.setIcon(imageIcon);
			imageLabel.revalidate();
			imageLabel.repaint();
//			programLogger.log(Level.INFO, "Repainted label");

			headerLabel.setText("Showing "+cells.size()+" nuclei in "+imageFile.getAbsolutePath());
			headerLabel.setIcon(null);
			
//			logger.log("New image loaded", Logger.DEBUG);
//			logger.log(new LogRecord(Level.INFO, "Looking for: "+urlToGif.getFile()));


		} catch (Exception e) { // end try
			programLogger.log(new LogRecord(Level.SEVERE, "Error in image processing"));
		} // end catch

	}
	
	/**
	 * Check the given nucleus size and circ parameters against options
	 * @param n the nucleus to check
	 * @return boolean ok
	 */
	private boolean checkNucleus(Nucleus n){
		boolean result = true;
		
		if(n.getArea() < options.getMinNucleusSize()){
			
			result = false;
		}
		
		if(n.getArea() > options.getMaxNucleusSize()){
			
			result = false;
		}
		
		if(n.getCircularity() < options.getMinNucleusCirc()){
			
			result = false;
		}
		
		if(n.getCircularity() > options.getMaxNucleusCirc()){
			
			result = false;
		}
		
		return result;
	}
	
	
	/**
	 * Rezize the given image processor to fit in the screen,
	 * and make an icon
	 * @param ip an image processor
	 * @return an image icon with the resized image
	 */
	private ImageIcon createViewableImage(ImageProcessor ip){
		int originalWidth = ip.getWidth();
		int originalHeight = ip.getHeight();

		// set the image width to be less than half the screen width
		int smallWidth = (int) ((double) screenSize.getWidth() * 0.65);

		// keep the image aspect ratio
		double ratio = (double) originalWidth / (double) originalHeight;
		int smallHeight = (int) (smallWidth / ratio);
		
		ImageIcon smallImageIcon;

		if(ip.getWidth()>smallWidth){
			
			smallImageIcon = new ImageIcon(ip.resize(smallWidth, smallHeight ).getBufferedImage());
			
		} else {
			
			smallImageIcon = new ImageIcon( ip.getBufferedImage()  );
		}
		return smallImageIcon;
	}

}
