package gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import analysis.AnalysisDataset;
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
		
		ImageImportWorker worker = new ImageImportWorker(dataset, table.getModel());
		worker.addPropertyChangeListener(this);
		
		worker.execute();
		
		this.setModal(false);
		this.pack();
		this.setVisible(true);
	}
	
	private void createUI(){
		
		this.setLayout(new BorderLayout());
		this.setTitle("Nuclei in "+dataset.getName());
		
		int cellCount = dataset.getCollection().cellCount();
		
		int remainder = cellCount % COLUMN_COUNT==0 ? 0 : 1;
		
		int rows = cellCount / COLUMN_COUNT + remainder;
		
		progressBar = new JProgressBar();
		JPanel header = new JPanel(new FlowLayout());
		header.add(progressBar);
		
		getContentPane().add( header , BorderLayout.NORTH);
		
		
		TableModel model = createEmptyTableModel(rows, COLUMN_COUNT);
		
		table = new JTable( model )
        {
            //  Returning the Class of each column will allow different
            //  renderers to be used based on Class
            public Class getColumnClass(int column){
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

        
        ListSelectionModel cellSelectionModel = table.getSelectionModel();
        cellSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        cellSelectionModel.addListSelectionListener(new ListSelectionListener() {
          public void valueChanged(ListSelectionEvent e) {
            String selectedData = null;

            int[] selectedRow = table.getSelectedRows();
            int[] selectedColumns = table.getSelectedColumns();

            for (int i = 0; i < selectedRow.length; i++) {
              for (int j = 0; j < selectedColumns.length; j++) {
                selectedData = table.getValueAt(selectedRow[i], selectedColumns[j]).toString();
               
              }
            }
            log(selectedData);
          }

        });
        
        
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(table);

        getContentPane().add( scrollPane , BorderLayout.CENTER);
		
		
		
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
				
				LabelInfo l = new LabelInfo(null, "Loading");
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
	    	
	    	progressBar.setValue(value);
	    	
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

	        return this;
	    }
	}
	


}
