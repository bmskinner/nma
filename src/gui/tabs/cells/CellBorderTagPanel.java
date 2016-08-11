package gui.tabs.cells;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import javax.swing.JMenuItem;
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
import gui.BorderTagEventListener;
import gui.DatasetEvent.DatasetMethod;
import gui.components.BorderTagEvent;
import gui.components.panels.BorderTagDualChartPanel;
import gui.components.panels.ProfileTypeOptionsPanel;
import gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;

@SuppressWarnings("serial")
public class CellBorderTagPanel extends AbstractCellDetailPanel implements BorderTagEventListener {
		
		private BorderTagDualChartPanel dualPanel;
		
		private ProfileTypeOptionsPanel profileOptions  = new ProfileTypeOptionsPanel();
		
		private JPanel buttonsPanel;
		
		public CellBorderTagPanel(CellViewModel model) {
			super(model);
			
			this.setLayout(new BorderLayout());
			
			buttonsPanel = makeButtonPanel();
			this.add(buttonsPanel, BorderLayout.NORTH);
			setButtonsEnabled(false);
			
			dualPanel = new BorderTagDualChartPanel();
			dualPanel.addBorderTagEventListener(this);
			
			this.add(dualPanel, BorderLayout.CENTER);

			this.setBorder(null);

			
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
					
					dualPanel.setCharts(chart, chart);
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

					profileOptions.setEnabled(true);	
					
					dualPanel.setCharts(chart, rangeChart);
				}

			} catch(Exception e){
				error("Error updating cell panel", e);
				JFreeChart chart = MorphologyChartFactory.getInstance().makeEmptyChart();
				dualPanel.setCharts(chart, chart);
				profileOptions.setEnabled(false);
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
		
		private void setBorderTagAction(BorderTagObject tag){

			if(tag!=null){
				
					int newTagIndex = dualPanel.getActiveIndex(); //activeProfileIndex;

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

		@Override
		public void borderTagEventReceived(BorderTagEvent event) {
			if(event.getSource() instanceof JMenuItem){
				BorderTagObject tag = event.getTag();
				setBorderTagAction(tag);
			}
			
		}
}
