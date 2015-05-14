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
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
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
import javax.swing.event.MouseInputAdapter;

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
	
	private double conversion;
	private int smallWidth; 
	private int smallHeight;
	
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
//				NucleusCollection collection = dataset.getCollection();
				
				this.subCollectionLeft  = MainWindow.makeNewCollection(dataset, "SubCollectionLeft");
				this.subCollectionRight = MainWindow.makeNewCollection(dataset, "SubCollectionRight");
				
				createGUI();

			} catch (Exception e) {
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
		JLabel helpLabel = new JLabel("Yellow nuclei are available to select", JLabel.LEFT);
		JLabel helpLabel2 = new JLabel("Click nuclei to add them to a sub-population", JLabel.LEFT);
		headingPanel.add(helpLabel);
		headingPanel.add(helpLabel2);
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
		
//		preImageLabel = new JLabel("", preImage, JLabel.CENTER);
		postImageLabel = new JLabel("", postImage, JLabel.CENTER);
		
		preImageLabel = new DrawableImageArea(preImage);
		
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
		smallWidth = (int) ((double) screenSize.getWidth() * 0.45);
//		final int smallWidth = (int) ((double) screenSize.getWidth() * 0.45);
		
		// keep the image aspect ratio
		double ratio = (double) originalWidth / (double) originalHeight;
		smallHeight = (int) (smallWidth / ratio);
//		final int smallHeight = (int) (smallWidth / ratio);
		
		// get the conversion factor to find original image coordinates when we click the scaled image
		conversion = (double) smallWidth / (double) originalWidth;
//		final double conversion = (double) smallWidth / (double) originalWidth;
		
		final ImagePlus preSmall;

		
		if(openProcessor.getWidth()>smallWidth){
			preSmall = new ImagePlus("small", openProcessor.resize(smallWidth, smallHeight ));
		} else {
			preSmall = new ImagePlus("small", openProcessor);
		}

		ImageIcon preImageIcon = new ImageIcon(preSmall.getBufferedImage());
		preImageLabel.setIcon(preImageIcon);

		
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

	private class DrawableImageArea extends JLabel {
		Rectangle currentRect = null;

		Rectangle rectToDraw = null;

		Rectangle previousRectDrawn = new Rectangle();

//		FishMappingWindow controller;

		public DrawableImageArea(ImageIcon image) {
			super("", image, JLabel.CENTER); //This component displays an image.
//			this.controller = controller;
			setOpaque(true);
			setMinimumSize(new Dimension(10, 10)); //don't hog space

			MyListener myListener = new MyListener();
			addMouseListener(myListener);
			addMouseMotionListener(myListener);
		}

		private class MyListener extends MouseInputAdapter {
			public void mousePressed(MouseEvent e) {
				int x = e.getX();
				int y = e.getY();
				currentRect = new Rectangle(x, y, 0, 0);
				updateDrawableRect(getWidth(), getHeight());
				repaint();
			}

			public void mouseDragged(MouseEvent e) {
				updateSize(e);
			}

			public void mouseReleased(MouseEvent e) {
				updateSize(e);
				
				 // correct scaling 
		    	int x = currentRect.x;
		    	int y = currentRect.y;
		    	int originalX = openProcessor.getWidth()>smallWidth ? (int) ( (double) x / (double) conversion) : x;
		    	int originalY = openProcessor.getWidth()>smallWidth ? (int) ( (double)y / (double) conversion) : y;
		    	
		    	int originalWidth = openProcessor.getWidth()>smallWidth ? (int) ( (double) currentRect.getWidth() / (double) conversion) : (int) currentRect.getWidth();
		    	int originalHeight = openProcessor.getWidth()>smallWidth ? (int) ( (double) currentRect.getHeight() / (double) conversion) : (int) currentRect.getHeight();
		    	
		    	Rectangle originalRect = new Rectangle(originalX, originalY, originalWidth, originalHeight);
		    	
		    	List<Nucleus> imageNuclei = FishMappingWindow.this.preFISHDataset.getCollection().getNuclei(openFile);
		    	for(Nucleus n : imageNuclei){

		    		if(originalRect.contains(n.getCentreOfMass().getX(), n.getCentreOfMass().getY())){
		    			double[] positions = n.getPosition();
		    			FloatPolygon polygon = Utils.createPolygon(n.getBorderList());
		    			PolygonRoi roi = new PolygonRoi(polygon, PolygonRoi.POLYGON);
		    			roi.setLocation(positions[Nucleus.X_BASE], positions[Nucleus.Y_BASE]);

		    			FishMappingWindow.this.selectedNucleiLeft.add(n.getID());
		    			FishMappingWindow.this.selectedNucleiRight.remove(n.getID());
		    			openProcessor.setColor(Color.GREEN);

		    			// update the image

		    			roi.setLocation(positions[Nucleus.X_BASE], positions[Nucleus.Y_BASE]);
		    			openProcessor.setLineWidth(2);
		    			openProcessor.draw(roi);
		    		}

		    	}
		    	ImagePlus preSmall;
		    	if(openProcessor.getWidth()>smallWidth){
		    		preSmall = new ImagePlus("small", openProcessor.resize(smallWidth, smallHeight ));
		    	} else {
		    		preSmall = new ImagePlus("small", openProcessor);
		    	}

		    	ImageIcon preImageIcon = new ImageIcon(preSmall.getBufferedImage());
		    	preImageLabel.setIcon(preImageIcon);
			}

			/*
			 * Update the size of the current rectangle and call repaint.
			 * Because currentRect always has the same origin, translate it if
			 * the width or height is negative.
			 * 
			 * For efficiency (though that isn't an issue for this program),
			 * specify the painting region using arguments to the repaint()
			 * call.
			 *  
			 */
			void updateSize(MouseEvent e) {
				int x = e.getX();
				int y = e.getY();
				currentRect.setSize(x - currentRect.x, y - currentRect.y);
				updateDrawableRect(getWidth(), getHeight());
				Rectangle totalRepaint = rectToDraw.union(previousRectDrawn);
				repaint(totalRepaint.x, totalRepaint.y, totalRepaint.width,
						totalRepaint.height);
			}
		}

		protected void paintComponent(Graphics g) {
			super.paintComponent(g); //paints the background and image

			//If currentRect exists, paint a box on top.
			if (currentRect != null) {
				//Draw a rectangle on top of the image.
				g.setXORMode(Color.white); //Color of line varies
				//depending on image colors
				g.drawRect(rectToDraw.x, rectToDraw.y, rectToDraw.width - 1,
						rectToDraw.height - 1);

			}
		}

		private void updateDrawableRect(int compWidth, int compHeight) {
			int x = currentRect.x;
			int y = currentRect.y;
			int width = currentRect.width;
			int height = currentRect.height;

			//Make the width and height positive, if necessary.
			if (width < 0) {
				width = 0 - width;
				x = x - width + 1;
				if (x < 0) {
					width += x;
					x = 0;
				}
			}
			if (height < 0) {
				height = 0 - height;
				y = y - height + 1;
				if (y < 0) {
					height += y;
					y = 0;
				}
			}

			//The rectangle shouldn't extend past the drawing area.
			if ((x + width) > compWidth) {
				width = compWidth - x;
			}
			if ((y + height) > compHeight) {
				height = compHeight - y;
			}

			//Update rectToDraw after saving old value.
			if (rectToDraw != null) {
				previousRectDrawn.setBounds(rectToDraw.x, rectToDraw.y,
						rectToDraw.width, rectToDraw.height);
				rectToDraw.setBounds(x, y, width, height);
			} else {
				rectToDraw = new Rectangle(x, y, width, height);
			}
		}
	}
}


