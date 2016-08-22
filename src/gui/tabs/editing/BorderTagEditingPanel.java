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
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import components.generic.BorderTagObject;
import components.generic.ProfileType;
import charting.charts.MorphologyChartFactory;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
import charting.options.TableOptions;
import gui.BorderTagEventListener;
import gui.DatasetEvent;
import gui.InterfaceEvent;
import gui.components.BorderTagEvent;
import gui.components.ColourSelecter.ColourSwatch;
import gui.components.panels.BorderTagDualChartPanel;
import gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;
import gui.dialogs.RulesetDialog;


@SuppressWarnings("serial")
public class BorderTagEditingPanel extends AbstractEditingPanel implements ActionListener, BorderTagEventListener {

	private JPanel buttonsPanel;
	
	private JButton ruleSetButton;
	
	private static final String STR_SHOW_RULESETS     = "Rulesets";
	
	private BorderTagDualChartPanel dualPanel;
	
	public BorderTagEditingPanel() {
		
		super();
		this.setLayout(new BorderLayout());
		
		buttonsPanel = makeButtonPanel();
		this.add(buttonsPanel, BorderLayout.NORTH);
		setButtonsEnabled(false);
		
		dualPanel = new BorderTagDualChartPanel();
		dualPanel.addBorderTagEventListener(this);
		
		this.add(dualPanel, BorderLayout.CENTER);

		

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
		
		dualPanel.setCharts(chart, rangeChart);

	}
	

	@Override
	protected void updateMultiple() {
		updateNull();
		
		
	}
	
	@Override
	protected void updateNull() {
		setButtonsEnabled(false);
		
		dualPanel.setCharts(MorphologyChartFactory.getInstance().makeEmptyChart(), 
				MorphologyChartFactory.getInstance().makeEmptyChart());
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

	@Override
	public void borderTagEventReceived(BorderTagEvent event) {
		if(event.getSource() instanceof JMenuItem){
			setBorderTagAction(event.getTag(), event.getIndex());
		}
		
	}

}
