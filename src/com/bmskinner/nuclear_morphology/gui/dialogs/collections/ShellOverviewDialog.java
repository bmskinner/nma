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
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;

import com.bmskinner.nuclear_morphology.analysis.image.ImageAnnotator;
import com.bmskinner.nuclear_morphology.analysis.image.ImageFilterer;
import com.bmskinner.nuclear_morphology.analysis.signals.shells.ShellAnalysisMethod.ShellAnalysisException;
import com.bmskinner.nuclear_morphology.analysis.signals.shells.ShellDetector;
import com.bmskinner.nuclear_morphology.analysis.signals.shells.ShellDetector.Shell;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult.ShrinkType;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalCollection;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.gui.components.SelectableCellIcon;
import com.bmskinner.nuclear_morphology.io.ImageImportWorker;
import com.bmskinner.nuclear_morphology.io.Io;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;

import ij.ImagePlus;
import ij.io.FileSaver;
import ij.process.ImageProcessor;

/**
 * Display all nuclei in the collection with signals and shell overlays
 * @author ben
 * @since 1.13.7
 *
 */
public class ShellOverviewDialog extends CollectionOverviewDialog {
	
	private static final String HEADER_LBL = "Double click a nucleus to export image to ";
	
	public ShellOverviewDialog(IAnalysisDataset dataset) {
        super(dataset);
    }

	@Override
	protected void createWorker(){
		worker = new ShellAnnotationWorker(dataset, table.getModel(), false);
        worker.addPropertyChangeListener(this);
        worker.execute();
	}
	
	@Override
	protected JPanel createHeader(){
		JPanel header = new JPanel(new FlowLayout());
		header.add(new JLabel(HEADER_LBL + dataset.getCollection().getOutputFolder().getAbsolutePath()));
        return header;
		
	}

	@Override
	protected void createUI() {

        this.setLayout(new BorderLayout());
        this.setTitle("Showing " + dataset.getCollection().size() + " cells in " + dataset.getName());

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
        		if (e.getClickCount() == 2) {
        			Point pnt = e.getPoint();
        			int row = table.rowAtPoint(pnt);
        			int col = table.columnAtPoint(pnt);                    
        			export(model.getCell(rows,  col));
        		}
        	}
        });

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(table);

        getContentPane().add(scrollPane, BorderLayout.CENTER);

    }
	
	private void export(ICell cell) {
		ImageProcessor full = renderFullImage(cell);

		File folder = dataset.getCollection().getOutputFolder();
		File outputfile = new File(folder,  cell.getNucleus().getNameAndNumber()+Io.TIFF_FILE_EXTENSION);

		FileSaver saver = new FileSaver(new ImagePlus("", full));
		saver.saveAsTiff(outputfile.getAbsolutePath());
	}
	
	private ImageProcessor renderFullImage(ICell c){
    	ImageProcessor ip;

        try {
            if (c.hasCytoplasm()) {
                ip = c.getCytoplasm().getComponentRGBImage();
            } else {
                ip = c.getNucleus().getComponentImage();
            }
        } catch (UnloadableImageException e) {
            stack("Cannot load image for component", e);
            return null;
        }
        
        if(!dataset.getCollection().getSignalManager().hasShellResult())
            return ip;

        
        int shellCount = dataset.getCollection().getSignalManager().getShellCount();
        if(shellCount==0) 
            fine("No shells present, cannot draw");

        ShrinkType t = dataset.getCollection().getSignalManager().getShrinkType().get();

        ImageAnnotator an = new ImageAnnotator(ip);

        for (Nucleus n : c.getNuclei()) {

        	 try {
        		
 				List<Shell> shells = new ShellDetector(n, shellCount, t, false).getShells();
 				
 				for(Shell shell : shells){
 					fine("Drawing shell at "+shell.getBase().toString());
 					an = an.annotate(shell, Color.ORANGE);
 				}
 			} catch (ShellAnalysisException e1) {
 				warn("Error making shells");
 				stack(e1.getMessage(), e1);
 			}
        	 
        	 ISignalCollection signalCollection = n.getSignalCollection();
        	 for (UUID id : signalCollection.getSignalGroupIds()) {

        		 Optional<Color> col = dataset.getCollection().getSignalGroup(id).get().getGroupColour(); 
        		 an = an.annotateSignal(n, id, col.orElse(Color.YELLOW));	                 
        	 }
        }
        
        ip = an.toProcessor();

        return ip;
    }
	
	public class ShellAnnotationWorker extends ImageImportWorker {

	    public ShellAnnotationWorker(IAnalysisDataset dataset, TableModel model, boolean rotate) {
	        super(dataset, model, rotate);
	    }

	    @Override
	    protected SelectableCellIcon importCellImage(ICell c) {
	        ImageProcessor ip = renderFullImage(c);

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

	        return new SelectableCellIcon(ip, c);
	    }
	}

}
