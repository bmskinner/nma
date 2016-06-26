package gui.tabs.cells;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.UUID;
import java.util.logging.Level;

import javax.swing.JButton;
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
import components.nuclear.NucleusBorderSegment;
import components.nuclei.Nucleus;
import gui.DatasetEvent.DatasetMethod;
import gui.InterfaceEvent.InterfaceMethod;
import gui.SignalChangeEvent;
import gui.components.DraggableOverlayChartPanel;
import gui.components.PositionSelectionChartPanel;
import gui.components.RectangleOverlayObject;
import gui.components.panels.ProfileTypeOptionsPanel;
import gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;
import gui.tabs.editing.SegmentsEditingPanel;

@SuppressWarnings("serial")
public class CellProfilePanel extends AbstractCellDetailPanel {
	
	private DraggableOverlayChartPanel chartPanel; // for displaying the legnth of a given segment
	private PositionSelectionChartPanel rangePanel; // a small chart to show the entire profile
	
	private ProfileTypeOptionsPanel profileOptions  = new ProfileTypeOptionsPanel();
	
	private JPanel buttonsPanel;
	private JButton flipButton;
	
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
		
		flipButton = new JButton("Reverse profile");
		panel.add(flipButton);
		flipButton.setEnabled(false);
		
		flipButton.addActionListener( e -> {
			this.setAnalysing(true);
			activeCell.getNucleus().reverse();
			activeDataset().getCollection().getProfileManager().createProfileCollections();
			this.setAnalysing(false);
			fireDatasetEvent(DatasetMethod.REFRESH_CACHE, getDatasets());
			
		} );
		
		return panel;
		
		
	}
	
	public void setButtonsEnabled(boolean b){
		profileOptions.setEnabled(b);
		flipButton.setEnabled(b);
	}
	
	public void update(Cell cell){

		super.update(cell);
		try{
			
			ProfileType type = profileOptions.getSelected();

			if(cell==null){
				JFreeChart chart = MorphologyChartFactory.getInstance().makeEmptyChart();
				chartPanel.setChart(chart);
				rangePanel.setChart(chart);
				setButtonsEnabled(false);

			} else {
				
				SegmentedProfile profile = null;
				
				ChartOptions options = new ChartOptionsBuilder()
					.setDatasets(getDatasets())
					.setCell(activeCell)
					.setNormalised(false)
					.setAlignment(ProfileAlignment.LEFT)
					.setTag(BorderTag.REFERENCE_POINT)
					.setShowMarkers(false)
					.setProfileType( type)
					.setSwatch(activeDataset().getSwatch())
					.setShowPoints(true)
					.build();
				
							
				JFreeChart chart = getChart(options);
				
				profile = activeCell.getNucleus().getProfile(type, BorderTag.REFERENCE_POINT);
								
				chartPanel.setChart(chart, profile, false); // use the profile, don't normalise
				updateChartPanelRange();
				
				
				/*
				 * Create the chart for the range panel
				 */
				
				ChartOptions rangeOptions = new ChartOptionsBuilder()
					.setDatasets(getDatasets())
					.setCell(activeCell)
					.setNormalised(false)
					.setAlignment(ProfileAlignment.LEFT)
					.setTag(BorderTag.REFERENCE_POINT)
					.setShowMarkers(false)
					.setProfileType( type)
					.setSwatch(activeDataset().getSwatch())
					.setShowPoints(false)
					.build();
				
				JFreeChart rangeChart = getChart(rangeOptions);
				
				rangePanel.setChart(rangeChart);

				setButtonsEnabled(true);		
			}

		} catch(Exception e){
			error("Error updating cell panel", e);
			JFreeChart chart = MorphologyChartFactory.getInstance().makeEmptyChart();
			chartPanel.setChart(chart);
			rangePanel.setChart(chart);
			setButtonsEnabled(false);
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
	
	@Override
	public void signalChangeReceived(SignalChangeEvent event) {
		if(event.type().contains("UpdateSegment") && event.getSource().equals(chartPanel)){
			fine("Heard segment update request");
			
			try{

				String[] array = event.type().split("\\|");
				int selectedSegMidpoint = Integer.valueOf(array[1]);
				String index = array[2];
				int indexValue = Integer.valueOf(index);

				Nucleus n = activeCell.getNucleus();
				SegmentedProfile profile = n.getProfile(ProfileType.ANGLE, BorderTag.REFERENCE_POINT);

				/*
				 * The numbering of segments is adjusted for profile charts, so we can't rely on 
				 * the segment name stored in the profile.
				 * 
				 * Get the name via the midpoint index of the segment that was selected. 
				 */
				NucleusBorderSegment seg = profile.getSegmentContaining(selectedSegMidpoint);

//				carry out the update
				updateSegmentIndex(true, indexValue, seg, n, profile);

				n.updateVerticallyRotatedNucleus();
				
				// Recache necessary charts
				this.clearChartCache();
				this.update(activeCell);
				fireDatasetEvent(DatasetMethod.REFRESH_CACHE, getDatasets());
			} catch(Exception e){
				error("Error updating segment", e);
			}

		}
		
		// Change the range of the main chart based on the lower chart  
		if(event.type().contains("UpdatePosition") && event.getSource().equals(rangePanel)){
			
			updateChartPanelRange();
			
		}

	}
	
	private void updateSegmentIndex(boolean start, int index, NucleusBorderSegment seg, Nucleus n, SegmentedProfile profile) throws Exception{
		
		int startPos = start ? seg.getStartIndex() : seg.getEndIndex();
		int newStart = start ? index : seg.getStartIndex();
		int newEnd   = start ? seg.getEndIndex() : index;
		
		int rawOldIndex =  n.getOffsetBorderIndex(BorderTag.REFERENCE_POINT, startPos);

						
		if(profile.update(seg, newStart, newEnd)){
			n.setProfile(ProfileType.ANGLE, BorderTag.REFERENCE_POINT, profile);
			finest("Updated nucleus profile with new segment boundaries");
			
			/* Check the border tags - if they overlap the old index
			 * replace them. 
			 */
			int rawIndex = n.getOffsetBorderIndex(BorderTag.REFERENCE_POINT, index);

			finest("Updating to index "+index+" from reference point");
			finest("Raw old border point is index "+rawOldIndex);
			finest("Raw new border point is index "+rawIndex);
			
			if(n.hasBorderTag(rawOldIndex)){						
				BorderTag tagToUpdate = n.getBorderTag(rawOldIndex);
				log(Level.FINE, "Updating tag "+tagToUpdate);
				n.setBorderTag(tagToUpdate, rawIndex);	
				
				// Update intersection point if needed
				if(tagToUpdate.equals(BorderTag.ORIENTATION_POINT)){
					n.setBorderTag(BorderTag.INTERSECTION_POINT, n.getBorderIndex(n.findOppositeBorder(n.getBorderTag(BorderTag.ORIENTATION_POINT))));
				}
				
			} else {
				finest("No border tag needing update at index "+rawOldIndex+" from reference point");
			}
		} else {
			log("Updating "+seg.getStartIndex()+" to index "+index+" failed: "+seg.getLastFailReason());
		}
	}

}
