package gui.tabs.cells;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.jfree.chart.JFreeChart;

import charting.charts.ConsensusNucleusChartFactory;
import charting.charts.OutlineChartFactory;
import charting.charts.panels.ExportableChartPanel;
import charting.options.DefaultChartOptions;
import charting.options.ChartOptionsBuilder;
import components.CellularComponent;
import gui.ChartOptionsRenderedEvent;
import gui.ChartSetEvent;
import gui.ChartSetEventListener;
import gui.DatasetEvent;
import gui.RotationMode;
import gui.components.panels.GenericCheckboxPanel;
import gui.components.panels.RotationSelectionSettingsPanel;
import gui.dialogs.CellCollectionOverviewDialog;

@SuppressWarnings("serial")
public class CellOutlinePanel extends AbstractCellDetailPanel implements ActionListener, ChartSetEventListener {
	
	private RotationSelectionSettingsPanel rotationPanel;

	private ExportableChartPanel panel;
	
	private GenericCheckboxPanel makeMeshPanel = new GenericCheckboxPanel("Compare to consensus");
	private GenericCheckboxPanel warpMeshPanel = new GenericCheckboxPanel("Warp to consensus");
	
	private JButton redrawBorderBtn = new JButton("Redraw outline");
	
	private CellBorderAdjustmentDialog cellBorderAdjustmentDialog;
			
	public CellOutlinePanel(CellViewModel model) {
		super(model);
		// make the chart for each nucleus
		this.setLayout(new BorderLayout());
		JFreeChart chart = ConsensusNucleusChartFactory.makeEmptyChart();

		
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
		
		cellBorderAdjustmentDialog = new CellBorderAdjustmentDialog(model);
		
		redrawBorderBtn.addActionListener( e ->{
			cellBorderAdjustmentDialog.load(model.getCell(), activeDataset());
		});
		redrawBorderBtn.setEnabled(false);
		settingsPanel.add(redrawBorderBtn);
		
		
		
		this.add(settingsPanel, BorderLayout.NORTH);
		
		panel = new ExportableChartPanel(chart);
		panel.setFixedAspectRatio(true);

		panel.addChartSetEventListener(this);
		
		this.add(panel, BorderLayout.CENTER);
		
	}
	
	private void updateSettingsPanels(){
		if(this.hasDatasets()){

			if(this.getCellModel().getCell()==null){
				rotationPanel.setEnabled(false);
				makeMeshPanel.setEnabled(false);
				warpMeshPanel.setEnabled(false);
				redrawBorderBtn.setEnabled(false);
			} else {
				
				redrawBorderBtn.setEnabled(true);
				
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
			redrawBorderBtn.setEnabled(false);
		}
	}
							
	public void update(){

		CellularComponent component = this.getCellModel().getComponent();
								
			updateSettingsPanels();
			
			DefaultChartOptions options = new ChartOptionsBuilder()
					.setDatasets(getDatasets())
					.setCell(this.getCellModel().getCell())
					.setRotationMode(rotationPanel.getSelected())
					.setShowAnnotations(true)
					.setShowSignals(true)
					.setShowMesh(makeMeshPanel.isSelected())
					.setShowWarp(warpMeshPanel.isSelected())
					.setShowMeshEdges(false)
					.setShowMeshFaces(true)
					.setInvertYAxis( rotationPanel.getSelected().equals(RotationMode.ACTUAL) ) // only invert for actual
					.setCellularComponent(component)
					.setTarget(panel)
					.build();
			
			setChart(options);			
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
		
		updateSettingsPanels();
		
		DefaultChartOptions options = new ChartOptionsBuilder()
				.build();
		
		setChart(options);
		
	}


	@Override
	protected JFreeChart createPanelChartType(DefaultChartOptions options) throws Exception {
		return new OutlineChartFactory(options).makeCellOutlineChart();
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

	@Override
	public void chartSetEventReceived(ChartSetEvent e) {
		if(this.getCellModel().hasCell()  ){ 
			panel.restoreAutoBounds();
		}
		
	}

}
