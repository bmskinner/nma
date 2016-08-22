package gui.tabs.cells;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;

import javax.swing.JPanel;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import charting.charts.ConsensusNucleusChartFactory;
import charting.charts.FixedAspectRatioChartPanel;
import charting.charts.OutlineChartFactory;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
import charting.options.TableOptions;
import components.CellularComponent;
import gui.DatasetEvent;
import gui.RotationMode;
import gui.components.panels.GenericCheckboxPanel;
import gui.components.panels.RotationSelectionSettingsPanel;
import gui.dialogs.CellCollectionOverviewDialog;

@SuppressWarnings("serial")
public class CellOutlinePanel extends AbstractCellDetailPanel implements ActionListener {
	
	private RotationSelectionSettingsPanel rotationPanel;

	private FixedAspectRatioChartPanel panel;
	private GenericCheckboxPanel makeMeshPanel = new GenericCheckboxPanel("Compare to consensus");
	private GenericCheckboxPanel warpMeshPanel = new GenericCheckboxPanel("Warp to consensus");
			
	public CellOutlinePanel(CellViewModel model) {
		super(model);
		// make the chart for each nucleus
		this.setLayout(new BorderLayout());
		JFreeChart chart = ConsensusNucleusChartFactory.getInstance().makeEmptyChart();

		
		JPanel settingsPanel = new JPanel(new FlowLayout());
		
		rotationPanel = new RotationSelectionSettingsPanel();
		rotationPanel.setEnabled(false);
		rotationPanel.addActionListener(this);
		
		makeMeshPanel.addActionListener(this);
		makeMeshPanel.setEnabled(false);
		
		warpMeshPanel.addActionListener(this);
		warpMeshPanel.setEnabled(false);
		
		settingsPanel.add(rotationPanel);
		settingsPanel.add(makeMeshPanel);
		settingsPanel.add(warpMeshPanel);
		
//		this.addChartOptionsRenderedEventListener(this);
		
		this.add(settingsPanel, BorderLayout.NORTH);
		
		panel = new FixedAspectRatioChartPanel(chart);
					
		this.add(panel, BorderLayout.CENTER);
		
	}
	
	private void updateSettingsPanels(){
		if(this.hasDatasets()){

			if(this.getCellModel().getCell()==null){
				rotationPanel.setEnabled(false);
				makeMeshPanel.setEnabled(false);
				warpMeshPanel.setEnabled(false);
			} else {
				// Only allow one mesh activity to be active
				rotationPanel.setEnabled(! warpMeshPanel.isSelected());
				makeMeshPanel.setEnabled(  ! warpMeshPanel.isSelected() );
				warpMeshPanel.setEnabled(  ! makeMeshPanel.isSelected() );

				if( ! activeDataset().getCollection().hasConsensusNucleus()){
					makeMeshPanel.setEnabled(false);
					warpMeshPanel.setEnabled(false);
				}
			}
		} else {
			rotationPanel.setEnabled(false);
			makeMeshPanel.setEnabled(false);
			warpMeshPanel.setEnabled(false);
		}
	}
							
	public void update(){

		CellularComponent component = this.getCellModel().getComponent();
								
			updateSettingsPanels();
			
			ChartOptions options = new ChartOptionsBuilder()
					.setDatasets(getDatasets())
					.setCell(this.getCellModel().getCell())
					.setRotationMode(rotationPanel.getSelected())
					.setShowAnnotations(false)
					.setShowMesh(makeMeshPanel.isSelected())
					.setShowWarp(warpMeshPanel.isSelected())
					.setShowMeshEdges(false)
					.setShowMeshFaces(true)
					.setInvertYAxis( rotationPanel.getSelected().equals(RotationMode.ACTUAL) ) // only invert for actual
					.setCellularComponent(component)
					.setTarget(panel)
					.build();
			
			
			JFreeChart chart;
			try {
				chart = getChart(options);
			} catch (Exception e) {
				warn("Error getting chart in cell outline panel");
				log(Level.FINE, "Error getting chart in cell outline panel", e);
				chart = ConsensusNucleusChartFactory.getInstance().makeErrorChart();
			}

			
			panel.setChart(chart);

			if(this.getCellModel().hasCell()  ){ 
				panel.restoreAutoBounds();
			}
			
	}

	
	@Override
	public void actionPerformed(ActionEvent e) {
		
		update();
		
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
//		activeCell = null;
		
		updateSettingsPanels();
		
		ChartOptions options = new ChartOptionsBuilder()
				.build();
		
		JFreeChart chart = getChart(options);
		panel.setChart(chart);
		
	}

	@Override
	protected TableModel createPanelTableType(TableOptions options) throws Exception {
		return null;
	}

	@Override
	protected JFreeChart createPanelChartType(ChartOptions options) throws Exception {
		return OutlineChartFactory.getInstance().makeCellOutlineChart(options);
	}
	
	@Override
	public void datasetEventReceived(DatasetEvent event){
		super.datasetEventReceived(event);
    	// Pass messages upwards
    	if(event.getSource() instanceof CellCollectionOverviewDialog){
    		fireDatasetEvent(new DatasetEvent(this, event));
    	}
    }

	@Override
	public void chartOptionsRenderedEventReceived(ChartOptionsRenderedEvent e) {

		update();
	}

}
