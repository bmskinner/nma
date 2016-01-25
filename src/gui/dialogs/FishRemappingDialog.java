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
import gui.MainWindow;
import gui.components.PaintableJPanel;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.io.DirectoryChooser;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import io.ImageExporter;
import io.ImageImporter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.MouseInputAdapter;

import utility.Utils;
import analysis.AnalysisDataset;
import analysis.nucleus.SignalFinder;
import components.Cell;
import components.CellCollection;
import components.nuclear.NuclearSignal;
import components.nuclei.Nucleus;

@SuppressWarnings("serial")
public class FishRemappingDialog extends ImageProber {

	public static final int NUCLEUS_OUTLINE_WIDTH = 2;
	
	private final AnalysisDataset dataset;
	private File postFISHImageDirectory;
		
	private List<UUID> selectedNucleiLeft = new ArrayList<UUID>(0);
	private List<UUID> selectedNucleiRight = new ArrayList<UUID>(0);
	
	private int offsetX = 0;
	private int offsetY = 0;
	
	private PaintableJPanel paintablePanel;
//	private Image originalImageZoom;
		
	public enum FishMappingImageType implements ImageType {
		
//		ORIGINAL_IMAGE   ("Original image"),
//		FISH_IMAGE		 ("FISHed image"),
//		ORIGINAL_IMAGE_LARGE   ("Full scale original"),
//		FISH_IMAGE_LARGE   ("Full scale FISH");
		
		ORIGINAL_IMAGE   ("Original image"),
		FISH_IMAGE		 ("FISHed image");
		
		private String name;
		
		FishMappingImageType(String name){
			this.name = name;
		}
		public String toString(){
			return this.name;
		}
		
		public ImageType[] getValues(){
			return FishMappingImageType.values();
		}
		
	}
		
	/**
	 * Create the dialog.
	 */
	public FishRemappingDialog(final MainWindow mw, final AnalysisDataset dataset, final Logger programLogger) {
		
		super(dataset.getAnalysisOptions(), programLogger, FishMappingImageType.ORIGINAL_IMAGE, dataset.getAnalysisOptions().getFolder());
		
		this.setTitle("FISH remapping");
		this.setHeaderText("Detected nuclei are outlined in yellow. Left or right click to add nuclei to new collections. Ctrl-click the fish image to offset the zoomed region");
		
		this.setCancelButtonText("Cancel");
		
		// set the collectio of pre-FISH images
		this.dataset = dataset;
		
		// Clear the 'large image' mouse listener
		// It must be replaced with a custom listener
		final ImageType originalImage = FishMappingImageType.ORIGINAL_IMAGE;
		
		for(MouseListener l : iconMap.get(originalImage).getMouseListeners()){
			iconMap.get(originalImage).removeMouseListener(l);
		}
		
		iconMap.get(originalImage).addMouseListener(new MouseAdapter() {
			
			@Override
			public void mousePressed(MouseEvent e) {
				
				Point originalPoint = convertIconLocationToOriginalImage(originalImage, e.getPoint());

				List<Cell> imageNuclei = FishRemappingDialog.this.dataset.getCollection().getCells(openImage);
				
				for(Cell c : imageNuclei){
					Nucleus n = c.getNucleus();

					FloatPolygon polygon = Utils.createPolygon(n.getOriginalBorderList());

					if(polygon.contains(originalPoint.x,  originalPoint.y)){
						respondToMouseEvent(e, c, procMap.get(originalImage));
					}
				}
				
				updateImageThumbnails();
			}
		});
		
//		iconMap.get(originalImage).addMouseMotionListener(new MouseAdapter(){
//			@Override
//			public void mouseMoved(MouseEvent e){
//				
//				Point location = e.getPoint();
//				Point originalPoint = convertIconLocationToOriginalImage(originalImage, location);
//
//				int w = 400;
//				int border = w/2;
//				/*
//				 * Get a rectangle around the location
//				 * Crop this section from the original image
//				 * Set the large icons to use the cropped images
//				 */
//				
//				int rX = originalPoint.x < border // if the point is less than the border distance from the left edge 
//						? 0                       // set the rectangle edge to zero
//						: originalPoint.x-border; // put the point a border distance to the left of the point
////								                   otherwise
////						: originalPoint.x > iconMap.get(originalImage).getWidth()-w // if the point is less than the border from teh right edge
////							? iconMap.get(originalImage).getWidth()-w // set the rectangle edge to the right edge - the full width of the rectangle
////									                          // otherwise
////									: originalPoint.x-border; // put the point a border distance to the left of the point
////							
//							
//				int rY = originalPoint.y < border 
//						? 0 
//						: originalPoint.y-border;
////						: originalPoint.y > iconMap.get(originalImage).getHeight()-w
////								? iconMap.get(originalImage).getHeight()-w
////								: originalPoint.y-border;
//				
//				Rectangle r = new Rectangle(rX, rY, w, w);
//				
//				// The original image
////				makeCroppedVersion(FishMappingImageType.ORIGINAL_IMAGE, FishMappingImageType.ORIGINAL_IMAGE_LARGE, r);
//
//				
//				// The fish image
//				Rectangle offsetR = new Rectangle(rX+(offsetX/2), rY+(offsetY/2), w, w);
////				makeCroppedVersion(FishMappingImageType.FISH_IMAGE, FishMappingImageType.FISH_IMAGE_LARGE, offsetR);
//								
//				updateImageThumbnails();
////				paintablePanel.setClip(offsetR);
//				
//			}
//		});
		
		// Clear the 'large FISH image' mouse listener
		// It must be replaced with a custom listener
		final ImageType fishImage = FishMappingImageType.FISH_IMAGE;

		for(MouseListener l : iconMap.get(fishImage).getMouseListeners()){
			iconMap.get(fishImage).removeMouseListener(l);
		}
		
		iconMap.get(fishImage).addMouseListener(new MouseAdapter() {
			
			@Override
			public void mousePressed(MouseEvent e) {
				
				if(e.isControlDown() && e.getButton()==MouseEvent.BUTTON1){
					
					Point originalPoint = convertIconLocationToOriginalImage(fishImage, e.getPoint());
					
					programLogger.log(Level.INFO, "Offset FISH to "+originalPoint.x + "-"+ originalPoint.y);
					
					// Get the middle of the FISH image
					int midX = procMap.get(fishImage).getWidth()  / 2;
					int midY = procMap.get(fishImage).getHeight() / 2;
										
					offsetX = originalPoint.x - midX;
					offsetY = originalPoint.y - midY;
					
					updateImageThumbnails();
				}
				
			}
		});
		
		
		paintablePanel = new PaintableJPanel();
		paintablePanel.setMaximumSize(new Dimension(400, 400));
		paintablePanel.setMinimumSize(new Dimension(400, 400));
//		
		this.add(paintablePanel, BorderLayout.NORTH);

		// ask for a folder of post-FISH images
		if(this.getPostFISHDirectory()){

			createFileList(dataset.getAnalysisOptions().getFolder());
			this.setVisible(true);
		} else {
			this.dispose();
		}
		
	}
	
	private void makeCroppedVersion(ImageType imageType, ImageType storageType, Rectangle r){
		// The original image
		ImageProcessor original = procMap.get(imageType).duplicate();
		procMap.put(storageType, null); // remove previous version
		original.setRoi(r);
		ImageProcessor crop = original.crop();
		procMap.put(storageType, crop);
		original = null; // free memory
	}
	
	/**
	 * Get a list of CellCollections, containing the selected nuclei.
	 * If no nuclei were selected, the list is empty
	 * @return 
	 */
	public List<CellCollection> getSubCollections(){
		List<CellCollection> result = new ArrayList<CellCollection>(0);
		
		if(!selectedNucleiLeft.isEmpty()){
			CellCollection subCollectionLeft  = new CellCollection(dataset, "SubCollectionLeft");
			for(UUID id : selectedNucleiLeft){
				Cell cell = dataset.getCollection().getCell(id);
				subCollectionLeft.addCell(new Cell(cell));
			}
			result.add(subCollectionLeft);
		}
		
		if(!selectedNucleiRight.isEmpty()){
			CellCollection subCollectionRight  = new CellCollection(dataset, "SubCollectionRight");
			for(UUID id : selectedNucleiRight){
				Cell cell = dataset.getCollection().getCell(id);
				subCollectionRight.addCell(new Cell(cell));
			}
			result.add(subCollectionRight);
		}
		return result;
	}
	
	/**
	 * Import the morphology image file and make a greyscale image
	 * @param imageFile
	 */
	private void importOriginalImage(File imageFile){
		ImageStack stack = ImageImporter.importImage(imageFile, programLogger);

		programLogger.log(Level.FINEST, "Converting image");
		ImageProcessor openProcessor = ImageExporter.makeGreyRGBImage(stack).getProcessor();
		openProcessor.invert();
		
		// Get the cells matching the imageFile
		for(Cell c : dataset.getCollection().getCells(imageFile)){
			drawNucleus(c, openProcessor);
		}
		procMap.put(FishMappingImageType.ORIGINAL_IMAGE, openProcessor);
	}
	
	/**
	 * Get the file name from the input, and import the corresponding
	 * file from the FISH image directory
	 * @param imageFile
	 */
	private void importFISHImage(File imageFile){
		// Import the image as a stack
		String imageName = imageFile.getName();
		File fishImageFile = new File(postFISHImageDirectory+File.separator+imageName);

		if(fishImageFile.exists()){
			ImageStack fishStack = ImageImporter.importImage(fishImageFile, programLogger);

			ImageProcessor fishProcessor = ImageExporter.convertToRGB(fishStack).getProcessor();

			procMap.put(FishMappingImageType.FISH_IMAGE, fishProcessor);

		} else {

			/*
			 * If there is no corresponding FISH image, don't display anything
			 */
			procMap.put(FishMappingImageType.FISH_IMAGE, null);
		}
	}
		
	
	@Override
	protected void importAndDisplayImage(File imageFile){

		try{
			setStatusLoading();
			this.setLoadingLabelText("Opening image "+index+": "+imageFile.getAbsolutePath()+"...");

			
			importOriginalImage(imageFile);

			
			importFISHImage(imageFile);
			
			
//			makeCroppedVersion(FishMappingImageType.ORIGINAL_IMAGE, FishMappingImageType.ORIGINAL_IMAGE_LARGE, r);
//			makeCroppedVersion(FishMappingImageType.FISH_IMAGE, FishMappingImageType.FISH_IMAGE_LARGE, r);

			
//			paintablePanel = new PaintableJPanel(openProcessor.getBufferedImage());
//			paintablePanel.setMinimumSize(new Dimension(400,400));
//			paintablePanel.setMaximumSize(new Dimension(400,400));
			
			updateImageThumbnails();

			this.setLoadingLabelText("Showing nuclei in "+imageFile.getAbsolutePath());
			this.setStatusLoaded();

		} catch(Exception e){
				programLogger.log(Level.SEVERE, "Error in signal probing", e);
			}
		}
	
	

	
	private void respondToMouseEvent(MouseEvent e, Cell c, ImageProcessor ip){
		
		
		// if present in list, remove it, otherwise add it
		if(FishRemappingDialog.this.selectedNucleiLeft.contains(c.getId()) ||  FishRemappingDialog.this.selectedNucleiRight.contains(c.getId()) ){

			FishRemappingDialog.this.selectedNucleiLeft.remove(c.getId());
			FishRemappingDialog.this.selectedNucleiRight.remove(c.getId());

		} else {

			if((e.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK){ // right button
				FishRemappingDialog.this.selectedNucleiRight.add(c.getId());
				FishRemappingDialog.this.selectedNucleiLeft.remove(c.getId());
			}

			if((e.getModifiers() & InputEvent.BUTTON1_MASK)	== InputEvent.BUTTON1_MASK){ // left button
				FishRemappingDialog.this.selectedNucleiLeft.add(c.getId());
				FishRemappingDialog.this.selectedNucleiRight.remove(c.getId());
			}

		}
		
		drawNucleus(c, ip);
		
	}
	
	private Color chooseNucleusOutlineColor(Cell c){
		Color color = Color.ORANGE;
		if(selectedNucleiLeft.contains(c.getId())){
			color = Color.GREEN;
		}
		if(selectedNucleiRight.contains(c.getId())){
			color = Color.MAGENTA;
		}
		
		return color;
	}
	
	private void drawNucleus(Cell c, ImageProcessor ip){
		
		// update the image
		Nucleus n = c.getNucleus();
		double[] positions = n.getPosition();
		ip.setColor(chooseNucleusOutlineColor(c));
		ip.setLineWidth(NUCLEUS_OUTLINE_WIDTH);
		FloatPolygon polygon = Utils.createPolygon(n.getBorderList());
		PolygonRoi roi = new PolygonRoi(polygon, PolygonRoi.POLYGON);
		roi.setLocation(positions[Nucleus.X_BASE], positions[Nucleus.Y_BASE]);
		ip.draw(roi);
	}



	/**
	 * Choose the directory containing the post-FISH images
	 * @return true if the directory is valid, false otherwise
	 */
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
	    this.postFISHImageDirectory = folder;
	    return true;
	}

//	@SuppressWarnings("serial")
//	private class DrawableImageArea extends JLabel {
//		Rectangle currentRect = null;
//
//		Rectangle rectToDraw = null;
//
//		Rectangle previousRectDrawn = new Rectangle();
//
////		FishMappingWindow controller;
//
//		public DrawableImageArea(ImageIcon image) {
//			super("", image, JLabel.CENTER); //This component displays an image.
////			this.controller = controller;
//			setOpaque(true);
//			setMinimumSize(new Dimension(10, 10)); //don't hog space
//
//			MyListener myListener = new MyListener();
//			addMouseListener(myListener);
//			addMouseMotionListener(myListener);
//		}
//
//		private class MyListener extends MouseInputAdapter {
//			public void mousePressed(MouseEvent e) {
//				int x = e.getX();
//				int y = e.getY();
//				currentRect = new Rectangle(x, y, 0, 0);
//				updateDrawableRect(getWidth(), getHeight());
//				repaint();
//			}
//
//			public void mouseDragged(MouseEvent e) {
//				updateSize(e);
//			}
//
//			public void mouseReleased(MouseEvent e) {
//				updateSize(e);
//				
//				 // correct scaling 
//		    	int x = currentRect.x;
//		    	int y = currentRect.y;
//		    	int originalX = openProcessor.getWidth()>smallWidth ? (int) ( (double) x / (double) conversion) : x;
//		    	int originalY = openProcessor.getWidth()>smallWidth ? (int) ( (double)y / (double) conversion) : y;
//		    	
//		    	int originalWidth = openProcessor.getWidth()>smallWidth ? (int) ( (double) currentRect.getWidth() / (double) conversion) : (int) currentRect.getWidth();
//		    	int originalHeight = openProcessor.getWidth()>smallWidth ? (int) ( (double) currentRect.getHeight() / (double) conversion) : (int) currentRect.getHeight();
//		    	
//		    	Rectangle originalRect = new Rectangle(originalX, originalY, originalWidth, originalHeight);
//		    	
//		    	List<Nucleus> imageNuclei = FishRemappingDialog.this.dataset.getCollection().getNuclei(openFile);
//		    	for(Nucleus n : imageNuclei){
//
//		    		if(originalRect.contains(n.getCentreOfMass().getX(), n.getCentreOfMass().getY())){
//		    			double[] positions = n.getPosition();
//		    			FloatPolygon polygon = Utils.createPolygon(n.getBorderList());
//		    			PolygonRoi roi = new PolygonRoi(polygon, PolygonRoi.POLYGON);
//		    			roi.setLocation(positions[Nucleus.X_BASE], positions[Nucleus.Y_BASE]);
//
//		    			FishRemappingDialog.this.selectedNucleiLeft.add(n.getID());
//		    			FishRemappingDialog.this.selectedNucleiRight.remove(n.getID());
//		    			openProcessor.setColor(Color.GREEN);
//
//		    			// update the image
//
//		    			roi.setLocation(positions[Nucleus.X_BASE], positions[Nucleus.Y_BASE]);
//		    			openProcessor.setLineWidth(2);
//		    			openProcessor.draw(roi);
//		    		}
//
//		    	}
//		    	ImagePlus preSmall;
//		    	if(openProcessor.getWidth()>smallWidth){
//		    		preSmall = new ImagePlus("small", openProcessor.resize(smallWidth, smallHeight ));
//		    	} else {
//		    		preSmall = new ImagePlus("small", openProcessor);
//		    	}
//
//		    	ImageIcon preImageIcon = new ImageIcon(preSmall.getBufferedImage());
//		    	preImageLabel.setIcon(preImageIcon);
//			}
//
//			/*
//			 * Update the size of the current rectangle and call repaint.
//			 * Because currentRect always has the same origin, translate it if
//			 * the width or height is negative.
//			 * 
//			 * For efficiency (though that isn't an issue for this program),
//			 * specify the painting region using arguments to the repaint()
//			 * call.
//			 *  
//			 */
//			void updateSize(MouseEvent e) {
//				int x = e.getX();
//				int y = e.getY();
//				currentRect.setSize(x - currentRect.x, y - currentRect.y);
//				updateDrawableRect(getWidth(), getHeight());
//				Rectangle totalRepaint = rectToDraw.union(previousRectDrawn);
//				repaint(totalRepaint.x, totalRepaint.y, totalRepaint.width,
//						totalRepaint.height);
//			}
//		}
//
//		protected void paintComponent(Graphics g) {
//			super.paintComponent(g); //paints the background and image
//
//			//If currentRect exists, paint a box on top.
//			if (currentRect != null) {
//				//Draw a rectangle on top of the image.
//				g.setXORMode(Color.white); //Color of line varies
//				//depending on image colors
//				g.drawRect(rectToDraw.x, rectToDraw.y, rectToDraw.width - 1,
//						rectToDraw.height - 1);
//
//			}
//		}
//
//		private void updateDrawableRect(int compWidth, int compHeight) {
//			int x = currentRect.x;
//			int y = currentRect.y;
//			int width = currentRect.width;
//			int height = currentRect.height;
//
//			//Make the width and height positive, if necessary.
//			if (width < 0) {
//				width = 0 - width;
//				x = x - width + 1;
//				if (x < 0) {
//					width += x;
//					x = 0;
//				}
//			}
//			if (height < 0) {
//				height = 0 - height;
//				y = y - height + 1;
//				if (y < 0) {
//					height += y;
//					y = 0;
//				}
//			}
//
//			//The rectangle shouldn't extend past the drawing area.
//			if ((x + width) > compWidth) {
//				width = compWidth - x;
//			}
//			if ((y + height) > compHeight) {
//				height = compHeight - y;
//			}
//
//			//Update rectToDraw after saving old value.
//			if (rectToDraw != null) {
//				previousRectDrawn.setBounds(rectToDraw.x, rectToDraw.y,
//						rectToDraw.width, rectToDraw.height);
//				rectToDraw.setBounds(x, y, width, height);
//			} else {
//				rectToDraw = new Rectangle(x, y, width, height);
//			}
//		}
//	}
}


