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
import gui.ThreadManager;
import ij.io.DirectoryChooser;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.swing.ImageIcon;
import javax.swing.table.TableModel;

import analysis.AnalysisDataset;
import analysis.detection.IconCell;
import analysis.signals.FishRemappingWorker;
import components.Cell;
import components.CellCollection;
import components.generic.XYPoint;

@SuppressWarnings("serial")
public class FishRemappingDialog extends ImageProber {

	public static final int NUCLEUS_OUTLINE_WIDTH = 2;
	
	private AnalysisDataset dataset;
	private File postFISHImageDirectory;
		
	private List<UUID> selectedNucleiLeft  = new ArrayList<UUID>(0); // Nuclei selected with the left button
	private List<UUID> selectedNucleiRight = new ArrayList<UUID>(0); // Nuclei selected with the right button
		
	
	/**
	 * Hold the stages of the detection pipeline to display 
	 */
	public enum FishMappingImageType implements ImageType {
		ORIGINAL_IMAGE 		("Original image", 0),
		FISH_IMAGE 			("FISHed image",   1);
		
		private String name;
		private int position; // the order in which the processed images should be displayed
		
		FishMappingImageType(String name, int position){
			this.name = name;
			this.position = position;
		}
		public String toString(){
			return this.name;
		}
		
		public ImageType[] getValues(){
			return FishMappingImageType.values();
		}
		@Override
		public int getPosition() {
			return position;
		}
	}
	
		
	/**
	 * Create the dialog.
	 */
	public FishRemappingDialog(MainWindow mw, AnalysisDataset dataset) {
		
		super(dataset.getAnalysisOptions(), FishMappingImageType.ORIGINAL_IMAGE, dataset.getAnalysisOptions().getFolder());
		
		this.setTitle("FISH remapping");
		
		this.setCancelButtonText("Cancel");
		this.setHeaderText("Nuclei are highlighted in blue. Click with the left or right button to assign them to groups.");
		
		
		// set the collectio of pre-FISH images
		this.dataset = dataset;
		
		
		// Remove existing listeners
		for(MouseListener l : table.getMouseListeners()){
			table.removeMouseListener(l);
		}
		
		// Add listener for nucleus click
		table.addMouseListener( new MouseAdapter(){
        	
        	@Override
        	public void mouseClicked(MouseEvent e){
        		if(e.getClickCount()==1){
        			
        			Point pnt = e.getPoint();
        			int row = table.rowAtPoint(pnt);
        			int col = table.columnAtPoint(pnt);
        			
        			if(row==0 && col==0){
        				
        				Runnable r = () -> {
        					originalImageClicked(e, pnt);
        				};
        				
        				ThreadManager.getInstance().execute(r);
        				
        				
        				
        			} else { // Show a large image for the FISH image when clicked

            			TableModel model = (TableModel)table.getModel();

            			IconCell selectedData = (IconCell) model.getValueAt( row, col );

            			if(selectedData.getLargeIcon()!=null){
            				new LargeImageDialog(selectedData, FishRemappingDialog.this);
            			}
        			}
        			
        			
        		}
        	}
        	
        });


		// ask for a folder of post-FISH images
		if(this.getPostFISHDirectory()){

			createFileList(  dataset.getCollection().getFolder()  );
			this.setVisible(true);
		} else {
			this.dispose();
		}
		
	}
	
	private void originalImageClicked(MouseEvent e, Point pnt){
		
		// Get the data model for this table
		TableModel model = (TableModel)table.getModel();
		
		int row = table.rowAtPoint(pnt);
		int col = table.columnAtPoint(pnt);
		
		// The coordinates are relative to the cell of the table.
		// The height of the image is less than the table height, so 
		// subtract the y difference
		double x = pnt.getX();
		double y = pnt.getY();
		
		finer("Clicked "+x+" : "+y);
		
		Rectangle cellRectangle = table.getCellRect(row, col, false);

		IconCell selectedData = (IconCell) model.getValueAt( row, col );
		        			
		int iconHeight = selectedData.getSmallIcon().getIconHeight() + 20; // add padding for text
		
		int offset = (cellRectangle.height - iconHeight) >>1;
		
		int offsetY = (int) (y-offset);
		
		finer("Offset to "+x+" : "+offsetY);

		// Translate coordinates back to large image
		double factor = selectedData.getFactor();
		
		double largeX = x * factor;
		double largeY = offsetY * factor;
		
		// See if the selected position in the large icon is in a nucleus
		
		for(Cell c : dataset.getCollection().getCells(openImage)){
			if(c.getNucleus().containsOriginalPoint(new XYPoint(largeX, largeY))){
				
				respondToMouseEvent(e, c);
				fine("Click is in nucleus");
				drawNucleus(c, selectedData.getLargeIcon().getImage());
				// Update the small icon
				selectedData.setSmallIcon( new ImageIcon(scaleImage( selectedData.getLargeIcon() )) );
				table.repaint(cellRectangle);

			}
			
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

		try {
			this.setStatusLoading();
			this.setLoadingLabelText("Opening image "+index+": "+imageFile.getAbsolutePath()+"...");
			table.setModel(createEmptyTableModel(rows, cols));
			
			for(int col=0; col<cols; col++){
	        	table.getColumnModel().getColumn(col).setCellRenderer(new IconCellRenderer());
	        }
			
			FishRemappingWorker worker = new FishRemappingWorker(imageFile, 
					 
					FishMappingImageType.ORIGINAL_IMAGE, 
					table.getModel(), postFISHImageDirectory);
			
			worker.setSmallIconSize(new Dimension(500, table.getRowHeight()-30));
			
			worker.addPropertyChangeListener(this);
			progressBar.setVisible(true);
			worker.execute();


		} catch (Exception e) { // end try
			error("Error in image processing", e);
		} 
	}
	
	
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		super.propertyChange(evt);
		
		IconCell infoCell = (IconCell) table.getModel().getValueAt(0, 0); // TODO: make dynamic
		
		Image largeImage = infoCell.getLargeIcon().getImage();
		
		// Get the cells matching the imageFile
		for(Cell c : dataset.getCollection().getCells(openImage)){
			drawNucleus(c, largeImage);
		}
		

		// Update the small icon
		infoCell.setSmallIcon( new ImageIcon(scaleImage( infoCell.getLargeIcon() )) );
		
		
		
	}
	
	/**
	 * Create a copy of the given processor, and scale it fit the maximum
	 * dimensions specified by setSmallIconSize(). The aspect ratio is preserved.
	 * @param ip
	 * @return
	 */
	protected Image scaleImage(ImageIcon ic){
				
		double aspect =  (double) ic.getIconWidth() / (double) ic.getIconHeight();
		
		
		Dimension smallDimension = new Dimension(500, table.getRowHeight()-30);
		
		double finalWidth = smallDimension.getHeight() * aspect; // fix height
		finalWidth = finalWidth > smallDimension.getWidth() 
				   ? smallDimension.getWidth() 
				   : finalWidth; // but constrain width too
				   
		return ic.getImage().getScaledInstance( (int) finalWidth, -1, Image.SCALE_SMOOTH);
	}
	
	

	
	private synchronized void respondToMouseEvent(MouseEvent e, Cell c){
		
		
		// if present in list, remove it, otherwise add it
		if(selectedNucleiLeft.contains(c.getId()) ||  selectedNucleiRight.contains(c.getId()) ){

			selectedNucleiLeft.remove(c.getId());
			selectedNucleiRight.remove(c.getId());

		} else {

			if((e.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK){ // right button
				selectedNucleiRight.add(c.getId());
				selectedNucleiLeft.remove(c.getId());
			}

			if((e.getModifiers() & InputEvent.BUTTON1_MASK)	== InputEvent.BUTTON1_MASK){ // left button
				selectedNucleiLeft.add(c.getId());
				selectedNucleiRight.remove(c.getId());
			}

		}
		
		
	}
	
	private Color chooseNucleusOutlineColor(Cell c){
		Color color = Color.BLUE;
		if(selectedNucleiLeft.contains(c.getId())){
			color = Color.GREEN;
		}
		if(selectedNucleiRight.contains(c.getId())){
			color = Color.RED;
		}
		
		return color;
	}
	
	private void drawNucleus(Cell c, Image image){
		
//		int position = FishMappingImageType.ORIGINAL_IMAGE.getPosition();
		
		Graphics2D g2 = (Graphics2D) image.getGraphics();
		
		
		g2.setColor(chooseNucleusOutlineColor(c));
		
		Shape p = c.getNucleus().toOriginalShape();
		
		g2.fill(p);
		
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
	    finer("Selected "+postFISHImageDirectory.getAbsolutePath()+" as post-FISH image directory");
	    return true;
	}

}


