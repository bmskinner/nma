/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package gui.tabs.cells;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.jfree.chart.JFreeChart;

import analysis.AnalysisDataset;
import components.Cell;
import gui.RotationMode;
import gui.ThreadManager;
import charting.charts.ConsensusNucleusChartFactory;
import charting.charts.OutlineChartFactory;
import charting.charts.CoupledProfileOutlineChartPanel.BorderPointEvent;
import charting.charts.FixedAspectRatioChartPanel;
import charting.charts.CoupledProfileOutlineChartPanel.BorderPointEventListener;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;


/**
 * This dialog allows the border points in a CellularComponent to be removed or moved.
 * Upon completion, the border FloatPolygon is re-interpolated to provide a 
 * new BorderPoint list
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class CellBorderAdjustmentDialog extends AbstractCellEditingDialog implements BorderPointEventListener {
	
	private FixedAspectRatioChartPanel panel;
	
	private JButton deletePointsBtn;
	
	/*
	 * Notes:
	 * 
	 * Click a point to highlight / select it. 
	 * 
	 * Drag a window to select points in the border. These must become highlighted.
	 * 
	 * Option to delete highlighted points
	 * 
	 * Option to move highlighted points.
	 * 
	 * The border list must be updated to reflect changes and deletions
	 * 
	 * Show a minimap
	 * 
	 * Right mouse down - drag zoomed image
	 * Left mouse down - select ponts
	 * 
	 */
	
	public CellBorderAdjustmentDialog(CellViewModel model){
		super( model );
		
		panel.restoreAutoBounds(); // fixed aspect must be set after components are packed		
	}
	
	@Override
	public void load(final Cell cell, final AnalysisDataset dataset){
		super.load(cell, dataset);
		this.setTitle("Adjusting border in "+cell.getNucleus().getNameAndNumber());
		updateCharts(cell);
		setVisible(true);
	}
	
	@Override
	protected void createUI(){

		try{
			finer("Creating border adjustment dialog");
			this.setLayout(new BorderLayout());

			JPanel header = createHeader();
			this.add(header, BorderLayout.NORTH);
			
	
			JFreeChart chart = ConsensusNucleusChartFactory.getInstance().makeEmptyChart();

			panel = new FixedAspectRatioChartPanel(chart);
			panel.addBorderPointEventListener(this);


			this.add(panel, BorderLayout.CENTER);
		} catch (Exception e){
			error("Error making UI", e);
		}
	}
	
	private JPanel createHeader(){
		JPanel panel = new JPanel(new FlowLayout());
		
		deletePointsBtn = new JButton("Delete selected point(s)");
		deletePointsBtn.addActionListener( e -> {
			
			log("Clicked delete button");
		});
		panel.add(deletePointsBtn);
			
		
		JButton undoBtn = new JButton("Undo");
		undoBtn.addActionListener( e ->{
			workingCell = new Cell(cell);
			updateCharts(workingCell);
			
		});
		panel.add(undoBtn);
		
		return panel;
	}
	

	
	
	@Override
	protected void updateCharts(Cell cell){
		
		Runnable r = () ->{
	
			finer("Making outline chart options");
			ChartOptions outlineOptions = new ChartOptionsBuilder()
				.setDatasets(dataset)
				.setCell(cell)
				.setRotationMode(RotationMode.ACTUAL)
				.setShowAnnotations(false)
				.setInvertYAxis( true ) // only invert for actual
				.setShowPoints(true)
				.setCellularComponent(cell.getNucleus())
				.build();
	
			finer("Making chart");
			JFreeChart outlineChart = OutlineChartFactory.getInstance().makeCellOutlineChart(outlineOptions);
			finer("Updating chart");
			panel.setChart(outlineChart);
			panel.restoreAutoBounds();
		};
		
		ThreadManager.getInstance().submit(r);
	}

	@Override
	public void borderPointEventReceived(BorderPointEvent event) {
		log("Border point event received");
		
	}

}
