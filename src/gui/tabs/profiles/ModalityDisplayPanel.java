package gui.tabs.profiles;

import gui.GlobalOptions;
import gui.components.panels.ProfileCollectionTypeSettingsPanel;
import gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;
import gui.tabs.DetailPanel;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.List;
import java.util.logging.Level;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;

import charting.charts.ExportableChartPanel;
import charting.charts.MorphologyChartFactory;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
import charting.options.TableOptions;
import components.generic.BorderTag;
import components.generic.BorderTagObject;
import components.generic.ProfileType;

@SuppressWarnings("serial")
public class ModalityDisplayPanel extends DetailPanel implements ActionListener {
		
		private JPanel mainPanel = new JPanel(new BorderLayout());
		private JList<String> pointList;
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
			chartPanel                = createPositionChartPanel();
			modalityProfileChartPanel = createModalityProfileChartPanel();
			
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
			DecimalFormat df = new DecimalFormat("#0.00");
			pointList = new JList<String>();
			DefaultListModel<String> model = new DefaultListModel<String>();
			for(Double d=0.0; d<=100; d+=0.5){
				model.addElement(df.format(d));
			}
			pointList.setModel(model);
			JScrollPane listPanel = new JScrollPane(pointList);

			pointList.addListSelectionListener(new ModalitySelectionListener());
			pointList.setEnabled(false);
			
			return listPanel;
		}
		
		
		
		
		public void setEnabled(boolean b){
			profileCollectionTypeSettingsPanel.setEnabled(b);
			pointList.setEnabled(b);
		}
		
		private ExportableChartPanel createPositionChartPanel(){
			
			JFreeChart chart = createPositionChart();
			ExportableChartPanel chartPanel = new ExportableChartPanel(chart);
			chartPanel.setMinimumDrawWidth( 0 );
			chartPanel.setMinimumDrawHeight( 0 );
			return chartPanel;
		}
		
		private JFreeChart createPositionChart(){
			JFreeChart chart = ChartFactory.createXYLineChart(null,
					"Probability", "Angle", null);
			XYPlot plot = chart.getXYPlot();
			plot.setBackgroundPaint(Color.WHITE);
			plot.getDomainAxis().setRange(0,360);
			plot.addDomainMarker(new ValueMarker(180, Color.BLACK, new BasicStroke(2f)));
			return chart;
		}
		
		private ExportableChartPanel createModalityProfileChartPanel(){
			
			JFreeChart chart = createModalityProfileChart();
			ExportableChartPanel chartPanel = new ExportableChartPanel(chart);
			chartPanel.setMinimumDrawWidth( 0 );
			chartPanel.setMinimumDrawHeight( 0 );
			return chartPanel;
		}
		
		private JFreeChart createModalityProfileChart(){
			JFreeChart chart = ChartFactory.createXYLineChart(null,
					"Position", "Probability", null);
			XYPlot plot = chart.getXYPlot();
			plot.setBackgroundPaint(Color.WHITE);
			plot.getDomainAxis().setRange(0,100);
			plot.getRangeAxis().setRange(0,1);
			return chart;
		}
		
		@Override
		protected void updateSingle() {
			updateMultiple();
		}
		
		@Override
		protected void updateMultiple() {
			this.setEnabled(true);

			ProfileType type = profileCollectionTypeSettingsPanel.getSelected();

			DecimalFormat df = new DecimalFormat("#0.00");
			DefaultListModel<String> model = new DefaultListModel<String>();
			if(isSingleDataset()){ // use the actual x-positions
				List<Double> xvalues = activeDataset().getCollection()
						.getProfileCollection(type)
						.getAggregate()
						.getXKeyset();

				for(Double d: xvalues){
					model.addElement(df.format(d));
				}


			} else {
				// use a standard 0.5 spacing
				for(Double d=0.0; d<=100; d+=0.5){
					model.addElement(df.format(d));
				}

			}
			pointList.setModel(model);

			String xString = pointList.getModel().getElementAt(0);
			double xvalue = Double.valueOf(xString);

			updatePositionChart(xvalue);	
			updateModalityProfileChart();
			
		}
		
		@Override
		protected void updateNull() {
			this.setEnabled(false);
			modalityProfileChartPanel.setChart(createModalityProfileChart());
			chartPanel.setChart(createPositionChart());
		}
		
		@Override
		protected JFreeChart createPanelChartType(ChartOptions options) throws Exception {
			if(options.isNormalised()){
			return MorphologyChartFactory.createModalityProfileChart(options);
			} else {
				return MorphologyChartFactory.createModalityPositionChart(options);
			}
		}
		
		@Override
		protected TableModel createPanelTableType(TableOptions options) throws Exception{
			return null;
		}
		
		public void updateModalityProfileChart() {
			
			ProfileType type = profileCollectionTypeSettingsPanel.getSelected();
			
			ChartOptions options = new ChartOptionsBuilder()
				.setDatasets(getDatasets())
				.setNormalised(true)
				.setAlignment(ProfileAlignment.LEFT)
				.setTag(BorderTagObject.REFERENCE_POINT)
				.setShowMarkers(false)
				.setProfileType(type)
				.setSwatch(GlobalOptions.getInstance().getSwatch())
				.setTarget(modalityProfileChartPanel)
				.build();
						
			
			setChart(options);
			
//			JFreeChart chart;
//			try {
//				chart = getChart(options);
//			} catch (Exception e) {
//				chart = MorphologyChartFactory.makeBlankProbabililtyChart();
//			}
//			modalityProfileChartPanel.setChart(chart);
			
		}
		
		public void updatePositionChart(double xvalue) {
			
			ProfileType type = profileCollectionTypeSettingsPanel.getSelected();
			
			ChartOptions options = new ChartOptionsBuilder()
				.setDatasets(getDatasets())
				.setNormalised(false)
				.setAlignment(ProfileAlignment.RIGHT)
				.setTag(BorderTagObject.REFERENCE_POINT)
				.setShowMarkers(false)
				.setProfileType(type)
				.setModalityPosition(xvalue)
				.setSwatch(GlobalOptions.getInstance().getSwatch())
				.setTarget(chartPanel)
				.build();
			
			JFreeChart chart;
			try {
				chart = getChart(options);
			} catch (Exception e) {
				chart = MorphologyChartFactory.makeBlankProbabililtyChart();
			}
			chartPanel.setChart(chart);
		}
		

		@Override
		public void actionPerformed(ActionEvent arg0) {

			updatePointSelection();
			updateModalityProfileChart();
			
		}
		
		private void updatePointSelection() {
			int row        = pointList.getSelectedIndex();
			String xString = pointList.getModel().getElementAt(row);
			double xvalue  = Double.valueOf(xString);
			
			int lastRow    = pointList.getModel().getSize()-1;
			
			if(xvalue==100 || row==lastRow){
				xvalue=0; // wrap arrays
			}
			
			log(Level.FINEST, "Selecting profile position "+xvalue +" at index "+row);
			updatePositionChart(xvalue);
		}
		
		private class ModalitySelectionListener implements ListSelectionListener {
			public void valueChanged(ListSelectionEvent e) {
				
				updatePointSelection();
				
			}
		}
	}
