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
import gui.MainWindow;
import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.io.DirectoryChooser;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import io.ImageExporter;
import io.ImageImporter;

import java.awt.Color;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import utility.Utils;
import analysis.AnalysisDataset;
import components.Cell;
import components.CellCollection;
import components.CellularComponent;
import components.nuclei.Nucleus;

@SuppressWarnings("serial")
public class FishRemappingDialog extends ImageProber {

	public static final int NUCLEUS_OUTLINE_WIDTH = 2;
	
	private AnalysisDataset dataset;
	private File postFISHImageDirectory;
		
	private List<UUID> selectedNucleiLeft = new ArrayList<UUID>(0);
	private List<UUID> selectedNucleiRight = new ArrayList<UUID>(0);
		
	public enum FishMappingImageType implements ImageType {
		
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
	public FishRemappingDialog(MainWindow mw, AnalysisDataset dataset) {
		
		super(dataset.getAnalysisOptions(), FishMappingImageType.ORIGINAL_IMAGE, dataset.getAnalysisOptions().getFolder());
		
		this.setTitle("FISH remapping");
		
		// set the collectio of pre-FISH images
		this.dataset = dataset;
		
		// Clear the 'large image' mouse listener
		// It must be replaced with a custom listener
		for(MouseListener l : iconMap.get(FishMappingImageType.ORIGINAL_IMAGE).getMouseListeners()){
			iconMap.get(FishMappingImageType.ORIGINAL_IMAGE).removeMouseListener(l);
		}
		
		iconMap.get(FishMappingImageType.ORIGINAL_IMAGE).addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {

//				getProgramLogger().log(Level.FINER, "Mouse clicked");
//				 correct scaling 
				ImageProcessor openProcessor = procMap.get(FishMappingImageType.ORIGINAL_IMAGE);
				
				double labelWidth = iconMap.get(FishMappingImageType.ORIGINAL_IMAGE).getWidth();
				double labelHeight = iconMap.get(FishMappingImageType.ORIGINAL_IMAGE).getHeight();
				
				double iconWidth = iconMap.get(FishMappingImageType.ORIGINAL_IMAGE).getIcon().getIconWidth();
				double iconHeight = iconMap.get(FishMappingImageType.ORIGINAL_IMAGE).getIcon().getIconHeight();
				
				double conversion = (double) iconWidth / (double) openProcessor.getWidth();
				
				
				double x = e.getX() ; // positions on label
				double y = e.getY() ;
				
				// convert to positions on the image icon
				double xIcon = x - (  (labelWidth  - iconWidth ) /2 );
				double yIcon = y - (  (labelHeight - iconHeight) /2 );;
				int originalX =  (int) ( xIcon / conversion) ;
				int originalY =  (int) ( yIcon / conversion) ;
				
//				getProgramLogger().log(Level.FINEST, "x: "+x);
//				getProgramLogger().log(Level.FINEST, "y: "+y);
//				getProgramLogger().log(Level.FINEST, "orignal x: "+originalX);
//				getProgramLogger().log(Level.FINEST, "orignal y: "+originalY);

				List<Cell> imageNuclei = FishRemappingDialog.this.dataset.getCollection().getCells(openImage);
				for(Cell c : imageNuclei){
					Nucleus n = c.getNucleus();
					double[] positions = n.getPosition();

					FloatPolygon polygon = n.createPolygon();
					PolygonRoi roi = new PolygonRoi(polygon, PolygonRoi.POLYGON);
					roi.setLocation(positions[CellularComponent.X_BASE], positions[CellularComponent.Y_BASE]);

					if(roi.contains(originalX, originalY)){
//						getProgramLogger().log(Level.FINER, "Nucleus clicked");
						respondToMouseEvent(e, c, openProcessor);
					}
				}
				updateImageThumbnails();
			}
		});
		
		

		// ask for a folder of post-FISH images
		if(this.getPostFISHDirectory()){

			createFileList(dataset.getAnalysisOptions().getFolder());
			this.setVisible(true);
		} else {
			this.dispose();
		}
		
	}
	
	/**
	 * Get a list of CellCollections, containing the selected nuclei.
	 * If no nuclei were selected, the list is empty
	 * @return 
	 * @throws Exception 
	 */
	public List<CellCollection> getSubCollections() throws Exception{
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
		
	
	@Override
	protected void importAndDisplayImage(File imageFile){

		try{
			setStatusLoading();
			this.setLoadingLabelText("Opening image "+index+": "+imageFile.getAbsolutePath()+"...");

			ImageStack stack = ImageImporter.getInstance().importImage(imageFile);

			// Import the image as a stack
			String imageName = imageFile.getName();

			log(Level.FINEST, "Converting image");
			ImageProcessor openProcessor = ImageExporter.getInstance().makeGreyRGBImage(stack).getProcessor();
			openProcessor.invert();
			procMap.put(FishMappingImageType.ORIGINAL_IMAGE, openProcessor);

						
			File fishImageFile = new File(postFISHImageDirectory+File.separator+imageName);
			ImageStack fishStack = ImageImporter.getInstance().importImage(fishImageFile);
			
			ImageProcessor fishProcessor = ImageExporter.getInstance().convertToRGB(fishStack).getProcessor();
			
			procMap.put(FishMappingImageType.FISH_IMAGE, fishProcessor);
			
			// Get the cells matching the imageFile
			for(Cell c : dataset.getCollection().getCells(imageFile)){
				drawNucleus(c, openProcessor);
			}

			

			updateImageThumbnails();

			this.setLoadingLabelText("Showing nuclei in "+imageFile.getAbsolutePath());
			this.setStatusLoaded();

		} catch(Exception e){
				log(Level.SEVERE, "Error in signal probing", e);
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
		FloatPolygon polygon = n.createPolygon();
		PolygonRoi roi = new PolygonRoi(polygon, PolygonRoi.POLYGON);
		roi.setLocation(positions[CellularComponent.X_BASE], positions[CellularComponent.Y_BASE]);
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


