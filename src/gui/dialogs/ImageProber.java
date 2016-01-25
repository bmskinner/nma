/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
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
package gui.dialogs;

import gui.ImageType;
import gui.LoadingIconDialog;
import ij.process.ImageProcessor;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.File;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import analysis.AnalysisOptions;
import analysis.nucleus.NucleusDetector;

@SuppressWarnings("serial")
public abstract class ImageProber extends LoadingIconDialog {
	
	Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
	
	private double windowWidth;
	private double windowHeight;
	
	private static double IMAGE_SCREEN_PROPORTION = 0.90;

	private final JPanel contentPanel = new JPanel();
	protected AnalysisOptions options; // the options to detect with
	protected File openImage;			// the image currently open
	
	protected Map<ImageType, JLabel> iconMap = new HashMap<ImageType, JLabel>(); // allow multiple images 
	protected Map<ImageType, ImageProcessor> procMap = new HashMap<ImageType, ImageProcessor>(); // allow multiple images 
	
	private ImageType imageType;
	
	private int rows = 0;
	private int cols = 2;
	
	private JLabel headerLabel = new JLabel("Objects meeting detection parameters are outlined in yellow; other objects are red. Click an image to view larger version.");
	
	private JButton okButton     = new JButton("Proceed with analysis");
	private JButton cancelButton = new JButton("Revise settings");
	
	private boolean ok = false;
	
	protected List<File> probableFiles;	// the list of image files
	protected int index = 0; 				// the index of the open file
	
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
 	
	/**
	 * Create the dialog.
	 */
	public ImageProber(AnalysisOptions options, Logger logger, ImageType type, File folder) {
		super(logger);
		if(options==null){
			throw new IllegalArgumentException("Options is null");
		} 

		try{
			this.options = options;

			this.imageType = type;
			
			for(ImageType key : imageType.getValues()){
				iconMap.put(key, null);
				procMap.put(key, null);
			}
			
			createGUI();

		} catch(Exception e){
			logger.log(Level.SEVERE, "Error creating prober", e);
		}
	}
	
	private void createGUI(){
		this.setModal(true);
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		this.setTitle("Image Prober");

		int w = (int) (screenSize.getWidth() * IMAGE_SCREEN_PROPORTION);
		windowWidth = w;
		int h = (int) (screenSize.getHeight() * IMAGE_SCREEN_PROPORTION);
		windowHeight = h;

		setBounds(100, 100, w, h);
		this.setLocationRelativeTo(null); // centre on screen
	

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
							programLogger.log(Level.FINEST, "Selecting next image");
							openImage = getNextImage();

							try{
								programLogger.log(Level.FINEST, "Opening image");
								importAndDisplayImage(openImage);
							} catch(Exception e){
								programLogger.log(Level.SEVERE, "Error opening image, skipping");
							}
							
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
							programLogger.log(Level.FINEST, "Selecting previous image");
							openImage = getPrevImage();
//							setStatusLoading();
							try{
								programLogger.log(Level.FINEST, "Opening image");
								importAndDisplayImage(openImage);
							} catch(Exception e){
								programLogger.log(Level.SEVERE, "Error opening image, skipping");
//								openImage = getNextImage();
//								importAndDisplayImage(openImage);
							}
							
//							programLogger.log(Level.FINEST, "Opening image");
//							importAndDisplayImage(openImage);
						}
					};	
					thr.start();

				}
			});
			contentPanel.add(prevButton, BorderLayout.WEST);
		}
	}
	
	/**
	 * Make the header panel with status label
	 * @return
	 */
	private JPanel createHeader(){
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());

		this.setLoadingLabelText("Examining input folders...");
		this.setStatusLoading();
		
		panel.add(headerLabel, BorderLayout.NORTH);

		panel.add(this.getLoadingLabel(), BorderLayout.SOUTH);

		return panel;
	}
	
	/**
	 * Update the heading text
	 * @param s
	 */
	protected void setHeaderText(String s){
		this.headerLabel.setText(s);
	}
	
	/**
	 * Make the image panel 
	 * @return
	 */
	private JPanel createImagePanel(){
		JPanel panel = new JPanel();
		
		if(imageType.getValues().length == 1){
			rows = 1;
			cols = 1;
		} else {
			rows = (int) Math.ceil(  ( (double) imageType.getValues().length / 2d)  );
			cols = 2;
		}

		programLogger.log(Level.FINEST, "Creating image panel size "+rows+" by "+cols);
		panel.setLayout(new GridLayout(rows, cols ));

		for(final ImageType key : imageType.getValues()){

			JLabel label = new JLabel("", this.getLoadingGif(), JLabel.CENTER);
			label.setText(key.toString());
			label.setHorizontalTextPosition(JLabel.CENTER);
			label.setVerticalTextPosition(JLabel.TOP);
			ImageIcon icon = (ImageIcon) label.getIcon();
			icon.getImage().flush();
			label.setIcon(this.getLoadingGif());
			
			panel.add(label);
			label.repaint();
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

		okButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				ImageProber.this.ok = true;
				programLogger.log(Level.FINEST, "Set ok to "+ok);
				ImageProber.this.setVisible(false);

			}
		});
		panel.add(okButton);

		getRootPane().setDefaultButton(okButton);

		cancelButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {

				ImageProber.this.ok = false;
				programLogger.log(Level.FINEST, "Set ok to "+ok);
				ImageProber.this.setVisible(false);

			}
		});
		panel.add(cancelButton);


		return panel;
	}
	
	/**
	 * Change the button text on the OK button
	 * @param s
	 */
	protected void setOKButtonText(String s){
		okButton.setText(s);
	}
	
	/**
	 * Change the button text on the cancel button
	 * @param s
	 */
	protected void setCancelButtonText(String s){
		cancelButton.setText(s);
	}

	
	/**
	 * Get if the downstream analysis is ok to run,
	 * or if the dialog has been cancelled
	 * @return
	 */
	public boolean getOK(){
		return this.ok;
	}

	/**
	 * Get the next image in the file list
	 * @return
	 */
	private File getNextImage(){

		if(index >= probableFiles.size()-1){
			index = 0;
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
			index = probableFiles.size()-1;
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
	protected void createFileList(final File folder){
		
		programLogger.log(Level.FINEST, "Generating file list");
//		setStatusLoading();
		
		Thread thr = new Thread(){
			public void run() {
				
				probableFiles = new ArrayList<File>();
				probableFiles = importImages(folder);
				
				if(probableFiles.size()>0){
					openImage = probableFiles.get(index);
					importAndDisplayImage(openImage);
				} else {
					programLogger.log(Level.WARNING, "No images found in folder");
					setStatusError();
				}
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
	 * Display the image for the given key in a new non-modal window
	 * @param key
	 */
	private void showLargeImage(ImageType key){
		final ImageIcon icon = createViewableImage(procMap.get(key), true);
		JOptionPane pane = new JOptionPane(null, JOptionPane.INFORMATION_MESSAGE, JOptionPane.OK_OPTION, icon);
        
		double scale = (double) icon.getIconHeight() / (double) procMap.get(key).getHeight();
		scale *=100;
		DecimalFormat df = new DecimalFormat("#0.00"); 
		
        Dialog dialog = pane.createDialog(this, key.toString()+": "+ df.format(scale) +"% scale");

        dialog.setModal(false);
        dialog.setVisible(true);
	}
	
	
	/**
	 * Set the header label and the image icons to display
	 * the loading gif
	 */
	@Override
	protected void setStatusLoading(){
		super.setStatusLoading();
		if(this.getLoadingGif()!=null){
			for(ImageType key : imageType.getValues()){
				
				JLabel label = iconMap.get(key);
				if(label!=null){
					ImageIcon icon = (ImageIcon) label.getIcon();
					if(icon.getImage()!=null){
						icon.getImage().flush();
					}
					label.setIcon(this.getLoadingGif());
				}
			}
		}
	}
	
	/**
	 * Set the header label and the image icons to display
	 * the loading gif
	 */
	protected void setStatusError(){
		
//		headerLabel.setIcon(null);
//		headerLabel.repaint();
		super.setStatusLoaded();

		for(ImageType key : imageType.getValues()){

			JLabel label = iconMap.get(key);
			ImageIcon icon = (ImageIcon) label.getIcon();
			icon.getImage().flush();
			label.setIcon(null);
			label.setText("Error reading image");
		}
		
	}
	
	/**
	 * Import the given file as an image, detect objects and
	 * display the image with annotated  outlines
	 * @param imageFile
	 */
	protected void importAndDisplayImage(File imageFile){
		programLogger.log(Level.FINEST, "Calling abstract class import method");
	}
	
	protected void updateImageThumbnails(){
		// update the map of icons
		for(ImageType key : imageType.getValues()){

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
			System.gc(); // try to clean up the unused thumbnails
		}
	}

	protected void testLog(){
		double rnd = Math.random() * logTestMessages.length;
		int index = (int) Math.floor(rnd);
		programLogger.log(Level.INFO, logTestMessages[index]+"...");
	}
	
	/**
	 * Given a location in the image icon, find the location in the original 
	 * full size image
	 * @param image
	 * @param iconLocation
	 * @return the converted point with int precision
	 */
	protected Point convertIconLocationToOriginalImage(ImageType image, Point iconLocation){
		
		// The original image
		ImageProcessor ip = procMap.get(image);
				
		int originalWidth = ip.getWidth();
		int labelWidth    = iconMap.get(image).getWidth();
		int iconWidth     = iconMap.get(image).getIcon().getIconWidth();
		int labelHeight   = iconMap.get(image).getHeight();
		int iconHeight    = iconMap.get(image).getIcon().getIconHeight();
		
		// Get the conversion ratio
		// Divide an icon value by this to get an original value
		double conversion = (double) iconWidth / (double) originalWidth;

		// Get the position on the label
		double labelX = iconLocation.getX();
		double labelY = iconLocation.getY();
		
		// convert to positions on the image icon
		double iconX = labelX - (  (labelWidth  - iconWidth ) / 2 );
		double iconY = labelY - (  (labelHeight - iconHeight) / 2 );
		
		int originalX =  (int) ( iconX / conversion) ;
		int originalY =  (int) ( iconY / conversion) ;
		
		return new Point(  originalX,  originalY  );
	}
	
	/**
	 * Calculate the aspect ratio of an image
	 * @param width
	 * @param height
	 * @return
	 */
	protected double getAspectRatio(int width, int height){
		return (double) width / (double) height;
	}
	
	/**
	 * Calculate a new height based on a new width, preserving the aspect ratio
	 * @param ip
	 * @param newHeight
	 * @return
	 */
	protected int getNewHeight(ImageProcessor ip, int newWidth){
		return (int) (newWidth / getAspectRatio(ip.getWidth(), ip.getHeight()));
	}
	
	/**
	 * Calculate a new width based on a new height, preserving the aspect ratio
	 * @param ip
	 * @param newHeight
	 * @return
	 */
	protected int getNewWidth(ImageProcessor ip, int newHeight){
		return (int) (newHeight * getAspectRatio(ip.getWidth(), ip.getHeight()));
	}

	/**
	 * Rezize the given image processor to fit in the screen,
	 * and make an icon
	 * @param ip an image processor
	 * @return an image icon with the resized image
	 */
	protected ImageIcon createViewableImage(ImageProcessor ip, boolean fullSize){
		
		programLogger.log(Level.FINEST, "Display has "+rows+" rows");
		programLogger.log(Level.FINEST, "Display has "+cols+" columns");
		
		if(ip==null){
			return new ImageIcon(); // blank image
		}
		
		ImageIcon smallImageIcon = null;
		
		// set the image width to be less than half the screen width
		int smallWidth = 0;
		if(fullSize){
			smallWidth = (int) ((double) screenSize.getWidth() * IMAGE_SCREEN_PROPORTION);
		} else {
			smallWidth = (int) ((double) windowWidth / (cols+1));
		}
		
		// keep the image aspect ratio
		int smallHeight = getNewHeight(ip, smallWidth);
		
		if(!fullSize){
			if(smallHeight > windowHeight / (rows+1) ){ // image is too high, adjust to scale on height
				smallHeight = (int) (windowHeight / (rows+1));
				smallWidth = getNewWidth(ip, smallHeight);
			}
		} else { // full size image must still be scaled to fit
			if(smallHeight > screenSize.getHeight() * IMAGE_SCREEN_PROPORTION){ // image is too high, adjust to scale on height
				smallHeight = (int) (windowHeight * IMAGE_SCREEN_PROPORTION);
				smallWidth = getNewWidth(ip, smallHeight);
			}
		}
		
		
		
		// Create the image
		
		

		if(ip.getWidth()>smallWidth || ip.getHeight() > smallHeight){
			
			smallImageIcon = new ImageIcon(ip.resize(smallWidth, smallHeight ).getBufferedImage());
			
		} else {
			
			smallImageIcon = new ImageIcon( ip.getBufferedImage()  );
		}
		return smallImageIcon;
	}

}
