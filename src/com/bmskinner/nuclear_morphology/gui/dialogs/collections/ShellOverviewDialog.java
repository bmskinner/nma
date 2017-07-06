package com.bmskinner.nuclear_morphology.gui.dialogs.collections;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableModel;

import com.bmskinner.nuclear_morphology.analysis.IAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.image.ImageAnnotator;
import com.bmskinner.nuclear_morphology.analysis.signals.ShellAnalysisException;
import com.bmskinner.nuclear_morphology.analysis.signals.ShellDetector;
import com.bmskinner.nuclear_morphology.analysis.signals.ShellDetector.Shell;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.Imageable;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.gui.tabs.cells_detail.LabelInfo;
import com.bmskinner.nuclear_morphology.io.ImageImportWorker;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.gui.Roi;
import ij.process.Blitter;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

/**
 * Display all nuclei in the collection with shell overlays
 * @author ben
 * @since 1.13.7
 *
 */
public class ShellOverviewDialog extends CollectionOverviewDialog {
	
	private static final int DEGREES_180 = 180;
	private static final int DEGREES_360 = 360;
	
	public ShellOverviewDialog(IAnalysisDataset dataset) {
        super(dataset);
    }

	protected void createWorker(){
		worker = new ShellAnnotationWorker(dataset, table.getModel(), true);
        worker.addPropertyChangeListener(this);
        worker.execute();
	}
	
	protected JPanel createHeader(){
		JPanel header = new JPanel(new FlowLayout());
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
                if (e.getClickCount() == 1) {

                    // Get the data model for this table
                    TableModel model = (TableModel) table.getModel();

                    Point pnt = e.getPoint();
                    int row = table.rowAtPoint(pnt);
                    int col = table.columnAtPoint(pnt);

                    LabelInfo selectedData = (LabelInfo) model.getValueAt(row, col);

                    selectedData.setSelected(!selectedData.isSelected());

                    table.repaint();

                }
            }

        });

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(table);

        getContentPane().add(scrollPane, BorderLayout.CENTER);

    }
	
	public class ShellAnnotationWorker extends ImageImportWorker implements Loggable {

//	    private final IAnalysisDataset dataset;
//	    private final TableModel       model;
//	    private static final int       COLUMN_COUNT = CellCollectionOverviewDialog.COLUMN_COUNT;
	    private int                    loaded       = 0;
//	    private boolean                rotate;

	    public ShellAnnotationWorker(IAnalysisDataset dataset, TableModel model, boolean rotate) {
	        super(dataset, model, rotate);
//	        this.dataset = dataset;
//	        this.model = model;
//	        this.rotate = rotate;
	    }

	    @Override
	    protected Boolean doInBackground() throws Exception {

	        for (ICell c : dataset.getCollection().getCells()) {

	            try {

	                ImageIcon ic = importCellImage(c);

	                LabelInfo inf = new LabelInfo(ic, c);

	                publish(inf);
	            } catch (Exception e) {
	                error("Error opening cell image", e);
	            }

	        }

	        return true;
	    }

	    @Override
	    public void done() {

	        finest("Worker completed task");

	        try {
	            if (this.get()) {
	                finest("Firing trigger for sucessful task");
	                firePropertyChange("Finished", getProgress(), IAnalysisWorker.FINISHED);

	            } else {
	                finest("Firing trigger for failed task");
	                firePropertyChange("Error", getProgress(), IAnalysisWorker.ERROR);
	            }
	        } catch (InterruptedException e) {
	            error("Interruption error in worker", e);
	            firePropertyChange("Error", getProgress(), IAnalysisWorker.ERROR);
	        } catch (ExecutionException e) {
	            error("Execution error in worker", e);
	            firePropertyChange("Error", getProgress(), IAnalysisWorker.ERROR);
	        }

	    }

	    @Override
	    protected ImageIcon importCellImage(ICell c) {
	        ImageProcessor ip;

	        try {
	            if (c.hasCytoplasm()) {
	                ip = c.getCytoplasm().getComponentRGBImage();
	            } else {
	                ip = c.getNucleus().getComponentImage();
	            }

	            // Nucleus n = c.getNucleus();
	            // ip = n.getComponentImage();

	        } catch (UnloadableImageException e) {
	            stack("Cannot load image for component", e);
	            return new ImageIcon();
	        }

	        ImageAnnotator an = new ImageAnnotator(ip);

	        for (Nucleus n : c.getNuclei()) {

	        	 try {
	        		 //TODO - get existing shell count
	 				List<Shell> shells = new ShellDetector(n, ShellDetector.DEFAULT_SHELL_COUNT).getShells();
	 				
	 				for(Shell shell : shells){
	 					fine("Drawing shell at "+shell.getBase().toString());
	 					an = an.annotate(shell);
	 				}
	 			} catch (ShellAnalysisException e1) {
	 				warn("Error making shells");
	 				stack(e1.getMessage(), e1);
	 			}
	        	 
	        	an = an.annotateSignals(n);
	        }
	        

	        ip = an.toProcessor();

	        // drawNucleus(c, ip);

	        if (rotate) {
	            try {
	                ip = rotateToVertical(c, ip);
	            } catch (UnavailableBorderTagException e) {
	                fine("Unable to rotate", e);
	            }
//	            ip.flipVertical(); // Y axis needs inverting
	        }
	        // Rescale the resulting image
	        ip = scaleImage(ip);

	        ImageIcon ic = new ImageIcon(ip.getBufferedImage());
	        return ic;
	    }

	    @Override
	    protected ImageProcessor rotateToVertical(ICell c, ImageProcessor ip) throws UnavailableBorderTagException {
	        // Calculate angle for vertical rotation
	        Nucleus n = c.getNucleus();

	        IPoint topPoint;
	        IPoint btmPoint;

	        if (!n.hasBorderTag(Tag.TOP_VERTICAL) || !n.hasBorderTag(Tag.BOTTOM_VERTICAL)) {
	            topPoint = n.getCentreOfMass();
	            btmPoint = n.getBorderPoint(Tag.ORIENTATION_POINT);

	        } else {

	            topPoint = n.getBorderPoint(Tag.TOP_VERTICAL);
	            btmPoint = n.getBorderPoint(Tag.BOTTOM_VERTICAL);

	            // Sometimes the points have been set to overlap in older datasets
	            if (topPoint.overlapsPerfectly(btmPoint)) {
	                topPoint = n.getCentreOfMass();
	                btmPoint = n.getBorderPoint(Tag.ORIENTATION_POINT);
	            }
	        }

	        // Find which point is higher in the image
	        IPoint upperPoint = topPoint.getY() > btmPoint.getY() ? topPoint : btmPoint;
	        IPoint lowerPoint = upperPoint == topPoint ? btmPoint : topPoint;

	        IPoint comp = IPoint.makeNew(lowerPoint.getX(), upperPoint.getY());

	        /*
	         * LA RA RB LB
	         * 
	         * T C C T B C C B \ | | / \ | | / B B T T
	         * 
	         * When Ux<Lx, angle describes the clockwise rotation around L needed to
	         * move U above it. When Ux>Lx, angle describes the anticlockwise
	         * rotation needed to move U above it.
	         * 
	         * If L is supposed to be on top, the clockwise rotation must be 180+a
	         * 
	         * However, the image coordinates have a reversed Y axis
	         */

	        double angleFromVertical = lowerPoint.findAngle(upperPoint, comp);

	        double angle = 0;
	        if (topPoint.isLeftOf(btmPoint) && topPoint.isAbove(btmPoint)) {
	            angle = DEGREES_360 - angleFromVertical;
	            // log("LA: "+angleFromVertical+" to "+angle); // Tested working
	        }

	        if (topPoint.isRightOf(btmPoint) && topPoint.isAbove(btmPoint)) {
	            angle = angleFromVertical;
	            // log("RA: "+angleFromVertical+" to "+angle); // Tested working
	        }

	        if (topPoint.isLeftOf(btmPoint) && topPoint.isBelow(btmPoint)) {
	            angle = angleFromVertical + DEGREES_180;
	            // angle = 180-angleFromVertical;
	            // log("LB: "+angleFromVertical+" to "+angle); // Tested working
	        }

	        if (topPoint.isRightOf(btmPoint) && topPoint.isBelow(btmPoint)) {
	            // angle = angleFromVertical+180;
	            angle = DEGREES_180 - angleFromVertical;
	            // log("RB: "+angleFromVertical+" to "+angle); // Tested working
	        }

	        // Increase the canvas size so rotation does not crop the nucleus
	        finer("Input: " + n.getNameAndNumber() + " - " + ip.getWidth() + " x " + ip.getHeight());
	        ImageProcessor newIp = createEnlargedProcessor(ip, angle);

//	        newIp.rotate(angle);
	        return newIp;
	    }

	    @Override
	    protected ImageProcessor createEnlargedProcessor(ImageProcessor ip, double degrees) {

	        double rad = Math.toRadians(degrees);

	        // Calculate the new width and height of the canvas
	        // new width is h sin(a) + w cos(a) and vice versa for height
	        double newWidth = Math.abs(Math.sin(rad) * ip.getHeight()) + Math.abs(Math.cos(rad) * ip.getWidth());
	        double newHeight = Math.abs(Math.sin(rad) * ip.getWidth()) + Math.abs(Math.cos(rad) * ip.getHeight());

	        int w = (int) Math.ceil(newWidth);
	        int h = (int) Math.ceil(newHeight);

	        // The new image may be narrower or shorter following rotation.
	        // To avoid clipping, ensure the image never gets smaller in either
	        // dimension.
	        w = w < ip.getWidth() ? ip.getWidth() : w;
	        h = h < ip.getHeight() ? ip.getHeight() : h;

	        // paste old image to centre of enlarged canvas
	        int xBase = (w - ip.getWidth()) >> 1;
	        int yBase = (h - ip.getHeight()) >> 1;

	        finer("New image " + w + " x " + h + " from " + ip.getWidth() + " x " + ip.getHeight() + " : Rot: " + degrees);

	        finest("Copy starting at " + xBase + ", " + yBase);

	        ImageProcessor newIp = new ColorProcessor(w, h);

	        newIp.setColor(Color.WHITE); // fill current space with white
	        newIp.fill();

	        newIp.setBackgroundValue(16777215); // fill on rotate is RGB int white
	        newIp.copyBits(ip, xBase, yBase, Blitter.COPY);
	        return newIp;
	    }

	    @Override
	    protected ImageProcessor scaleImage(ImageProcessor ip) {
	        double aspect = (double) ip.getWidth() / (double) ip.getHeight();
	        double finalWidth = 150 * aspect; // fix height
	        finalWidth = finalWidth > 150 ? 150 : finalWidth; // but constrain width
	                                                          // too

	        ip = ip.resize((int) finalWidth);
	        return ip;
	    }

	    @Override
	    protected void process(List<LabelInfo> chunks) {

	        for (LabelInfo im : chunks) {

	            int row = loaded / COLUMN_COUNT;
	            int col = loaded % COLUMN_COUNT;
	            // log("Image: "+loaded+" - Row "+row+" col "+col);

	            model.setValueAt(im, row, col);

	            loaded++;
	        }

	        int percent = (int) ((double) loaded / (double) dataset.getCollection().size() * 100);

	        if (percent >= 0 && percent <= 100) {
	            setProgress(percent); // the integer representation of the percent
	        }
	    }
	}

}
