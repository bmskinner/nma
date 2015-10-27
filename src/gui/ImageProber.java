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

import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import io.ImageExporter;
import io.ImageImporter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import utility.Constants;
import utility.Utils;
import analysis.AnalysisOptions;
import analysis.ImageFilterer;
import analysis.nucleus.NucleusDetector;
import analysis.nucleus.NucleusFinder;
import components.Cell;
import components.nuclei.Nucleus;

@SuppressWarnings("serial")
public class ImageProber extends JDialog {
	
	Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
	
	private static double IMAGE_SCREEN_PROPORTION = 0.25;

	private final JPanel contentPanel = new JPanel();
	private AnalysisOptions options; // the options to detect with
	private File openImage;			// the image currently open

	private Logger programLogger;
//	private JLabel imageLabel;		// the JLabel to hold the image

//	private ImageIcon imageIcon = null;	// the icon with the image, for display in JLabel
	private JLabel headerLabel;		// the header text and loading gif
	
	private ImageIcon loadingGif = null; // the icon for the loading gif
	
	private Map<ImageType, JLabel> iconMap = new HashMap<ImageType, JLabel>(); // allow multiple images 
	private Map<ImageType, ImageProcessor> procMap = new HashMap<ImageType, ImageProcessor>(); // allow multiple images 
	
	private boolean ok = false;
	
	private List<File> probableFiles;	// the list of image files
	private int index = 0; 				// the index of the open file
	
	private static final String[] logTestMessages = {
		"Constructing additional pylons",
		"Detecting strategic launch",
		"Staying a while, and listening",
		"Bullseyeing womp rats",
		"Avoiding a land war in Asia",
		"Baking a delicious cake",
		"Deciding who you're gonna call",
		"Loving it when a plan comes together",
		"Wondering why it has to be snakes",
		"Generating 1.21 gigawatts",
		"Not simply walking into Mordor",
		"Taking the hobbits to Isengard",
		"Searching out there for the truth",
		"Feeding them after midnight",
		"Learning the princess is in another castle",
		"Getting your ass to Mars",
		"Changing the laws of physics",
		"Likely to be eaten by a grue",
		"Never giving up, never surrendering",
		"Making everything shiny",
		"Crossing the streams",
		"Reversing the polarity of the neutron flow",
		"Phoning home",
		"Requiring a bigger boat",
		"Shaking, not stirring",
		"Reaching 88 miles per hour",
		"Considering it only a flesh wound",
		"Activating the machine that goes ping",
		"Weighing the same as a duck",
		"Noting that winter is coming"
	};
 
	private enum ImageType {
		KUWAHARA ("Kuwahara"),
		FLATTENED ("Flattened"),
		EDGE_DETECTION ("Edges"),
		MORPHOLOGY_CLOSED ("Closed"),
		DETECTED_OBJECTS ("Detected"),
		REJECTED_OBJECTS ("Rejected");
		
		private String name;
		
		ImageType(String name){
			this.name = name;
		}
		public String toString(){
			return this.name;
		}
	}
	
	/**
	 * Create the dialog.
	 */
	public ImageProber(AnalysisOptions options, Logger logger) {

		if(options==null){
			throw new IllegalArgumentException("Options is null");
		} 

		try{
			this.setModal(true);
			this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
			this.options = options;
			this.programLogger = logger;
			//		this.logger = new Logger(logFile, "ImageProber");
			this.setTitle("Image Prober");

			int w = (int) (screenSize.getWidth() * 0.75);
			int h = (int) (screenSize.getHeight() * 0.75);

			setBounds(100, 100, w, h);
			this.setLocationRelativeTo(null); // centre on screen

			// Load the gif (may be in a res folder depending on Eclipse version)
			String pathToGif = "res/ajax-loader.gif";	
			boolean ok = loadResources(pathToGif);
			if(!ok){
				pathToGif = "ajax-loader.gif";	
				ok = loadResources(pathToGif);
			}
			if(!ok){
				programLogger.log(Level.WARNING, "Resource loading failed (gif): "+pathToGif);
			}

			getContentPane().setLayout(new BorderLayout());
			contentPanel.setLayout(new BorderLayout());
			contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

			for(ImageType key : ImageType.values()){
				iconMap.put(key, null);
				procMap.put(key, null);
			}


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
		} catch(Exception e){
			logger.log(Level.SEVERE, "Error creating prober", e);
		}
	}

	/**
	 * Fetch the gif loading resources
	 * 
	 */
	private boolean loadResources(String pathToGif){
		boolean ok = false;
		try{
			
			// Get current classloader
			ClassLoader cl = this.getClass().getClassLoader();
			URL urlToGif = cl.getResource(pathToGif);
			
			if(urlToGif!=null){
				loadingGif = new ImageIcon(urlToGif);

				if(loadingGif==null){
					programLogger.log(new LogRecord(Level.WARNING, "Unable to load gif"));

				} else {
					ok = true;
				}

			} 
			
		} catch (Exception e){
			programLogger.log(new LogRecord(Level.WARNING, "Cannot load gif resource: "+e.getMessage()));
		}
		return ok;
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
		
		panel.add(new JLabel("Objects meeting nucleus parameters are outlined in yellow. Click an image to view larger version."), BorderLayout.NORTH);

		panel.add(headerLabel, BorderLayout.SOUTH);

		return panel;
	}
	


	/**
	 * Make the image panel 
	 * @return
	 */
	private JPanel createImagePanel(){
		JPanel panel = new JPanel();
//		panel.setLayout(new BorderLayout());
		panel.setLayout(new GridLayout(3, 2));

		for(final ImageType key : ImageType.values()){

			JLabel label = new JLabel("", loadingGif, JLabel.CENTER);
			label.setText(key.toString());
			label.setHorizontalTextPosition(JLabel.CENTER);
			label.setVerticalTextPosition(JLabel.TOP);
			panel.add(label);
			iconMap.put(key, label);
			
			label.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent arg0) {

					Thread thr = new Thread(){
						public void run() {
							showLargeImage(key);
						}
					};	
					thr.start();

				}
			});
		}


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
	
	
	private void showLargeImage(ImageType key){
		final ImageIcon icon = createViewableImage(procMap.get(key), true);
		JOptionPane pane = new JOptionPane(null, JOptionPane.INFORMATION_MESSAGE, JOptionPane.OK_OPTION, icon);
				
//				JOptionPane.showMessageDialog(null, null, key, JOptionPane.INFORMATION_MESSAGE, icon);
        
        Dialog dialog = pane.createDialog(this, key.toString());
        // the line below is added to the example from the docs
        dialog.setModal(false); // this says not to block background components
        dialog.setVisible(true);
//        dialog.show();
        
//        JDialog dialog = new JDialog(){
//        	
//        	
//        	
//        };
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

//				imageLabel.setIcon(loadingGif);
//				imageLabel.repaint();
				for(ImageType key : ImageType.values()){
					
					JLabel label = iconMap.get(key);
					label.setIcon(loadingGif);
				}
			}
			
//			logger.log("Importing file: "+imageFile.getAbsolutePath(), Logger.DEBUG);
			ImageStack imageStack = ImageImporter.importImage(imageFile, programLogger);
			
			
			/*
			 * TODO
			 * Insert steps to show each applied filter in the same order as from analysis
			 * Kuwahara filtering
			 * Chromocentre flattening
			 * Edge detector
			 *    Morphology closing
			 * Final image
			 * 
			 * Make an icon from each
			 */
			
			ImageProcessor kuwaharaProcessor = ImageFilterer.runKuwaharaFiltering(imageStack, Constants.COUNTERSTAIN, options.getCannyOptions("nucleus").getKuwaharaKernel());
			procMap.put(ImageType.KUWAHARA, kuwaharaProcessor);
			
			ImageProcessor flattenProcessor = ImageFilterer.squashChromocentres(imageStack, Constants.COUNTERSTAIN, options.getCannyOptions("nucleus").getFlattenThreshold());
			procMap.put(ImageType.FLATTENED, flattenProcessor);
			
			ImageProcessor openProcessor = ImageExporter.convert(imageStack).getProcessor();
			procMap.put(ImageType.DETECTED_OBJECTS, openProcessor);
			
			ImageProcessor edgesProcessor = ImageFilterer.runEdgeDetector(flattenProcessor, options.getCannyOptions("nucleus"));
			procMap.put(ImageType.EDGE_DETECTION, edgesProcessor);
			
			ImageProcessor closedProcessor = ImageFilterer.morphologyClose(edgesProcessor, options.getCannyOptions("nucleus").getClosingObjectRadius());
			procMap.put(ImageType.MORPHOLOGY_CLOSED, closedProcessor);
			
//			programLogger.log(Level.INFO, "Searching image...");
//			testLog();
			List<Cell> cells = NucleusFinder.getCells(imageStack, 
					options, 
					programLogger, 
					imageFile, 
					null);
		
			for(Cell cell : cells){

				drawNucleus(cell, openProcessor);
			}
			
			
			programLogger.log(Level.INFO, "Displaying nuclei");
			
			// update the map of icons
			for(ImageType key : ImageType.values()){
				
				JLabel label = iconMap.get(key);
				ImageIcon icon = null;
				if(label.getIcon()!=null){
					icon = (ImageIcon) label.getIcon();
					icon.getImage().flush();
				}
				icon = createViewableImage(procMap.get(key), false);
				label.setIcon(icon);
				label.revalidate();
				label.repaint();
			}

//			if(imageIcon!=null){
//				imageIcon.getImage().flush();
//			}
			
//			imageIcon = createViewableImage(openProcessor);

//			programLogger.log(Level.INFO, "Created icon");
//			imageLabel.setIcon(imageIcon);
//			imageLabel.revalidate();
//			imageLabel.repaint();
//			programLogger.log(Level.INFO, "Repainted label");

			headerLabel.setText("Showing "+cells.size()+" nuclei in "+imageFile.getAbsolutePath());
			headerLabel.setIcon(null);
			headerLabel.repaint();

		} catch (Exception e) { // end try
			programLogger.log(Level.SEVERE, "Error in image processing", e);
		} // end catch

	}
	
	/**
	 * Draw the outline of a nucleus on the given processor
	 * @param cell
	 * @param ip
	 */
	private void drawNucleus(Cell cell, ImageProcessor ip){
		Nucleus n = cell.getNucleus();
		// annotate the image processor with the nucleus outline
		
		if(checkNucleus(n)){
			ip.setColor(Color.YELLOW);
		} else {
			ip.setColor(Color.RED);
		}
		
		
		double[] positions = n.getPosition();
		FloatPolygon polygon = Utils.createPolygon(n.getBorderList());
		PolygonRoi roi = new PolygonRoi(polygon, PolygonRoi.POLYGON);
		roi.setLocation(positions[Nucleus.X_BASE], positions[Nucleus.Y_BASE]);
		ip.setLineWidth(2);
		ip.draw(roi);
	}
	
	private void testLog(){
		double rnd = Math.random() * logTestMessages.length;
		int index = (int) Math.floor(rnd);
		programLogger.log(Level.INFO, logTestMessages[index]+"...");
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
	private ImageIcon createViewableImage(ImageProcessor ip, boolean fullSize){
		int originalWidth = ip.getWidth();
		int originalHeight = ip.getHeight();

		// set the image width to be less than half the screen width
		int smallWidth = 0;
		if(fullSize){
			smallWidth = (int) ((double) screenSize.getWidth() * 0.75);
		} else {
			smallWidth = (int) ((double) screenSize.getWidth() * IMAGE_SCREEN_PROPORTION);
		}

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
