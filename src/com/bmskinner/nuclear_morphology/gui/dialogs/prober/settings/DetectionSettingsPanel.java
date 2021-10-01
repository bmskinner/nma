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
package com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings;

import com.bmskinner.nuclear_morphology.components.options.HashOptions;

/**
 * The top level settings class for detection options
 * 
 * @author ben
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public abstract class DetectionSettingsPanel extends SettingsPanel {

    protected HashOptions options;

    public DetectionSettingsPanel(HashOptions op) {

        options = op;
    }

    /**
     * Set the options values and update the spinners to match
     * 
     * @param options
     */
    public void set(HashOptions options) {
    	this.options.set(options);
    	update();
    }
    
    /**
     * Update the given option and notify listeners that the options 
     * have changed
     * @param option
     * @param value
     */
    protected void updateOptions(String option, int value) {
    	options.setInt(option, value);
    	fireOptionsChangeEvent();
    }
    
    /**
     * Update the given option and notify listeners that the options 
     * have changed
     * @param option
     * @param value
     */
    protected void updateOptions(String option, double value) {
    	options.setDouble(option, value);
    	fireOptionsChangeEvent();
    }
    
    /**
     * Update the given option and notify listeners that the options 
     * have changed
     * @param option
     * @param value
     */
    protected void updateOptions(String option, boolean value) {
    	options.setBoolean(option, value);
    	fireOptionsChangeEvent();
    }
}
