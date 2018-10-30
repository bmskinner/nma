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
package com.bmskinner.nuclear_morphology.components.options;

import java.io.File;

import org.eclipse.jdt.annotation.NonNull;

/**
 * A hash based replacement for the nuclear signal detection options
 * 
 * @author ben
 * @since 1.13.4
 *
 */
public class DefaultNuclearSignalHashOptions extends AbstractHashDetectionOptions
        implements INuclearSignalOptions {

    private static final long serialVersionUID = 1L;

    @Deprecated
    private SignalDetectionMode mode;

    public DefaultNuclearSignalHashOptions(@NonNull File folder) {
        super(folder);
        setDouble(MAX_FRACTION, DEFAULT_MAX_SIGNAL_FRACTION);
        setString(DETECTION_MODE_KEY, DEFAULT_METHOD.name());
        setMinSize(DEFAULT_MIN_SIGNAL_SIZE);
        setMaxSize(DEFAULT_MAX_SIGNAL_SIZE);
        setMinCirc(INuclearSignalOptions.DEFAULT_MIN_CIRC);
        setMaxCirc(INuclearSignalOptions.DEFAULT_MAX_CIRC);
        setChannel(DEFAULT_CHANNEL);
        setThreshold(DEFAULT_SIGNAL_THRESHOLD);
        setScale(DEFAULT_SCALE);
        setBoolean(IDetectionOptions.IS_RGB, DEFAULT_IS_RGB);
        setBoolean(IDetectionOptions.IS_NORMALISE_CONTRAST, DEFAULT_IS_NORMALISE);

    }

    /**
     * Construct from an existing nuclear options
     * 
     * @param template
     */
    public DefaultNuclearSignalHashOptions(@NonNull INuclearSignalOptions template) {
        super(template);
    }

    @Override
	public DefaultNuclearSignalHashOptions setSize(double min, double max) {
        super.setSize(min, max);
        return this;
    }

    @Override
	public DefaultNuclearSignalHashOptions setCircularity(double min, double max) {
        super.setCircularity(min, max);
        return this;
    }

    @Override
    public double getMaxFraction() {
        return getDouble(MAX_FRACTION);
    }

    @Override
    public SignalDetectionMode getDetectionMode() {
        return stringMap.containsKey(DETECTION_MODE_KEY) ? SignalDetectionMode.valueOf(getString(DETECTION_MODE_KEY)) : mode;
    }

    @Override
    public void setMaxFraction(double maxFraction) {
        setDouble(MAX_FRACTION, maxFraction);

    }

    @Override
    public void setDetectionMode(SignalDetectionMode detectionMode) {
        setString(DETECTION_MODE_KEY, detectionMode.name());
    }
    
	@Override
	public void setShellOptions(@NonNull IShellOptions o) {
		subMap.put(IDetectionSubOptions.SHELL_OPTIONS, o);
	}

	@Override
	public boolean hasShellOptions() {
		return subMap.containsKey(IDetectionSubOptions.SHELL_OPTIONS);
	}

	@Override
	public IShellOptions getShellOptions() {
		return (IShellOptions) subMap.get(IDetectionSubOptions.SHELL_OPTIONS);
	}

    @Override
    public IDetectionOptions duplicate() {
        return new DefaultNuclearSignalHashOptions(this);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();

        long temp = Double.doubleToLongBits(this.getMaxFraction());
        result = prime * result + (int) (temp ^ (temp >>> 32));
        if(mode!=null)
        	result = prime * result + mode.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object o) {

        if (!super.equals(o))
            return false;
        if (!(o instanceof INuclearSignalOptions))
            return false;
        INuclearSignalOptions other = (INuclearSignalOptions) o;
        if (Double.doubleToLongBits(getMaxFraction()) != Double.doubleToLongBits(other.getMaxFraction()))
            return false;
        if (!getDetectionMode().equals(other.getDetectionMode()))
            return false;
        return true;

    }
}
