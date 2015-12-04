package gui.components;

import gui.tabs.DetailPanel;
import stats.NucleusStatistic;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * This class is extended for making a panel with multiple stats histograms
 * arranged vertically
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public abstract class HistogramsTabPanel extends DetailPanel implements ActionListener {
	
	protected Map<String, SelectableChartPanel> chartPanels = new HashMap<String, SelectableChartPanel>();

	protected JPanel 		mainPanel; // hold the charts
	protected JPanel		headerPanel; // hold buttons
	protected ProbabilityDensityCheckboxPanel useDensityPanel = new ProbabilityDensityCheckboxPanel();
	protected MeasurementUnitSettingsPanel measurementUnitSettingsPanel = new MeasurementUnitSettingsPanel();

	protected JScrollPane scrollPane; // hold the main panel
	
	public HistogramsTabPanel(Logger programLogger){
		super(programLogger);

		this.setLayout(new BorderLayout());

		try {
			mainPanel = new JPanel();
			mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

			headerPanel = new JPanel(new FlowLayout());

			
			headerPanel.add(useDensityPanel);
			useDensityPanel.addActionListener(this);
			
			headerPanel.add(measurementUnitSettingsPanel);
			measurementUnitSettingsPanel.addActionListener(this);


			this.add(headerPanel, BorderLayout.NORTH);

			// add the scroll pane to the tab
			scrollPane  = new JScrollPane(mainPanel);
			this.add(scrollPane, BorderLayout.CENTER);
		} catch(Exception e){
			programLogger.log(Level.SEVERE, "Error creating panel", e);
		}

	}
	
	 @Override
     public void actionPerformed(ActionEvent e) {

         try {
        	 programLogger.log(Level.FINEST, "Updating abstract histogram tab panel");
             this.update(getDatasets());
         } catch (Exception e1) {
         	programLogger.log(Level.SEVERE, "Error updating histogram panel from action listener", e1);
         }
         
         
     }
	 	 
	 protected int getFilterDialogResult(double lower, double upper){
			DecimalFormat df = new DecimalFormat("#.##");
			Object[] options = { "Filter collection" , "Cancel", };
			int result = JOptionPane.showOptionDialog(null, "Filter between "+df.format(lower)+"-"+df.format(upper)+"?", "Confirm filter",

					JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,

					null, options, options[0]);
			return result;
		}
	 
	 
}
