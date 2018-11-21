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
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableModel;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.VirtualCellCollection;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.tabs.cells_detail.LabelInfo;
import com.bmskinner.nuclear_morphology.io.ImageImportWorker;
import com.bmskinner.nuclear_morphology.io.ImageImporter;

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
	private static final int DEGREES_180 = 180;
	private static final int DEGREES_360 = 360;
	
	

    public CellCollectionOverviewDialog(IAnalysisDataset dataset) {
        super(dataset);
    }

	@Override
	protected void createWorker(){
		worker = new ImageImportWorker(dataset, table.getModel(), true);
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
        rotateBtn.addActionListener(e -> {
            progressBar.setVisible(true);
            ImageImportWorker worker = new ImageImportWorker(dataset, table.getModel(), rotateBtn.isSelected());
            worker.addPropertyChangeListener(this);

            worker.execute();

        });
        header.add(rotateBtn);
        
        String[] componentStrings = {  }; // e.g Nucleus,  Nuclear signals. Switch images / channels as needed 
        JComboBox<String> componntBox    = new JComboBox<>(componentStrings);
//        channelBox.addActionListener(e -> );


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

    

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        int value = 0;
        try {
            Object newValue = evt.getNewValue();

            if (newValue.getClass().isAssignableFrom(Integer.class)) {
                value = (int) newValue;

            }
            if (value >= 0 && value <= 100) {
                progressBar.setValue(value);
            }

            if (evt.getPropertyName().equals("Finished")) {
                finest("Worker signaled finished");
                progressBar.setVisible(false);

            }

        } catch (Exception e) {
            error("Error getting value from property change", e);
        }

    }

    

}
