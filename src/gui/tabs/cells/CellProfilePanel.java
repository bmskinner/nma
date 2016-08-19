package gui.tabs.cells;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.logging.Level;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import charting.charts.MorphologyChartFactory;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
import charting.options.TableOptions;
import components.generic.BorderTagObject;
import components.generic.ProfileType;
import components.generic.SegmentedProfile;
import components.nuclear.NucleusBorderSegment;
import components.nuclei.Nucleus;
import gui.DatasetEvent.DatasetMethod;
import gui.SignalChangeEvent;
import gui.dialogs.CellResegmentationDialog;
import gui.components.panels.ProfileTypeOptionsPanel;
import gui.components.panels.SegmentationDualChartPanel;
import gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;

@SuppressWarnings("serial")
public class CellProfilePanel extends AbstractCellDetailPanel {
	
	private SegmentationDualChartPanel dualPanel;
	
	private ProfileTypeOptionsPanel profileOptions  = new ProfileTypeOptionsPanel();
	
	private JPanel buttonsPanel;
	private JButton flipButton;
	private JButton resegmentButton;
	
	public CellProfilePanel(CellViewModel model) {
		super(model);

		this.setLayout(new BorderLayout());
		this.setBorder(null);
		
		dualPanel = new SegmentationDualChartPanel();
		dualPanel.addSignalChangeListener(this);
		this.add(dualPanel, BorderLayout.CENTER);
		
		buttonsPanel = makeButtonPanel();
		this.add(buttonsPanel, BorderLayout.NORTH);
				
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
		profileOptions.addActionListener(  e -> update()   );
		
		flipButton = new JButton("Reverse profile");
		panel.add(flipButton);
		flipButton.setEnabled(false);
		
		flipButton.addActionListener( e -> {
			this.setAnalysing(true);
			this.getCellModel().getCell().getNucleus().reverse();
			activeDataset().getCollection().getProfileManager().createProfileCollections();
			this.setAnalysing(false);
			refreshChartCache();
			fireDatasetEvent(DatasetMethod.REFRESH_CACHE, getDatasets());
			
		} );
		
		
		resegmentButton = new JButton("Resegment");
		panel.add(resegmentButton);
		resegmentButton.setEnabled(false);
		
		resegmentButton.addActionListener( e -> {
			new CellResegmentationDialog(getCellModel().getCell(), activeDataset());
			
		} );
		
		return panel;
		
		
	}
	
	public void setButtonsEnabled(boolean b){
		profileOptions.setEnabled(b);
		flipButton.setEnabled(b);
		resegmentButton.setEnabled(b);
	}
	
	public void update(){
		
		try{
			
			ProfileType type = profileOptions.getSelected();

			if(getCellModel().getCell()==null){
				JFreeChart chart = MorphologyChartFactory.getInstance().makeEmptyChart();
				
				dualPanel.setCharts(chart, chart);
				setButtonsEnabled(false);

			} else {
				
				SegmentedProfile profile = null;
				
				ChartOptions options = new ChartOptionsBuilder()
					.setDatasets(getDatasets())
					.setCell(this.getCellModel().getCell())
					.setNormalised(false)
					.setAlignment(ProfileAlignment.LEFT)
					.setTag(BorderTagObject.REFERENCE_POINT)
					.setShowMarkers(false)
					.setProfileType( type)
					.setSwatch(activeDataset().getSwatch())
					.setShowPoints(true)
					.build();
				
							
				JFreeChart chart = getChart(options);
				
				profile = this.getCellModel().getCell().getNucleus().getProfile(type, BorderTagObject.REFERENCE_POINT);
			
				
				/*
				 * Create the chart for the range panel
				 */
				
				ChartOptions rangeOptions = new ChartOptionsBuilder()
					.setDatasets(getDatasets())
					.setCell(this.getCellModel().getCell())
					.setNormalised(false)
					.setAlignment(ProfileAlignment.LEFT)
					.setTag(BorderTagObject.REFERENCE_POINT)
					.setShowMarkers(false)
					.setProfileType( type)
					.setSwatch(activeDataset().getSwatch())
					.setShowPoints(false)
					.build();
				
				JFreeChart rangeChart = getChart(rangeOptions);
				
				dualPanel.setCharts(chart, profile, false, rangeChart);
				setButtonsEnabled(true);		
			}

		} catch(Exception e){
			error("Error updating cell panel", e);
			JFreeChart chart = MorphologyChartFactory.getInstance().makeEmptyChart();
			dualPanel.setCharts(chart, chart);
			setButtonsEnabled(false);
		}
		
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
		if(event.type().contains("UpdateSegment") ){
			fine("Heard segment update request");
			
			try{

				String[] array = event.type().split("\\|");
				int selectedSegMidpoint = Integer.valueOf(array[1]);
				String index = array[2];
				int indexValue = Integer.valueOf(index);

				Nucleus n = this.getCellModel().getCell().getNucleus();
				SegmentedProfile profile = n.getProfile(ProfileType.ANGLE, BorderTagObject.REFERENCE_POINT);

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
				refreshChartCache();
				fireDatasetEvent(DatasetMethod.REFRESH_CACHE, getDatasets());
			} catch(Exception e){
				error("Error updating segment", e);
			}

		}

	}
	
	private void updateSegmentIndex(boolean start, int index, NucleusBorderSegment seg, Nucleus n, SegmentedProfile profile) throws Exception{
		
		int startPos = start ? seg.getStartIndex() : seg.getEndIndex();
		int newStart = start ? index : seg.getStartIndex();
		int newEnd   = start ? seg.getEndIndex() : index;
		
		int rawOldIndex =  n.getOffsetBorderIndex(BorderTagObject.REFERENCE_POINT, startPos);

						
		if(profile.update(seg, newStart, newEnd)){
			n.setProfile(ProfileType.ANGLE, BorderTagObject.REFERENCE_POINT, profile);
			finest("Updated nucleus profile with new segment boundaries");
			
			/* Check the border tags - if they overlap the old index
			 * replace them. 
			 */
			int rawIndex = n.getOffsetBorderIndex(BorderTagObject.REFERENCE_POINT, index);

			finest("Updating to index "+index+" from reference point");
			finest("Raw old border point is index "+rawOldIndex);
			finest("Raw new border point is index "+rawIndex);
			
			if(n.hasBorderTag(rawOldIndex)){						
				BorderTagObject tagToUpdate = n.getBorderTag(rawOldIndex);
				log(Level.FINE, "Updating tag "+tagToUpdate);
				n.setBorderTag(tagToUpdate, rawIndex);	
				
				// Update intersection point if needed
				if(tagToUpdate.equals(BorderTagObject.ORIENTATION_POINT)){
					n.setBorderTag(BorderTagObject.INTERSECTION_POINT, n.getBorderIndex(n.findOppositeBorder(n.getBorderTag(BorderTagObject.ORIENTATION_POINT))));
				}
				
			} else {
				finest("No border tag needing update at index "+rawOldIndex+" from reference point");
			}
		} else {
			log("Updating "+seg.getStartIndex()+" to index "+index+" failed: "+seg.getLastFailReason());
		}
	}
	
	@Override
	public void refreshChartCache(){
		clearChartCache();
		finest("Updating chart after clear");
		this.update();
	}

}
