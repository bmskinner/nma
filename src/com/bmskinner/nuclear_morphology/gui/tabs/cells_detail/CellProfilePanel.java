package com.bmskinner.nuclear_morphology.gui.tabs.cells_detail;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.charting.charts.MorphologyChartFactory;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.gui.ChartSetEvent;
import com.bmskinner.nuclear_morphology.gui.ChartSetEventListener;
import com.bmskinner.nuclear_morphology.gui.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.GlobalOptions;
import com.bmskinner.nuclear_morphology.gui.SegmentEvent;
import com.bmskinner.nuclear_morphology.gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;
import com.bmskinner.nuclear_morphology.gui.components.panels.ProfileTypeOptionsPanel;
import com.bmskinner.nuclear_morphology.gui.components.panels.SegmentationDualChartPanel;
import com.bmskinner.nuclear_morphology.gui.dialogs.CellResegmentationDialog;

@SuppressWarnings("serial")
public class CellProfilePanel extends AbstractCellDetailPanel implements ChartSetEventListener {
	
	private SegmentationDualChartPanel dualPanel;
	
	private ProfileTypeOptionsPanel profileOptions  = new ProfileTypeOptionsPanel();
	
	private JPanel buttonsPanel;

	private JButton resegmentButton;
	
	// A JDialog is a top level container, and these are not subject to GC on disposal
	// according to https://stackoverflow.com/questions/15863178/memory-leaking-on-jdialog-closing
	// Hence, only keep one dialog, and prevent multiple copies spawning by loading the active cell
	// in when needed
	private final CellResegmentationDialog resegDialog;
	
	
	public CellProfilePanel(final CellViewModel model) {
		super(model); // lol
		
		resegDialog = new CellResegmentationDialog(model);

		this.setLayout(new BorderLayout());
		this.setBorder(null);
		
		dualPanel = new SegmentationDualChartPanel();
		dualPanel.addSegmentEventListener(this);
		dualPanel.getMainPanel().addChartSetEventListener(this);
		
		
		JPanel chartPanel = new JPanel();
		chartPanel.setLayout(new GridBagLayout());
		
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.EAST;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth  = 1; 
		c.gridheight = 1;
		c.fill = GridBagConstraints.BOTH;      //reset to default
		c.weightx = 1.0; 
		c.weighty = 0.7;
		
		chartPanel.add(dualPanel.getMainPanel(), c);
		c.weighty = 0.3;
		c.gridx = 0;
		c.gridy = 1;
		chartPanel.add(dualPanel.getRangePanel(), c);
		
		this.add(chartPanel, BorderLayout.CENTER);
		
		buttonsPanel = makeButtonPanel();
		this.add(buttonsPanel, BorderLayout.NORTH);
		
		resegDialog.addDatasetEventListener(this);
				
		setEnabled(false);
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
		
		panel.add(profileOptions);
		profileOptions.addActionListener(  e -> update()   );
		
		resegmentButton = new JButton("Resegment");
		panel.add(resegmentButton);
		resegmentButton.setEnabled(false);
		
		resegmentButton.addActionListener( e -> {
				
			resegDialog.load(getCellModel().getCell(), activeDataset());
		} );
		
		return panel;
		
		
	}
	
	public void setEnabled(boolean b){
		super.setEnabled(b);
		profileOptions.setEnabled(b);
		resegmentButton.setEnabled(b);
	}
	
	public synchronized void update(){
		
		try{
			
			ProfileType type = profileOptions.getSelected();

			if(this.getCellModel().hasCell()){
				
				ISegmentedProfile profile = this.getCellModel().getCell().getNucleus().getProfile(type, Tag.REFERENCE_POINT);
				
				
				ChartOptions options = new ChartOptionsBuilder()
					.setDatasets(getDatasets())
					.setCell(this.getCellModel().getCell())
					.setNormalised(false)
					.setAlignment(ProfileAlignment.LEFT)
					.setTag(Tag.REFERENCE_POINT)
					.setShowMarkers(false)
					.setProfileType( type)
					.setSwatch(GlobalOptions.getInstance().getSwatch())
					.setShowPoints(true)
					.setShowXAxis(false)
					.setShowYAxis(false)
					.setTarget(dualPanel.getMainPanel())
					.build();
				
				setChart(options);		
						
				
				
				/*
				 * Create the chart for the range panel
				 */
				
				ChartOptions rangeOptions = new ChartOptionsBuilder()
					.setDatasets(getDatasets())
					.setCell(this.getCellModel().getCell())
					.setNormalised(false)
					.setAlignment(ProfileAlignment.LEFT)
					.setTag(Tag.REFERENCE_POINT)
					.setShowMarkers(false)
					.setProfileType( type)
					.setSwatch(GlobalOptions.getInstance().getSwatch())
					.setShowPoints(false)
					.setShowXAxis(false)
					.setShowYAxis(false)
					.setTarget(dualPanel.getRangePanel())
					.build();
				
				setChart(rangeOptions);
				
				

				setEnabled(true);	


			} else {
				JFreeChart chart1 = MorphologyChartFactory.createEmptyChart();
				JFreeChart chart2 = MorphologyChartFactory.createEmptyChart();
				
				dualPanel.setCharts(chart1, chart2);
				setEnabled(false);			
					
			}

		} catch(Exception e){
			error("Error updating cell panel", e);
			JFreeChart chart1 = MorphologyChartFactory.createEmptyChart();
			JFreeChart chart2 = MorphologyChartFactory.createEmptyChart();
			dualPanel.setCharts(chart1, chart2);
			setEnabled(false);
		}
		
	}
	
	@Override
	public void setChartsAndTablesLoading(){
		super.setChartsAndTablesLoading();
		JFreeChart chart1 = MorphologyChartFactory.createLoadingChart();
		JFreeChart chart2 = MorphologyChartFactory.createLoadingChart();
		
		dualPanel.setCharts(chart1, chart2);
	}
	
	@Override
	public void chartSetEventReceived(ChartSetEvent e) {
		ISegmentedProfile profile;
		try {
			profile = this.getCellModel()
					.getCell()
					.getNucleus()
					.getProfile(profileOptions.getSelected(), Tag.REFERENCE_POINT);
			dualPanel.setProfile(profile, false);
		} catch (ProfileException | UnavailableBorderTagException | UnavailableProfileTypeException e1) {
			fine("Error getting profile", e1);
//			JFreeChart chart1 = MorphologyChartFactory.makeErrorChart();
//			JFreeChart chart2 = MorphologyChartFactory.makeErrorChart();
//			dualPanel.setCharts(chart1, chart2);
		}
		
		
	}


	@Override
	protected JFreeChart createPanelChartType(ChartOptions options){
		return new MorphologyChartFactory(options).makeIndividualNucleusProfileChart( );
	}
	
	@Override
	public void datasetEventReceived(DatasetEvent event) {
		if(event.getSource() instanceof CellResegmentationDialog){ // Pass upwards
			fireDatasetEvent(event.method(), event.getDatasets());
		}
	}
		
	
	@Override
	public void refreshChartCache(){
		clearChartCache();
		finest("Updating chart after clear");
		this.update();
	}

	@Override
	public void segmentEventReceived(SegmentEvent event) {
		
		if(event.getType()==SegmentEvent.MOVE_START_INDEX){
			try{
				
				// This is a manual change, so disable any lock
				this.getCellModel().getCell().getNucleus().setLocked(false);

				//	Carry out the update
				activeDataset().getCollection()
					.getProfileManager()
					.updateCellSegmentStartIndex(getCellModel().getCell(), event.getId(), event.getIndex()); 
				
				// even if no lock was previously set, there should be one now a manual adjustment was made
				this.getCellModel().getCell().getNucleus().setLocked(true);

				// Recache necessary charts
				refreshChartCache();
				fireDatasetEvent(DatasetEvent.REFRESH_CACHE, getDatasets());
			} catch(Exception e){
				error("Error updating segment", e);
			}
		}
	}


}
