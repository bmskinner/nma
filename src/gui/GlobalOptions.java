package gui;

import gui.components.ColourSelecter.ColourSwatch;

import java.util.logging.Level;

import components.generic.MeasurementScale;


/**
 * This holds the options set globally for the program
 * @author bms41
 *
 */
public class GlobalOptions {
	
	private static GlobalOptions instance;
	
	private MeasurementScale scale;
	
	private Level logLevel;
	
	private ColourSwatch swatch;
	
	private boolean violinPlots; // show violin plots or just boxplots
	
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
	}

	public MeasurementScale getScale() {
		return scale;
	}

	public void setScale(MeasurementScale scale) {
		this.scale = scale;
	}

	public Level getLogLevel() {
		return logLevel;
	}

	public void setLogLevel(Level logLevel) {
		this.logLevel = logLevel;
	}

	public ColourSwatch getSwatch() {
		return swatch;
	}

	public void setSwatch(ColourSwatch swatch) {
		this.swatch = swatch;
	}

	public boolean isViolinPlots() {
		return violinPlots;
	}

	public void setViolinPlots(boolean violinPlots) {
		this.violinPlots = violinPlots;
	}
	
	public boolean isFillConsensus() {
		return fillConsensus;
	}

	public void setFillConsensus(boolean fillConsensus) {
		this.fillConsensus = fillConsensus;
	}

}
