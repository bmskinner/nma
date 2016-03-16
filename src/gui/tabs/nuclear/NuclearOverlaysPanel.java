package gui.tabs.nuclear;

import java.awt.BorderLayout;
import java.util.logging.Level;

import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import charting.charts.OutlineChartFactory;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
import charting.options.TableOptions;
import gui.components.FixedAspectRatioChartPanel;
import gui.components.panels.ProbabilityDensityCheckboxPanel;
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
	private ProbabilityDensityCheckboxPanel checkBoxPanel; // use for aligning nuclei
	
	public NuclearOverlaysPanel(){
		
		this.setLayout(new BorderLayout());
		
		checkBoxPanel = new ProbabilityDensityCheckboxPanel();
		checkBoxPanel.setText("Align nuclei");
		checkBoxPanel.addActionListener( a ->  update(getDatasets())  );
		this.add(checkBoxPanel, BorderLayout.NORTH);
		
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

	@Override
	protected void updateSingle() throws Exception {
		
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
		
	}

	@Override
	protected void updateNull() throws Exception {
		updateSingle();
		
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
