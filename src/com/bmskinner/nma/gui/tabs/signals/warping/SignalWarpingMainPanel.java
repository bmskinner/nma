package com.bmskinner.nma.gui.tabs.signals.warping;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.gui.tabs.DetailPanel;

public class SignalWarpingMainPanel extends DetailPanel {

	private static final Logger LOGGER = Logger.getLogger(SignalWarpingMainPanel.class.getName());

	private static final @NonNull String PANEL_TITLE_LBL = "Warping";
	private static final String PANEL_DESC_LBL = "Locations of signals warped to the consensus shape";

	public SignalWarpingMainPanel() {
		super(PANEL_TITLE_LBL, PANEL_DESC_LBL);
		setLayout(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.BOTH;
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.gridheight = 1;
		constraints.gridwidth = 2;
		constraints.weightx = 1;
		constraints.weighty = 1;
		constraints.anchor = GridBagConstraints.CENTER;
		SignalWarpingTablePanel tp = new SignalWarpingTablePanel();
		add(tp, constraints);

		constraints.fill = GridBagConstraints.BOTH;
		constraints.gridx = 2;
		constraints.gridy = 0;
		constraints.gridheight = 1;
		constraints.gridwidth = 1;
		constraints.weightx = 0.5;
		constraints.weighty = 1;
		constraints.anchor = GridBagConstraints.CENTER;

		SignalWarpingChartPanel cp = new SignalWarpingChartPanel();

		tp.addWarpedSignalSelectionChangeListener(cp);

		add(cp, constraints);

	}
}
