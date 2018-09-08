/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/

package com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings;

import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;

/**
 * The top level settings class for detection options
 * 
 * @author ben
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public abstract class DetectionSettingsPanel extends SettingsPanel {

    protected IDetectionOptions options;

    public DetectionSettingsPanel(IDetectionOptions op) {

        options = op;
    }

    /**
     * Set the options values and update the spinners to match
     * 
     * @param options
     */
    public void set(IDetectionOptions options) {
    	this.options.set(options);
    	update();
    }
}
