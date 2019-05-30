/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nuclear_morphology.gui.tabs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.charting.datasets.AnalysisDatasetTableCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.tables.AbstractTableCreator;
import com.bmskinner.nuclear_morphology.charting.options.AbstractOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;
import com.bmskinner.nuclear_morphology.gui.components.renderers.JTextAreaCellRenderer;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEvent;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * This panel shows any merge sources for a merged dataset, and the analysis
 * options used to create the merge
 * 
 * @author bms41
 * @since 1.9.0
 *
 */
@SuppressWarnings("serial")
public class MergesDetailPanel extends DetailPanel {
	
	private static final Logger LOGGER = Logger.getLogger(Loggable.ROOT_LOGGER);

    private ExportableTable sourceParametersTable;
    private JLabel headerLabel = new JLabel(Labels.NULL_DATASETS);

    private static final String RECOVER_BUTTON_TEXT = "Recover source";
    private static final String PANEL_TITLE_LBL = "Merges";

    public MergesDetailPanel(@NonNull InputSupplier context) {
        super(context);

        try {
            createUI();
        } catch (Exception e) {
        	LOGGER.log(Loggable.STACK, "Error creating merge panel", e);
        }
    }
    
    @Override
    public String getPanelTitle(){
        return PANEL_TITLE_LBL;
    }

    private void createUI() {

        this.setLayout(new BorderLayout());

        this.add(createHeaderPanel(), BorderLayout.NORTH);
        this.add(createTablePanel(), BorderLayout.CENTER);
    }
    
    private JPanel createTablePanel() {
    	JPanel panel = new JPanel();
    	panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    	
    	TableCellRenderer buttonRenderer = new JButtonRenderer();
        TableCellRenderer textRenderer = new JTextAreaCellRenderer();
        
    	sourceParametersTable = new ExportableTable() {
    		 @Override
             public TableCellRenderer getCellRenderer(int row, int column) {
             	if( (this.getValueAt(row, 0).equals(Labels.Merges.RECOVER_SOURCE)) 
             			&& column>0) {
             		return buttonRenderer;
             	}
             	return textRenderer;
             }
    	};
    	
    	MouseListener mouseListener = new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent e) {				
				int row = sourceParametersTable.rowAtPoint(e.getPoint());
				int col = sourceParametersTable.columnAtPoint(e.getPoint());
				if(col==0)
					return;

	        	if(sourceParametersTable.getValueAt(row, 0).equals(Labels.Merges.RECOVER_SOURCE) && 
	        			sourceParametersTable.getValueAt(row, col)!=null) {
	        		IAnalysisDataset mergeSource = (IAnalysisDataset) sourceParametersTable.getValueAt(row, col);
	        		getDatasetEventHandler().fireDatasetEvent(DatasetEvent.EXTRACT_SOURCE, mergeSource);
	        	}
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				// Not needed
			}

			@Override
			public void mouseExited(MouseEvent e) {
				// Not needed
			}

			@Override
			public void mousePressed(MouseEvent e) {
				// Not needed
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				// Not needed
			}
        	
        };
        sourceParametersTable.addMouseListener(mouseListener);    	
    	sourceParametersTable.setModel(AbstractTableCreator.createBlankTable());

    	sourceParametersTable.setEnabled(false);
    	sourceParametersTable.setDefaultRenderer(Object.class, new JTextAreaCellRenderer());
        JScrollPane scrollPane = new JScrollPane(sourceParametersTable);

        JPanel tablePanel = new JPanel(new BorderLayout());

        tablePanel.add(scrollPane, BorderLayout.CENTER);
        tablePanel.add(sourceParametersTable.getTableHeader(), BorderLayout.NORTH);
        
        panel.add(tablePanel);
        return panel;
    }

    @Override
    public synchronized void setChartsAndTablesLoading() {
        super.setChartsAndTablesLoading();
        sourceParametersTable.setModel(AbstractTableCreator.createLoadingTable());
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel();
        panel.add(headerLabel);
        return panel;

    }

    @Override
    protected synchronized void updateSingle() {

        headerLabel.setText(
                Labels.SINGLE_DATASET + " with " + activeDataset().getAllMergeSources().size() + " merge sources");

        List<IAnalysisDataset> mergeSources = new ArrayList<>(activeDataset().getAllMergeSources());

        TableOptions options = new TableOptionsBuilder().setDatasets(mergeSources)
                .setTarget(sourceParametersTable)
                .build();
        
        options.setBoolean(AbstractOptions.SHOW_RECOVER_MERGE_SOURCE_KEY,  true);
        setTable(options);

    }

    @Override
    protected synchronized void updateMultiple() {
        updateNull();
        headerLabel.setText(Labels.MULTIPLE_DATASETS);
    }

    @Override
    protected synchronized void updateNull() {
    	sourceParametersTable.setModel(AbstractTableCreator.createBlankTable());
        headerLabel.setText(Labels.NULL_DATASETS);
    }

    @Override
    protected synchronized JFreeChart createPanelChartType(@NonNull ChartOptions options) {
        return null;
    }

    @Override
    protected synchronized TableModel createPanelTableType(@NonNull TableOptions options) {
		return new AnalysisDatasetTableCreator(options).createAnalysisParametersTable();
    }
    
    /**
     * Render a button in a cell. Note, this is non-functional - it just paints 
     * a button shape. Use a mouse listener on the table for functionality
     * @author bms41
     * @since 1.16.0
     *
     */
    private class JButtonRenderer extends JButton  implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
			String text = value==null ? "" : value instanceof IAnalysisDataset ? RECOVER_BUTTON_TEXT : "";
            setText(text);
            setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            return this;
        }
    }
}
