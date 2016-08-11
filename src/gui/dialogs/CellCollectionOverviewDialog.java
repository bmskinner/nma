package gui.dialogs;

import java.awt.BorderLayout;
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
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import analysis.AnalysisDataset;
import gui.LoadingIconDialog;
import io.ImageImportWorker;

@SuppressWarnings("serial")
public class CellCollectionOverviewDialog extends LoadingIconDialog implements PropertyChangeListener {
	
	public static final int COLUMN_COUNT = 2;
	
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
		
		int cellCount = dataset.getCollection().cellCount();
		
		int rows = cellCount / COLUMN_COUNT;
		
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
            	return ImageIcon.class;
            }
        };
        
        table.setRowHeight(200);
        
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(table);
		scrollPane.setColumnHeaderView(table.getTableHeader());
        getContentPane().add( scrollPane , BorderLayout.CENTER);
		
		
		
	}
	
	private TableModel createEmptyTableModel(int rows, int cols){
		DefaultTableModel model = new DefaultTableModel();
		model.setRowCount(rows);
		model.setColumnCount(cols);
		
		for(int row=0; row<rows; row++){
			for(int col=0; col<cols; col++){
				
				JLabel l = new JLabel("Loading");
//				l.setMinimumSize(new Dimension(200, 200));
				model.setValueAt("Loading", row, col);
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

}
