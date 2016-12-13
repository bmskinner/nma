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
package com.bmskinner.nuclear_morphology.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

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
import com.bmskinner.nuclear_morphology.gui.ImageType;
import com.bmskinner.nuclear_morphology.gui.LoadingIconDialog;

@SuppressWarnings("serial")
public abstract class ImageProber extends LoadingIconDialog implements PropertyChangeListener {
	
	public static final int DEFAULT_COLUMN_COUNT = 2;
	private static final double IMAGE_SCREEN_PROPORTION = 0.90;

	private int rowHeight = 200;
	
	protected JProgressBar progressBar;
	

	protected IAnalysisOptions options; // the options to detect with
	protected File            openImage;			// the image currently open

	private ImageType imageType;
	
	protected int rows = 0;
	protected int cols = 2;
	
	protected JTable table; 
	
	private JLabel headerLabel = new JLabel("Objects meeting detection parameters are outlined in yellow; other objects are red. Click an image to view larger version.");
	
	private JButton okButton     = new JButton("Proceed with analysis");
	private JButton cancelButton = new JButton("Revise settings");
	
	private boolean ok = false;
	
	protected List<File> imageFiles;	// the list of image files
	protected int index = 0; 				// the index of the open file
	 	
	/**
	 * Create the dialog.
	 */
	public ImageProber(IAnalysisOptions options, ImageType type, File folder) {
		super();
		if(options==null){
			throw new IllegalArgumentException("Options is null");
		} 

		try{
			this.options = options;
			this.imageType = type;
			
			createGUI();
			
			this.setLocationRelativeTo(null); // centre on screen
			
			this.setModal(true);

		} catch(Exception e){
			error("Error creating image prober", e);
		}
	}
	
	private void createGUI(){

		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			
			public void windowClosing(WindowEvent e) {
				ok = false;
				finest("Set ok to "+ok);
				ImageProber.this.setVisible(false);
			}


		});
		
		this.setTitle("Image Prober");

		Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
		
		int w = (int) (screenSize.getWidth() * IMAGE_SCREEN_PROPORTION);
//		windowWidth = w;
		int h = (int) (screenSize.getHeight() * IMAGE_SCREEN_PROPORTION);
//		windowHeight = h;

		setBounds(100, 100, w, h);
		
	
		setLayout(new BorderLayout());
		

		JPanel header = this.createHeader();
		this.add(header, BorderLayout.NORTH);

		JPanel footer = this.createFooter();
		this.add(footer, BorderLayout.SOUTH);

		
		table = createImageTable();
		
		JPanel centralPanel = new JPanel(new BorderLayout());
		centralPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(table);
		
		centralPanel.add(scrollPane, BorderLayout.CENTER);
		
		progressBar = new JProgressBar();
		progressBar.setString("Working...");
		progressBar.setStringPainted(true);
		progressBar.setVisible(false);
		centralPanel.add(progressBar, BorderLayout.SOUTH);

		
		this.add(centralPanel, BorderLayout.CENTER);


		JButton nextButton = new JButton("Next");
		nextButton.addActionListener( e ->{
			openImage = getNextImage();

			try{
				finest("Opening image "+openImage.getAbsolutePath());
				importAndDisplayImage(openImage);
			} catch(Exception ex){
				error("Error opening image", ex);
			}

		});
		
		
		JButton prevButton = new JButton("Prev");
		prevButton.addActionListener( e ->{
			openImage = getPrevImage();

			try{
				finest("Opening image "+openImage.getAbsolutePath());
				importAndDisplayImage(openImage);
			} catch(Exception ex){
				error("Error opening image", ex);
			}

		});


		this.add(nextButton, BorderLayout.EAST);
		this.add(prevButton, BorderLayout.WEST);

	}
	
	/**
	 * Make the header panel with status label
	 * @return
	 */
	private JPanel createHeader(){
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());

		this.setLoadingLabelText("Examining input folders...");
		this.setStatusLoading();
		
		panel.add(headerLabel, BorderLayout.NORTH);

		panel.add(this.getLoadingLabel(), BorderLayout.SOUTH);

		return panel;
	}
	

	
	/**
	 * Update the heading text
	 * @param s
	 */
	protected void setHeaderText(String s){
		this.headerLabel.setText(s);
	}
	
	private JTable createImageTable(){
		
		int values = imageType.getValues().length;
		if(values == 1){
			rows = 1;
			cols = 1;
		} else {
			rows = values%DEFAULT_COLUMN_COUNT==0 ? values >> 1 : (values >> 1) + 1;
			cols = DEFAULT_COLUMN_COUNT;
		}

		finer("Creating image panel size "+rows+" by "+cols+" for "+imageType.getValues().length+" steps");
		
		
		
		TableModel model = createEmptyTableModel(rows, cols);
		
		table = new JTable( model ) {
            //  Returning the Class of each column will allow different
            //  renderers to be used based on Class
            public Class<?> getColumnClass(int column){
            	return IconCell.class;
            }
        };
        
        for(int col=0; col<cols; col++){
        	table.getColumnModel().getColumn(col).setCellRenderer(new IconCellRenderer());
        }
        
//		int rowHeight = 200;
        rowHeight =  (int) Math.ceil(((java.awt.Toolkit.getDefaultToolkit().getScreenSize().getHeight() * IMAGE_SCREEN_PROPORTION) / (rows+1)));
//		int rowHeight =  (int) (windowHeight / (rows+1));
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

        			IconCell selectedData = (IconCell) model.getValueAt( row, col );

        			if(selectedData.getLargeIcon()!=null){
        				new LargeImageDialog(selectedData, ImageProber.this);
        			}
        			
        		}
        	}
        	
        });
		
		return table;
		
	}
	
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
		int values = imageType.getValues().length;
		
		for(int row=0; row<rows; row++){
			for(int col=0; col<cols; col++){
				
//				IconCell cell = new IconCell(null, imageType);
				
				if(count<values){
				
					IconCell cell = new IconCell(null, imageType.getValues()[count++]);

					model.setValueAt(cell, row, col);
				} else {
					IconCell cell = new IconCell(null, null);
					model.setValueAt(cell, row, col);
				}
			}
		}

		return model;
	}
	

	/**
	 * Make the footer panel, with ok and cancel buttons
	 * @return
	 */
	private JPanel createFooter(){
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.RIGHT));
				

		okButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				ImageProber.this.ok = true;
				log(Level.FINEST, "Set ok to "+ok);
				ImageProber.this.setVisible(false);

			}
		});
		panel.add(okButton);

		getRootPane().setDefaultButton(okButton);

		cancelButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {

				ImageProber.this.ok = false;
				log(Level.FINEST, "Set ok to "+ok);
				ImageProber.this.setVisible(false);

			}
		});
		panel.add(cancelButton);


		return panel;
	}
	
	/**
	 * Change the button text on the OK button
	 * @param s
	 */
	protected void setOKButtonText(String s){
		okButton.setText(s);
	}
	
	/**
	 * Change the button text on the cancel button
	 * @param s
	 */
	protected void setCancelButtonText(String s){
		cancelButton.setText(s);
	}

	
	/**
	 * Get if the downstream analysis is ok to run,
	 * or if the dialog has been cancelled
	 * @return
	 */
	public boolean getOK(){
		return this.ok;
	}

	/**
	 * Get the next image in the file list
	 * @return
	 */
	private File getNextImage(){

		if(index >= imageFiles.size()-1){
			index = 0;
		} else {
			index++;
		}
		File f =  imageFiles.get(index);
		return f;

	}
	
	/**
	 * Get the previous image in the file list
	 * @return
	 */
	private File getPrevImage(){
		
		if(index <= 0){
			index = imageFiles.size()-1;
		} else {
			index--;
		}

		File f =  imageFiles.get(index);
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
					openImage = imageFiles.get(index);
					importAndDisplayImage(openImage);
				} else {
					warn("No images found in folder");
					JOptionPane.showMessageDialog(ImageProber.this,  
							"No images found in folder.", 
							"Nope.",
							JOptionPane.ERROR_MESSAGE);
					
					ImageProber.this.ok = false;
					ImageProber.this.setVisible(false);
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
	private List<File> importImages(File folder){

		List<File> files = new ArrayList<File>();

		for (File file :  folder.listFiles()) {

			boolean ok = NucleusDetectionWorker.checkFile(file); // check file extension

			if(ok){
				files.add(file);
			}
			
			if(file.isDirectory()){
				files.addAll(importImages(file));
			}
		}
		return files;
	}
		
		
	/**
	 * Import the given file as an image, detect objects and
	 * display the image with annotated  outlines
	 * @param imageFile
	 */
	protected abstract void importAndDisplayImage(File imageFile);
	
	
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
				this.setLoadingLabelText("Showing nuclei in "+openImage.getAbsolutePath());
				this.setStatusLoaded();
				table.repaint();
				repaint();
				
			}
	    	
	    } catch (Exception e){
	    	error("Error getting value from property change", e);
	    }
		
	}
	

	public class IconCellRenderer extends DefaultTableCellRenderer	{
	    @Override
	    public Component getTableCellRendererComponent(
	        JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
	        
	    	super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

	        IconCell info = (IconCell) value;
	        setHorizontalAlignment(JLabel.CENTER);
	        setHorizontalTextPosition(JLabel.CENTER);
	        setVerticalTextPosition(JLabel.BOTTOM);
	        setVerticalAlignment(JLabel.TOP); // image has no offset
	        setBackground(Color.WHITE);
	        
	        if(info==null){
	        	setText("");
	        	return this;
	        }
	        
	        if(info.hasType()){
	        	setText(info.toString());
	        } else {
	        	setText("");
	        }
	        
	        if(info.hasSmallIcon()){
	        	setIcon( info.getSmallIcon() );
	        } else {
	        	setIcon(null);
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
		public LargeImageDialog(final IconCell cell, Window parent){
			super(parent);
			
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
			this.setTitle(cell.getType().toString());
			
	        this.setModal(false);
	        this.setResizable(false);
	        this.pack();
	        this.setLocationRelativeTo(null);
	        this.setVisible(true);
		}
		
//		private ImageIcon scale(ImageIcon icon){
//			
//			Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
//			
//			int w = (int) (screenSize.getWidth() * IMAGE_SCREEN_PROPORTION);
//			int h = (int) (screenSize.getHeight() * IMAGE_SCREEN_PROPORTION);
//			
//			int width = icon.getIconWidth() > w ? w : icon.getIconWidth();
//			int height = 
//			
//			if(icon.getIconHeight()>)
//			
//			return image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
//			
//		}
		
	}
	
}
