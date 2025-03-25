/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nma.gui.dialogs.prober.settings;

import java.awt.BorderLayout;
import java.io.File;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.gui.dialogs.prober.OptionsChangeEvent;
import com.bmskinner.nma.logging.Loggable;

/**
 * The settings panel for detection nuclear signals. This is designed to be
 * included in an image prober, and will fire a prober reload event when
 * settings are changed.
 * 
 * @author Ben Skinner
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class SignalDetectionSettingsPanel extends SettingsPanel {

	private static final Logger LOGGER = Logger
			.getLogger(SignalDetectionSettingsPanel.class.getName());

	private HashOptions options;
	private IAnalysisOptions parent;
	private File folder;

	private static final String OBJECT_FINDING_LBL = "Object finding";
	private static final String SIZE_SETTINGS_LBL = "Filtering";
	private static final String THRESHOLD_LBL = "Thresholding";
	private static final String CHANNEL_LBL = "Image";
	private static final String COPY_LBL = "Copy";

	public SignalDetectionSettingsPanel(@NonNull File folder, @NonNull IAnalysisOptions parent,
			@NonNull HashOptions options) {

		try {
			this.folder = folder;
			this.parent = parent;
			this.options = options;

			this.add(createPanel(), BorderLayout.CENTER);
		} catch (Exception e) {
			LOGGER.log(Loggable.STACK, e.getMessage(), e);
		}
	}

	private JPanel createPanel() {

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		SettingsPanel copyPanel = new CopySignalDetectionSettingsFromOpenDatasetPanel(folder,
				parent,
				options);
		SettingsPanel sizePanel = new SignalSizeSettingsPanel(options);
		SettingsPanel threshPanel = new ThresholdSettingsPanel(options);
		SettingsPanel methodPanel = new SignalMethodSettingsPanel(options);
		SettingsPanel channelPanel = new ImageChannelSettingsPanel(options);

		copyPanel.setBorder(BorderFactory.createTitledBorder(COPY_LBL));
		methodPanel.setBorder(BorderFactory.createTitledBorder(OBJECT_FINDING_LBL));
		sizePanel.setBorder(BorderFactory.createTitledBorder(SIZE_SETTINGS_LBL));
		threshPanel.setBorder(BorderFactory.createTitledBorder(THRESHOLD_LBL));
		channelPanel.setBorder(BorderFactory.createTitledBorder(CHANNEL_LBL));

		this.addSubPanel(copyPanel);
		this.addSubPanel(methodPanel);
		this.addSubPanel(threshPanel);
		this.addSubPanel(sizePanel);
		this.addSubPanel(channelPanel);

		panel.add(copyPanel);
		panel.add(channelPanel);
		panel.add(threshPanel);
		panel.add(methodPanel);
		panel.add(sizePanel);

		return panel;
	}

	@Override
	public void optionsChangeEventReceived(OptionsChangeEvent e) {

		if (this.hasSubPanel((SettingsPanel) e.getSource())) {
			update();
			fireProberReloadEvent();
		}
	}
}
