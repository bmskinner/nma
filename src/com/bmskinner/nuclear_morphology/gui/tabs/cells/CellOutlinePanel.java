package com.bmskinner.nuclear_morphology.gui.tabs.cells;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.charting.charts.ConsensusNucleusChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.MorphologyChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.OutlineChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.gui.ChartOptionsRenderedEvent;
import com.bmskinner.nuclear_morphology.gui.ChartSetEvent;
import com.bmskinner.nuclear_morphology.gui.ChartSetEventListener;
import com.bmskinner.nuclear_morphology.gui.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.RotationMode;
import com.bmskinner.nuclear_morphology.gui.components.panels.GenericCheckboxPanel;
import com.bmskinner.nuclear_morphology.gui.components.panels.RotationSelectionSettingsPanel;
import com.bmskinner.nuclear_morphology.gui.dialogs.CellCollectionOverviewDialog;

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
	
	private synchronized void updateSettingsPanels(){

		if(this.isMultipleDatasets() || ! this.hasDatasets()){
			rotationPanel.setEnabled(false);
			makeMeshPanel.setEnabled(false);
			warpMeshPanel.setEnabled(false);
			redrawBorderBtn.setEnabled(false);
			return;
		}



		if( ! this.getCellModel().hasCell()){
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
	}
							
	public synchronized void update(){
		
		if(this.isMultipleDatasets() || ! this.hasDatasets()){
			panel.setChart(MorphologyChartFactory.createEmptyChart());
			return;
		}

		CellularComponent component = this.getCellModel().getComponent();

		updateSettingsPanels();

		ChartOptions options = new ChartOptionsBuilder()
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
		panel.setChart(MorphologyChartFactory.createEmptyChart());
		updateSettingsPanels();

		
	}
	
	@Override
	public void setChartsAndTablesLoading(){
		super.setChartsAndTablesLoading();
		
		panel.setChart(MorphologyChartFactory.createLoadingChart());
	}


	@Override
	protected JFreeChart createPanelChartType(ChartOptions options){
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
