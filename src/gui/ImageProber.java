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

import ij.process.ImageProcessor;

import java.awt.BorderLayout;
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
import java.util.logging.Logger;

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
public abstract class ImageProber extends JDialog {
	
	Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
	
	private double windowWidth;
	private double windowHeight;
	
	private static double IMAGE_SCREEN_PROPORTION = 0.75;

	private final JPanel contentPanel = new JPanel();
	protected AnalysisOptions options; // the options to detect with
	protected File openImage;			// the image currently open

	protected Logger programLogger;

	protected JLabel headerLabel = new JLabel("Examining input folders...");	// the header text and loading gif
	
	protected ImageIcon loadingGif = null; // the icon for the loading gif
	
	protected Map<ImageType, JLabel> iconMap = new HashMap<ImageType, JLabel>(); // allow multiple images 
	protected Map<ImageType, ImageProcessor> procMap = new HashMap<ImageType, ImageProcessor>(); // allow multiple images 
	
	private ImageType imageType;
	
	private int rows = 0;
	private int cols = 0;
	
	
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

		if(options==null){
			throw new IllegalArgumentException("Options is null");
		} 

		try{
			this.options = options;
			this.programLogger = logger;
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

							programLogger.log(Level.FINEST, "Opening image");
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
							programLogger.log(Level.FINEST, "Selecting previous image");
							openImage = getPrevImage();
//							setStatusLoading();
							programLogger.log(Level.FINEST, "Opening image");
							importAndDisplayImage(openImage);
						}
					};	
					thr.start();

				}
			});
			contentPanel.add(prevButton, BorderLayout.WEST);
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
					programLogger.log(Level.WARNING, "Unable to load gif");

				} else {
					ok = true;
				}

			} 
			
		} catch (Exception e){
			programLogger.log(Level.SEVERE, "Cannot load gif resource", e);
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

//		headerLabel = new JLabel("Examining input folders...");
		headerLabel.setIcon(loadingGif);
		
		panel.add(new JLabel("Objects meeting detection parameters are outlined in yellow; other objects are red. Click an image to view larger version."), BorderLayout.NORTH);

		panel.add(headerLabel, BorderLayout.SOUTH);

		return panel;
	}
	


	/**
	 * Make the image panel 
	 * @return
	 */
	private JPanel createImagePanel(){
		JPanel panel = new JPanel();
		
		rows = (int) Math.ceil( imageType.getValues().length / 2 );
		cols = 2;
//		panel.setLayout(new BorderLayout());
		panel.setLayout(new GridLayout(cols, rows ));

		for(final ImageType key : imageType.getValues()){

			JLabel label = new JLabel("", loadingGif, JLabel.CENTER);
			label.setText(key.toString());
			label.setHorizontalTextPosition(JLabel.CENTER);
			label.setVerticalTextPosition(JLabel.TOP);
			ImageIcon icon = (ImageIcon) label.getIcon();
			icon.getImage().flush();
			label.setIcon(loadingGif);
			
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
	protected void createFileList(final File folder){
		
		programLogger.log(Level.FINEST, "Generating file list");
//		setStatusLoading();
		
		Thread thr = new Thread(){
			public void run() {
				
				probableFiles = new ArrayList<File>();
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
	 * Display the image for the given key in a new non-modal window
	 * @param key
	 */
	private void showLargeImage(ImageType key){
		final ImageIcon icon = createViewableImage(procMap.get(key), true);
		JOptionPane pane = new JOptionPane(null, JOptionPane.INFORMATION_MESSAGE, JOptionPane.OK_OPTION, icon);
        
        Dialog dialog = pane.createDialog(this, key.toString());
//        for(Component c : dialog.getComponents()){
//        	IJ.log(c.getName());
//        	if(c instanceof JButton){
//        		dialog.remove(c);
//        	}
//        }

        dialog.setModal(false);
        dialog.setVisible(true);
	}
	
	
	/**
	 * Set the header label and the image icons to display
	 * the loading gif
	 */
	protected void setStatusLoading(){
		if(loadingGif!=null){
			ImageIcon hicon = (ImageIcon) headerLabel.getIcon();
			if(hicon!=null){
				hicon.getImage().flush();
			}
			headerLabel.setIcon(loadingGif);
			headerLabel.repaint();

			for(ImageType key : imageType.getValues()){
				
				JLabel label = iconMap.get(key);
				ImageIcon icon = (ImageIcon) label.getIcon();
				icon.getImage().flush();
				label.setIcon(loadingGif);
			}
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
		}
	}

	protected void testLog(){
		double rnd = Math.random() * logTestMessages.length;
		int index = (int) Math.floor(rnd);
		programLogger.log(Level.INFO, logTestMessages[index]+"...");
	}
	
	
	/**
	 * Rezize the given image processor to fit in the screen,
	 * and make an icon
	 * @param ip an image processor
	 * @return an image icon with the resized image
	 */
	protected ImageIcon createViewableImage(ImageProcessor ip, boolean fullSize){
		int originalWidth = ip.getWidth();
		int originalHeight = ip.getHeight();

		// set the image width to be less than half the screen width
		int smallWidth = 0;
		if(fullSize){
			smallWidth = (int) ((double) screenSize.getWidth() * 0.75);
		} else {
			smallWidth = (int) ((double) windowWidth * 0.40);
		}
		
		// keep the image aspect ratio
		double ratio = (double) originalWidth / (double) originalHeight;
		int smallHeight = (int) (smallWidth / ratio);
		
		if(smallHeight > windowHeight / (rows+1) && !fullSize ){ // image is too high, adjust to scale on height
			smallHeight = (int) (windowHeight / (rows+1));
			smallWidth = (int) (smallHeight * ratio);
		}
		
		// Create the image
		
		ImageIcon smallImageIcon;

		if(ip.getWidth()>smallWidth || ip.getHeight() > smallHeight){
			
			smallImageIcon = new ImageIcon(ip.resize(smallWidth, smallHeight ).getBufferedImage());
			
		} else {
			
			smallImageIcon = new ImageIcon( ip.getBufferedImage()  );
		}
		return smallImageIcon;
	}

}
