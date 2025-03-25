package com.bmskinner.nma.gui.tabs.analysis_info;

import java.awt.BorderLayout;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.gui.components.ExportableTable;
import com.bmskinner.nma.gui.components.renderers.JTextAreaCellRenderer;
import com.bmskinner.nma.gui.tabs.TableDetailPanel;
import com.bmskinner.nma.visualisation.options.AbstractOptions;
import com.bmskinner.nma.visualisation.options.TableOptions;
import com.bmskinner.nma.visualisation.options.TableOptionsBuilder;
import com.bmskinner.nma.visualisation.tables.AbstractTableCreator;
import com.bmskinner.nma.visualisation.tables.AnalysisDatasetTableCreator;

/**
 * Displays tables of rulesets for selected datasets
 * 
 * @author Ben Skinner
 * @since 2.2.0
 *
 */
@SuppressWarnings("serial")
public class AnalysisRulesetsDetailPanel extends TableDetailPanel {

	private static final String PANEL_TITLE_LBL = "Rulesets";
	private static final String PANEL_DESC_LBL = "Show rulesets used to detect landmarks";

	private static final String HEADER_LBL = "Green rows have the same value in all columns";
	private ExportableTable table;
	JScrollPane scrollPane;

	public AnalysisRulesetsDetailPanel() {
		super(PANEL_TITLE_LBL, PANEL_DESC_LBL);

		this.setLayout(new BorderLayout());

		JPanel header = new JPanel();
		header.add(new JLabel(HEADER_LBL));

		this.add(header, BorderLayout.NORTH);
		this.add(createTablePanel(), BorderLayout.CENTER);

	}

	private JPanel createTablePanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		table = new ExportableTable();
		table.setModel(AbstractTableCreator.createBlankTable());

		table.setEnabled(false);
		table.setDefaultRenderer(Object.class, new JTextAreaCellRenderer());
		scrollPane = new JScrollPane(table);

		JPanel tablePanel = new JPanel(new BorderLayout());

		tablePanel.add(scrollPane, BorderLayout.CENTER);
		tablePanel.add(table.getTableHeader(), BorderLayout.NORTH);

		panel.add(tablePanel);
		return panel;
	}

	@Override
	public synchronized void setLoading() {
		super.setLoading();
		table.setModel(AbstractTableCreator.createLoadingTable());
	}

	@Override
	protected TableModel createPanelTableType(@NonNull TableOptions options) {
		return new AnalysisDatasetTableCreator(options).createAnalysisRulesetsTable();
	}

	@Override
	protected synchronized void updateSingle() {
		updateMultiple();
	}

	@Override
	protected synchronized void updateMultiple() {
		updateAnalysisParametersPanel();
	}

	@Override
	protected synchronized void updateNull() {
		table.setModel(AbstractTableCreator.createBlankTable());
	}

	/**
	 * Update the analysis panel with data from the given datasets
	 * 
	 */
	private void updateAnalysisParametersPanel() {

		TableOptions options = new TableOptionsBuilder()
				.setDatasets(getDatasets())
				.setTarget(table)
				.setScrollPane(scrollPane)
				.setBoolean(AbstractOptions.IS_MERGE_SOURCE_OPTIONS_TABLE, false)
				.build();

		setTable(options);

	}

}