package com.bmskinner.nuclear_morphology.gui.dialogs.collections;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;
import java.util.UUID;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableModel;

import com.bmskinner.nuclear_morphology.analysis.image.ImageAnnotator;
import com.bmskinner.nuclear_morphology.analysis.signals.ShellAnalysisException;
import com.bmskinner.nuclear_morphology.analysis.signals.ShellDetector;
import com.bmskinner.nuclear_morphology.analysis.signals.ShellDetector.Shell;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalCollection;
import com.bmskinner.nuclear_morphology.components.nuclear.UnavailableSignalGroupException;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.gui.tabs.cells_detail.LabelInfo;
import com.bmskinner.nuclear_morphology.io.ImageImportWorker;
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

	protected void createWorker(){
		worker = new ShellAnnotationWorker(dataset, table.getModel(), false);
        worker.addPropertyChangeListener(this);
        worker.execute();
	}
	
	protected JPanel createHeader(){
		JPanel header = new JPanel(new FlowLayout());
		header.add(new JLabel(HEADER_LBL + dataset.getCollection().getOutputFolder().getAbsolutePath()));
        return header;
		
	}

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

        TableModel model = createEmptyTableModel(rows, COLUMN_COUNT);

        table = new JTable(model) {
            // Returning the Class of each column will allow different
            // renderers to be used based on Class
            public Class<?> getColumnClass(int column) {
                return JLabel.class;
            }
        };

        for (int col = 0; col < COLUMN_COUNT; col++) {
            table.getColumnModel().getColumn(col).setCellRenderer(new LabelInfoRenderer());
        }

        table.setRowHeight(180);
        table.setCellSelectionEnabled(true);
        table.setRowSelectionAllowed(false);
        table.setColumnSelectionAllowed(false);
        table.setTableHeader(null);

        ListSelectionModel cellSelectionModel = table.getSelectionModel();
        cellSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        table.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {

                    // Get the data model for this table
                    TableModel model = (TableModel) table.getModel();

                    Point pnt = e.getPoint();
                    int row = table.rowAtPoint(pnt);
                    int col = table.columnAtPoint(pnt);

                    LabelInfo selected = (LabelInfo) model.getValueAt(row, col);
                    
                    ICell c = selected.getCell();
                    
                    ImageProcessor full = renderFullImage(c);

                    File folder = dataset.getCollection().getOutputFolder();
                    File outputfile = new File(folder,  c.getNucleus().getNameAndNumber()+".tiff");

                    FileSaver saver = new FileSaver(new ImagePlus("", full));
                    saver.saveAsTiff(outputfile.getAbsolutePath());
                }
            }
            
            

        });

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(table);

        getContentPane().add(scrollPane, BorderLayout.CENTER);

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

        ImageAnnotator an = new ImageAnnotator(ip);

        for (Nucleus n : c.getNuclei()) {

        	 try {
        		
 				List<Shell> shells = new ShellDetector(n, ShellDetector.DEFAULT_SHELL_COUNT).getShells();
 				
 				for(Shell shell : shells){
 					fine("Drawing shell at "+shell.getBase().toString());
 					an = an.annotate(shell, Color.ORANGE);
 				}
 			} catch (ShellAnalysisException e1) {
 				warn("Error making shells");
 				stack(e1.getMessage(), e1);
 			}
        	 
        	 ISignalCollection signalCollection = n.getSignalCollection();
             for (UUID id : signalCollection.getSignalGroupIDs()) {
             	     
				try {
					Color colour = dataset.getCollection().getSignalGroup(id).getGroupColour();
					an = an.annotateSignal(n, id, colour);
				} catch (UnavailableSignalGroupException e) {
					stack("No signal group", e);
				}	                 
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
	    protected ImageIcon importCellImage(ICell c) {
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
	        ip = scaleImage(ip);

	        ImageIcon ic = new ImageIcon(ip.getBufferedImage());
	        return ic;
	    }
	}

}
