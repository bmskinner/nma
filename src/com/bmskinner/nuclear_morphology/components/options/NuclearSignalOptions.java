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
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.CellularComponent;

/**
 * An early implementation of nuclear signal options. Use a hash version instead
 * 
 * @author bms41
 *
 */
@Deprecated
public class NuclearSignalOptions implements INuclearSignalOptions {
	
	private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private static final long serialVersionUID = 1L;

    public int    threshold;
    public double minCirc, maxCirc, minSize, maxFraction;
    public int    detectionMode;

    public NuclearSignalOptions() {
        this(DEFAULT_SIGNAL_THRESHOLD, INuclearSignalOptions.DEFAULT_MIN_CIRC, INuclearSignalOptions.DEFAULT_MAX_CIRC,
                DEFAULT_MIN_SIGNAL_SIZE, DEFAULT_MAX_SIGNAL_FRACTION, SignalDetectionMode.FORWARD);

    }

    public NuclearSignalOptions(int threshold, double minCirc, double maxCirc, double minSize, double maxFraction,
            SignalDetectionMode detectionMode) {
        this.threshold = threshold;
        this.minCirc = minCirc;
        this.maxCirc = maxCirc;
        this.minSize = minSize;
        this.maxFraction = maxFraction;

        switch (detectionMode) {
        case FORWARD:
            this.detectionMode = 0;
            break;
        case REVERSE:
            this.detectionMode = 1;
            break;
        case ADAPTIVE:
            this.detectionMode = 2;
            break;
        }
    }

    /**
     * Construct from a template object
     * 
     * @param template
     */
    protected NuclearSignalOptions(INuclearSignalOptions template) {
        threshold = template.getThreshold();

        minCirc = template.getMinCirc();
        maxCirc = template.getMaxCirc();
        minSize = template.getMinSize();
        maxFraction = template.getMaxFraction();

        SignalDetectionMode mode = template.getDetectionMode();
        switch (mode) {
        case FORWARD:
            this.detectionMode = 0;
            break;
        case REVERSE:
            this.detectionMode = 1;
            break;
        case ADAPTIVE:
            this.detectionMode = 2;
            break;
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.signals.INuclearSignalOptions#getThreshold()
     */
    @Override
    public int getThreshold() {
        return threshold;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.signals.INuclearSignalOptions#setThreshold(int)
     */
    @Override
    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.signals.INuclearSignalOptions#getMinCirc()
     */
    @Override
    public double getMinCirc() {
        return minCirc;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.signals.INuclearSignalOptions#setMinCirc(double)
     */
    @Override
    public void setMinCirc(double minCirc) {
        this.minCirc = minCirc;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.signals.INuclearSignalOptions#getMaxCirc()
     */
    @Override
    public double getMaxCirc() {
        return maxCirc;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.signals.INuclearSignalOptions#setMaxCirc(double)
     */
    @Override
    public void setMaxCirc(double maxCirc) {
        this.maxCirc = maxCirc;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.signals.INuclearSignalOptions#getMinSize()
     */
    @Override
    public double getMinSize() {
        return minSize;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.signals.INuclearSignalOptions#setMinSize(double)
     */
    @Override
    public void setMinSize(double minSize) {
        this.minSize = minSize;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.signals.INuclearSignalOptions#getMaxFraction()
     */
    @Override
    public double getMaxFraction() {
        return maxFraction;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.signals.INuclearSignalOptions#setMaxFraction(double)
     */
    @Override
    public void setMaxFraction(double maxFraction) {
        this.maxFraction = maxFraction;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.signals.INuclearSignalOptions#getDetectionMode()
     */
    @Override
    public SignalDetectionMode getDetectionMode() {

        switch (detectionMode) {
        case 0:
            return SignalDetectionMode.FORWARD;

        case 1:
            return SignalDetectionMode.REVERSE;

        case 2:
            return SignalDetectionMode.ADAPTIVE;

        default:
            return SignalDetectionMode.FORWARD;
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.signals.INuclearSignalOptions#setDetectionMode(int)
     */
    @Override
    public void setDetectionMode(SignalDetectionMode detectionMode) {
        switch (detectionMode) {
        case FORWARD:
            this.detectionMode = 0;
            break;
        case REVERSE:
            this.detectionMode = 1;
            break;
        case ADAPTIVE:
            this.detectionMode = 2;
            break;
        }
    }

    @Override
    public File getFolder() {
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
        return null;
    }

    @Override
    public double getMaxSize() {
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
        return 0;
    }

    @Override
    public double getScale() {
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
        return 0;
    }

    @Override
    public int getChannel() {
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
        return 0;
    }

    @Override
    public boolean isNormaliseContrast() {
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
        return false;
    }

    @Override
    public boolean hasCannyOptions() {
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
        return false;
    }

    @Override
    public ICannyOptions getCannyOptions() {
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
        return null;
    }

    @Override
    public boolean isValid(CellularComponent c) {
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
        return false;
    }

    @Override
    public void setChannel(int channel) {
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
    }

    @Override
    public void setScale(double scale) {
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
    }

    @Override
    public void setMaxSize(double maxNucleusSize) {
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
    }

    @Override
    public void setFolder(File folder) {
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
    }

    @Override
    public void setCannyOptions(ICannyOptions canny) {
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
    }

    @Override
    public IDetectionOptions duplicate() {
        return new NuclearSignalOptions(this);
    }

    @Override
    public void setNormaliseContrast(boolean b) {
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);

    }

    @Override
    public void set(IDetectionOptions options) {
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
    }

//    @Override
//    public IDetectionOptions unlock() {
//        return this;
//    }

    @Override
    public boolean isUseHoughTransform() {
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
        return false;
    }

//    @Override
//    public IDetectionOptions lock() {
//        return this;
//    }

    @Override
    public void setHoughOptions(IHoughDetectionOptions hough) {
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
    }

    @Override
    public IDetectionSubOptions getSubOptions(String s) {
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
        return null;
    }

    @Override
    public boolean isRGB() {
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
        return false;
    }

    @Override
    public void setSubOptions(String s, IDetectionSubOptions sub) {
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
    }

    @Override
    public void setRGB(boolean b) {
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
    }

    @Override
    public List<String> getKeys() {
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
        return null;
    }

    @Override
    public boolean hasSubOptions(String key) {
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
        return false;
    }

    @Override
    public double getDouble(String s) {
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
        return 0;
    }

    @Override
    public int getInt(String s) {
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
        return 0;
    }

    @Override
    public boolean getBoolean(String s) {
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
        return false;
    }

    @Override
    public void setDouble(String s, double d) {
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
    }

    @Override
    public void setInt(String s, int i) {
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
    }

    @Override
    public void setBoolean(String s, boolean b) {
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
    }

    @Override
    public float getFloat(String s) {
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
        return 0;
    }

    @Override
    public void setFloat(String s, float f) {
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
        LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
    }

	@Override
	public Map<String, Object> getEntries() {
		LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
		return null;
	}

	@Override
	public Object getValue(String key) {
		LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
		return null;
	}

	@Override
	public List<String> getBooleanKeys() {
		LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
		return null;
	}

	@Override
	public List<String> getIntegerKeys() {
		LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
		return null;
	}

	@Override
	public List<String> getDoubleKeys() {
		LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
		return null;
	}

	@Override
	public List<String> getFloatKeys() {
		LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
		return null;
	}

	@Override
	public Set<String> getSubOptionKeys() {
		LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
		return null;
	}

	@Override
	public String getString(String s) {
		LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
		return null;
	}

	@Override
	public void setString(String k, String v) {
		LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
		
	}

	@Override
	public List<String> getStringKeys() {
		LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
		return null;
	}

	@Override
	public void setShellOptions(@NonNull IShellOptions o) {
		LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
		
	}

	@Override
	public boolean hasShellOptions() {
		LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
		return false;
	}

	@Override
	public IShellOptions getShellOptions() {
		LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
		return null;
	}

	@Override
	public void set(HashOptions o) {
		LOGGER.warning("Unimplemented method: " + this.getClass().getName()+"::"+Thread.currentThread().getStackTrace()[1]);
	}

}
