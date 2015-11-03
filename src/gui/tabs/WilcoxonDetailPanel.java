/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
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
import java.awt.Dimension;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableModel;

import components.nuclear.NucleusStatistic;
import utility.Constants;
import analysis.AnalysisDataset;
import charting.NucleusStatsTableOptions;
import charting.TableOptions;
import charting.datasets.NucleusTableDatasetCreator;

public class WilcoxonDetailPanel extends DetailPanel {

	private static final long serialVersionUID = 1L;
		
	private Map<NucleusStatistic, JTable> tables = new HashMap<NucleusStatistic, JTable>();

	public WilcoxonDetailPanel(Logger programLogger) throws Exception {
		super(programLogger);
		this.setLayout(new BorderLayout());
		
		JScrollPane scrollPane = new JScrollPane();
		JPanel panel = new JPanel();
		scrollPane.setViewportView(panel);
		

		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		
		Dimension minSize = new Dimension(10, 10);
		Dimension prefSize = new Dimension(10, 10);
		Dimension maxSize = new Dimension(Short.MAX_VALUE, 10);
		panel.add(new Box.Filler(minSize, prefSize, maxSize));
		
		JPanel infoPanel = new JPanel();
		infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
		infoPanel.add(new JLabel("Pairwise comparisons between populations using Mann-Whitney U test (aka Wilcoxon rank-sum test)"));
		infoPanel.add(new JLabel("Above the diagonal: Mann-Whitney U statistics"));
		infoPanel.add(new JLabel("Below the diagonal: p-values"));
		infoPanel.add(new JLabel("p-values significant at 5% and 1% levels after Bonferroni correction are highlighted in yellow and green"));
		
		
		for(NucleusStatistic stat : NucleusStatistic.values()){
			JTable table = new JTable(NucleusTableDatasetCreator.createWilcoxonNuclearStatTable(null, stat));
			tables.put(stat, table);
			addWilconxonTable(panel, table, stat.toString());
		}
		
		scrollPane.setColumnHeaderView(tables.get(NucleusStatistic.AREA).getTableHeader());

		
		this.add(infoPanel, BorderLayout.NORTH);
		this.add(scrollPane, BorderLayout.CENTER);
	}
	/**
	 * Prepare a wilcoxon table
	 * @param panel the JPanel to add the table to
	 * @param table the table to add
	 * @param model the model to provide
	 * @param label the label for the table
	 */
	private void addWilconxonTable(JPanel panel, JTable table, String label){
		Dimension minSize = new Dimension(10, 10);
		Dimension prefSize = new Dimension(10, 10);
		Dimension maxSize = new Dimension(Short.MAX_VALUE, 10);
		panel.add(new Box.Filler(minSize, prefSize, maxSize));
		panel.add(new JLabel(label));
		panel.add(table);
		table.setEnabled(false);
	}
		
	/**
	 * Update the wilcoxon panel with data from the given datasets
	 * @param list the datasets
	 * @throws Exception 
	 */
	public void update(final List<AnalysisDataset> list) {
		programLogger.log(Level.FINE, "Updating Wilcoxon panel");
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				try{
					
					if(!list.isEmpty() && list!=null){
						
						for(NucleusStatistic stat : NucleusStatistic.values()){
							
							TableModel model;
							
							TableOptions options = new NucleusStatsTableOptions(list, stat);
							if(getTableCache().hasTable(options)){
								programLogger.log(Level.FINEST, "Fetched cached Wilcoxon table: "+stat);
								model = getTableCache().getTable(options);
							} else {
								model = NucleusTableDatasetCreator.createWilcoxonNuclearStatTable(list, stat);
								programLogger.log(Level.FINEST, "Added cached Wilcoxon table: "+stat);
							}
							
							tables.get(stat).setModel(model);
							setRenderer(tables.get(stat));

						}

					} else {
						for(NucleusStatistic stat : NucleusStatistic.values()){
							TableModel model = NucleusTableDatasetCreator.createWilcoxonNuclearStatTable(null, stat);
							tables.get(stat).setModel(model);
							setRenderer(tables.get(stat));

						}
					}
					programLogger.log(Level.FINEST, "Updated Wilcoxon panel");
				} catch (Exception e) {
					programLogger.log(Level.SEVERE, "Error making Wilcoxon table", e);
				}
			}});
	}
	
	private void setRenderer(JTable table){
		int columns = table.getColumnModel().getColumnCount();
		if(columns>1){
			for(int i=1;i<columns;i++){
				table.getColumnModel().getColumn(i).setCellRenderer(new WilcoxonTableCellRenderer());
			}
		}
	}
	
	/**
	 * Colour a table cell background based on its value to show statistical 
	 * significance. Shows yellow for values below a Bonferroni-corrected cutoff
	 * of 0.05, and green for values below a Bonferroni-corrected cutoff
	 * of 0.01
	 */
	public class WilcoxonTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer {

		private static final long serialVersionUID = 1L;

		public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, java.lang.Object value, boolean isSelected, boolean hasFocus, int row, int column) {
	        
	      //Cells are by default rendered as a JLabel.
	        JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

	        String cellContents = l.getText();
	        if(cellContents!=null && !cellContents.equals("")){ // ensure value
//	        	
		        double pvalue = Double.valueOf(cellContents);
		        
		        Color colour = Color.WHITE; // default
		        
		        int numberOfTests = 5; // correct for the different variables measured;
		        double divisor = (double) (   (table.getColumnCount()-2)  * numberOfTests); // for > 2 datasets with numberOFtests tests per dataset
		        
		        double fivePct = Constants.FIVE_PERCENT_SIGNIFICANCE_LEVEL / divisor; // Bonferroni correction
		        double onePct = Constants.ONE_PERCENT_SIGNIFICANCE_LEVEL /   divisor;
//		        IJ.log("Columns: "+table.getColumnCount());
		        
		        if(pvalue<=fivePct){
		        	colour = Color.YELLOW;
		        }
		        
		        if(pvalue<=onePct){
		        	colour = Color.GREEN;
		        }
		        l.setBackground(colour);

	        } else {
	            l.setBackground(Color.LIGHT_GRAY);
	        }

	      //Return the JLabel which renders the cell.
	      return l;
	    }
	}
}
