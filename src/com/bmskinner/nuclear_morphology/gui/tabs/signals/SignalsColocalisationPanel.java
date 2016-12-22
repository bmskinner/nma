package com.bmskinner.nuclear_morphology.gui.tabs.signals;

import java.awt.BorderLayout;

import javax.swing.JScrollPane;
import javax.swing.table.TableModel;

import com.bmskinner.nuclear_morphology.charting.datasets.AbstractDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.NuclearSignalDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptionsBuilder;
import com.bmskinner.nuclear_morphology.gui.GlobalOptions;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;

/**
 * Show the minimum distances between signals within a dataset
 * @author ben
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class SignalsColocalisationPanel extends DetailPanel {
	
	private static final String TABLE_TOOLTIP = "Shows the median distance between closest signal pairs";
	
	private ExportableTable table;			// table for analysis parameters
	private JScrollPane     scrollPane;


	public SignalsColocalisationPanel(){
		super();
		this.setLayout(new BorderLayout());

		table  = new ExportableTable(AbstractDatasetCreator.createBlankTable());
		table.setToolTipText(TABLE_TOOLTIP);

		table.setEnabled(false);
		scrollPane = new JScrollPane(table);
		this.add(scrollPane, BorderLayout.CENTER);
	}
	
	@Override
	protected void updateSingle() {
		
		TableOptions options = new TableOptionsBuilder()
			.setDatasets(getDatasets())
			.setScale(GlobalOptions.getInstance().getScale())
			.setTarget(table)
			.build();
		
		setTable(options);		
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
	public void setChartsAndTablesLoading(){
		super.setChartsAndTablesLoading();
		table.setModel(AbstractDatasetCreator.createLoadingTable());	
		
	}
	
	
	@Override
	protected TableModel createPanelTableType(TableOptions options){
		return new NuclearSignalDatasetCreator().createSignalColocalisationTable(options);
	}
}
