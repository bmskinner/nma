package com.bmskinner.nuclear_morphology.gui.dialogs.prober;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.swing.ImageIcon;
import javax.swing.table.TableModel;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.VirtualCellCollection;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.gui.GlobalOptions;
import com.bmskinner.nuclear_morphology.gui.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter.ColourSwatch;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.workers.FishRemappingProberWorker;

public class FishRemappingProberPanel extends ImageProberPanel {

	private final IAnalysisDataset dataset;
	private final File nucleusDir;
	private final File fishDir;
	
	private List<UUID> selectedNucleiLeft  = new ArrayList<UUID>(96); // Nuclei selected with the left button
	private List<UUID> selectedNucleiRight = new ArrayList<UUID>(96); // Nuclei selected with the right button
	
	private Set<ICell> openCells = new HashSet<ICell>();
	
	public FishRemappingProberPanel(final Window parent, 
			final IDetectionOptions options, 
			final ImageSet set, 
			final IAnalysisDataset dataset,
			final File fishDir){
		
		super(parent, options, set);
		
		this.dataset   = dataset;
		this.fishDir = fishDir;
		// fetch nuclei based on file names
		nucleusDir = dataset.getAnalysisOptions()
						.getDetectionOptions(IAnalysisOptions.NUCLEUS)
						.getFolder();
		
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

            			ImageProberTableCell selectedData = (ImageProberTableCell) model.getValueAt( row, col );

            			if(selectedData.getLargeIcon()!=null){
            				new LargeImageDialog(selectedData, parent);
            			}
        			}
        			
        			
        		}
        	}
        	
        });
		
		
		
		createFileList(options.getFolder());
	}
	
	/**
	 * Import the given file as an image, detect nuclei and
	 * display the image with annotated nuclear outlines
	 * @param imageFile
	 */
	@Override
	protected void importAndDisplayImage(File imageFile){

		if(imageFile==null){
			throw new IllegalArgumentException(NULL_FILE_ERROR);
		}
		
		try {

			setImageLabel(imageFile.getAbsolutePath());
			
			
			table.setModel(createEmptyTableModel(rows, cols));
			
			for(int col=0; col<cols; col++){
	        	table.getColumnModel().getColumn(col).setCellRenderer(new IconCellRenderer());
	        }
			
			
			
			String imageName = imageFile.getName();
			
			File nucleusFile = new File(nucleusDir, imageName);
			
			Set<Nucleus> list = dataset.getCollection().getNuclei(nucleusFile);
			
			worker = new FishRemappingProberWorker(imageFile, 
					options, 
					imageSet, 
					table.getModel(),
					fishDir,
					list);
			
			worker.setSmallIconSize(new Dimension(SMALL_ICON_WIDTH, table.getRowHeight()-30));
			
			worker.addPropertyChangeListener(this);
			progressBar.setVisible(true);
			worker.execute();


		} catch (Exception e) { // end try
			error(e.getMessage(), e);
		} 
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		super.propertyChange(evt);
		
		ImageProberTableCell infoCell = (ImageProberTableCell) table.getModel().getValueAt(0, 0); // TODO: make dynamic
		
		Image largeImage = infoCell.getLargeIcon().getImage();
		
		// Get the cells matching the imageFile
		for(ICell c : dataset.getCollection().getCells(openImage)){
			drawNucleus(c, largeImage);
		}
		

		// Update the small icon
		infoCell.setSmallIcon( new ImageIcon(scaleImage( infoCell.getLargeIcon() )) );

	}
	
	private void originalImageClicked(MouseEvent e, Point pnt){
		
		// Get the data model for this table
		TableModel model = (TableModel)table.getModel();
				
		int row = table.rowAtPoint(   pnt);
		int col = table.columnAtPoint(pnt);
		
		// The coordinates are relative to the cell of the table.
		// The height of the image is less than the table height, so 
		// subtract the y difference
		double x = pnt.getX();
		double y = pnt.getY();
		
		finer("Clicked "+x+" : "+y);
		
		
		/*
		 * The coordinates within the cell must be converted
		 * to coordinates within the small image in the IconCell.
		 * 
		 * The x coordinates are not always correct. The IconCell is aligned
		 * horizontally, so the difference in width between the IconCell and the
		 * table cell can be used as an offset
		 * 
		 * The imageprober has vertical alignment to the top of the cell,
		 * so y coordinates should also be correct
		 * 
		 * 
		 */
		
		// Get the rectangle covering the cell of the table
		Rectangle cellRectangle = table.getCellRect(row, col, false);

		// Get the icon cell at the clicked row and column
		ImageProberTableCell selectedData = (ImageProberTableCell) model.getValueAt( row, col );

		// Get the width of the icon in the icon cell
		int iconWidth = selectedData.getSmallIcon().getIconWidth();

//		// Get the width of the column of interest
		int columnWidth = cellRectangle.width;
		
		finer("Column width is "+columnWidth);
		finer("IconCell width is "+iconWidth);

//		Split the difference
		int offset = (columnWidth - iconWidth) >>1;

		x = x-offset;
		
		finer("Clicked in small image "+x+" : "+y);
		
		if(x < 0 || x > iconWidth){
			return; // out of bounds of icon
		}
		
		if(y > selectedData.getSmallIcon().getIconHeight()){
			return; // out of image bounds in cell
		}

		// Translate coordinates back to large image
		double factor = selectedData.getFactor();
		
		double largeX = x * factor;
		double largeY = y * factor;
		
		IPoint p = IPoint.makeNew(largeX, largeY);
		finer("Clicked in large image "+p.toString());
		
		// See if the selected position in the large icon is in a nucleus
		
		for(ICell c : openCells){
			if(c.getNucleus().containsOriginalPoint( p )){
				
				respondToMouseEvent(e, c);
				fine("Click is in nucleus");
				
				
				
				drawNucleus(c, selectedData.getLargeIcon().getImage());
				// Update the small icon
				selectedData.setSmallIcon( new ImageIcon(scaleImage( selectedData.getLargeIcon() )) );
				table.repaint(cellRectangle);
				return; // don't keep searching

			}
			
		}
	}
	
	private void drawNucleus(ICell c, Image image){
		
		Graphics2D g2 = (Graphics2D) image.getGraphics();
		
		Color oldColor = g2.getColor();
		g2.setColor(chooseNucleusOutlineColor(c));
		
		Shape p = c.getNucleus().toOriginalShape();
		
		g2.fill(p);
		g2.setColor(oldColor);
		
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
	
	private Color chooseNucleusOutlineColor(ICell c){
		Color color = Color.BLUE;
		ColourSwatch swatch = GlobalOptions.getInstance().getSwatch();
		if(selectedNucleiLeft.contains(c.getId())){

			if(swatch.equals(ColourSwatch.ACCESSIBLE_SWATCH)){
				color = Color.CYAN;
			} else {
				color = Color.GREEN;
			}
		}
		if(selectedNucleiRight.contains(c.getId())){
			if(swatch.equals(ColourSwatch.ACCESSIBLE_SWATCH)){
				color = Color.YELLOW;
			} else {
				color = Color.RED;
			}
		}
		
		return color;
	}
	
	/**
	 * Get a list of CellCollections, containing the selected nuclei.
	 * If no nuclei were selected, the list is empty
	 * @return 
	 * @throws Exception 
	 */
	public List<ICellCollection> getSubCollections() {
		List<ICellCollection> result = new ArrayList<ICellCollection>(0);
		
		if(!selectedNucleiLeft.isEmpty()){
			ICellCollection subCollectionLeft  = new VirtualCellCollection(dataset, "SubCollectionLeft");
			for(UUID id : selectedNucleiLeft){
				ICell cell = dataset.getCollection().getCell(id);
				subCollectionLeft.addCell(cell);
			}
			result.add(subCollectionLeft);
		}
		
		if(!selectedNucleiRight.isEmpty()){
			ICellCollection subCollectionRight  = new VirtualCellCollection(dataset, "SubCollectionRight");
			for(UUID id : selectedNucleiRight){
				ICell cell = dataset.getCollection().getCell(id);
				subCollectionRight.addCell(cell);
			}
			result.add(subCollectionRight);
		}
		return result;
	}
	
	private synchronized void respondToMouseEvent(MouseEvent e, ICell c){
		
		
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
}