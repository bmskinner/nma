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
package gui.tabs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import charting.datasets.NucleusTableDatasetCreator;
import charting.options.ChartOptions;
import charting.options.TableOptions;
import charting.options.TableOptionsBuilder;
import charting.options.TableOptions.TableType;
import gui.components.ExportableTable;

public class VennDetailPanel extends DetailPanel {


	private static final long serialVersionUID = 1L;
	
	private JPanel mainPanel = new JPanel();
	
	private ExportableTable vennTable;

	public VennDetailPanel() {
		super();
		this.setLayout(new BorderLayout());
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		

		try {
			JPanel vennPanel = new JPanel(new BorderLayout());

			JScrollPane scrollPane = new JScrollPane();
			scrollPane.setViewportView(mainPanel);

			this.add(scrollPane, BorderLayout.CENTER);

			TableOptions options = new TableOptionsBuilder()
			.setDatasets(null)
			.setType(TableType.VENN)
			.build();

			TableModel model = getTable(options);

			vennTable = new ExportableTable(model);
			vennPanel.add(vennTable, BorderLayout.CENTER);
			vennPanel.add(vennTable.getTableHeader(), BorderLayout.NORTH);
			mainPanel.add(vennPanel);
			vennTable.setEnabled(false);
		} catch(Exception e){
			log(Level.SEVERE, "Error updating venn panel", e);
		}
		
	}
	
	@Override
	protected void updateSingle() {
		updateNull();
	}
	

	@Override
	protected void updateMultiple() {
		log(Level.FINE, "Updating venn panel");

		TableOptions options = new TableOptionsBuilder()
			.setDatasets(getDatasets())
			.setType(TableType.VENN)
			.build();



		TableModel model = getTable(options);
		
		vennTable.setModel(model);
		setRenderer(vennTable, new VennTableCellRenderer());

		log(Level.FINEST, "Updated venn panel");
		
	}
	
	@Override
	protected JFreeChart createPanelChartType(ChartOptions options) throws Exception {
		return null;
	}
	
	@Override
	protected void updateNull() {		
		
		TableOptions options = new TableOptionsBuilder()
			.setDatasets(null)
			.setType(TableType.VENN)
			.build();
		
		TableModel model = getTable(options);
		vennTable.setModel(model);
		setRenderer(vennTable, new VennTableCellRenderer());
	}
	
	@Override
	protected TableModel createPanelTableType(TableOptions options) throws Exception{
		return NucleusTableDatasetCreator.getInstance().createVennTable(options);
	}
		
	/**
	 * Colour table cell backsground to show pairwise comparisons. All cells are white, apart
	 * from the diagonal, which is made light grey
	 */
	class VennTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer {

		private static final long serialVersionUID = 1L;

		public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, java.lang.Object value, boolean isSelected, boolean hasFocus, int row, int column) {
	        
	      //Cells are by default rendered as a JLabel.
	        JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

	        String cellContents = l.getText();
	        if(cellContents!=null && !cellContents.equals("")){ // ensure value

	        	String columnName = table.getColumnName(column);
	        	String[] array = cellContents.split("%");
		        String[] array2 = array[0].split("\\(");
		        
		        double pct;
		        try {
		        	
		        	NumberFormat nf = NumberFormat.getInstance();
		        	pct = nf.parse(array2[1]).doubleValue();
//		        	pct = Double.valueOf(array2[1]);
		        } catch (Exception e){
		        	log(Level.FINEST, "Error getting value: "+cellContents+" in column "+columnName, e);
		        	pct = 0;
		        }
		        		        
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
