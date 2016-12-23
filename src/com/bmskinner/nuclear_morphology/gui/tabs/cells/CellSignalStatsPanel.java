package com.bmskinner.nuclear_morphology.gui.tabs.cells;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;

import com.bmskinner.nuclear_morphology.charting.datasets.AbstractDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.AnalysisDatasetTableCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.CellTableDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptionsBuilder;
import com.bmskinner.nuclear_morphology.gui.GlobalOptions;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;


@SuppressWarnings("serial")
public class CellSignalStatsPanel extends AbstractCellDetailPanel {
	
	private static final String HEADER_LBL    = "Pairwise distances between the centres of mass of all signals";
	private static final String TABLE_TOOLTIP = "Shows the distances between the centres of mass of signals";
	
	private ExportableTable table; // individual cell stats
	
	private JScrollPane scrollPane;
	
	public CellSignalStatsPanel(CellViewModel model) {
		super(model);
		this.setLayout(new BorderLayout());
		
		JPanel header    = createHeader();
		
		scrollPane = new JScrollPane();

		TableModel tableModel = AnalysisDatasetTableCreator.createBlankTable();		
		
		table = new ExportableTable(tableModel);
		table.setEnabled(false);
		table.setToolTipText(TABLE_TOOLTIP);
		
		scrollPane.setViewportView(table);
		scrollPane.setColumnHeaderView(table.getTableHeader());
		
		
		this.add(header,     BorderLayout.NORTH);
		this.add(scrollPane, BorderLayout.CENTER);
		
		this.setEnabled(false);
	}
	
	/**
	 * Create the header panel
	 * @return
	 */
	private JPanel createHeader(){
		JPanel panel = new JPanel();
		
		JLabel label = new JLabel(HEADER_LBL);
		
		panel.add(label);
		return panel;
	}
	
	public synchronized void update(){
		
		if(this.isMultipleDatasets() || ! this.hasDatasets()){
			table.setModel(AbstractDatasetCreator.createBlankTable());
			return;
		}
		
		TableOptions options = new TableOptionsBuilder()
			.setDatasets(getDatasets())
			.setCell(this.getCellModel().getCell())
			.setScale(GlobalOptions.getInstance().getScale())
			.setTarget(table)
			.build();

		try{
			
			setTable(options);

		} catch(Exception e){
			warn("Error updating cell stats table");
			stack("Error updating cell stats table", e);
		}
	}
	
	@Override
	public void setChartsAndTablesLoading(){
		
		table.setModel(AbstractDatasetCreator.createLoadingTable());
	}
	
	@Override
	protected void updateSingle() {
		update();
	}



	@Override
	protected void updateMultiple() {
		updateNull();
	}



	@Override
	protected void updateNull() {
		table.setModel(AbstractDatasetCreator.createBlankTable());
		
	}



	@Override
	protected TableModel createPanelTableType(TableOptions options){
		
		if(getCellModel().hasCell()){
			return new CellTableDatasetCreator(getCellModel().getCell()).createPairwiseSignalDistanceTable(options);
		} else {
			return CellTableDatasetCreator.createBlankTable();
		}
	}

}
