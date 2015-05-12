package no.gui;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.io.DirectoryChooser;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import no.analysis.AnalysisDataset;
import no.collections.NucleusCollection;
import no.export.ImageExporter;
import no.imports.ImageImporter;
import no.nuclei.Nucleus;
import no.utility.Utils;

public class FishMappingWindow extends JDialog {

	private static final long serialVersionUID = 1L;
	public static final int NUCLEUS_OUTLINE_WIDTH = 3;
	private final JPanel contentPanel = new JPanel();
	
	Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
	
	private AnalysisDataset preFISHDataset;
	private File postFISHImageDirectory;
	
	private JLabel preImageLabel;
	private JLabel postImageLabel;
	
	JButton prevButton;
	JButton nextButton;
	
	JLabel fileLabel;
	
	private File openFile;
	private ImageProcessor openProcessor;
	
	private JPanel imagePane;
	
	private boolean isFinished = false;
	
	private NucleusCollection subCollectionLeft;
	private NucleusCollection subCollectionRight;
	
	private List<UUID> selectedNucleiLeft = new ArrayList<UUID>(0);
	private List<UUID> selectedNucleiRight = new ArrayList<UUID>(0);
	
	private int currentImage = 0;
	
	/**
	 * Create the dialog.
	 */
	public FishMappingWindow(MainWindow mw, AnalysisDataset dataset) {
		
		super(mw, true);
		
//		 IJ.log("Preparing setup");

		// set the collectio of pre-FISH images
		this.preFISHDataset = dataset;

		// ask for a folder of post-FISH images
		if(this.getPostFISHDirectory()){

			try {
				NucleusCollection collection = dataset.getCollection();
				
				Constructor<?> collectionConstructor =  dataset.getAnalysisOptions().getCollectionClass().getConstructor(new Class<?>[]{File.class, String.class, String.class, File.class});

				this.subCollectionLeft = (NucleusCollection) collectionConstructor.newInstance(collection.getFolder(), 
						collection.getOutputFolderName(), 
						"SubCollectionLeft", 
						collection.getDebugFile()
						);
				
				this.subCollectionRight = (NucleusCollection) collectionConstructor.newInstance(collection.getFolder(), 
						collection.getOutputFolderName(), 
						"SubCollectionRight", 
						collection.getDebugFile()
						);

				// only proceed with making GUI if there is something to analyse
//				IJ.log("Preparing GUI");
				createGUI();

				// iterate through the pre and post-FISH images, higlighting pre-FISH nuclei
			} catch (NoSuchMethodException e) {
				IJ.log(e.getMessage());
				for(StackTraceElement el : e.getStackTrace()){
					IJ.log(el.toString());
				}
			} catch (SecurityException e) {
				IJ.log(e.getMessage());
				for(StackTraceElement el : e.getStackTrace()){
					IJ.log(el.toString());
				}
			} catch (InstantiationException e) {
				IJ.log(e.getMessage());
				for(StackTraceElement el : e.getStackTrace()){
					IJ.log(el.toString());
				}
			} catch (IllegalAccessException e) {
				IJ.log(e.getMessage());
				for(StackTraceElement el : e.getStackTrace()){
					IJ.log(el.toString());
				}
			} catch (IllegalArgumentException e) {
				IJ.log(e.getMessage());
				for(StackTraceElement el : e.getStackTrace()){
					IJ.log(el.toString());
				}
			} catch (InvocationTargetException e) {
				IJ.log(e.getMessage());
				for(StackTraceElement el : e.getStackTrace()){
					IJ.log(el.toString());
				}
			}
		}
	}
	
	public boolean isFinished(){
		return this.isFinished;
	}
	
	public List<NucleusCollection> getSubCollections(){
		List<NucleusCollection> result = new ArrayList<NucleusCollection>(0);
		result.add(subCollectionLeft);
		result.add(subCollectionRight);
		return result;
	}
		
	public void createGUI(){
		
		
		setBounds(100, 100, 450, 300);
		contentPanel.setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPanel);
//		IJ.log("Made content panel");
		
		// panel for text labels
		JPanel headingPanel = new JPanel();
		headingPanel.setLayout(new BoxLayout(headingPanel, BoxLayout.Y_AXIS));
		
		JLabel headingLabel = new JLabel("Select nuclei to keep for analysis", JLabel.LEFT);
		headingPanel.add(headingLabel);
		JLabel helpLabel = new JLabel("Yellow nuclei are in the population; green are selected for remapping", JLabel.LEFT);
		headingPanel.add(helpLabel);
		fileLabel = new JLabel("", JLabel.RIGHT);
		headingPanel.add(fileLabel);
		
		contentPanel.add(headingPanel, BorderLayout.NORTH);
		//---------------
		// add the next image button
		//---------------
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new FlowLayout(FlowLayout.CENTER));
		
//		final JButton prevButton = new JButton("Previous");
//		final JButton nextButton = new JButton("Next");
		prevButton = new JButton("Previous");
		nextButton = new JButton("Next");
		
		nextButton.addMouseListener(new NextButtonClickedAdapter());
		prevButton.addMouseListener(new PrevButtonClickedAdapter());
		
		prevButton.setEnabled(false);
		buttonPane.add(prevButton);

		buttonPane.add(nextButton);

		//---------------
		// add the cancel button panel
		//---------------

		JButton cancelButton = new JButton("Cancel");
		cancelButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent arg0) {
				FishMappingWindow.this.dispose();			
			}
		});
		buttonPane.add(cancelButton);

		contentPanel.add(buttonPane, BorderLayout.SOUTH);
//		IJ.log("Made buttons");
		
		//---------------
		// add the image panel
		//---------------
		
		imagePane = new JPanel();
		imagePane.setLayout(new BoxLayout(imagePane, BoxLayout.X_AXIS));
		
		ImageIcon preImage = new ImageIcon(new BufferedImage(100,100,BufferedImage.TYPE_INT_RGB));
		ImageIcon postImage = new ImageIcon(new BufferedImage(100,100,BufferedImage.TYPE_INT_RGB));
		
		preImageLabel = new JLabel("", preImage, JLabel.CENTER);
		postImageLabel = new JLabel("", postImage, JLabel.CENTER);
		
		
		imagePane.add(preImageLabel);
		Dimension minSize = new Dimension(10, 100);
		Dimension prefSize = new Dimension(20, 100);
		Dimension maxSize = new Dimension(Short.MAX_VALUE, 100);
		imagePane.add(new Box.Filler(minSize, prefSize, maxSize));
		imagePane.add(postImageLabel);
		
		contentPanel.add(imagePane, BorderLayout.CENTER);

		
		File firstImage = this.preFISHDataset.getCollection().getImageFiles().get(0);
		fileLabel.setText("Image file: "+firstImage.getAbsolutePath());
		openImages(firstImage);

		this.pack(); 
		this.setVisible(true);
	}
	
	
	// open the images for pre and post. Annotate the nuclei
	private void openImages(File preFile){
				
		openFile = preFile;
				
		ImageStack preStack = ImageImporter.importImage(preFile, this.preFISHDataset.getDebugFile());
		ImagePlus preImage = ImageExporter.convert(preStack);

		openProcessor = preImage.getProcessor();
		List<Nucleus> imageNuclei = this.preFISHDataset.getCollection().getNuclei(openFile);
		

		for(Nucleus n : imageNuclei){

			// if present in list, colour green, else yellow
			if(selectedNucleiLeft.contains(n.getID())){
				openProcessor.setColor(Color.GREEN);
			} else {

				if(selectedNucleiRight.contains(n.getID())){
					openProcessor.setColor(Color.MAGENTA);

				} else {
					openProcessor.setColor(Color.YELLOW);
				}
			}

			double[] positions = n.getPosition();
			FloatPolygon polygon = Utils.createPolygon(n.getBorderList());
			PolygonRoi roi = new PolygonRoi(polygon, PolygonRoi.POLYGON);
			roi.setLocation(positions[Nucleus.X_BASE], positions[Nucleus.Y_BASE]);
			openProcessor.setLineWidth(2);
			openProcessor.draw(roi);

		}
	
		int originalWidth = openProcessor.getWidth();
		int originalHeight = openProcessor.getHeight();
				

		// set the image width to be less than half the screen width
		final int smallWidth = (int) ((double) screenSize.getWidth() * 0.45);
		
		// keep the image aspect ratio
		double ratio = (double) originalWidth / (double) originalHeight;
		final int smallHeight = (int) (smallWidth / ratio);
		
		// get the conversion factor to find original image coordinates when we click the scaled image
		final double conversion = (double) smallWidth / (double) originalWidth;
		
		final ImagePlus preSmall;

		
		if(openProcessor.getWidth()>smallWidth){
			preSmall = new ImagePlus("small", openProcessor.resize(smallWidth, smallHeight ));
		} else {
			preSmall = new ImagePlus("small", openProcessor);
		}

		ImageIcon preImageIcon = new ImageIcon(preSmall.getBufferedImage());
		preImageLabel.setIcon(preImageIcon);

		
		// stop the listeners building up and overlapping
		for(MouseListener m : preImageLabel.getMouseListeners()){
			preImageLabel.removeMouseListener(m);
		}
		
		preImageLabel.addMouseListener(new MouseAdapter() {
		    @Override
		    public void mousePressed(MouseEvent e) {
		    			    
		        // correct scaling 
		    	int x = e.getX();
		    	int y = e.getY();
		    	int originalX = openProcessor.getWidth()>smallWidth ? (int) ( (double) x / (double) conversion) : x;
		    	int originalY = openProcessor.getWidth()>smallWidth ? (int) ( (double)y / (double) conversion) : y;
		    	
		    	
		    	List<Nucleus> imageNuclei = FishMappingWindow.this.preFISHDataset.getCollection().getNuclei(openFile);
		    	for(Nucleus n : imageNuclei){
		    		

		    		double[] positions = n.getPosition();
		    		
		    		FloatPolygon polygon = Utils.createPolygon(n.getBorderList());
		    		PolygonRoi roi = new PolygonRoi(polygon, PolygonRoi.POLYGON);
		    		roi.setLocation(positions[Nucleus.X_BASE], positions[Nucleus.Y_BASE]);
		    		
		    		if(roi.contains(originalX, originalY)){

		    			drawNucleus(n, openProcessor, e, roi);
		    			
		    			openProcessor.setColor(Color.CYAN);
		    			openProcessor.drawOval(originalX, originalY, 3, 3);
		    			
		    			ImagePlus preSmall;
		    			if(openProcessor.getWidth()>smallWidth){
		    				preSmall = new ImagePlus("small", openProcessor.resize(smallWidth, smallHeight ));
		    			} else {
		    				preSmall = new ImagePlus("small", openProcessor);
		    			}

			    		ImageIcon preImageIcon = new ImageIcon(preSmall.getBufferedImage());
			    		preImageLabel.setIcon(preImageIcon);
		    		}
		    	}
		    }
		});

		// Open the image from the post FISH directory
		String imageName = preFile.getName(); // the file name e.g. P60.tif
		String postFile = this.postFISHImageDirectory.getAbsolutePath()+File.separator+imageName;
		ImagePlus postImage = new ImagePlus(postFile);
		
		// make an image icon and display
		ImagePlus postSmall;
//		ImagePlus preSmall = new ImagePlus("small", ip.resize(smallWidth, smallHeight ));
		if(openProcessor.getWidth()>smallWidth){
			postSmall = new ImagePlus("small", postImage.getProcessor().resize(smallWidth, smallHeight));
		} else {
			postSmall = postImage;
		}
		
//		ImagePlus postSmall = new ImagePlus("small", postImage.getProcessor().resize(smallWidth, smallHeight));
		ImageIcon postImageIcon = new ImageIcon(postSmall.getBufferedImage());
		postImageLabel.setIcon(postImageIcon);

	}

	private void drawNucleus(Nucleus n, ImageProcessor ip, MouseEvent e, Roi roi){


		// if present in list, remove it, otherwise add it
		if(FishMappingWindow.this.selectedNucleiLeft.contains(n.getID()) ||  FishMappingWindow.this.selectedNucleiRight.contains(n.getID()) ){

			FishMappingWindow.this.selectedNucleiLeft.remove(n.getID());
			FishMappingWindow.this.selectedNucleiRight.remove(n.getID());
			ip.setColor(Color.YELLOW);

		} else {

			if((e.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK){ // right button
				FishMappingWindow.this.selectedNucleiRight.add(n.getID());
				FishMappingWindow.this.selectedNucleiLeft.remove(n.getID());
				ip.setColor(Color.MAGENTA);
			}

			if((e.getModifiers() & InputEvent.BUTTON1_MASK)	== InputEvent.BUTTON1_MASK){ // left button
				FishMappingWindow.this.selectedNucleiLeft.add(n.getID());
				FishMappingWindow.this.selectedNucleiRight.remove(n.getID());
				ip.setColor(Color.GREEN);
			}

		}

		// update the image
		double[] positions = n.getPosition();
		roi.setLocation(positions[Nucleus.X_BASE], positions[Nucleus.Y_BASE]);
		ip.setLineWidth(2);
		ip.draw(roi);
	}



	private boolean getPostFISHDirectory(){
		DirectoryChooser localOpenDialog = new DirectoryChooser("Select directory of post-FISH images...");
	    String folderName = localOpenDialog.getDirectory();

	    if(folderName==null) return false; // user cancelled
	   
	    File folder =  new File(folderName);
	    
	    if(!folder.isDirectory() ){
	    	return false;
	    }
	    if(!folder.exists()){
	    	return false; // check folder is ok
	    }
//	    IJ.log("Got directory: "+folder.getAbsolutePath());
	    this.postFISHImageDirectory = folder;
	    return true;
	}
	
	
	class NextButtonClickedAdapter extends MouseAdapter {
		
		@Override
		public void mousePressed(MouseEvent arg0) {
			
			if(arg0.getSource()==nextButton){

				currentImage++;

				if(currentImage==0){
					FishMappingWindow.this.prevButton.setEnabled(false);
				}
				if(currentImage>0){
					prevButton.setEnabled(true);
				}

				if(nextButton.getText().equals("Next")){

					if(currentImage==FishMappingWindow.this.preFISHDataset.getCollection().getImageFiles().size()-1){
						// set next click to be end
						nextButton.setText("Done");
					}


					//				IJ.log("Opening "+currentImage);
					File imagefile = FishMappingWindow.this.preFISHDataset.getCollection().getImageFiles().get(currentImage);
					int imageDisplayCount = currentImage+1;
					String progress = "Image "+imageDisplayCount+" of "+FishMappingWindow.this.preFISHDataset.getCollection().getImageFiles().size();
					fileLabel.setText("Image file: "+imagefile.getAbsolutePath()+" : "+progress);
					openImages(imagefile);


				} else { // end of analysis; make a collection from all the nuclei selected
					for(Nucleus n : FishMappingWindow.this.preFISHDataset.getCollection().getNuclei()){
						if (FishMappingWindow.this.selectedNucleiLeft.contains(n.getID())){
							FishMappingWindow.this.subCollectionLeft.addNucleus(n);
						}
						if (FishMappingWindow.this.selectedNucleiRight.contains(n.getID())){
							FishMappingWindow.this.subCollectionRight.addNucleus(n);
						}

					}
					FishMappingWindow.this.subCollectionLeft.setName(FishMappingWindow.this.preFISHDataset.getName()+"_left_subset");
					FishMappingWindow.this.subCollectionRight.setName(FishMappingWindow.this.preFISHDataset.getName()+"_right_subset");
					FishMappingWindow.this.setVisible(false);
					FishMappingWindow.this.isFinished = true;
				}
			} else {
				IJ.log("Next button got spurious trigger");
			}
		}
	}
	

	class PrevButtonClickedAdapter extends MouseAdapter {

		@Override
		public void mousePressed(MouseEvent arg0) {
			
			if(arg0.getSource()==prevButton){
			currentImage--;
			
			if(currentImage==0){
				prevButton.setEnabled(false);
			}
			if(currentImage>0){
				prevButton.setEnabled(true);
			}
			
			if(currentImage<FishMappingWindow.this.preFISHDataset.getCollection().getImageFiles().size()-1){
				nextButton.setText("Next");
			}
			
			
//			IJ.log("Opening "+currentImage);
			File imagefile = FishMappingWindow.this.preFISHDataset.getCollection().getImageFiles().get(currentImage);
			int imageDisplayCount = currentImage+1;
			String progress = "Image "+imageDisplayCount+" of "+FishMappingWindow.this.preFISHDataset.getCollection().getImageFiles().size();
			fileLabel.setText("Image file: "+imagefile.getAbsolutePath()+" : "+progress);
			openImages(imagefile);
			} else {
				IJ.log("Prev button got spurious trigger");
			}
		}
	}
	
//	class ImageClickedAdapter extends MouseAdapter {
//
//		@Override
//		public void mousePressed(MouseEvent e) {
//
//
//			// correct scaling 
//			int x = e.getX();
//			int y = e.getY();
//			int originalX = openProcessor.getWidth()>smallWidth ? (int) ( (double) x / (double) conversion) : x;
//			int originalY = openProcessor.getWidth()>smallWidth ? (int) ( (double)y / (double) conversion) : y;
//
//
//			List<Nucleus> imageNuclei = FishMappingWindow.this.preFISHDataset.getCollection().getNuclei(openFile);
//			IJ.log("");
//			for(Nucleus n : imageNuclei){
//
//				IJ.log("Checking "+n.getImageName()+"-"+n.getNucleusNumber());
//
//				double[] positions = n.getPosition();
//
//				FloatPolygon polygon = Utils.createPolygon(n.getBorderList());
//				PolygonRoi roi = new PolygonRoi(polygon, PolygonRoi.POLYGON);
//				roi.setLocation(positions[Nucleus.X_BASE], positions[Nucleus.Y_BASE]);
//
//				// there is one point in each roi that when clicked, replaces with another image
//				// see testing set 4 -> 2
//				// somehow the ip is set to a different image file
//				// the roi overlap is correct for the wrong nucleus
//
//				if(roi.contains(originalX, originalY)){
//
//					drawNucleus(n, openProcessor, e, roi);
//					IJ.log("  Match found");
//
//					openProcessor.setColor(Color.CYAN);
//					openProcessor.drawOval(originalX, originalY, 3, 3);
//
//					ImagePlus preSmall;
//					if(openProcessor.getWidth()>smallWidth){
//						preSmall = new ImagePlus("small", openProcessor.resize(smallWidth, smallHeight ));
//						IJ.log("  resized");
//					} else {
//						preSmall = new ImagePlus("small", openProcessor);
//						IJ.log("  raw");
//					}
//
//					ImageIcon preImageIcon = new ImageIcon(preSmall.getBufferedImage());
//					preImageLabel.setIcon(preImageIcon);
//				}
//			}
//		}
//	}
}
