package com.bmskinner.nma.gui.tabs.analysis_info;

import java.awt.BorderLayout;

import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import com.bmskinner.nma.gui.tabs.DetailPanel;

/**
 * Top level tab panel for analysis settings and parameters
 * 
 * @author Ben Skinner
 * @since 2.2.0
 *
 */
@SuppressWarnings("serial")
public class AnalysisInfoDetailPanel extends DetailPanel {

	private JTabbedPane tabPanel;

	private static final String PANEL_TITLE_LBL = "Analysis info";
	private static final String PANEL_DESC_LBL = "View analysis parameters";

	public AnalysisInfoDetailPanel() {
		super(PANEL_TITLE_LBL, PANEL_DESC_LBL);
		this.setLayout(new BorderLayout());
		tabPanel = new JTabbedPane(SwingConstants.TOP);

		DetailPanel paramsPanel = new AnalysisParametersDetailPanel();
		addPanel(tabPanel, paramsPanel);

		DetailPanel rulesetsPanel = new AnalysisRulesetsDetailPanel();
		addPanel(tabPanel, rulesetsPanel);

		this.add(tabPanel, BorderLayout.CENTER);
	}

}