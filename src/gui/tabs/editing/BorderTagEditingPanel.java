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
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.table.TableModel;

import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.entity.XYItemEntity;

import components.generic.BorderTag;
import components.generic.BorderTagObject;
import components.generic.BorderTag.BorderTagType;
import components.generic.ProfileType;
import charting.charts.DraggableOverlayChartPanel;
import charting.charts.MorphologyChartFactory;
import charting.charts.PositionSelectionChartPanel;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
import charting.options.TableOptions;
import gui.DatasetEvent;
import gui.InterfaceEvent;
import gui.SignalChangeEvent;
import gui.DatasetEvent.DatasetMethod;
import gui.InterfaceEvent.InterfaceMethod;
import gui.components.ColourSelecter.ColourSwatch;
import gui.components.RectangleOverlayObject;
import gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;
import gui.dialogs.RulesetDialog;
import gui.tabs.DetailPanel;


@SuppressWarnings("serial")
public class BorderTagEditingPanel extends DetailPanel implements ActionListener {
	
	private DraggableOverlayChartPanel chartPanel; // for displaying the legnth of a given segment
	private PositionSelectionChartPanel rangePanel; // a small chart to show the entire profile
	
	private JPanel buttonsPanel;
//	private JButton setButton;
	
	JPopupMenu popupMenu = new JPopupMenu("Popup");
	
	private int activeProfileIndex = 0;
	
	private JButton ruleSetButton;
	
	private static final String STR_SHOW_RULESETS     = "RuleSets";
	
//	private static final int RANGE_WINDOW = 10;
	
	public BorderTagEditingPanel() {
		
		super();
		
		this.setLayout(new BorderLayout());
		
		createBorderTagPopup();
		
		Dimension minimumChartSize = new Dimension(50, 100);
		Dimension preferredChartSize = new Dimension(400, 300);
		
		JFreeChart profileChart = MorphologyChartFactory.getInstance().makeEmptyChart();
		chartPanel = new DraggableOverlayChartPanel(profileChart, null, true);
		
		chartPanel.setMinimumSize(minimumChartSize);
		chartPanel.setPreferredSize(preferredChartSize);
		chartPanel.setMinimumDrawWidth(  0 );
		chartPanel.setMinimumDrawHeight( 0 );
		chartPanel.addSignalChangeListener(this);
		this.add(chartPanel, BorderLayout.CENTER);
		
		chartPanel.addChartMouseListener(new ChartMouseListener() {

		    public void chartMouseClicked(ChartMouseEvent e) {
		    	XYItemEntity ent = (XYItemEntity) e.getEntity();
		    	int series = ent.getSeriesIndex();
		    	int item   = ent.getItem();
		    	double x   = ent.getDataset().getXValue(series, item);
		    	
		    	activeProfileIndex = (int) x;
		    	MouseEvent ev = e.getTrigger();
		    	popupMenu.show(ev.getComponent(), ev.getX(), ev.getY());

		    }

		    public void chartMouseMoved(ChartMouseEvent e) {
		    }
		    	
		});
				
		
		buttonsPanel = makeButtonPanel();
		this.add(buttonsPanel, BorderLayout.NORTH);
		setButtonsEnabled(false);
		
		/*
		 * TESTING: A second chart panel at the south
		 * with a domain overlay crosshair to define the 
		 * centre of the zoomed range on the 
		 * centre chart panel 
		 */
		JFreeChart rangeChart = MorphologyChartFactory.getInstance().makeEmptyChart();
		rangePanel = new PositionSelectionChartPanel(rangeChart);
		rangePanel.setPreferredSize(minimumChartSize);
		rangePanel.addSignalChangeListener(this);

		this.add(rangePanel, BorderLayout.SOUTH);
		updateChartPanelRange();
		
		

	}
	
	private void createBorderTagPopup(){
		
		for(BorderTagObject tag : BorderTagObject.values()){
			JMenuItem item = new JMenuItem(tag.toString());
		    
			item.addActionListener(new ActionListener() {
		      public void actionPerformed(ActionEvent e) {
//		        log("Selected "+tag);
		        setBorderTagAction(tag);
		      }
		    });
		    popupMenu.add(item);
		    
		    /*
		     * We can't handle changing the OP or RP yet -
		     * requires segment boundary changes
		     */
		    if( tag.equals(BorderTagObject.INTERSECTION_POINT)){
		    	item.setEnabled(false);
		    }
		}
		 
	}
	
	public void setButtonsEnabled(boolean b){
		ruleSetButton.setEnabled(b);
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
		
		
		
		JLabel text = new JLabel("Click a point to set as a border tag");
		panel.add(text);
		
		ruleSetButton = new JButton(STR_SHOW_RULESETS);
		ruleSetButton.addActionListener(this);
		panel.add(ruleSetButton);
				
		return panel;
		
		
	}
	
	/**
	 * Set the main chart panel domain range to centre on the 
	 * position in the range panel
	 */
	private void updateChartPanelRange(){
		
		RectangleOverlayObject ob = rangePanel.getDomainRectangleOverlay();
		
		double min = ob.getMinValue();
		double max = ob.getMaxValue();
		
		chartPanel.getChart().getXYPlot().getDomainAxis().setRange(min, max);
	}
	
	@Override
	protected void updateSingle() {
		
		setButtonsEnabled(true);
		
		ColourSwatch swatch = activeDataset().getSwatch();
		
		ChartOptions options = new ChartOptionsBuilder()
			.setDatasets(getDatasets())
			.setNormalised(false)
			.setAlignment(ProfileAlignment.LEFT)
			.setTag(BorderTagObject.REFERENCE_POINT)
			.setShowMarkers(true)
			.setProfileType( ProfileType.ANGLE)
			.setShowPoints(true)
			.setSwatch(swatch)
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
			.setTag(BorderTagObject.REFERENCE_POINT)
			.setShowMarkers(true)
			.setProfileType( ProfileType.ANGLE)
			.setSwatch(swatch)
			.setShowPoints(false)
			.setShowAnnotations(false)
			.build();
		
		JFreeChart rangeChart = getChart(rangeOptions);
		
		rangePanel.setChart(rangeChart);
	}
	

	@Override
	protected void updateMultiple() {
		updateNull();
		
		
	}
	
	@Override
	protected void updateNull() {
		setButtonsEnabled(false);
		chartPanel.setChart(MorphologyChartFactory.getInstance().makeEmptyChart());
		rangePanel.setChart(MorphologyChartFactory.getInstance().makeEmptyChart());
	}
	
	@Override
	protected JFreeChart createPanelChartType(ChartOptions options) throws Exception {
		return MorphologyChartFactory.getInstance().makeMultiSegmentedProfileChart(options);
	}
	
	@Override
	protected TableModel createPanelTableType(TableOptions options) throws Exception{
		return null;
	}
		
	
	@Override
	public void signalChangeReceived(SignalChangeEvent event) {		
		
		// Change the range of the main chart based on the lower chart  
		if(event.type().contains("UpdatePosition") && event.getSource().equals(rangePanel)){
			
			updateChartPanelRange();
			
		}

	}
	
	
	private void setBorderTagAction(BorderTagObject tag){

		if(tag!=null){
			
				int newTagIndex = activeProfileIndex;

				log("Updating "+tag+" to index "+newTagIndex);
				
				this.setAnalysing(true);

				activeDataset()
					.getCollection()
					.getProfileManager()
					.updateBorderTag(tag, newTagIndex);
				
				
				this.refreshChartCache();
				
				if(tag.type().equals(BorderTagType.CORE)){
					log("Resegmenting dataset");
					fireDatasetEvent(DatasetMethod.REFRESH_MORPHOLOGY, getDatasets());
				} else {					
					fine("Firing refresh cache request for loaded datasets");
					fireInterfaceEvent(InterfaceMethod.RECACHE_CHARTS);
				}

				this.setAnalysing(false);

			
		} else {
			fine("Tag is null");
			return;
		}

	}

	@Override
	public void actionPerformed(ActionEvent e) {

		if(e.getSource()==ruleSetButton){
			RulesetDialog d =  new RulesetDialog(activeDataset());
			d.addInterfaceEventListener(this);
			d.addDatasetEventListener(this);
			d.setVisible(true);
		}
		
	}
	
	@Override
	public void interfaceEventReceived(InterfaceEvent event){
    	super.interfaceEventReceived(event);// Pass messages upwards
    	
    	if(event.getSource() instanceof RulesetDialog){
    		fine("Heard interface event");
    		fireInterfaceEvent(event.method());
    	}
    	
    }

	@Override
    public void datasetEventReceived(DatasetEvent event){
    	super.datasetEventReceived(event);
    	
    	if(event.getSource() instanceof RulesetDialog){
    		fine("Heard dataset event");
    		fireDatasetEvent(event.method(), event.getDatasets());
    	}
    }

}
