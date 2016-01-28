package gui.tabs.profiles;

import gui.components.panels.BorderTagOptionsPanel;
import gui.components.panels.ProfileAlignmentOptionsPanel;
import gui.components.panels.ProfileMarkersOptionsPanel;
import gui.tabs.DetailPanel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JPanel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import components.generic.ProfileType;

import charting.charts.MorphologyChartFactory;

@SuppressWarnings("serial")
public abstract class AbstractProfileDisplayPanel extends DetailPanel implements ActionListener {
		
		Dimension minimumChartSize = new Dimension(50, 100);
		Dimension preferredChartSize = new Dimension(400, 300);

		protected JPanel buttonPanel = new JPanel(new FlowLayout());
		protected ChartPanel chartPanel;
		
		protected BorderTagOptionsPanel borderTagOptionsPanel = new BorderTagOptionsPanel();
		protected ProfileAlignmentOptionsPanel profileAlignmentOptionsPanel = new ProfileAlignmentOptionsPanel();
		protected ProfileMarkersOptionsPanel profileMarkersOptionsPanel = new ProfileMarkersOptionsPanel();
		
		public AbstractProfileDisplayPanel(Logger logger){
			super(logger);
			this.setLayout(new BorderLayout());
			JFreeChart rawChart = MorphologyChartFactory.makeEmptyProfileChart(ProfileType.REGULAR);
			chartPanel = MorphologyChartFactory.makeProfileChartPanel(rawChart);
			
			chartPanel.setMinimumDrawWidth( 0 );
			chartPanel.setMinimumDrawHeight( 0 );
			this.setMinimumSize(minimumChartSize);
			this.setPreferredSize(preferredChartSize);
			this.add(chartPanel, BorderLayout.CENTER);
					
			// add the alignments panel to the tab
			
			buttonPanel.add(profileAlignmentOptionsPanel);
			profileAlignmentOptionsPanel.addActionListener(this);
			profileAlignmentOptionsPanel.setEnabled(false);
			
			buttonPanel.add(borderTagOptionsPanel);
			borderTagOptionsPanel.addActionListener(this);
			borderTagOptionsPanel.setEnabled(false);
			
			buttonPanel.add(profileMarkersOptionsPanel);
			profileMarkersOptionsPanel.addActionListener(this);
			profileMarkersOptionsPanel.setEnabled(false);
						
			this.add(buttonPanel, BorderLayout.NORTH);
		}
		
		public void setEnabled(boolean b){
			profileAlignmentOptionsPanel.setEnabled(b);
			borderTagOptionsPanel.setEnabled(b);
			profileMarkersOptionsPanel.setEnabled(b);
		}
		
		@Override
		protected void updateSingle() throws Exception {
			
			this.setEnabled(true);

		}
		
		@Override
		protected void updateMultiple() throws Exception {
			// Don't allow marker selection for multiple datasets
			this.setEnabled(true);
			profileMarkersOptionsPanel.setEnabled(false);
		}
		
		@Override
		protected void updateNull() throws Exception {
			this.setEnabled(false);		
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			
			update(getDatasets());
		}
	}


