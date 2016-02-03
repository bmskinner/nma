package gui.tabs.profiles;

import gui.components.ExportableChartPanel;
import gui.components.panels.BorderTagOptionsPanel;
import gui.components.panels.ProfileCollectionTypeSettingsPanel;
import gui.components.panels.ProfileMarkersOptionsPanel;
import gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;
import gui.tabs.DetailPanel;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.ui.TextAnchor;

import stats.DipTester;
import utility.Constants;
import charting.charts.MorphologyChartFactory;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
import components.CellCollection;
import components.generic.BooleanProfile;
import components.generic.BorderTag;
import components.generic.Profile;
import components.generic.ProfileType;

@SuppressWarnings("serial")
public class VariabilityDisplayPanel extends DetailPanel implements ActionListener, ChangeListener {
	
	private JPanel buttonPanel = new JPanel(new FlowLayout());
	protected ExportableChartPanel chartPanel;
	private JSpinner pvalueSpinner;

	private BorderTagOptionsPanel borderTagOptionsPanel = new BorderTagOptionsPanel();
	private ProfileCollectionTypeSettingsPanel profileCollectionTypeSettingsPanel = new ProfileCollectionTypeSettingsPanel();
	private ProfileMarkersOptionsPanel profileMarkersOptionsPanel = new ProfileMarkersOptionsPanel();

	public VariabilityDisplayPanel(Logger logger){
		super(logger);
		this.setLayout(new BorderLayout());

		JFreeChart variablityChart = ChartFactory.createXYLineChart(null,
				"Position", "IQR", null);
		XYPlot variabilityPlot = variablityChart.getXYPlot();
		variabilityPlot.setBackgroundPaint(Color.WHITE);
		variabilityPlot.getDomainAxis().setRange(0,100);
		chartPanel = new ExportableChartPanel(variablityChart);
		chartPanel.setMinimumDrawWidth( 0 );
		chartPanel.setMinimumDrawHeight( 0 );
		this.add(chartPanel, BorderLayout.CENTER);

		buttonPanel.add(borderTagOptionsPanel);
		borderTagOptionsPanel.addActionListener(this);
		borderTagOptionsPanel.setEnabled(false);


		pvalueSpinner = new JSpinner(new SpinnerNumberModel(Constants.FIVE_PERCENT_SIGNIFICANCE_LEVEL,	0d, 1d, 0.001d));
		pvalueSpinner.setEnabled(false);
		pvalueSpinner.addChangeListener(this);
		JComponent field = ((JSpinner.DefaultEditor) pvalueSpinner.getEditor());
		Dimension prefSize = field.getPreferredSize();
		prefSize = new Dimension(50, prefSize.height);
		field.setPreferredSize(prefSize);

		// add extra fields to the header panel
		buttonPanel.add(new JLabel("Dip test p-value:"));
		buttonPanel.add(pvalueSpinner);


		profileCollectionTypeSettingsPanel.addActionListener(this);
		profileCollectionTypeSettingsPanel.setEnabled(false);
		buttonPanel.add(profileCollectionTypeSettingsPanel);

		buttonPanel.revalidate();

		this.add(buttonPanel, BorderLayout.NORTH);
	}

	public void setEnabled(boolean b){
		borderTagOptionsPanel.setEnabled(b);
		profileCollectionTypeSettingsPanel.setEnabled(b);
		profileMarkersOptionsPanel.setEnabled(b);
		pvalueSpinner.setEnabled(b);
	}

//	public void update(List<AnalysisDataset> list){
//
//		
//	}

	/**
	 * Update the profile panel with data from the given datasets
	 * @param list the datasets
	 * @param normalised flag for raw or normalised lengths
	 * @param rightAlign flag for left or right alignment (no effect if normalised is true)
	 */	
	private void updateProfiles(ChartOptions options){

		try {
			if(options.isSingleDataset()){
				JFreeChart chart = MorphologyChartFactory.makeVariabilityChart(options);


				if(options.isShowMarkers()){ // add the bimodal regions
					CellCollection collection = options.firstDataset().getCollection();

					// dip test the profiles

					double significance = (Double) pvalueSpinner.getValue();
					BooleanProfile modes  = DipTester.testCollectionIsUniModal(collection, options.getTag(), significance, options.getType());


					// add any regions with bimodal distribution to the chart
					XYPlot plot = chart.getXYPlot();

					Profile xPositions = modes.getPositions(100);

					for(int i=0; i<modes.size(); i++){
						double x = xPositions.get(i);
						if(modes.get(i)==true){
							ValueMarker marker = new ValueMarker(x, Color.black, new BasicStroke(2f));
							plot.addDomainMarker(marker);
						}
					}

					double ymax = DatasetUtilities.findMaximumRangeValue(plot.getDataset()).doubleValue();
					DecimalFormat df = new DecimalFormat("#0.000"); 
					XYTextAnnotation annotation = new XYTextAnnotation("Markers for non-unimodal positions (p<"+df.format(significance)+")",1, ymax);
					annotation.setTextAnchor(TextAnchor.TOP_LEFT);
					plot.addAnnotation(annotation);
				}

				chartPanel.setChart(chart);
			} else { // multiple nuclei
				JFreeChart chart = MorphologyChartFactory.makeVariabilityChart(options);
				chartPanel.setChart(chart);
			}
		} catch (Exception e) {
			programLogger.log(Level.SEVERE, "Error in plotting variability chart", e);
		}	
	}


	@Override
	public void actionPerformed(ActionEvent e) {

		update(getDatasets());

	}

	@Override
	public void stateChanged(ChangeEvent arg0) {
		if(arg0.getSource()==pvalueSpinner){
			JSpinner j = (JSpinner) arg0.getSource();
			try {
				j.commitEdit();
			} catch (ParseException e) {
				programLogger.log(Level.SEVERE, "Error setting p-value spinner", e);
			}
		}
		update(getDatasets());

	}

	@Override
	protected void updateSingle() throws Exception {

			this.setEnabled(true);

			BorderTag tag = borderTagOptionsPanel.getSelected();
			boolean showMarkers = profileMarkersOptionsPanel.showMarkers();
			ProfileType type = profileCollectionTypeSettingsPanel.getSelected();

			ChartOptions options =  new ChartOptionsBuilder()
			.setDatasets(getDatasets())
			.setLogger(programLogger)
			.setNormalised(true)
			.setAlignment(ProfileAlignment.LEFT)
			.setTag(tag)
			.setShowMarkers(showMarkers)
			.setProfileType(type)
			.build();

			updateProfiles(options);
		
	}

	@Override
	protected void updateMultiple() throws Exception {
		updateSingle();
		// Don't allow marker selection for multiple datasets
		profileMarkersOptionsPanel.setEnabled(false);
		pvalueSpinner.setEnabled(false);
	}

	@Override
	protected void updateNull() throws Exception {
		updateSingle();
		this.setEnabled(false);
	}
}
