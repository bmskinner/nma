package gui.tabs.profiles;

import gui.GlobalOptions;
import gui.components.panels.ProfileCollectionTypeSettingsPanel;
import gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;
import gui.tabs.DetailPanel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jfree.chart.JFreeChart;

import charting.charts.MorphologyChartFactory;
import charting.charts.panels.ExportableChartPanel;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
import components.generic.ProfileType;
import components.generic.Tag;

@SuppressWarnings("serial")
public class ModalityDisplayPanel extends DetailPanel implements ActionListener, ListSelectionListener {
		
		private JPanel mainPanel = new JPanel(new BorderLayout());
		private JList<Double> pointList;
		private ExportableChartPanel chartPanel;
		private ExportableChartPanel modalityProfileChartPanel; // hold a chart showing p-values across the profile
		private ProfileCollectionTypeSettingsPanel profileCollectionTypeSettingsPanel = new ProfileCollectionTypeSettingsPanel();
		
		public ModalityDisplayPanel(){
			super();
			 createUI();
			
		}
		
		private void createUI(){
			this.setLayout(new BorderLayout());
			
			/*
			 * Make the settings panel
			 */
			JPanel headerPanel = createHeader();
			this.add(headerPanel, BorderLayout.NORTH);
			
			/*
			 * Make the main chart panels
			 */
			chartPanel                = new ExportableChartPanel( MorphologyChartFactory.createEmptyChart());
			modalityProfileChartPanel = new ExportableChartPanel( MorphologyChartFactory.createEmptyChart());
			
			mainPanel.add(chartPanel, BorderLayout.WEST);
			mainPanel.add(modalityProfileChartPanel, BorderLayout.CENTER);

			/*
			 * Make the list of positions along the profile
			 */
			JScrollPane listPanel = createListPanel();
			this.add(listPanel, BorderLayout.WEST);
			
			this.add(mainPanel, BorderLayout.CENTER);
			
			
		}
		
		private JPanel createHeader(){
			JPanel panel = new JPanel();
			profileCollectionTypeSettingsPanel.addActionListener(this);
			profileCollectionTypeSettingsPanel.setEnabled(false);
			panel.add(profileCollectionTypeSettingsPanel);
			return panel;
		}
		
		private JScrollPane createListPanel(){
//			DecimalFormat df = new DecimalFormat("#0.00");
			pointList = new JList<Double>();
			
			pointList.setModel(createEmptyListModel());
			JScrollPane listPanel = new JScrollPane(pointList);

			pointList.addListSelectionListener(this);
			pointList.setEnabled(false);
			
			pointList.setCellRenderer(new ModalityListCellRenderer());
			
			return listPanel;
		}
		
		
		public ListModel<Double> createEmptyListModel(){
			DefaultListModel<Double> model = new DefaultListModel<Double>();
			for(Double d=0.0; d<=100; d+=0.5){
				model.addElement(d);
			}
			return model;
		}
		
		
		public void setEnabled(boolean b){
			super.setEnabled(b);
			profileCollectionTypeSettingsPanel.setEnabled(b);
			pointList.setEnabled(b);
		}

		
		@Override
		protected void updateSingle() {
			this.setEnabled(true);
			
			ProfileType type = profileCollectionTypeSettingsPanel.getSelected();
			
			DefaultListModel<Double> model = new DefaultListModel<Double>();
			List<Double> xvalues = activeDataset().getCollection()
					.getProfileCollection(type)
					.getAggregate()
					.getXKeyset();

			for(Double d: xvalues){
				model.addElement(d);
			}
			pointList.setModel(model);
			pointList.setCellRenderer(new ModalityListCellRenderer());
			pointList.setSelectedIndex(0);
			
			updateModalityProfileChart();
//			updatePositionChart(0);
		}
		
		@Override
		protected void updateMultiple() {
			this.setEnabled(true);

			pointList.setModel(createEmptyListModel());
			pointList.setCellRenderer(new ModalityListCellRenderer());
			pointList.setSelectedIndex(0);

			updateModalityProfileChart();
//			updatePositionChart(0);
		}
		
		@Override
		protected void updateNull() {
			this.setEnabled(false);
			
			updateModalityProfileChart();
			updatePositionChart(0);

		}
		
		@Override
		protected JFreeChart createPanelChartType(ChartOptions options) throws Exception {
			if(options.isNormalised()){
				return new MorphologyChartFactory(options).createModalityProfileChart();
			} else {
				return new MorphologyChartFactory(options).createModalityPositionChart();
			}
		}

		
		public void updateModalityProfileChart() {
			
			ProfileType type = profileCollectionTypeSettingsPanel.getSelected();
			
			ChartOptions options = new ChartOptionsBuilder()
				.setDatasets(getDatasets())
				.setNormalised(true) // here, the boolean is used to indicate the main chart
				.setAlignment(ProfileAlignment.LEFT)
				.setTag(Tag.REFERENCE_POINT)
				.setShowMarkers(false)
				.setProfileType(type)
				.setSwatch(GlobalOptions.getInstance().getSwatch())
				.setTarget(modalityProfileChartPanel)
				.build();
						
			
			setChart(options);
			
		}
		
		public void updatePositionChart(double xvalue) {
			
			ProfileType type = profileCollectionTypeSettingsPanel.getSelected();
			
			ChartOptions options = new ChartOptionsBuilder()
				.setDatasets(getDatasets())
				.setNormalised(false) // here, the boolean is used to indicate the position chart
				.setAlignment(ProfileAlignment.RIGHT)
				.setTag(Tag.REFERENCE_POINT)
				.setShowMarkers(false)
				.setProfileType(type)
				.setModalityPosition(xvalue)
				.setSwatch(GlobalOptions.getInstance().getSwatch())
				.setTarget(chartPanel)
				.build();
			
			
			setChart(options);

		}
		

		@Override
		public void actionPerformed(ActionEvent arg0) {

			updatePointSelection();
			updateModalityProfileChart();
			
		}
		
		private void updatePointSelection() {
			
			if(pointList.getSelectedValue()!=null){

				double xvalue = pointList.getSelectedValue();

				int lastRow    = pointList.getModel().getSize()-1;

				if(xvalue==100 || pointList.getSelectedIndex()==lastRow){
					xvalue=0; // wrap arrays
				}

				finest("Selecting profile position "+xvalue);
				updatePositionChart(xvalue);
			}
		}
		

		public void valueChanged(ListSelectionEvent e) {

			updatePointSelection();

		}
		
		private class ModalityListCellRenderer extends DefaultListCellRenderer {

			DecimalFormat df = new DecimalFormat("#0.00");
			
			  public Component getListCellRendererComponent(JList list, Object value, int index,
			      boolean isSelected, boolean cellHasFocus) {
				  
			    super.getListCellRendererComponent(list, value, index,
			        isSelected, cellHasFocus);
			    
			    this.setText(df.format(value));
			    return this;
			  }
			}
		
	}
