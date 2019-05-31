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
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * The default implementation of the nuclear signal options interface
 * 
 * @author ben
 * @deprecated since 1.13.4
 *
 */
@Deprecated
public class DefaultNuclearSignalOptions extends AbstractDetectionOptions implements INuclearSignalOptions {
	
	private static final Logger LOGGER = Logger.getLogger(Loggable.ROOT_LOGGER);

    private static final long serialVersionUID = 1L;

    private double              maxFraction;
    private SignalDetectionMode mode;

    public DefaultNuclearSignalOptions(File folder) {
        super(folder);
        maxFraction = DEFAULT_MAX_SIGNAL_FRACTION;
        mode = DEFAULT_METHOD;

        setMinSize(DEFAULT_MIN_SIGNAL_SIZE);
        setMaxSize(DEFAULT_MAX_SIGNAL_SIZE);
        setMinCirc(INuclearSignalOptions.DEFAULT_MIN_CIRC);
        setMaxCirc(INuclearSignalOptions.DEFAULT_MAX_CIRC);
        setChannel(DEFAULT_CHANNEL);
        setThreshold(DEFAULT_SIGNAL_THRESHOLD);
        setScale(DEFAULT_SCALE);

    }

    /**
     * Construct from an existing nuclear options
     * 
     * @param template
     */
    public DefaultNuclearSignalOptions(INuclearSignalOptions template) {
        super(template);
        this.maxFraction = template.getMaxFraction();
        this.mode = template.getDetectionMode();

    }

    public DefaultNuclearSignalOptions setSize(double min, double max) {
        super.setSize(min, max);
        return this;
    }

    public DefaultNuclearSignalOptions setCircularity(double min, double max) {
        super.setCircularity(min, max);
        return this;
    }

    @Override
    public double getMaxFraction() {
        return maxFraction;
    }

    @Override
    public SignalDetectionMode getDetectionMode() {
        return mode;
    }

    @Override
    public void setMaxFraction(double maxFraction) {
        this.maxFraction = maxFraction;

    }

    @Override
    public void setDetectionMode(SignalDetectionMode detectionMode) {
        mode = detectionMode;

    }

    @Override
    public IDetectionOptions duplicate() {
        return new DefaultNuclearSignalOptions(this);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();

        long temp = Double.doubleToLongBits(maxFraction);
        result = prime * result + (int) (temp ^ (temp >>> 32));
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

        if (Double.doubleToLongBits(maxFraction) != Double.doubleToLongBits(other.getMaxFraction()))
            return false;

        if (mode != other.getDetectionMode())
            return false;

        return true;

    }

//    @Override
//    public IMutableDetectionOptions unlock() {
//        return this;
//    }

    @Override
    public boolean isUseHoughTransform() {
        LOGGER.warning("Unimplemented method in " + this.getClass().getName());
        LOGGER.warning("Unimplemented method in " + this.getClass().getName());
        return false;
    }

//    @Override
//    public IDetectionOptions lock() {
//        LOGGER.warning("Unimplemented method in " + this.getClass().getName());
//        LOGGER.warning("Unimplemented method in " + this.getClass().getName());
//        return null;
//    }

    @Override
    public void setHoughOptions(IHoughDetectionOptions hough) {
        LOGGER.warning("Unimplemented method in " + this.getClass().getName());
        LOGGER.warning("Unimplemented method in " + this.getClass().getName());
    }

    @Override
    public IDetectionSubOptions getSubOptions(String s) {
        LOGGER.warning("Unimplemented method in " + this.getClass().getName());
        return null;
    }

    @Override
    public boolean isRGB() {
        LOGGER.warning("Unimplemented method in " + this.getClass().getName());
        return false;
    }

    @Override
    public void setSubOptions(String s, IDetectionSubOptions sub) {
        LOGGER.warning("Unimplemented method in " + this.getClass().getName());
    }

    @Override
    public void setRGB(boolean b) {
        LOGGER.warning("Unimplemented method in " + this.getClass().getName());
    }

    @Override
    public List<String> getKeys() {
        LOGGER.warning("Unimplemented method in " + this.getClass().getName());
        LOGGER.warning("Unimplemented method in " + this.getClass().getName());
        return null;
    }

    @Override
    public boolean hasSubOptions(String key) {
        LOGGER.warning("Unimplemented method in " + this.getClass().getName());
        LOGGER.warning("Unimplemented method in " + this.getClass().getName());
        return false;
    }

    @Override
    public double getDouble(String s) {
        LOGGER.warning("Unimplemented method in " + this.getClass().getName());
        LOGGER.warning("Unimplemented method in " + this.getClass().getName());
        return 0;
    }

    @Override
    public int getInt(String s) {
        LOGGER.warning("Unimplemented method in " + this.getClass().getName());
        return 0;
    }

    @Override
    public boolean getBoolean(String s) {
        LOGGER.warning("Unimplemented method in " + this.getClass().getName());
        return false;
    }

    @Override
    public void setDouble(String s, double d) {
        LOGGER.warning("Unimplemented method in " + this.getClass().getName());

    }

    @Override
    public void setInt(String s, int i) {
        LOGGER.warning("Unimplemented method in " + this.getClass().getName());
        LOGGER.warning("Unimplemented method in " + this.getClass().getName());
    }

    @Override
    public void setBoolean(String s, boolean b) {
        LOGGER.warning("Unimplemented method in " + this.getClass().getName());
        LOGGER.warning("Unimplemented method in " + this.getClass().getName());
    }

    @Override
    public float getFloat(String s) {
        LOGGER.warning("Unimplemented method in " + this.getClass().getName());
        LOGGER.warning("Unimplemented method in " + this.getClass().getName());
        return 0;
    }

    @Override
    public void setFloat(String s, float f) {
        LOGGER.warning("Unimplemented method in " + this.getClass().getName());
        LOGGER.warning("Unimplemented method in " + this.getClass().getName());

    }

	@Override
	public List<String> getBooleanKeys() {
		 LOGGER.warning("Unimplemented method in " + this.getClass().getName());
		return null;
	}

	@Override
	public List<String> getIntegerKeys() {
		 LOGGER.warning("Unimplemented method in " + this.getClass().getName());
		return null;
	}

	@Override
	public List<String> getDoubleKeys() {
		 LOGGER.warning("Unimplemented method in " + this.getClass().getName());
		return null;
	}

	@Override
	public List<String> getFloatKeys() {
		 LOGGER.warning("Unimplemented method in " + this.getClass().getName());
		return null;
	}

	@Override
	public Set<String> getSubOptionKeys() {
		 LOGGER.warning("Unimplemented method in " + this.getClass().getName());
		return null;
	}

	@Override
	public String getString(String s) {
		LOGGER.warning("Unimplemented method in " + this.getClass().getName());
		return null;
	}

	@Override
	public void setString(String k, String v) {
		LOGGER.warning("Unimplemented method in " + this.getClass().getName());
	}

	@Override
	public List<String> getStringKeys() {
		LOGGER.warning("Unimplemented method in " + this.getClass().getName());
		return null;
	}

	@Override
	public void setShellOptions(@NonNull IShellOptions o) {
		LOGGER.warning("Unimplemented method in " + this.getClass().getName());
		
	}

	@Override
	public boolean hasShellOptions() {
		LOGGER.warning("Unimplemented method in " + this.getClass().getName());
		return false;
	}

	@Override
	public IShellOptions getShellOptions() {
		LOGGER.warning("Unimplemented method in " + this.getClass().getName());
		return null;
	}

	@Override
	public void set(HashOptions o) {
		// TODO Auto-generated method stub
		
	}
    
}
