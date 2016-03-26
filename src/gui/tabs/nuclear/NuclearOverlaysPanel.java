package gui.tabs.nuclear;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.logging.Level;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import analysis.AnalysisDataset;
import charting.charts.OutlineChartFactory;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
import charting.options.TableOptions;
import gui.components.FixedAspectRatioChartPanel;
import gui.components.panels.GenericCheckboxPanel;
import gui.dialogs.ConsensusCompareDialog;
import gui.tabs.DetailPanel;

/**
 * This class is designed to display the vertically oriented
 * nuclei within the collection, overlaid atop each other
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class NuclearOverlaysPanel extends DetailPanel {
	
	private FixedAspectRatioChartPanel 	chartPanel; 		// hold the nuclei
	private GenericCheckboxPanel checkBoxPanel; // use for aligning nuclei
	private JButton compareConsensusButton;
	
	public NuclearOverlaysPanel(){
		
		this.setLayout(new BorderLayout());
		
		JPanel header = createHeader();
		this.add(header, BorderLayout.NORTH);
		
		try {
			
			ChartOptions options = new ChartOptionsBuilder()
			.setDatasets(getDatasets())
			.build();
			
			JFreeChart chart = getChart(options);
			
			chartPanel = new FixedAspectRatioChartPanel(chart);
			this.add(chartPanel, BorderLayout.CENTER);
			
		} catch (Exception e) {
			log(Level.SEVERE, "Error creating overlays panel");
			log(Level.FINE, "Error creating overlays panel", e);
		}
		
	}
	
	private JPanel createHeader(){
		JPanel panel = new JPanel(new FlowLayout());
		
		checkBoxPanel = new GenericCheckboxPanel("Align nuclei to consensus");
		checkBoxPanel.addActionListener( a ->  update(getDatasets())  );
		checkBoxPanel.setEnabled(false);
		
		compareConsensusButton = new JButton("Compare consensus");
		compareConsensusButton.addActionListener( a -> new ConsensusCompareDialog(getDatasets())  );
		compareConsensusButton.setEnabled(false);
		
		panel.add(checkBoxPanel);
		panel.add(compareConsensusButton);
		return panel;
	}

	@Override
	protected void updateSingle() throws Exception {
		compareConsensusButton.setEnabled(false);
		
		boolean hasConsensus = activeDataset().getCollection().hasConsensusNucleus();
		checkBoxPanel.setEnabled(hasConsensus);
		
		boolean alignNuclei = checkBoxPanel.isSelected();
		ChartOptions options = new ChartOptionsBuilder()
			.setDatasets(getDatasets())
			.setNormalised(alignNuclei)
			.build();
		
		JFreeChart chart = getChart(options);
		chartPanel.setChart(chart);
		
		chartPanel.restoreAutoBounds();
		
	}

	@Override
	protected void updateMultiple() throws Exception {
		updateSingle();
		
		/*
		 * Check if all selected datasets have a consensus
		 */
		boolean setConsensusButton = true;
		for(AnalysisDataset d : getDatasets()){
			if( ! d.getCollection().hasConsensusNucleus()){
				setConsensusButton = false;
			}
		}
		compareConsensusButton.setEnabled(setConsensusButton);
		
		checkBoxPanel.setEnabled(false);
	}

	@Override
	protected void updateNull() throws Exception {
		compareConsensusButton.setEnabled(false);
		checkBoxPanel.setEnabled(false);
		
		ChartOptions options = new ChartOptionsBuilder()
			.setDatasets(getDatasets())
			.setNormalised(false)
			.build();
		
		JFreeChart chart = getChart(options);
		chartPanel.setChart(chart);		
	}

	@Override
	protected TableModel createPanelTableType(TableOptions options)
			throws Exception {
		return null;
	}

	@Override
	protected JFreeChart createPanelChartType(ChartOptions options) throws Exception {
		return OutlineChartFactory.createVerticalNucleiChart(options);
	}
	
	

}
