package gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import analysis.AnalysisDataset;
import components.Cell;
import components.CellCollection;
import gui.DatasetEvent;
import gui.LoadingIconDialog;
import gui.tabs.cells.LabelInfo;
import io.ImageImportWorker;

/**
 * This displays all the nuclei in the given dataset
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class CellCollectionOverviewDialog extends LoadingIconDialog implements PropertyChangeListener {
	
	public static final int COLUMN_COUNT = 3;
	
	private AnalysisDataset dataset;
	private JTable table;
	private JProgressBar progressBar;
	
	
	
	public CellCollectionOverviewDialog(AnalysisDataset dataset){
		super();
		this.dataset = dataset;
		
		createUI();
		
		ImageImportWorker worker = new ImageImportWorker(dataset, table.getModel(), true);
		worker.addPropertyChangeListener(this);
		
		worker.execute();
		
		this.setModal(false);
		this.pack();
		this.setVisible(true);
	}
	
	private void createUI(){
		
		this.setLayout(new BorderLayout());
		this.setTitle("Showing "+dataset.getCollection().cellCount()+" nuclei in "+dataset.getName());
		
		int cellCount = dataset.getCollection().cellCount();
		
		int remainder = cellCount % COLUMN_COUNT==0 ? 0 : 1;
		
		int rows = cellCount / COLUMN_COUNT + remainder;
		
		progressBar = new JProgressBar();
		progressBar.setString("Loading");
		progressBar.setStringPainted(true);
		
		JPanel header = new JPanel(new FlowLayout());
		
		JCheckBox rotateBtn = new JCheckBox("Rotate vertical", true);
		rotateBtn.addActionListener( e-> {
			progressBar.setVisible(true);
			ImageImportWorker worker = new ImageImportWorker(dataset, table.getModel(), rotateBtn.isSelected());
			worker.addPropertyChangeListener(this);
			
			worker.execute();
		
		});
		header.add(rotateBtn);
		
		JCheckBox selectAll = new JCheckBox("Select all");
		selectAll.addActionListener( e-> {
			
			boolean b = selectAll.isSelected();
			for(int r=0; r<table.getModel().getRowCount(); r++){

				for(int c=0; c<table.getModel().getColumnCount(); c++){
					LabelInfo info = (LabelInfo) table.getModel().getValueAt(r, c);
					
					info.setSelected(b);
				}
			}
			table.repaint();
		
		});
		header.add(selectAll);

		
		JButton curateBtn = new JButton("Make new collection from selected");
		curateBtn.addActionListener( e ->{
			
			makeNewCollection();
		
		});
		
		header.add(curateBtn);
		
		getContentPane().add( header , BorderLayout.NORTH);
		getContentPane().add( progressBar , BorderLayout.SOUTH);
		
		
		TableModel model = createEmptyTableModel(rows, COLUMN_COUNT);
		
		table = new JTable( model )
        {
            //  Returning the Class of each column will allow different
            //  renderers to be used based on Class
            public Class<?> getColumnClass(int column){
            	return JLabel.class;
            }
        };
        
        for(int col=0; col<COLUMN_COUNT; col++){
        	table.getColumnModel().getColumn(col).setCellRenderer(new LabelInfoRenderer());
        }
        
        table.setRowHeight(180);
        table.setCellSelectionEnabled(true);
        table.setRowSelectionAllowed(false);
        table.setColumnSelectionAllowed(false);
        table.setTableHeader(null);

        
        ListSelectionModel cellSelectionModel = table.getSelectionModel();
        cellSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        table.addMouseListener( new MouseAdapter(){
        	
        	@Override
        	public void mouseClicked(MouseEvent e){
        		if(e.getClickCount()==1){
        			
        			// Get the data model for this table
        			TableModel model = (TableModel)table.getModel();
        			
        			Point pnt = e.getPoint();
        			int row = table.rowAtPoint(pnt);
        			int col = table.columnAtPoint(pnt);

        			LabelInfo selectedData = (LabelInfo) model.getValueAt( row, col );

        			selectedData.setSelected( !selectedData.isSelected() );
        			        			
        			table.repaint();
        			
        		}
        	}
        	
        });
      
        
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(table);

        getContentPane().add( scrollPane , BorderLayout.CENTER);
		
		
		
	}
	
	private void makeNewCollection(){
		List<Cell> cells = new ArrayList<Cell>();
		for(int r=0; r<table.getModel().getRowCount(); r++){

			for(int c=0; c<table.getModel().getColumnCount(); c++){
				LabelInfo info = (LabelInfo) table.getModel().getValueAt(r, c);
				
				if(info.isSelected() && info.getCell()!=null){
					cells.add(info.getCell());
				}
			}
		}
		
		CellCollection newCollection = new CellCollection(dataset, dataset.getName()+"_Curated");
		for(Cell c : cells){
			newCollection.addCell(new Cell(c));
		}
		log("Added "+cells.size()+" cells to new collection");
		
		if(cells.size()>0){
			dataset.addChildCollection(newCollection);
			
			List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
			list.add(dataset.getChildDataset(newCollection.getID()));
			log("Firing dataset events");
			fireDatasetEvent(DatasetEvent.PROFILING_ACTION, list);
		}
	}
	
	private TableModel createEmptyTableModel(int rows, int cols){
		DefaultTableModel model = new DefaultTableModel(){
			@Override
			public boolean isCellEditable(int row, int column) { // custom isCellEditable function
				return false;
			}
		};
		
		model.setRowCount(rows);
		model.setColumnCount(cols);
		
		for(int row=0; row<rows; row++){
			for(int col=0; col<cols; col++){
				
				LabelInfo l = new LabelInfo(null, null);
				model.setValueAt(l, row, col);
			}
		}

		return model;
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
	    	}
	    	
	    	
	    	if(evt.getPropertyName().equals("Finished")){
				finest("Worker signaled finished");
				progressBar.setVisible(false);
				
			}
	    	
	    } catch (Exception e){
	    	error("Error getting value from property change", e);
	    }
		
	}
	
//	@SuppressWarnings("serial")
	public class LabelInfoRenderer extends DefaultTableCellRenderer	{
	    @Override
	    public Component getTableCellRendererComponent(
	        JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
	        
	    	super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

	        LabelInfo info = (LabelInfo)value;
	        setIcon( info.getIcon() );
	        setHorizontalAlignment(JLabel.CENTER);
	        setHorizontalTextPosition(JLabel.CENTER);
	        setVerticalTextPosition(JLabel.BOTTOM);
	        
	        if(info.isSelected()){
	        	setBackground(Color.GREEN);
	        } else {
	        	setBackground(Color.WHITE);
	        }
	        

	        return this;
	    }
	}
	


}
