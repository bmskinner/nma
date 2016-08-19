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

package gui.dialogs;

import gui.GlobalOptions;
import gui.RotationMode;
import gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;






















import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import utility.ProfileException;
import analysis.AnalysisDataset;
import charting.charts.CoupledProfileOutlineChartPanel;
import charting.charts.CoupledProfileOutlineChartPanel.BorderPointEvent;
import charting.charts.CoupledProfileOutlineChartPanel.BorderPointEventListener;
import charting.charts.ExportableChartPanel;
import charting.charts.FixedAspectRatioChartPanel;
import charting.charts.MorphologyChartFactory;
import charting.charts.OutlineChartFactory;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
import components.Cell;
import components.generic.BorderTagObject;
import components.generic.ProfileType;
import components.generic.SegmentedProfile;
import components.nuclear.BorderPoint;
import components.nuclear.NucleusBorderSegment;
import components.nuclei.Nucleus;

@SuppressWarnings("serial")
public class CellResegmentationDialog extends MessagingDialog implements BorderPointEventListener{
	
	private CoupledProfileOutlineChartPanel panel;
	private Cell cell;
	
	private Cell workingCell;
	private AnalysisDataset dataset;
	
	private boolean isRunning = false;
	private boolean isSelectRP = false;
	private List<NucleusBorderSegment> newSegments;
	private Map<BorderTagObject, Integer> newTags;
	int segCount = 0;
	int segStart = 0;
	int segStop  = 0;
	
	JButton resegmentBtn;
	JButton reassignRpBtn;
	
	
	public CellResegmentationDialog(Cell cell, AnalysisDataset dataset){
		super();
		
		if(cell==null || dataset==null){
			throw new IllegalArgumentException("Cell or dataset is null");
		}
		
		this.cell = cell;
		this.dataset = dataset;
		this.workingCell = new Cell(cell);
		createUI();
		
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		this.pack();
		this.setLocationRelativeTo(null);
		this.setModal(false);
		this.setVisible(true);
		
	}
	
	private void createUI(){

		try{
			this.setLayout(new BorderLayout());
			this.setTitle("Resegmenting "+cell.getNucleus().getNameAndNumber());
			
			JPanel header = createHeader();
			this.add(header, BorderLayout.NORTH);
			
//			JTable table = new JTable();
			
			JPanel mainPanel = new JPanel(new BorderLayout());


			ChartPanel profile = new ExportableChartPanel(MorphologyChartFactory.getInstance().makeEmptyChart(ProfileType.ANGLE));
			ChartPanel outline = new ExportableChartPanel(OutlineChartFactory.getInstance().makeEmptyChart());

			panel = new CoupledProfileOutlineChartPanel(profile, outline, workingCell);
			panel.addBorderPointEventListener(this);
			
			updateCharts(workingCell);

			mainPanel.add(profile, BorderLayout.SOUTH);
			mainPanel.add(outline, BorderLayout.CENTER);
			this.add(mainPanel, BorderLayout.CENTER);
		} catch (Exception e){
			error("Error making UI", e);
		}
	}
	
	private JPanel createHeader(){
		JPanel panel = new JPanel(new FlowLayout());
		
		reassignRpBtn = new JButton("Reassign RP");
		reassignRpBtn.addActionListener( e -> {
			
			this.isSelectRP = true;
			newTags     = new HashMap<BorderTagObject, Integer>();
			log("Select RP");
			resegmentBtn.setEnabled(false);
			reassignRpBtn.setEnabled(false);
		});
		panel.add(reassignRpBtn);
		
		resegmentBtn = new JButton("Resegment");
		resegmentBtn.addActionListener( e -> {
			this.isRunning = true;
			newSegments = new ArrayList<NucleusBorderSegment>();
			segCount    = 0;
			segStart    = workingCell.getNucleus().getBorderIndex(BorderTagObject.REFERENCE_POINT);
			log("Select endpoint for seg 0");
			resegmentBtn.setEnabled(false);
			reassignRpBtn.setEnabled(false);
		});
		panel.add(resegmentBtn);
		
		JButton undoBtn = new JButton("Undo");
		undoBtn.addActionListener( e ->{
			workingCell = new Cell(cell);
			updateCharts(workingCell);
			
		});
		panel.add(undoBtn);
		
		
		return panel;
	}
	
	private void resegmentationComplete(){

		try {
			isRunning = false;
			resegmentBtn.setEnabled(true);
			reassignRpBtn.setEnabled(true);

			Nucleus n = workingCell.getNucleus();
			UUID id = n.getProfile(ProfileType.ANGLE, BorderTagObject.REFERENCE_POINT).getSegments().get(segCount).getID();


			NucleusBorderSegment last = new NucleusBorderSegment(segStart, n.getBorderIndex(BorderTagObject.REFERENCE_POINT), n.getBorderLength(), id);
			newSegments.add(last);

			log("Assigned all segments");
			try {
				NucleusBorderSegment.linkSegments(newSegments);
			} catch (ProfileException e) {
				error("Cannot link segments", e);
			}
			log("Resegmenting complete");

			SegmentedProfile oldProfile = workingCell.getNucleus().getProfile(ProfileType.ANGLE, BorderTagObject.REFERENCE_POINT);
			SegmentedProfile newProfile = new SegmentedProfile(oldProfile);

			newProfile.setSegments(newSegments);
			try {
				workingCell.getNucleus().setProfile(ProfileType.ANGLE, BorderTagObject.REFERENCE_POINT, newProfile);
			} catch (Exception e) {
				error("Error setting profile", e);
			}

			// Draw the new cell
			updateCharts(workingCell);
		} catch(Exception e){
			error("Error resegmenting", e);
		}

	}
	
	private void moveRPComplete(){

		try {
			isSelectRP = false;
			resegmentBtn.setEnabled(true);
			reassignRpBtn.setEnabled(true);
			
			int newRpIndex =  newTags.get(BorderTagObject.REFERENCE_POINT);
			
			log("Selected RP index: "+newRpIndex);

			// Make a new cell with the updated RP	
			workingCell.getNucleus().setBorderTag(BorderTagObject.REFERENCE_POINT, newRpIndex);
			
			log("Updated RP");

			// Draw the new cell
			updateCharts(workingCell);
			
			log("Finished updating RP chart");


		} catch(Exception e){
			error("Error moving RP", e);
		}
	}
	
	private void updateCharts(Cell cell){
		
		log("Making profile chart options");
		ChartOptions profileOptions = new ChartOptionsBuilder()
			.setDatasets(dataset)
			.setCell(cell)
			.setNormalised(false)
			.setAlignment(ProfileAlignment.LEFT)
			.setTag(BorderTagObject.REFERENCE_POINT)
			.setShowMarkers(false)
			.setProfileType( ProfileType.ANGLE )
			.setSwatch(GlobalOptions.getInstance().getSwatch())
			.setShowPoints(true)
			.build();

		log("Making outline chart options");
		ChartOptions outlineOptions = new ChartOptionsBuilder()
			.setDatasets(dataset)
			.setCell(cell)
			.setRotationMode(RotationMode.ACTUAL)
			.setShowAnnotations(false)
			.setInvertYAxis( true ) // only invert for actual
			.setCellularComponent(cell.getNucleus())
			.build();

		log("Making charts");
		JFreeChart profileChart = MorphologyChartFactory.getInstance().makeIndividualNucleusProfileChart(profileOptions);
		JFreeChart outlineChart = OutlineChartFactory.getInstance().makeCellOutlineChart(outlineOptions);
		log("Updating charts");
		panel.setCell(cell);
		panel.getProfilePanel().setChart(profileChart);
		panel.getOutlinePanel().setChart(outlineChart);
	}

	@Override
	public void borderPointEventReceived(BorderPointEvent event) {
		BorderPoint p = event.getPoint();
		Nucleus n = workingCell.getNucleus();
		
		if(isSelectRP){

			newTags.put(BorderTagObject.REFERENCE_POINT, n.getBorderIndex(p));
			moveRPComplete();
		}
		
		if(isRunning){

			UUID id = n.getProfile(ProfileType.ANGLE, BorderTagObject.REFERENCE_POINT).getSegments().get(segCount).getID();

			segStop = n.getBorderIndex(p);
			NucleusBorderSegment seg = new NucleusBorderSegment(segStart, segStop, n.getBorderLength(), id);
			newSegments.add(seg);
			log("Added new seg");
			segStart = segStop;

			segCount++;
			log("Select the endpoint for segment "+segCount);


			if(segCount==n.getProfile(ProfileType.ANGLE, BorderTagObject.REFERENCE_POINT).getSegmentCount()-1){
				resegmentationComplete();
			}



			
		}
		
	}

}
