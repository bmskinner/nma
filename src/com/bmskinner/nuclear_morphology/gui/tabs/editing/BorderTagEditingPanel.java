///*******************************************************************************
// * Copyright (C) 2018 Ben Skinner
// * 
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// * 
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// * 
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// ******************************************************************************/
//package com.bmskinner.nuclear_morphology.gui.tabs.editing;
//
//import java.awt.BorderLayout;
//import java.awt.Component;
//import java.awt.FlowLayout;
//import java.awt.GridBagConstraints;
//import java.awt.GridBagLayout;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import java.util.logging.Logger;
//
//import javax.swing.JButton;
//import javax.swing.JLabel;
//import javax.swing.JMenuItem;
//import javax.swing.JPanel;
//
//import org.eclipse.jdt.annotation.NonNull;
//import org.jfree.chart.JFreeChart;
//
//import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
//import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
//import com.bmskinner.nuclear_morphology.core.GlobalOptions;
//import com.bmskinner.nuclear_morphology.core.InputSupplier;
//import com.bmskinner.nuclear_morphology.gui.components.BorderTagEvent;
//import com.bmskinner.nuclear_morphology.gui.components.panels.BorderTagDualChartPanel;
//import com.bmskinner.nuclear_morphology.gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;
//import com.bmskinner.nuclear_morphology.gui.events.BorderTagEventListener;
//import com.bmskinner.nuclear_morphology.gui.events.DatasetEvent;
//import com.bmskinner.nuclear_morphology.visualisation.charts.AbstractChartFactory;
//import com.bmskinner.nuclear_morphology.visualisation.charts.ProfileChartFactory;
//import com.bmskinner.nuclear_morphology.visualisation.options.ChartOptions;
//import com.bmskinner.nuclear_morphology.visualisation.options.ChartOptionsBuilder;
//
//@SuppressWarnings("serial")
//public class BorderTagEditingPanel extends AbstractEditingPanel implements ActionListener, BorderTagEventListener {
//
//	private static final Logger LOGGER = Logger.getLogger(BorderTagEditingPanel.class.getName());
//
//	private static final String PANEL_TITLE_LBL = "Border tags";
//
//	private JPanel buttonsPanel;
//
//	private JButton ruleSetButton;
//
//	private static final String STR_SHOW_RULESETS = "Rulesets";
//
//	private transient BorderTagDualChartPanel dualPanel;
//
//	public BorderTagEditingPanel() {
//
//		super(PANEL_TITLE_LBL);
//		this.setLayout(new BorderLayout());
//
//		buttonsPanel = makeButtonPanel();
//		this.add(buttonsPanel, BorderLayout.NORTH);
//		setButtonsEnabled(false);
//
//		dualPanel = new BorderTagDualChartPanel();
//		dualPanel.addBorderTagEventListener(this);
//
//		JPanel chartPanel = new JPanel();
//		chartPanel.setLayout(new GridBagLayout());
//
//		GridBagConstraints c = new GridBagConstraints();
//		c.anchor = GridBagConstraints.EAST;
//		c.gridx = 0;
//		c.gridy = 0;
//		c.gridwidth = 1;
//		c.gridheight = 1;
//		c.fill = GridBagConstraints.BOTH; // reset to default
//		c.weightx = 1.0;
//		c.weighty = 0.7;
//
//		chartPanel.add(dualPanel.getMainPanel(), c);
//		c.weighty = 0.3;
//		c.gridx = 0;
//		c.gridy = 1;
//		chartPanel.add(dualPanel.getRangePanel(), c);
//
//		this.add(chartPanel, BorderLayout.CENTER);
//
//	}
//
//	public void setButtonsEnabled(boolean b) {
//		ruleSetButton.setEnabled(b);
//	}
//
//	private JPanel makeButtonPanel() {
//
//		JPanel panel = new JPanel(new FlowLayout()) {
//			@Override
//			public void setEnabled(boolean b) {
//				super.setEnabled(b);
//				for (Component c : this.getComponents()) {
//					c.setEnabled(b);
//				}
//			}
//		};
//
//		JLabel text = new JLabel("Click a point to set as a border tag");
//		panel.add(text);
//
//		ruleSetButton = new JButton(STR_SHOW_RULESETS);
//		ruleSetButton.addActionListener(this);
////        panel.add(ruleSetButton); //TODO: enable once rulesets are working again
//
//		return panel;
//	}
//
//	@Override
//	protected synchronized void updateSingle() {
//
//		setButtonsEnabled(true);
//
//		boolean normaliseProfile = false; // cannot be normalised because we
//											// must get absolute indexes
//
//		ChartOptions options = new ChartOptionsBuilder().setDatasets(getDatasets()).setNormalised(normaliseProfile)
//				.setAlignment(ProfileAlignment.LEFT).setTag(Landmark.REFERENCE_POINT).setShowMarkers(true)
//				.setProfileType(ProfileType.ANGLE).setShowProfiles(false).setShowIQR(false).setShowPoints(true)
//				.setSwatch(GlobalOptions.getInstance().getSwatch()).setShowAnnotations(false).setShowXAxis(false)
//				.setShowYAxis(false).setTarget(dualPanel.getMainPanel()).build();
//
//		// We can't set the chart using the normal method because
//		// we need both charts to be created and in scope
////		JFreeChart chart = getChart(options);
//
//		/*
//		 * Create the chart for the range panel
//		 */
//
//		ChartOptions rangeOptions = new ChartOptionsBuilder().setDatasets(getDatasets()).setNormalised(normaliseProfile)
//				.setAlignment(ProfileAlignment.LEFT).setTag(Landmark.REFERENCE_POINT).setShowMarkers(true)
//				.setProfileType(ProfileType.ANGLE).setShowProfiles(false).setShowIQR(false)
//				.setSwatch(GlobalOptions.getInstance().getSwatch()).setShowPoints(false).setShowAnnotations(false)
//				.setShowXAxis(false).setShowYAxis(false).setTarget(dualPanel.getRangePanel()).build();
//
////		JFreeChart rangeChart = getChart(rangeOptions);
//
////		dualPanel.setCharts(chart, rangeChart);
//		dualPanel.createBorderTagPopup(activeDataset());
//
//	}
//
//	@Override
//	protected synchronized void updateMultiple() {
//		JFreeChart mainChart = AbstractChartFactory.createMultipleDatasetEmptyChart();
//		JFreeChart rangeChart = AbstractChartFactory.createMultipleDatasetEmptyChart();
//		dualPanel.setCharts(mainChart, rangeChart);
//		setButtonsEnabled(false);
//	}
//
//	@Override
//	protected synchronized void updateNull() {
//		setButtonsEnabled(false);
//		dualPanel.setCharts(AbstractChartFactory.createEmptyChart(), AbstractChartFactory.createEmptyChart());
//	}
//
//	@Override
//	public void setLoading() {
//		super.setLoading();
//		dualPanel.setCharts(AbstractChartFactory.createLoadingChart(), AbstractChartFactory.createLoadingChart());
//	}
//
//	@Override
//	protected JFreeChart createPanelChartType(ChartOptions options) {
//		return new ProfileChartFactory(options).createProfileChart();
//	}
//
//	@Override
//	public void actionPerformed(ActionEvent e) {
//
//		if (e.getSource() == ruleSetButton) {
//			RulesetDialog d = new RulesetDialog(activeDataset());
//			d.addDatasetEventListener(this);
//			d.setVisible(true);
//		}
//
//	}
//
//	@Override
//	public void eventReceived(DatasetEvent event) {
//		super.eventReceived(event);
//
//		if (event.getSource() instanceof RulesetDialog) {
//			LOGGER.fine("Heard dataset event");
//			this.getDatasetEventHandler().fireDatasetEvent(event.method(), event.getDatasets());
//		}
//	}
//
//	@Override
//	public void borderTagEventReceived(BorderTagEvent event) {
//		if (event.getSource() instanceof JMenuItem) {
//			setBorderTagAction(event.getTag(), event.getIndex());
//		}
//
//	}
//
//}
