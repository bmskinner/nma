/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nuclear_morphology.gui.dialogs.collections;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;

import com.bmskinner.nuclear_morphology.analysis.image.ImageAnnotator;
import com.bmskinner.nuclear_morphology.analysis.image.ImageFilterer;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.VirtualCellCollection;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.Lobe;
import com.bmskinner.nuclear_morphology.components.nuclei.LobedNucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.INuclearSignalOptions;
import com.bmskinner.nuclear_morphology.gui.components.SelectableCellIcon;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEvent;
import com.bmskinner.nuclear_morphology.io.ImageImportWorker;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;

import ij.process.ImageProcessor;

/**
 * This displays all the nuclei in the given dataset, annotated
 * to show nuclei.
 * 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class CellCollectionOverviewDialog extends CollectionOverviewDialog {
	
	private static final String MAKE_NEW_COLLECTION_LBL = "Make new collection from selected";
	private static final String SELECT_ALL_LBL = "Select all";
	private static final String ROTATE_VERTICAL_LBL = "Rotate vertical";
	private static final String NUCLEAR_SIGNAL_LBL = "Nuclear signal: ";
	private static final int DEGREES_180 = 180;
	private static final int DEGREES_360 = 360;
    public static final int ROW_IMAGE_HEIGHT   = 150;
    
    /**
     * Map ids for e.g. signal groups to a display name.
     * Used in the component selection combobox
     * @author bms41
     * @since 1.15.0
     *
     */
    private class ComponentSelectionItem {
    	public UUID uuid;
    	public String displayName;
    	
    	public ComponentSelectionItem(String s, UUID id) {
    		this.uuid = id;
    		this.displayName = s;
    	}
    	
    	@Override
    	public String toString() {
    		return displayName;
    	}
    }
	

    public CellCollectionOverviewDialog(IAnalysisDataset dataset) {
        super(dataset);
    }

	@Override
	protected void createWorker(){
		worker = new CellImportWorker(dataset, table.getModel(), true, CellularComponent.NUCLEUS);
        worker.addPropertyChangeListener(this);
        worker.execute();
        
        this.addWindowListener(new WindowAdapter() {
        	 @Override
        	    public void windowClosing(WindowEvent e) {
        	        worker.cancel(true);
        	 }
        });
	}
		
	@Override
	protected JPanel createHeader(){
		JPanel header = new JPanel(new FlowLayout());

        JCheckBox rotateBtn = new JCheckBox(ROTATE_VERTICAL_LBL, true);
        
        List<Object> components = new ArrayList<>();
        components.add(CellularComponent.CYTOPLASM);
        components.add(CellularComponent.NUCLEUS);
        for(UUID signalGroupId : dataset.getCollection().getSignalGroupIDs()) {
        	ISignalGroup sg = dataset.getCollection().getSignalGroup(signalGroupId).get();
        	components.add(new ComponentSelectionItem(NUCLEAR_SIGNAL_LBL+sg.getGroupName(), signalGroupId));
        }

        JComboBox<Object> componntBox    = new JComboBox<>(components.toArray(new Object[0]));
        componntBox.setSelectedItem(CellularComponent.NUCLEUS);
        componntBox.addActionListener(e -> {
        	worker.cancel(true);
        	worker.removePropertyChangeListener(this);
            progressBar.setVisible(true);
            progressBar.setValue(0);
            worker = new CellImportWorker(dataset, table.getModel(), rotateBtn.isSelected(), componntBox.getSelectedItem());
            worker.addPropertyChangeListener(this);
            worker.execute();

        });
        
        rotateBtn.addActionListener(e -> {
        	worker.cancel(true);
        	worker.removePropertyChangeListener(this);
            progressBar.setVisible(true);
            progressBar.setValue(0);
            worker = new CellImportWorker(dataset, table.getModel(), rotateBtn.isSelected(), componntBox.getSelectedItem());
            worker.addPropertyChangeListener(this);
            worker.execute();

        });
        
        
        header.add(componntBox);
        header.add(rotateBtn);

        JCheckBox selectAll = new JCheckBox(SELECT_ALL_LBL);
        selectAll.addActionListener(e -> {
            model.setAllSelected(selectAll.isSelected());
            table.repaint();
        });
        header.add(selectAll);

        JButton curateBtn = new JButton(MAKE_NEW_COLLECTION_LBL);
        curateBtn.addActionListener(e ->  makeNewCollection());
        header.add(curateBtn);
        return header;
		
	}

	@Override
	protected void createUI() {

        this.setLayout(new BorderLayout());
        this.setTitle(String.format("Showing %s cells in %s", dataset.getCollection().size(), dataset.getName()));

        int cellCount = dataset.getCollection().size();

        int remainder = cellCount % COLUMN_COUNT == 0 ? 0 : 1;

        int rows = cellCount / COLUMN_COUNT + remainder;

        progressBar = new JProgressBar();
        progressBar.setString(LOADING_LBL);
        progressBar.setStringPainted(true);

        
        JPanel header = createHeader();
        getContentPane().add(header, BorderLayout.NORTH);
        getContentPane().add(progressBar, BorderLayout.SOUTH);
        
        model = new CellCollectionOverviewModel(rows, COLUMN_COUNT);
        
        createTable();
        
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    Point pnt = e.getPoint();
                    int row = table.rowAtPoint(pnt);
                    int col = table.columnAtPoint(pnt);
                    model.toggleSelected(row,  col);
                    table.repaint(table.getCellRect(row, col, true)); // need to trigger otherwise it will only update on the next click
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(table);

        getContentPane().add(scrollPane, BorderLayout.CENTER);

    }

    private void makeNewCollection() {
    	List<ICell> cells = model.getSelected();

        ICellCollection newCollection = new VirtualCellCollection(dataset, dataset.getName() + "_Curated");
        for (ICell c : cells) {
            newCollection.addCell(c);
        }
        log("Added " + cells.size() + " cells to new collection");

        // We don;t want to run a new profiling because this will bugger up the
        // segment patterns of
        // the original cells. We need to copy the segments over as with FISH
        // remapping

        if (cells.size() > 0) {
            dataset.addChildCollection(newCollection);
            List<IAnalysisDataset> list = new ArrayList<>();
            list.add(dataset.getChildDataset(newCollection.getID()));
            fireDatasetEvent(DatasetEvent.COPY_PROFILE_SEGMENTATION, list, dataset);
        }
    }
    
    /**
     * Import the cell images for the curator
     * @author ben
     * @since 1.15.0
     *
     */
    public class CellImportWorker extends ImageImportWorker {
    	
    	private Object component;

	    /**
	     * Constructor
	     * @param dataset the dataset to fetch cells from 
	     * @param model the table to display cells in
	     * @param rotate if true, cells are rotated to vertical
	     * @param component the component of the cell to display
	     */
	    public CellImportWorker(IAnalysisDataset dataset, TableModel model, boolean rotate, Object component) {
	        super(dataset, model, rotate);
	        this.component = component;
	    }

	    private ImageProcessor importCytoplasm(ICell c) throws UnloadableImageException {
	    	if(!c.hasCytoplasm())
	    		return ImageFilterer.createWhiteColorProcessor(150, 150);
	    	ImageProcessor ip = c.getCytoplasm().getComponentRGBImage();
	    	ImageAnnotator an = new ImageAnnotator(ip);
	    	an = an.annotateBorder(c.getCytoplasm(), c.getCytoplasm(), Color.CYAN);
	    	for (Nucleus n : c.getNuclei()) {
	    		an.annotateBorder(n, c.getCytoplasm(), Color.ORANGE);

	    		if (n instanceof LobedNucleus) {

	    			for (Lobe l : ((LobedNucleus) n).getLobes()) {
	    				an.annotateBorder(l, c.getCytoplasm(), Color.RED);
	    				an.annotatePoint(l.getCentreOfMass(), c.getCytoplasm(), Color.GREEN);
	    			}
	    		}
	    	}

	    	ip = an.toProcessor();

	    	if (rotate) {
	    		try {
	    			ip = rotateToVertical(c, ip);
	    		} catch (UnavailableBorderTagException e) {
	    			stack("Unable to rotate", e);
	    		}
	    		ip.flipVertical(); // Y axis needs inverting
	    	}
	    	// Rescale the resulting image
	    	ip = new ImageFilterer(ip).resizeKeepingAspect(150, 150).toProcessor();
	    	return ip;
	    }
	    
	    private ImageProcessor importNucleus(ICell c) throws UnloadableImageException {
	    	ImageProcessor ip = c.getNucleus().getComponentImage();
	    	ImageAnnotator an = new ImageAnnotator(ip);
	    	for (Nucleus n : c.getNuclei()) {
	    		an = an.annotateSegments(n, n);
	    	}
	    	ip = an.toProcessor();

	    	if (rotate) {
	    		try {
	    			ip = rotateToVertical(c, ip);
	    		} catch (UnavailableBorderTagException e) {
	    			stack("Unable to rotate", e);
	    		}
	    		ip.flipVertical(); // Y axis needs inverting
	    	}
	    	// Rescale the resulting image
	    	return new ImageFilterer(ip).resizeKeepingAspect(150, 150).toProcessor();
	    }
	    
	    private ImageProcessor importSignal(UUID signal, ICell c) throws UnloadableImageException, ImageImportException {
	    	
	    	INuclearSignalOptions signalOptions = dataset.getAnalysisOptions().get().getNuclearSignalOptions(signal);
	    	
	    	if(signalOptions==null)
	    		return ImageFilterer.createWhiteColorProcessor(150, 150);
	    	
	    	File signalFile = new File(signalOptions.getFolder(),c.getNucleus().getSourceFileName());
	    	ImageProcessor ip = new ImageImporter(signalFile).importImage(signalOptions.getChannel());
	    	ip.invert();
	    	ImageAnnotator an = new ImageAnnotator(ip);
	    	an.convertToColorProcessor();
	    	an.crop(c.getNucleus());
	    	for (Nucleus n : c.getNuclei()) {
	    		an = an.annotateSegments(n, n);
	    	}
	    	ip = an.toProcessor();

	    	if (rotate) {
	    		try {
	    			ip = rotateToVertical(c, ip);
	    		} catch (UnavailableBorderTagException e) {
	    			stack("Unable to rotate", e);
	    		}
	    		ip.flipVertical(); // Y axis needs inverting
	    	}
	    	// Rescale the resulting image
	    	return new ImageFilterer(ip).resizeKeepingAspect(150, 150).toProcessor();
	    }
	    
	    @Override
		protected SelectableCellIcon importCellImage(ICell c) {
	        ImageProcessor ip = null;

	        try {
	        	
	            if (CellularComponent.CYTOPLASM.equals(component.toString()))
	                ip = importCytoplasm(c);
	                
	            if (CellularComponent.NUCLEUS.equals(component.toString()))
	            	 ip = importNucleus(c);
	            
	            if(component instanceof ComponentSelectionItem) {
	            	UUID signalId = ((ComponentSelectionItem)component).uuid;
	            	ip = importSignal(signalId, c);
	            }

	            
	            return new SelectableCellIcon(ip, c);
	            
	        } catch (UnloadableImageException | ImageImportException e) {
	            stack("Cannot load image for component", e);
	            return new SelectableCellIcon(ImageFilterer.createBlackColorProcessor(150, 150), c);
	        }

	       
	       
	    }
	}


}
