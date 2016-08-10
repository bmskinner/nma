package gui.tabs.cells;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.table.TableModel;

import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.entity.XYItemEntity;

import charting.charts.DraggableOverlayChartPanel;
import charting.charts.MorphologyChartFactory;
import charting.charts.PositionSelectionChartPanel;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
import charting.options.TableOptions;
import components.Cell;
import components.generic.BorderTag;
import components.generic.BorderTagObject;
import components.generic.ProfileType;
import components.generic.SegmentedProfile;
import gui.DatasetEvent.DatasetMethod;
import gui.SignalChangeEvent;
import gui.components.RectangleOverlayObject;
import gui.components.panels.ProfileTypeOptionsPanel;
import gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;

@SuppressWarnings("serial")
public class CellBorderTagPanel extends AbstractCellDetailPanel {
		
		private DraggableOverlayChartPanel chartPanel; // for displaying the legnth of a given segment
		private PositionSelectionChartPanel rangePanel; // a small chart to show the entire profile
		
		private ProfileTypeOptionsPanel profileOptions  = new ProfileTypeOptionsPanel();
		
		private JPanel buttonsPanel;
		
		private JPopupMenu popupMenu = new JPopupMenu("Popup");
		
		private int activeProfileIndex = 0; // the position that has been clicked
		
		public CellBorderTagPanel(CellViewModel model) {
			super(model);

			this.setLayout(new BorderLayout());
			createBorderTagPopup();
			
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
		
		private void createBorderTagPopup(){
			
			for(BorderTagObject tag : BorderTagObject.values()){
				JMenuItem item = new JMenuItem(tag.toString());
			    
				item.addActionListener(new ActionListener() {
			      public void actionPerformed(ActionEvent e) {

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
			
			
			return panel;
			
			
		}
		
		public void setButtonsEnabled(boolean b){
			profileOptions.setEnabled(b);
		}
		
		public void update(){

			try{
				
				ProfileType type = profileOptions.getSelected();

				if( ! this.getCellModel().hasCell()){
					JFreeChart chart = MorphologyChartFactory.getInstance().makeEmptyChart();
					chartPanel.setChart(chart);
					rangePanel.setChart(chart);
					profileOptions.setEnabled(false);

				} else {
					
					SegmentedProfile profile = null;
					
					ChartOptions options = new ChartOptionsBuilder()
						.setDatasets(getDatasets())
						.setCell(this.getCellModel().getCell())
						.setNormalised(false)
						.setAlignment(ProfileAlignment.LEFT)
						.setTag(BorderTagObject.REFERENCE_POINT)
						.setShowMarkers(true)
						.setProfileType( type)
						.setSwatch(activeDataset().getSwatch())
						.setShowAnnotations(false)
						.setShowPoints(true)
						.build();
					
								
					JFreeChart chart = getChart(options);
					
					profile = this.getCellModel().getCell().getNucleus().getProfile(type, BorderTagObject.REFERENCE_POINT);
									
					chartPanel.setChart(chart, null, false); // no profile, don't normalise
					updateChartPanelRange();
					
					
					/*
					 * Create the chart for the range panel
					 */
					
					ChartOptions rangeOptions = new ChartOptionsBuilder()
						.setDatasets(getDatasets())
						.setCell(this.getCellModel().getCell())
						.setNormalised(false)
						.setAlignment(ProfileAlignment.LEFT)
						.setTag(BorderTagObject.REFERENCE_POINT)
						.setShowMarkers(true)
						.setProfileType( type)
						.setSwatch(activeDataset().getSwatch())
						.setShowPoints(false)
						.setShowAnnotations(false)
						.build();
					
					JFreeChart rangeChart = getChart(rangeOptions);
					
					rangePanel.setChart(rangeChart);

					profileOptions.setEnabled(true);				
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

					log("Updating nucleus "+tag+" to index "+newTagIndex);
					this.setAnalysing(true);
					
					this.getCellModel().getCell().getNucleus().setBorderTag(BorderTagObject.REFERENCE_POINT, tag, newTagIndex);
					this.getCellModel().getCell().getNucleus().updateVerticallyRotatedNucleus();
					
					
					if(tag.equals(BorderTagObject.REFERENCE_POINT)){
						// Update the profile aggregate to use the new RP
						activeDataset().getCollection().getProfileManager().createProfileCollections();
					}
					this.setAnalysing(false);
					this.refreshChartCache();
					this.fireDatasetEvent(DatasetMethod.REFRESH_CACHE, getDatasets());

				
			} else {
				fine("Tag is null");
				return;
			}

		}
		
		@Override
		public void refreshChartCache(){
			clearChartCache();
			finest("Updating chart after clear");
			this.update();
		}
}
