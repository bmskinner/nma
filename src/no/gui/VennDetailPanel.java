package no.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableModel;

import no.analysis.AnalysisDataset;
import datasets.NucleusTableDatasetCreator;

public class VennDetailPanel extends DetailPanel {


	private static final long serialVersionUID = 1L;
	
	private JTable vennTable;

	public VennDetailPanel() {
		
		this.setLayout(new BorderLayout());
		
		vennTable = new JTable(NucleusTableDatasetCreator.createVennTable(null));
		this.add(vennTable, BorderLayout.CENTER);
		vennTable.setEnabled(false);
		this.add(vennTable.getTableHeader(), BorderLayout.NORTH);

	}
	
	/**
	 * Update the venn panel with data from the given datasets
	 * @param list the datasets
	 */
	public void update(final List<AnalysisDataset> list){
		
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
			
				// format the numbers and make into a tablemodel
				TableModel model = NucleusTableDatasetCreator.createVennTable(null);
				
				if(!list.isEmpty() && list!=null){
					model = NucleusTableDatasetCreator.createVennTable(list);
				}
				vennTable.setModel(model);
				
				int columns = vennTable.getColumnModel().getColumnCount();

				if(columns>1){
					for(int i=1;i<columns;i++){
						vennTable.getColumnModel().getColumn(i).setCellRenderer(new VennTableCellRenderer());
					}
				}
		}});
	}
	
	/**
	 * Colour table cell background to show pairwise comparisons. All cells are white, apart
	 * from the diagonal, which is made light grey
	 */
	class VennTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer {

		private static final long serialVersionUID = 1L;

		public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, java.lang.Object value, boolean isSelected, boolean hasFocus, int row, int column) {
	        
	      //Cells are by default rendered as a JLabel.
	        JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

	        String cellContents = l.getText();
	        if(cellContents!=null && !cellContents.equals("")){ // ensure value
//	        	IJ.log(cellContents);
	        	String[] array = cellContents.split("%");
//	        	 IJ.log(array[0]);
		        String[] array2 = array[0].split("\\(");
//		        IJ.log(array2[1]);
		        double pct = Double.valueOf(array2[1]);
		        
//		        IJ.log("Pct: "+pct);
		        double colourIndex = 255 - ((pct/100) * 255);
		        
		        Color colour = new Color((int) colourIndex,(int) colourIndex, 255);
		        l.setBackground(colour);
		        
		        if(pct>60){
		        	l.setForeground(Color.WHITE);
		        } else {
		        	l.setForeground(Color.black);
		        }
		        
	        } else {
	            l.setBackground(Color.LIGHT_GRAY);
	        }

	      //Return the JLabel which renders the cell.
	      return l;
	    }
	}

}
