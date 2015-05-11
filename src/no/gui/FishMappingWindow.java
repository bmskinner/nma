package no.gui;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.io.DirectoryChooser;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import no.nuclei.Nucleus;
import no.utility.Utils;

public class FishMappingWindow extends JDialog {

	private static final long serialVersionUID = 1L;
	private final JPanel contentPanel = new JPanel();
	
	private AnalysisDataset preFISHDataset;
	private File postFISHImageDirectory;
	
	private JLabel preImageLabel;
	private JLabel postImageLabel;
	
	private JPanel imagePane;
	
	private boolean isFinished = false;
	
	private NucleusCollection subCollection;
	
	private List<UUID> selectedNuclei = new ArrayList<UUID>(0);
	
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

				this.subCollection = (NucleusCollection) collectionConstructor.newInstance(collection.getFolder(), 
						collection.getOutputFolderName(), 
						"SubColletion", 
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
	
	public NucleusCollection getSubCollection(){
		return this.subCollection;
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
		final JLabel fileLabel = new JLabel("", JLabel.RIGHT);
		headingPanel.add(fileLabel);
		
		contentPanel.add(headingPanel, BorderLayout.NORTH);
		//---------------
		// add the next image button
		//---------------
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new FlowLayout(FlowLayout.CENTER));
		
		final JButton prevButton = new JButton("Previous");
		final JButton nextButton = new JButton("Next");
		prevButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				
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
				
				
//				IJ.log("Opening "+currentImage);
				File imagefile = FishMappingWindow.this.preFISHDataset.getCollection().getImageFiles().get(currentImage);
				fileLabel.setText("Image file: "+imagefile.getAbsolutePath());
				openImages(imagefile);

			}
		});
		prevButton.setEnabled(false);
		buttonPane.add(prevButton);


		nextButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				
				currentImage++;
				
				if(currentImage==0){
					prevButton.setEnabled(false);
				}
				if(currentImage>0){
					prevButton.setEnabled(true);
				}
				
				if(nextButton.getText().equals("Next")){

					if(currentImage==FishMappingWindow.this.preFISHDataset.getCollection().getImageFiles().size()-1){
						// set next click to be end
						nextButton.setText("Done");
					}

					
//					IJ.log("Opening "+currentImage);
					File imagefile = FishMappingWindow.this.preFISHDataset.getCollection().getImageFiles().get(currentImage);
					fileLabel.setText("Image file: "+imagefile.getAbsolutePath());
					openImages(imagefile);
					
					
				} else { // end of analysis; make a collection from all the nuclei selected
					for(Nucleus n : FishMappingWindow.this.preFISHDataset.getCollection().getNuclei()){
						if (FishMappingWindow.this.selectedNuclei.contains(n.getID())){
							FishMappingWindow.this.subCollection.addNucleus(n);
						}
					}
					FishMappingWindow.this.subCollection.setName(FishMappingWindow.this.preFISHDataset.getName()+"_subset");
					FishMappingWindow.this.setVisible(false);
					FishMappingWindow.this.isFinished = true;
				}
				
			}
		});
		buttonPane.add(nextButton);

		//---------------
		// add the cancel button panel
		//---------------

		JButton cancelButton = new JButton("Cancel");
		cancelButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
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
	private void openImages(final File preFile){
		
		String imageName = preFile.getName();
		
		ImagePlus preImage = new ImagePlus(preFile.getAbsolutePath());

		final ImageProcessor ip = preImage.getProcessor();
		
		for(Nucleus n : this.preFISHDataset.getCollection().getNuclei()){
			
			if(n.getSourceFile().equals(preFile)){ // these are the nuclei in the image; draw them
				
				// if present in list, colour green, else yellow
				if(FishMappingWindow.this.selectedNuclei.contains(n.getID())){
    				ip.setColor(Color.GREEN);
				} else {
    				ip.setColor(Color.YELLOW);
				}
								
				double[] positions = n.getPosition();
				FloatPolygon polygon = Utils.createPolygon(n.getBorderList());
				PolygonRoi roi = new PolygonRoi(polygon, PolygonRoi.POLYGON);
				roi.setLocation(positions[Nucleus.X_BASE], positions[Nucleus.Y_BASE]);
				ip.setLineWidth(2);
				ip.draw(roi);
				
			}
		}
		
		int originalWidth = ip.getWidth();
		int originalHeight = ip.getHeight();
				
		// open the images as ImageIcons
		// find the size of the image to scale proportionally
		final int smallWidth = 500;
		double ratio = (double) originalWidth / (double) originalHeight;
		final int smallHeight = (int) (smallWidth / ratio);
		
		// get the conversion factor to find original image coordinates when we click the scaled image
		final double conversion = (double) smallWidth / (double) originalWidth;
		
		ImagePlus preSmall = new ImagePlus("small", ip.resize(smallWidth, smallHeight ));

		ImageIcon preImageIcon = new ImageIcon(preSmall.getBufferedImage());
		preImageLabel.setIcon(preImageIcon);
		
		preImageLabel.addMouseListener(new MouseAdapter() 
		{
		    @Override
		    public void mouseClicked(MouseEvent e) 
		    {
		        //statement
		    	int x = e.getX();
		    	int y = e.getY();
		    	int originalX = (int) (x / conversion);
		    	int originalY = (int) (y / conversion);
//		    	IJ.log("Click: "+x+", "+y+" : "+originalX+", "+originalY);
		    	for(Nucleus n : FishMappingWindow.this.preFISHDataset.getCollection().getNuclei()){
		    		
		    		if(n.getSourceFile().equals(preFile)){

		    			double[] positions = n.getPosition();
		    			FloatPolygon polygon = Utils.createPolygon(n.getBorderList());
		    			PolygonRoi roi = new PolygonRoi(polygon, PolygonRoi.POLYGON);
		    			roi.setLocation(positions[Nucleus.X_BASE], positions[Nucleus.Y_BASE]);
		    			if(roi.contains(originalX, originalY)){
		    				
		    				// if present in list, remove it, otherwise add it
		    				if(FishMappingWindow.this.selectedNuclei.contains(n.getID())){
		    					
		    					FishMappingWindow.this.selectedNuclei.remove(n.getID());
			    				ip.setColor(Color.YELLOW);
		    					
		    				} else {
		    					FishMappingWindow.this.selectedNuclei.add(n.getID());
			    				ip.setColor(Color.GREEN);
		    				}
		    				
		    				// update the image
		    				roi.setLocation(positions[Nucleus.X_BASE], positions[Nucleus.Y_BASE]);
		    				ip.setLineWidth(2);
		    				ip.draw(roi);
		    				ImagePlus preSmall = new ImagePlus("small", ip.resize(smallWidth, smallHeight ));

		    				ImageIcon preImageIcon = new ImageIcon(preSmall.getBufferedImage());
		    				preImageLabel.setIcon(preImageIcon);
		    			}
		    		}
					
		    	}
		    }
		});


		String postFile = this.postFISHImageDirectory.getAbsolutePath()+File.separator+imageName;
		ImagePlus postImage = new ImagePlus(postFile);
		
		ImagePlus postSmall = new ImagePlus("small", postImage.getProcessor().resize(smallWidth, smallHeight));
		ImageIcon postImageIcon = new ImageIcon(postSmall.getBufferedImage());
		postImageLabel.setIcon(postImageIcon);
		
//		IJ.log("Opened post-image: "+postFile);

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

}
