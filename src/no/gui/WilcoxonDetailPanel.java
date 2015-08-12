package no.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import utility.Constants;
import no.analysis.AnalysisDataset;
import datasets.NucleusDatasetCreator;
import datasets.NucleusTableDatasetCreator;

public class WilcoxonDetailPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	
	private JTable wilcoxonAreaTable;
	private JTable wilcoxonPerimTable;
	private JTable wilcoxonFeretTable;
	private JTable wilcoxonMinFeretTable;
	private JTable wilcoxonDifferenceTable;

	public WilcoxonDetailPanel() {
		this.setLayout(new BorderLayout());
		
		JScrollPane scrollPane = new JScrollPane();
		JPanel panel = new JPanel();
		scrollPane.setViewportView(panel);
		
//		JPanel wilcoxonPartsPanel = new JPanel();
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
		
		wilcoxonAreaTable = new JTable(NucleusTableDatasetCreator.createWilcoxonAreaTable(null));
		addWilconxonTable(panel, wilcoxonAreaTable, "Areas");
		scrollPane.setColumnHeaderView(wilcoxonAreaTable.getTableHeader());

		
		wilcoxonPerimTable = new JTable(NucleusTableDatasetCreator.createWilcoxonPerimeterTable(null));
		addWilconxonTable(panel, wilcoxonPerimTable, "Perimeters");
		
		wilcoxonMinFeretTable = new JTable(NucleusTableDatasetCreator.createWilcoxonMinFeretTable(null));
		addWilconxonTable(panel, wilcoxonMinFeretTable, "Min feret");

		
		wilcoxonFeretTable = new JTable(NucleusTableDatasetCreator.createWilcoxonMaxFeretTable(null));
		addWilconxonTable(panel, wilcoxonFeretTable, "Feret");
		
		
		wilcoxonDifferenceTable = new JTable(NucleusTableDatasetCreator.createWilcoxonVariabilityTable(null));
		addWilconxonTable(panel, wilcoxonDifferenceTable, "Differences to median");
		
//		panel.add(wilcoxonPartsPanel, BorderLayout.CENTER);
		
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
	 */
	public void update(List<AnalysisDataset> list){
		// format the numbers and make into a tablemodel

		wilcoxonAreaTable.setModel(NucleusTableDatasetCreator.createWilcoxonAreaTable(list));
		
		int columns = wilcoxonAreaTable.getColumnModel().getColumnCount();
		for(int i=1;i<columns;i++){
			wilcoxonAreaTable.getColumnModel().getColumn(i).setCellRenderer(new WilcoxonTableCellRenderer());
		}
		
		wilcoxonPerimTable.setModel(NucleusTableDatasetCreator.createWilcoxonPerimeterTable(list));
		columns = wilcoxonPerimTable.getColumnModel().getColumnCount();
		for(int i=1;i<columns;i++){
			wilcoxonPerimTable.getColumnModel().getColumn(i).setCellRenderer(new WilcoxonTableCellRenderer());
		}
		
		wilcoxonMinFeretTable.setModel(NucleusTableDatasetCreator.createWilcoxonMinFeretTable(list));
		columns = wilcoxonMinFeretTable.getColumnModel().getColumnCount();
		for(int i=1;i<columns;i++){
			wilcoxonMinFeretTable.getColumnModel().getColumn(i).setCellRenderer(new WilcoxonTableCellRenderer());
		}
		
		wilcoxonFeretTable.setModel(NucleusTableDatasetCreator.createWilcoxonMaxFeretTable(list));
		columns = wilcoxonFeretTable.getColumnModel().getColumnCount();
		for(int i=1;i<columns;i++){
			wilcoxonFeretTable.getColumnModel().getColumn(i).setCellRenderer(new WilcoxonTableCellRenderer());
		}
		
		wilcoxonDifferenceTable.setModel(NucleusTableDatasetCreator.createWilcoxonVariabilityTable(list));
		columns = wilcoxonDifferenceTable.getColumnModel().getColumnCount();
		for(int i=1;i<columns;i++){
			wilcoxonDifferenceTable.getColumnModel().getColumn(i).setCellRenderer(new WilcoxonTableCellRenderer());
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
