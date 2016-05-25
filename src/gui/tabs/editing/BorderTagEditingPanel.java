/*******************************************************************************
 *  	Copyright (C) 2015, 2016 Ben Skinner
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
 *     GNU General Public License for more details. Gluten-free. May contain 
 *     traces of LDL asbestos. Avoid children using heavy machinery while under the
 *     influence of alcohol.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package gui.tabs.editing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import components.generic.BorderTag;
import components.generic.ProfileType;
import charting.charts.MorphologyChartFactory;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
import charting.options.TableOptions;
import gui.SignalChangeEvent;
import gui.DatasetEvent.DatasetMethod;
import gui.components.DraggableOverlayChartPanel;
import gui.components.PositionSelectionChartPanel;
import gui.components.ColourSelecter.ColourSwatch;
import gui.components.RectangleOverlayObject;
import gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;
import gui.tabs.DetailPanel;


@SuppressWarnings("serial")
public class BorderTagEditingPanel extends DetailPanel implements ActionListener {
	
	private DraggableOverlayChartPanel chartPanel; // for displaying the legnth of a given segment
	private PositionSelectionChartPanel rangePanel; // a small chart to show the entire profile
	
	private JPanel buttonsPanel;
	private JButton setButton;
	
	private static final String STR_SET_BORDER_TAG     = "Set border tag";
	private static final int RANGE_WINDOW = 30;
	
	public BorderTagEditingPanel() {
		
		super();
		
		this.setLayout(new BorderLayout());
		Dimension minimumChartSize = new Dimension(50, 100);
		Dimension preferredChartSize = new Dimension(400, 300);
		
		JFreeChart profileChart = MorphologyChartFactory.makeEmptyProfileChart(ProfileType.REGULAR);
		chartPanel = new DraggableOverlayChartPanel(profileChart, null, true);
		
		chartPanel.setMinimumSize(minimumChartSize);
		chartPanel.setPreferredSize(preferredChartSize);
		chartPanel.setMinimumDrawWidth( 0 );
		chartPanel.setMinimumDrawHeight( 0 );
		chartPanel.addSignalChangeListener(this);
		this.add(chartPanel, BorderLayout.CENTER);
				
		
		buttonsPanel = makeButtonPanel();
		this.add(buttonsPanel, BorderLayout.NORTH);
		setButtonsEnabled(false);
		
		/*
		 * TESTING: A second chart panel at the south
		 * with a domain overlay crosshair to define the 
		 * centre of the zoomed range on the 
		 * centre chart panel 
		 */
		JFreeChart rangeChart = MorphologyChartFactory.makeEmptyProfileChart(ProfileType.REGULAR);
		rangePanel = new PositionSelectionChartPanel(rangeChart);
		rangePanel.setPreferredSize(minimumChartSize);
		rangePanel.addSignalChangeListener(this);
		this.add(rangePanel, BorderLayout.SOUTH);
		updateChartPanelRange();
		
		

	}
	
	public void setButtonsEnabled(boolean b){
		setButton.setEnabled(b);
	}
	
	private JPanel makeButtonPanel(){
		
		JPanel panel = new JPanel(new FlowLayout()){
			@Override
			public void setEnabled(boolean b){
				super.setEnabled(b);
				for(Component c : this.getComponents()){
					c.setEnabled(b);
				}
			}
		};
		
		setButton = new JButton(STR_SET_BORDER_TAG);
		setButton.addActionListener(this);
		panel.add(setButton);
				
		return panel;
		
		
	}
	
	/**
	 * Set the main chart panel domain range to centre on the 
	 * position in the range panel, +- 10
	 */
	private void updateChartPanelRange(){
		
		RectangleOverlayObject ob = rangePanel.getDomainRectangleOverlay();
//		double xValue = rangePanel.getDomainCrosshairPosition();
//		finest("Range panel crosshair is at "+xValue);
		
		double min = ob.getMinValue();
		double max = ob.getMaxValue();
		
//		double min = xValue-RANGE_WINDOW;
//		double max = xValue+RANGE_WINDOW;
		chartPanel.getChart().getXYPlot().getDomainAxis().setRange(min, max);
	}
	
	@Override
	protected void updateSingle() throws Exception {
		
		setButtonsEnabled(true);
		
		ChartOptions options = new ChartOptionsBuilder()
			.setDatasets(getDatasets())
			.setNormalised(false)
			.setAlignment(ProfileAlignment.LEFT)
			.setTag(BorderTag.REFERENCE_POINT)
			.setShowMarkers(true)
			.setProfileType( ProfileType.REGULAR)
			.setShowPoints(true)
			.setSwatch(ColourSwatch.NO_SWATCH)
			.setShowAnnotations(false)
			.build();
		
					
		JFreeChart chart = getChart(options);
				
		chartPanel.setChart(chart, null, false);
		updateChartPanelRange();
		
		
		/*
		 * Create the chart for the range panel
		 */
		
		ChartOptions rangeOptions = new ChartOptionsBuilder()
			.setDatasets(getDatasets())
			.setNormalised(false)
			.setAlignment(ProfileAlignment.LEFT)
			.setTag(BorderTag.REFERENCE_POINT)
			.setShowMarkers(true)
			.setProfileType( ProfileType.REGULAR)
			.setSwatch(ColourSwatch.NO_SWATCH)
			.setShowPoints(false)
			.setShowAnnotations(false)
			.build();
		
		JFreeChart rangeChart = getChart(rangeOptions);
		
		rangePanel.setChart(rangeChart);
	}
	

	@Override
	protected void updateMultiple() throws Exception {
		updateNull();
		
		
	}
	
	@Override
	protected void updateNull() throws Exception {
		setButtonsEnabled(false);
		chartPanel.setChart(MorphologyChartFactory.makeEmptyProfileChart(ProfileType.REGULAR));
	}
	
	@Override
	protected JFreeChart createPanelChartType(ChartOptions options) throws Exception {
		return MorphologyChartFactory.makeMultiSegmentedProfileChart(options);
	}
	
	@Override
	protected TableModel createPanelTableType(TableOptions options) throws Exception{
		return null;
	}
		
	
	@Override
	public void signalChangeReceived(SignalChangeEvent event) {
//		if(event.type().contains("UpdateSegment") && event.getSource().equals(chartPanel)){
//			log(Level.FINEST, "Heard update segment request");
//			try{
//
//				
//				SegmentsEditingPanel.this.setAnalysing(true);
//
//				String[] array = event.type().split("\\|");
//				int segMidpointIndex = Integer.valueOf(array[1]);
//				int index = Integer.valueOf(array[2]);
//				
//				UUID segID = activeDataset()
//						.getCollection()
//						.getProfileCollection(ProfileType.REGULAR)
//						.getSegmentedProfile(BorderTag.REFERENCE_POINT)
//						.getSegmentContaining(segMidpointIndex)
//						.getID();
//				updateSegmentStartIndex(segID, index);
//
//			} catch(Exception e){
//				log(Level.SEVERE, "Error updating segment", e);
//			} finally {
//				SegmentsEditingPanel.this.setAnalysing(false);
//			}
//
//		}
		
		
		// Change the range of the main chart based on the lower chart  
		if(event.type().contains("UpdatePosition") && event.getSource().equals(rangePanel)){
			
			updateChartPanelRange();
			
		}

	}
	
	
	private void setBorderTagAction(){

		BorderTag tag = (BorderTag) JOptionPane.showInputDialog(null, 
				"Choose tag to set",
				"Choose tag",
				JOptionPane.QUESTION_MESSAGE, 
				null, 
				BorderTag.values(), 
				BorderTag.TOP_VERTICAL);

		if(tag!=null){
			
			SpinnerNumberModel sModel = new SpinnerNumberModel(0, 0, 400, 1); // TODO: replace with profile max
			JSpinner spinner = new JSpinner(sModel);


			int option = JOptionPane.showOptionDialog(null, 
					spinner, 
					"Choose the new position", 
					JOptionPane.OK_CANCEL_OPTION, 
					JOptionPane.QUESTION_MESSAGE, null, null, null);
			
			if (option == JOptionPane.CANCEL_OPTION) {
				log("Cancelled update");
				return;
				
			} else if (option == JOptionPane.OK_OPTION)	{
				
				int newTagIndex = (int) spinner.getModel().getValue();
				
				log("Updating "+tag+" to index "+newTagIndex);
				
				activeDataset()
					.getCollection()
					.getProfileManager()
					.updateBorderTag(tag, newTagIndex);
				
			}
			
			
		} else {
			log("Cancelled update");
			return;
		}


//			if(activeDataset()
//					.getCollection()
//					.getProfileManager().testSegmentsMergeable(mergeOption.getOne(), mergeOption.getTwo())){
//				activeDataset()
//					.getCollection()
//					.getProfileManager()
//					.mergeSegments(mergeOption.getOne(), mergeOption.getTwo());
//				
//				log(Level.FINEST, "Merged segments: "+mergeOption.toString());
//				log(Level.FINEST, "Refreshing chart cache for editing panel");
//				this.refreshChartCache();
//				log(Level.FINEST, "Firing general refresh cache request for loaded datasets");
//				fireDatasetEvent(DatasetMethod.REFRESH_CACHE, getDatasets());
//			} else {
//				JOptionPane.showMessageDialog(this, "Cannot merge segments: they would cross a core border tag");
//			}
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		if(e.getSource().equals(setButton)){
			fireDatasetEvent(DatasetMethod.CLEAR_CACHE, getDatasets());
			setBorderTagAction();
			
		}
		
	}

}
