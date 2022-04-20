package com.bmskinner.nuclear_morphology.gui.tabs.signals;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;

public class SignalWarpingMainPanel extends DetailPanel {

	private static final Logger LOGGER = Logger.getLogger(SignalWarpingMainPanel.class.getName());

	private static final @NonNull String PANEL_TITLE_LBL = "Warping";

	/** Launch signal warping */
	private JButton runWarpBtn;

	public SignalWarpingMainPanel() {
		super(PANEL_TITLE_LBL);
		setLayout(new BorderLayout());
		SignalWarpingChartPanel cp = new SignalWarpingChartPanel();
		SignalWarpingTablePanel tp = new SignalWarpingTablePanel();

		add(createHeader(), BorderLayout.NORTH);
		add(tp, BorderLayout.WEST);
		add(cp, BorderLayout.CENTER);

	}

	private JPanel createHeader() {
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout());

		runWarpBtn = new JButton(Labels.Signals.WARP_BTN_LBL);
		runWarpBtn.setToolTipText(Labels.Signals.WARP_BTN_TOOLTIP);
		runWarpBtn.addActionListener(e -> {
			LOGGER.fine("Running warping!");
		});
		runWarpBtn.setEnabled(false);
		panel.add(runWarpBtn);
		return panel;
	}
}
