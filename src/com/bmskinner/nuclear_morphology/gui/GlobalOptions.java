package com.bmskinner.nuclear_morphology.gui;

import java.io.File;
import java.util.logging.Level;

import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter.ColourSwatch;


/**
 * This holds the options set globally for the program
 * @author bms41
 *
 */
public class GlobalOptions {
	
	private static volatile GlobalOptions instance;
	
	private File defaultDir; // where to fall back to for finding images or saving files
	
	private MeasurementScale scale;
	
	private Level logLevel;
	
	private ColourSwatch swatch;
	
	private boolean violinPlots; // show violin plots or just boxplots
	
	private boolean antiAliasing = false;
	
	private boolean convertDatasets = true;
	
	/**
	 * Should the consensus nucleus plots be filled, or empty
	 */
	private boolean fillConsensus;
	
	public static GlobalOptions getInstance(){
		if(instance==null){
			instance = new GlobalOptions();
		}
		return instance;
	}
	
	private GlobalOptions(){
		setDefaults();
	}
	
	public void setDefaults(){
		this.logLevel    = Level.INFO;
		this.scale       = MeasurementScale.PIXELS;
		this.swatch      = ColourSwatch.REGULAR_SWATCH;
		this.violinPlots = true;
		this.fillConsensus = true;
		this.antiAliasing  = false;
	}

	public MeasurementScale getScale() {
		return scale;
	}

	public void setScale(MeasurementScale scale) {
		this.scale = scale;
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
		return violinPlots;
	}

	public void setViolinPlots(boolean violinPlots) {
		this.violinPlots = violinPlots;
	}
	
	public synchronized boolean isFillConsensus() {
		return fillConsensus;
	}

	public synchronized void setFillConsensus(boolean fillConsensus) {
		this.fillConsensus = fillConsensus;
	}
	
	public synchronized boolean isAntiAlias() {
		return antiAliasing;
	}

	public synchronized void setAntiAlias(boolean antiAliasing) {
		this.antiAliasing = antiAliasing;
	}

	public synchronized boolean isConvertDatasets() {
		return convertDatasets;
	}

	public synchronized void setConvertDatasets(boolean convertDatasets) {
		this.convertDatasets = convertDatasets;
	}
	
	public synchronized File getDefaultDir() {
		return this.defaultDir;
	}
	
	public synchronized void setDefaultDir(File f) {
		this.defaultDir = f;
	}
	

}
