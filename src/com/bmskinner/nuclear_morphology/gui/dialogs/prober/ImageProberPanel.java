package com.bmskinner.nuclear_morphology.gui.dialogs.prober;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import com.bmskinner.nuclear_morphology.analysis.detection.IconCell;
import com.bmskinner.nuclear_morphology.analysis.nucleus.NucleusDetectionWorker;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.ImageProber.IconCellRenderer;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.NucleusDetectionImageProber.NucleusImageType;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.workers.NucleusProberWorker;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * This panel holds a table that shows the detection steps for a given
 * method of detection. Buttons allow stepping through a folder of files.
 * @author ben
 * @since 1.13.4
 */
@SuppressWarnings("serial")
public abstract class ImageProberPanel extends JPanel
	implements Loggable, 
				PropertyChangeListener, 
				ProberReloadEventListener {
		
	public static final int     DEFAULT_COLUMN_COUNT = 2;
	private static final double IMAGE_SCREEN_PROPORTION = 0.90;
	
	private static final String HEADER_LBL = "Objects meeting detection parameters are outlined in yellow; other objects are red. Click an image to view larger version.";
	private static final String FOLDER_LBL = "Showing images in ";
	private static final String PREV_IMAGE_BTN = "Prev";
	private static final String NEXT_IMAGE_BTN = "Next";
	
	private int rowHeight = 200;
	
	protected JProgressBar progressBar;
	
	protected IDetectionOptions options; // the options to detect with

	protected ImageSet imageSet;
	
	protected int rows = 0;
	protected int cols = 2;
	
	protected JTable table; 
	
	protected List<File> imageFiles; // the list of image files
	protected File openImage;	     // the image currently open
	protected int fileIndex = 0; 	 // the index of the open file
	
	private Window parent;
	
	public ImageProberPanel(final Window parent, final IDetectionOptions options, final ImageSet set){
		super();
		
		if(options==null || set==null){
			throw new IllegalArgumentException("Options or image set is null");
		}
		
		this.options = options;
		this.imageSet = set;
		
		setLayout(new BorderLayout());
		

		JPanel headerPanel = createHeader();
		JPanel tablePanel  = createTablePanel();

		JButton nextButton = new JButton(NEXT_IMAGE_BTN);
		nextButton.addActionListener( e ->{
			openImage = getNextImage();
			importAndDisplayImage(openImage);
		});
		
		
		JButton prevButton = new JButton(PREV_IMAGE_BTN);
		prevButton.addActionListener( e ->{
			openImage = getPrevImage();
			importAndDisplayImage(openImage);
		});
		

		this.add(headerPanel, BorderLayout.NORTH);
		this.add(tablePanel,  BorderLayout.CENTER);
		this.add(nextButton,  BorderLayout.EAST);
		this.add(prevButton,  BorderLayout.WEST);
	
	}
	

	/**
	 * Run the prober worker on the currently open image
	 */
	public void run() {
		importAndDisplayImage(openImage);
	}
	
	private JPanel createTablePanel(){
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(new EmptyBorder(5, 5, 5, 5));
		
		table = createImageTable();
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(table);
		
		panel.add(scrollPane, BorderLayout.CENTER);
		
		progressBar = new JProgressBar();
		progressBar.setString("Working...");
		progressBar.setStringPainted(true);
		progressBar.setVisible(false);
		panel.add(progressBar, BorderLayout.SOUTH);
		return panel;
	}
	
	/**
	 * Make the header panel with status label
	 * @return
	 */
	private JPanel createHeader(){
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		
		panel.add( new JLabel(HEADER_LBL));
		panel.add( new JLabel(FOLDER_LBL + options.getFolder().getAbsolutePath()));

		return panel;
	}
	
	/**
	 * Make the table that will hold the images
	 * @return
	 */
	private JTable createImageTable(){
		
		int values = imageSet.size();
		if(values == 1){
			rows = 1;
			cols = 1;
		} else {
			rows = values%DEFAULT_COLUMN_COUNT==0 ? values >> 1 : (values >> 1) + 1;
			cols = DEFAULT_COLUMN_COUNT;
		}

		finer("Creating image panel size "+rows+" by "+cols+" for "+imageSet.size()+" steps");
		
		
		TableModel model = createEmptyTableModel(rows, cols);
		
		table = new JTable( model ) {
            //  Returning the Class of each column will allow different
            //  renderers to be used based on Class
            public Class<?> getColumnClass(int column){
            	return ImageProberTableCell.class;
            }
        };
        
        for(int col=0; col<cols; col++){
        	table.getColumnModel().getColumn(col).setCellRenderer(new IconCellRenderer());
        }
        
        rowHeight =  (int) Math.ceil(((java.awt.Toolkit.getDefaultToolkit().getScreenSize().getHeight() * IMAGE_SCREEN_PROPORTION) / (rows+1)));

		table.setRowHeight(rowHeight);	

        table.setCellSelectionEnabled(true);
        table.setRowSelectionAllowed(false);
        table.setColumnSelectionAllowed(false);
        table.setTableHeader(null);
        
        table.addMouseListener( new MouseAdapter(){
        	
        	@Override
        	public void mouseClicked(MouseEvent e){
        		if(e.getClickCount()==1){
        			
        			// Get the data model for this table
        			TableModel model = (TableModel)table.getModel();
        			
        			Point pnt = e.getPoint();
        			int row = table.rowAtPoint(pnt);
        			int col = table.columnAtPoint(pnt);

        			ImageProberTableCell selectedData = (ImageProberTableCell) model.getValueAt( row, col );

        			if(selectedData.getLargeIcon()!=null){
        				new LargeImageDialog(selectedData, parent);
        			}
        			
        		}
        	}
        	
        });
		
		return table;
		
	}
	
	/**
	 * Import the given file as an image, detect objects and
	 * display the image with annotated  outlines
	 * @param imageFile
	 */
	protected abstract void importAndDisplayImage(File imageFile);
	
	protected TableModel createEmptyTableModel(int rows, int cols){
		DefaultTableModel model = new DefaultTableModel(){
			@Override
			public boolean isCellEditable(int row, int column) { // custom isCellEditable function
				return false;
			}
		};
		
		model.setRowCount(rows);
		model.setColumnCount(cols);
		
		int count = 0;
		int values = imageSet.size();
		
		for(int row=0; row<rows; row++){
			for(int col=0; col<cols; col++){
				ImageProberTableCell cell;
				if(count<values){
					cell = new ImageProberTableCell(null, imageSet.getType(count), true, count);

				} else {
					cell = new ImageProberTableCell(null, null, true, count);
					
				}
				model.setValueAt(cell, row, col);
				count++;
			}
		}

		return model;
	}
	

	
	/**
	 * Get the next image in the file list
	 * @return
	 */
	private File getNextImage(){

		if(fileIndex >= imageFiles.size()-1){
			fileIndex = 0;
		} else {
			fileIndex++;
		}
		File f =  imageFiles.get(fileIndex);
		return f;

	}
	
	/**
	 * Get the previous image in the file list
	 * @return
	 */
	private File getPrevImage(){
		
		if(fileIndex <= 0){
			fileIndex = imageFiles.size()-1;
		} else {
			fileIndex--;
		}

		File f =  imageFiles.get(fileIndex);
		return f;
	}
	
	
	/**
	 * Create a list of image files in the given folder
	 * @param folder
	 */
	protected void createFileList(final File folder){
		
		finest("Generating file list from "+folder.getAbsolutePath());

		
		Thread thr = new Thread(){
			public void run() {
				
				imageFiles = new ArrayList<File>();
				imageFiles = importImages(folder);
				
				if(imageFiles.size()>0){
					openImage = imageFiles.get(fileIndex);
					importAndDisplayImage(openImage);
				} else {
					warn("No images found in folder");
					JOptionPane.showMessageDialog(ImageProberPanel.this,  
							"No images found in folder.", 
							"Nope.",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		};	
		thr.start();

	}
		
	/**
	 * Check each file in the given folder for suitability
	 * If the folder contains folders, check recursively
	 * @param folder the folder to check
	 * @return a list of image files
	 */
	private List<File> importImages(final File folder){

		List<File> files = new ArrayList<File>();

		for (File file :  folder.listFiles()) {

			boolean ok = ImageImporter.checkFile(file); // check file extension

			if(ok){
				files.add(file);
			}
			
			if(file.isDirectory()){
				files.addAll(importImages(file));
			}
		}
		return files;
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		int value = 0;
	    try{
	    	Object newValue = evt.getNewValue();
	    	
	    	if(newValue.getClass().isAssignableFrom(Integer.class)){
	    		value = (int) newValue;
	    		
	    	}
	    	if(value >=0 && value <=100){
	    		progressBar.setValue(value);
//	    		finest("Progress: "+value);
	    		table.repaint();
	    		repaint();
	    	}
	    	
	    	
	    	if(evt.getPropertyName().equals("Finished")){
//				log("Worker signaled finished");
				progressBar.setVisible(false);
				table.repaint();
				repaint();
				
			}
	    	
	    } catch (Exception e){
	    	error("Error getting value from property change", e);
	    }
		
	}
		
	@Override
	public void proberReloadEventReceived(ProberReloadEvent e) {
		importAndDisplayImage(openImage);
	}

	public class IconCellRenderer extends DefaultTableCellRenderer	{
		@Override
		public Component getTableCellRendererComponent(


				JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			try {
				super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

				ImageProberTableCell info = (ImageProberTableCell) value;
				setHorizontalAlignment(JLabel.CENTER);
				setHorizontalTextPosition(JLabel.CENTER);
				setVerticalTextPosition(JLabel.BOTTOM);
				setVerticalAlignment(JLabel.TOP); // image has no offset
				setBackground(Color.WHITE);

				if(info==null){
					setText("");
					return this;
				}

				setText(info.toString());

				if(info.hasSmallIcon()){
					setIcon( info.getSmallIcon() );
				} else {
					setIcon(null);
				}
			}
			catch (Exception e){
				error("Renderer error", e);
			}

			return this;
		}


}
	
	/**
	 * Show images in a non-modal window at IMAGE_SCREEN_PROPORTION of the 
	 * screen width or size
	 *
	 */
	public class LargeImageDialog extends JDialog {
		
		/**
		 * Create a full-scale image for the given key in this ImageProber.
		 * @param key the image to show
		 * @param parent the parent ImageProber window
		 */
		public LargeImageDialog(final ImageProberTableCell cell, Window parent){
			super( parent );
			
			this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
			
			final ImageIcon icon = cell.getLargeIcon();
			
			this.setLayout(new BorderLayout());
			
			
			this.add(new JLabel(icon), BorderLayout.CENTER);

			// Show the scaling factor in the title bar
//			double scale = (double) icon.getIconHeight() / (double) procMap.get(key).getHeight();
//			scale *=100;
//			DecimalFormat df = new DecimalFormat("#0.00"); 
//
//	        this.setTitle(key.toString()+": "+ df.format(scale) +"% scale");
			this.setTitle(cell.toString());
			
	        this.setModal(false);
	        this.setResizable(false);
	        this.pack();
	        this.setLocationRelativeTo(null);
	        this.setVisible(true);
		}
		
	}

	
	
}
