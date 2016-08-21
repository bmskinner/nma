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

import gui.DatasetEvent.DatasetMethod;
import gui.GlobalOptions;
import gui.RotationMode;
import gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import utility.ProfileException;
import analysis.AnalysisDataset;
import charting.charts.ConsensusNucleusChartFactory;
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
	
	private JButton resegmentBtn;
	private JButton reassignRpBtn;
	private JButton reverseProfileBtn;
	
	public CellResegmentationDialog(final Cell cell, final AnalysisDataset dataset){
		super( null );
		
		if(cell==null || dataset==null){
			throw new IllegalArgumentException("Cell or dataset is null");
		}
		
		this.cell = cell;
		this.dataset = dataset;
		this.workingCell = new Cell(cell);
		createUI();
		
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		this.pack();
		panel.getOutlinePanel().restoreAutoBounds(); // fixed aspect must be set after components are packed
		
		this.addWindowListener(new WindowAdapter() {
			
			public void windowClosing(WindowEvent e) {
				
				Object[] options = { "Save changes" , "Discard changes" };
				int save = JOptionPane.showOptionDialog(CellResegmentationDialog.this,
						"Save changes to cell segmentation?", 
						"Save cell?",
						JOptionPane.DEFAULT_OPTION, 
						JOptionPane.QUESTION_MESSAGE,
						null, options, options[0]);

				// Replace the input cell with the working cell
				if(save==0){
					dataset.getCollection().replaceCell(workingCell);

					// Trigger a dataset update and reprofiling
					dataset.getCollection().getProfileManager().createProfileCollections();
					fireDatasetEvent(DatasetMethod.REFRESH_CACHE, dataset);
				} 
				dispose();
				
			}
		});
		
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

			JFreeChart outlineChart = ConsensusNucleusChartFactory.getInstance().makeEmptyChart();

			ChartPanel profile = new ExportableChartPanel(MorphologyChartFactory.getInstance().makeEmptyChart(ProfileType.ANGLE));
			ChartPanel outline = new FixedAspectRatioChartPanel(outlineChart);

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
			setEnabled(false);
		});
		panel.add(reassignRpBtn);
		
		resegmentBtn = new JButton("Resegment");
		resegmentBtn.addActionListener( e -> {
			this.isRunning = true;
			newSegments = new ArrayList<NucleusBorderSegment>();
			segCount    = 0;
			segStart    = workingCell.getNucleus().getBorderIndex(BorderTagObject.REFERENCE_POINT);
			log("Select endpoint for segment 0");
			setEnabled(false);
		});
		panel.add(resegmentBtn);
		
		reverseProfileBtn = new JButton("Flip profile");
		reverseProfileBtn.addActionListener( e -> {
			workingCell.getNucleus().reverse();
			updateCharts(workingCell);
		});
		panel.add(reverseProfileBtn);
		
		JButton undoBtn = new JButton("Undo");
		undoBtn.addActionListener( e ->{
			workingCell = new Cell(cell);
			updateCharts(workingCell);
			
		});
		panel.add(undoBtn);
		
		
		return panel;
	}
	
	@Override
	public void setEnabled(boolean b){
		resegmentBtn.setEnabled(b);
		reassignRpBtn.setEnabled(b);
		reverseProfileBtn.setEnabled(b);
	}
	
	private void resegmentationComplete(){

		try {
			isRunning = false;
			setEnabled(true);

			Nucleus n = workingCell.getNucleus();
			UUID id = n.getProfile(ProfileType.ANGLE, BorderTagObject.REFERENCE_POINT).getSegments().get(segCount).getID();


			NucleusBorderSegment last = new NucleusBorderSegment(segStart, n.getBorderIndex(BorderTagObject.REFERENCE_POINT), n.getBorderLength(), id);
			newSegments.add(last);
			finer("Added "+last.toString());

			finer("Assigned all segments");
			try {
				NucleusBorderSegment.linkSegments(newSegments);
			} catch (ProfileException e) {
				error("Cannot link segments", e);
			}
			log("Resegmenting complete");
			SegmentedProfile newProfile = workingCell.getNucleus().getProfile(ProfileType.ANGLE);
			newProfile.setSegments(newSegments);
			finer("Segments added: ");
			finer(NucleusBorderSegment.toString(newSegments));
			finer("New profile:");
			finer(newProfile.toString());
			finer("RP index: "+n.getBorderIndex(BorderTagObject.REFERENCE_POINT));
			
			try {
				workingCell.getNucleus().setProfile(ProfileType.ANGLE, newProfile);
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
			setEnabled(true);
			
			int newRpIndex =  newTags.get(BorderTagObject.REFERENCE_POINT);
			
			fine("Selected RP index: "+newRpIndex);

			// Make a new cell with the updated RP	
			workingCell.getNucleus().setBorderTag(BorderTagObject.REFERENCE_POINT, newRpIndex);
			
			fine("Updated RP");

			// Draw the new cell
			updateCharts(workingCell);
			
			fine("Finished updating RP chart");


		} catch(Exception e){
			error("Error moving RP", e);
		}
	}
	
	private void updateCharts(Cell cell){
		
		finer("Making profile chart options");
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

		finer("Making outline chart options");
		ChartOptions outlineOptions = new ChartOptionsBuilder()
			.setDatasets(dataset)
			.setCell(cell)
			.setRotationMode(RotationMode.ACTUAL)
			.setShowAnnotations(false)
			.setInvertYAxis( true ) // only invert for actual
			.setCellularComponent(cell.getNucleus())
			.build();

		finer("Making charts");
		JFreeChart profileChart = MorphologyChartFactory.getInstance().makeIndividualNucleusProfileChart(profileOptions);
		JFreeChart outlineChart = OutlineChartFactory.getInstance().makeCellOutlineChart(outlineOptions);
		finer("Updating charts");
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

			UUID id = n.getProfile(ProfileType.ANGLE, BorderTagObject.REFERENCE_POINT).getSegments().get(segCount++).getID();

			segStop = n.getBorderIndex(p);
			NucleusBorderSegment seg = new NucleusBorderSegment(segStart, segStop, n.getBorderLength(), id);
			newSegments.add(seg);
			finer("Added "+seg.toString());
			segStart = segStop;

			log("Select endpoint for segment "+segCount);


			if(segCount==n.getProfile(ProfileType.ANGLE, BorderTagObject.REFERENCE_POINT).getSegmentCount()-1){
				resegmentationComplete();
			}



			
		}
		
	}

}
