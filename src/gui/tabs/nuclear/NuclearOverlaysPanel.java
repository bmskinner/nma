package gui.tabs.nuclear;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.logging.Level;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import analysis.AnalysisDataset;
import analysis.profiles.ProfileManager;
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
	private JButton makeOverlayChartButton;
	
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
			
			chartPanel.addComponentListener( new ComponentAdapter(){

				@Override
				public void componentResized(ComponentEvent e) {
					chartPanel.restoreAutoBounds();
					
				}
				
			});
			
			this.add(chartPanel, BorderLayout.CENTER);
			
			makeCreateButton();
			
		} catch (Exception e) {
			warn("Error creating overlays panel");
			log(Level.FINE, "Error creating overlays panel", e);
		}
		
	}
	
	private JPanel createHeader(){
		JPanel panel = new JPanel(new FlowLayout());
		
		checkBoxPanel = new GenericCheckboxPanel("Align nuclei to consensus");
		checkBoxPanel.addActionListener( a ->  update(getDatasets())  );
		checkBoxPanel.setEnabled(false);
		
		compareConsensusButton = new JButton("Compare consensus");
		compareConsensusButton.addActionListener( a -> createConsensusCompareDialog()  );
		compareConsensusButton.setEnabled(false);
		
		panel.add(checkBoxPanel);
		panel.add(compareConsensusButton);
		return panel;
	}
	
	private void makeCreateButton(){
		makeOverlayChartButton = new JButton("Create chart");
		makeOverlayChartButton.addActionListener( a -> createSafeChart(getChartOptions())  );
		makeOverlayChartButton.setEnabled(true);
		chartPanel.add(makeOverlayChartButton);
		makeOverlayChartButton.setVisible(false);
	}
	
	private void createConsensusCompareDialog(){
		
		boolean ok = true;
		
		int segCount = activeDataset().getCollection().getProfileManager().getSegmentCount();
		for(AnalysisDataset d : getDatasets()){
			
			if( ! d.getCollection().hasConsensusNucleus()){
				ok = false;
				warn("Dataset "+d.getName()+" does not have a consensus nucleus");
			}
			
			if(d.getCollection().getProfileManager().getSegmentCount() != segCount){
				ok = false;
				warn("Dataset "+d.getName()+" has a different segment pattern to "+activeDataset().getName());
			}
			
		}

		if(ok){
			finer("Creating consensus compare dialog");
			new ConsensusCompareDialog(getDatasets());
		} else {
			warn("Unable to create consensus compare dialog");
		}
	}
	
	/**
	 * Get the chart options object for the selected datasets and parameters
	 * @return
	 */
	private ChartOptions getChartOptions(){
		boolean alignNuclei = checkBoxPanel.isSelected();
		
		return new ChartOptionsBuilder()
		.setDatasets(getDatasets())
		.setNormalised(alignNuclei)
		.build();
	}

	@Override
	protected void updateSingle() {
//		log(Level.INFO, "Updating overlays panel: single");
		compareConsensusButton.setEnabled(false);
		makeOverlayChartButton.setVisible(false);
		
		boolean hasConsensus = activeDataset().getCollection().hasConsensusNucleus();
		checkBoxPanel.setEnabled(hasConsensus);
		
		
		ChartOptions options = getChartOptions();
		
		/*
		 * Insert a button to generate the chart if not present
		 */
		if(this.chartCache.hasChart(options)){
			
			JFreeChart chart = getChart(options);
			chartPanel.setChart(chart);
			chartPanel.restoreAutoBounds();
		} else {
			
			options = new ChartOptionsBuilder()
			.build();
			
			JFreeChart chart = getChart(options);
			chartPanel.setChart(chart);
			makeOverlayChartButton.setVisible(true);
		}

		
	}
	
	/**
	 * Create the overlay chart. Invoked by makeOverlayChartButton
	 * @param options
	 */
	private void createSafeChart(ChartOptions options){
		makeOverlayChartButton.setVisible(false);
		setAnalysing(true);
		log(Level.FINE, "Creating overlay chart on button click");
		 try {
			getChart(options);
			update(getDatasets());
		} catch (Exception e) {
			logError("Error making chart", e);
			update(getDatasets());
		} finally{
			setAnalysing(false);
		}
	}

	@Override
	protected void updateMultiple() {
		makeOverlayChartButton.setVisible(false);
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
		
		if( ! ProfileManager.segmentCountsMatch(getDatasets())){
			setConsensusButton = false;
		}
		
		compareConsensusButton.setEnabled(setConsensusButton);
		
		checkBoxPanel.setEnabled(false);
	}

	@Override
	protected void updateNull() {
		compareConsensusButton.setEnabled(false);
		checkBoxPanel.setEnabled(false);
		makeOverlayChartButton.setVisible(false);
		
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
		finest("Creating nuclear overlay chart");
		return OutlineChartFactory.getInstance().createVerticalNucleiChart(options);
	}
	
	

}
