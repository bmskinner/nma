/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.core;

import java.io.File;
import java.util.logging.Level;

import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.options.AbstractHashOptions;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter.ColourSwatch;

/**
 * This holds the options set globally for the program
 * 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class GlobalOptions extends AbstractHashOptions {

    private static volatile GlobalOptions instance;
    private static final Object           lockObject = new Object(); // synchronisation
    
    public static final String DEFAULT_DIR_KEY              = "DEFAULT_DIR";
    public static final String DEFAULT_DISPLAY_SCALE_KEY    = "DEFAULT_DISPLAY_SCALE";
    public static final String DEFAULT_SWATCH_KEY           = "DEFAULT_COLOUR_SWATCH";
    public static final String DEFAULT_NUCLEUS_TYPE_KEY     = "DEFAULT_NUCLEUS_TYPE";

    public static final String DEFAULT_IMAGE_SCALE_KEY      = "DEFAULT_IMAGE_SCALE";
    
    // Should the consensus plots be filled or empty
    public static final String DEFAULT_FILL_CONSENSUS_KEY   = "FILL_CONSENSUS";
    public static final String DEFAULT_USE_ANTIALIASING_KEY = "USE_ANTIALIASING";
    public static final String REFOLD_OVERRIDE_KEY          = "REFOLD_OVERRIDE";
    
    // Should violin plots be shown instead of boxplots
    public static final String IS_VIOLIN_KEY                = "IS_VIOLIN";
    public static final String IS_USE_ANTIALIASING          = "USE_ANTIALIASING";
    
    public static final String IS_CONVERT_DATASETS_KEY      = "CONVERT_DATASETS";
    
    public static final String IS_DOCKABLE_INTERFACE_KEY    = "DOCKABLE_INTERFACE";

    private File defaultDir; // where to fall back to for finding images or
                             // saving files

    private MeasurementScale scale;

    private Level logLevel;

    private ColourSwatch swatch;

    private static double DEFAULT_SCALE = 1;

    private NucleusType defaultType;


    /**
     * Get the global options for the program.
     * 
     * @return
     */
    public static GlobalOptions getInstance() {

        if (instance != null) {
            return instance;
        } else {

            synchronized (lockObject) {
                if (instance == null) {
                    instance = new GlobalOptions();
                }
            }

            return instance;
        }

    }

    private GlobalOptions() {
        setDefaults();
    }

    public void setDefaults() {
        this.logLevel = Level.INFO;
        this.scale = MeasurementScale.PIXELS;
        this.swatch = ColourSwatch.REGULAR_SWATCH;
        setBoolean(IS_VIOLIN_KEY, true);
        setBoolean(DEFAULT_FILL_CONSENSUS_KEY, true);
        setBoolean(IS_USE_ANTIALIASING, true);
        setDouble(DEFAULT_IMAGE_SCALE_KEY, DEFAULT_SCALE);
        this.defaultDir = new File(System.getProperty("user.home"));
        this.defaultType = NucleusType.RODENT_SPERM;
        setBoolean(REFOLD_OVERRIDE_KEY, false);
        setBoolean(IS_CONVERT_DATASETS_KEY, true);
        setBoolean(IS_DOCKABLE_INTERFACE_KEY, true);
    }

    public NucleusType getDefaultType() {
        return defaultType;
    }

    public void setDefaultType(NucleusType defaultType) {
        this.defaultType = defaultType;
    }

    public synchronized MeasurementScale getScale() {
        return scale;
    }

    public void setScale(MeasurementScale scale) {
        this.scale = scale;
    }

    public synchronized double getImageScale() {

        return getDouble(DEFAULT_IMAGE_SCALE_KEY);
    }

    public void setImageScale(double scale) {
        setDouble(DEFAULT_IMAGE_SCALE_KEY, scale);
    }

    public synchronized Level getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(Level logLevel) {
        this.logLevel = logLevel;
    }

    public synchronized ColourSwatch getSwatch() {
        return swatch;
    }

    public void setSwatch(ColourSwatch swatch) {
        this.swatch = swatch;
    }

    public synchronized boolean isViolinPlots() {
        return getBoolean(IS_VIOLIN_KEY);
    }

    public void setViolinPlots(boolean violinPlots) {
        setBoolean(IS_VIOLIN_KEY, violinPlots);
    }

    public synchronized boolean isFillConsensus() {
        return getBoolean(DEFAULT_FILL_CONSENSUS_KEY);
    }

    public synchronized void setFillConsensus(boolean fillConsensus) {
        setBoolean(DEFAULT_FILL_CONSENSUS_KEY, fillConsensus);
    }
    
    public synchronized boolean isOverrideRefold() {
        return getBoolean(REFOLD_OVERRIDE_KEY);
    }

    public synchronized void setOverrideRefold(boolean b) {
        setBoolean(REFOLD_OVERRIDE_KEY, b);
    }

    public synchronized boolean isAntiAlias() {
        return getBoolean(IS_USE_ANTIALIASING);
    }

    public synchronized void setAntiAlias(boolean antiAliasing) {
        setBoolean(IS_USE_ANTIALIASING, antiAliasing);
    }

    public synchronized boolean isConvertDatasets() {
    	return getBoolean(IS_CONVERT_DATASETS_KEY);
    }

    public synchronized void setConvertDatasets(boolean convertDatasets) {
    	setBoolean(IS_CONVERT_DATASETS_KEY, convertDatasets);
    }
    
    public synchronized boolean isUseDockableInterface() {
    	return getBoolean(IS_DOCKABLE_INTERFACE_KEY);
    }

    public synchronized void setUseDockableInterface(boolean b) {
    	setBoolean(IS_DOCKABLE_INTERFACE_KEY, b);
    }

    /**
     * Get the default directory for exporting results or beginning new
     * analyses. If this has not been set in the config file, it defaults to the
     * user home directory.
     * 
     * @return
     */
    public synchronized File getDefaultDir() {
        if (defaultDir.exists()) {
            return defaultDir;
        } else {
            return new File(System.getProperty("user.home"));
        }
    }

    public synchronized void setDefaultDir(File f) {
        this.defaultDir = f;
    }

}
