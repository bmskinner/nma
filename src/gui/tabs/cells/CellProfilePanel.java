package gui.tabs.cells;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JPanel;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import charting.charts.MorphologyChartFactory;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
import charting.options.TableOptions;
import components.Cell;
import components.generic.BorderTag;
import components.generic.ProfileType;
import components.generic.SegmentedProfile;
import components.nuclei.Nucleus;
import gui.components.DraggableOverlayChartPanel;
import gui.components.PositionSelectionChartPanel;
import gui.components.RectangleOverlayObject;
import gui.components.panels.ProfileTypeOptionsPanel;
import gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;

@SuppressWarnings("serial")
public class CellProfilePanel extends AbstractCellDetailPanel {
	
	private DraggableOverlayChartPanel chartPanel; // for displaying the legnth of a given segment
	private PositionSelectionChartPanel rangePanel; // a small chart to show the entire profile
	
	private ProfileTypeOptionsPanel profileOptions  = new ProfileTypeOptionsPanel();
	
	private JPanel buttonsPanel;
	
	public CellProfilePanel() {
		super();

		this.setLayout(new BorderLayout());
		this.setBorder(null);
		Dimension minimumChartSize = new Dimension(50, 100);
		Dimension preferredChartSize = new Dimension(400, 300);
		
		JFreeChart profileChart = MorphologyChartFactory.getInstance().makeEmptyChart();
		chartPanel = new DraggableOverlayChartPanel(profileChart, null, true);
		
		chartPanel.setMinimumSize(minimumChartSize);
		chartPanel.setPreferredSize(preferredChartSize);
		chartPanel.setMinimumDrawWidth( 0 );
		chartPanel.setMinimumDrawHeight( 0 );
		chartPanel.addSignalChangeListener(this);
		this.add(chartPanel, BorderLayout.CENTER);
		
		buttonsPanel = makeButtonPanel();
		this.add(buttonsPanel, BorderLayout.NORTH);
		
		
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
		
		setButtonsEnabled(false);
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
		profileOptions.addActionListener(  e -> update(activeCell)   );
		
		
		return panel;
		
		
	}
	
	public void setButtonsEnabled(boolean b){
		profileOptions.setEnabled(b);
	}
	
	public void update(Cell cell){

		super.update(cell);
		try{
			
			ProfileType type = profileOptions.getSelected();

			if(cell==null){
				JFreeChart chart = MorphologyChartFactory.getInstance().makeEmptyChart();
				chartPanel.setChart(chart);
				rangePanel.setChart(chart);
				profileOptions.setEnabled(false);

			} else {
				
				SegmentedProfile profile = null;
				
				ChartOptions options = new ChartOptionsBuilder()
					.setDatasets(getDatasets())
					.setCell(activeCell)
					.setNormalised(true)
					.setAlignment(ProfileAlignment.LEFT)
					.setTag(BorderTag.REFERENCE_POINT)
					.setShowMarkers(false)
					.setProfileType( type)
					.setSwatch(activeDataset().getSwatch())
					.setShowPoints(true)
					.build();
				
							
				JFreeChart chart = getChart(options);
				
				profile = activeDataset().getCollection()
						.getProfileCollection(type)
						.getSegmentedProfile(BorderTag.REFERENCE_POINT);
				
				chartPanel.setChart(chart, profile, true);
				updateChartPanelRange();
				
				
				/*
				 * Create the chart for the range panel
				 */
				
				ChartOptions rangeOptions = new ChartOptionsBuilder()
					.setDatasets(getDatasets())
					.setCell(activeCell)
					.setNormalised(true)
					.setAlignment(ProfileAlignment.LEFT)
					.setTag(BorderTag.REFERENCE_POINT)
					.setShowMarkers(false)
					.setProfileType( type)
					.setSwatch(activeDataset().getSwatch())
					.setShowPoints(false)
					.build();
				
				JFreeChart rangeChart = getChart(rangeOptions);
				
				rangePanel.setChart(rangeChart);

				profileOptions.setEnabled(true);
				Nucleus nucleus = cell.getNucleus();
				
//				ChartOptions options = new ChartOptionsBuilder()
//						.setSwatch(activeDataset().getSwatch())
//						.setProfileType(type)
//						.build();

//				JFreeChart chart = MorphologyChartFactory.makeIndividualNucleusProfileChart(nucleus, options);

//				chartPanel.setChart(chart, nucleus.getProfile(ProfileType.ANGLE, BorderTag.REFERENCE_POINT), false);
				
			}

		} catch(Exception e){
			error("Error updating cell panel", e);
			JFreeChart chart = MorphologyChartFactory.getInstance().makeEmptyChart();
			chartPanel.setChart(chart);
			rangePanel.setChart(chart);
			profileOptions.setEnabled(false);
		}

	}
	
	/**
	 * Set the main chart panel domain range to centre on the 
	 * position in the range panel, +- 10
	 */
	private void updateChartPanelRange(){
		
		RectangleOverlayObject ob = rangePanel.getDomainRectangleOverlay();
		double min = ob.getMinValue();
		double max = ob.getMaxValue();
		chartPanel.getChart().getXYPlot().getDomainAxis().setRange(min, max);
	}

	@Override
	protected TableModel createPanelTableType(TableOptions options) throws Exception {
		return null;
	}

	@Override
	protected JFreeChart createPanelChartType(ChartOptions options) throws Exception {
		return MorphologyChartFactory.getInstance().makeIndividualNucleusProfileChart( options);
	}

}
