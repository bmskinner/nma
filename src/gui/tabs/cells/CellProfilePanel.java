package gui.tabs.cells;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.UUID;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.jfree.chart.JFreeChart;

import charting.charts.MorphologyChartFactory;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
import components.generic.BorderTagObject;
import components.generic.ProfileType;
import components.generic.SegmentedProfile;
import components.nuclear.NucleusBorderSegment;
import components.nuclei.Nucleus;
import gui.DatasetEvent;
import gui.GlobalOptions;
import gui.DatasetEvent.DatasetMethod;
import gui.SegmentEvent;
import gui.dialogs.CellResegmentationDialog;
import gui.components.panels.ProfileTypeOptionsPanel;
import gui.components.panels.SegmentationDualChartPanel;
import gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;

@SuppressWarnings("serial")
public class CellProfilePanel extends AbstractCellDetailPanel {
	
	private SegmentationDualChartPanel dualPanel;
	
	private ProfileTypeOptionsPanel profileOptions  = new ProfileTypeOptionsPanel();
	
	private JPanel buttonsPanel;

	private JButton resegmentButton;
	
	// A JDialog is a top level container, and these are not subject to GC on disposal
	// according to https://stackoverflow.com/questions/15863178/memory-leaking-on-jdialog-closing
	// Hence, only keep one dialog, and prevent multiple copies spawning by loading the active cell
	// in when needed
	private CellResegmentationDialog resegDialog = new CellResegmentationDialog();
	
	
	public CellProfilePanel(CellViewModel model) {
		super(model); // lol

		this.setLayout(new BorderLayout());
		this.setBorder(null);
		
		dualPanel = new SegmentationDualChartPanel();
		dualPanel.addSegmentEventListener(this);
		this.add(dualPanel, BorderLayout.CENTER);
		
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
	
	public void update(){
		
		try{
			
			ProfileType type = profileOptions.getSelected();

			if(this.getCellModel().hasCell()){
				
				SegmentedProfile profile = this.getCellModel().getCell().getNucleus().getProfile(type, BorderTagObject.REFERENCE_POINT);
				
				
				ChartOptions options = new ChartOptionsBuilder()
					.setDatasets(getDatasets())
					.setCell(this.getCellModel().getCell())
					.setNormalised(false)
					.setAlignment(ProfileAlignment.LEFT)
					.setTag(BorderTagObject.REFERENCE_POINT)
					.setShowMarkers(false)
					.setProfileType( type)
					.setSwatch(GlobalOptions.getInstance().getSwatch())
					.setShowPoints(true)
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
					.setTag(BorderTagObject.REFERENCE_POINT)
					.setShowMarkers(false)
					.setProfileType( type)
					.setSwatch(GlobalOptions.getInstance().getSwatch())
					.setShowPoints(false)
					.setTarget(dualPanel.getRangePanel())
					.build();
				
				setChart(rangeOptions);
				
				dualPanel.setProfile(profile, false);

				setEnabled(true);	


			} else {
				JFreeChart chart = MorphologyChartFactory.getInstance().makeEmptyChart();
				
				dualPanel.setCharts(chart, chart);
				setEnabled(false);			
					
			}

		} catch(Exception e){
			error("Error updating cell panel", e);
			JFreeChart chart = MorphologyChartFactory.getInstance().makeEmptyChart();
			dualPanel.setCharts(chart, chart);
			setEnabled(false);
		}
		
	}
	


	@Override
	protected JFreeChart createPanelChartType(ChartOptions options) throws Exception {
		return MorphologyChartFactory.getInstance().makeIndividualNucleusProfileChart( options);
	}
	
	@Override
	public void datasetEventReceived(DatasetEvent event) {
		if(event.getSource() instanceof CellResegmentationDialog){ // Pass upwards
			fireDatasetEvent(event.method(), event.getDatasets());
		}
	}
		
	private void updateSegmentIndex(boolean start, int index, NucleusBorderSegment seg, Nucleus n, SegmentedProfile profile) throws Exception{
		
		boolean wasLocked = this.getCellModel().getCell().getNucleus().isLocked();
		this.getCellModel().getCell().getNucleus().setLocked(false);
		
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
				fine("Updating tag "+tagToUpdate);
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
		this.getCellModel().getCell().getNucleus().setLocked(wasLocked);
	}
	
	@Override
	public void refreshChartCache(){
		clearChartCache();
		finest("Updating chart after clear");
		this.update();
	}

	@Override
	public void segmentEventReceived(SegmentEvent event) {
		// TODO Auto-generated method stub
		
		if(event.getType()==SegmentEvent.MOVE_START_INDEX){
			try{

				
				UUID id = event.getId();
				int index = event.getIndex();


				Nucleus n = this.getCellModel().getCell().getNucleus();
				SegmentedProfile profile = n.getProfile(ProfileType.ANGLE, BorderTagObject.REFERENCE_POINT);

				/*
				 * The numbering of segments is adjusted for profile charts, so we can't rely on 
				 * the segment name stored in the profile.
				 * 
				 * Get the name via the midpoint index of the segment that was selected. 
				 */
				NucleusBorderSegment seg = profile.getSegment(id);

				//	Carry out the update
				updateSegmentIndex(true, index, seg, n, profile);

				n.updateVerticallyRotatedNucleus();

				// Recache necessary charts
				refreshChartCache();
				fireDatasetEvent(DatasetMethod.REFRESH_CACHE, getDatasets());
			} catch(Exception e){
				error("Error updating segment", e);
			}
		}
	}

}
