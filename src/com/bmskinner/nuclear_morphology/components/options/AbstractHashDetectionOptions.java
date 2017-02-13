package com.bmskinner.nuclear_morphology.components.options;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.stats.NucleusStatistic;

/**
 * A replacement for the AbstractDetectionOptions providing more
 * extensibility for the future by using a map rather than
 * fixed fields for the stored options
 * @author ben
 * @since 1.13.4
 *
 */
public abstract class AbstractHashDetectionOptions 
	extends AbstractHashOptions
	implements IMutableDetectionOptions {
	
	private static final long serialVersionUID = 1L;
	
	public static final String THRESHOLD = "Threshold";
	public static final String CHANNEL   = "Channel";
	public static final String MIN_CIRC  = "Min circ";
	public static final String MAX_CIRC  = "Max circ";
	public static final String MIN_SIZE  = "Min size";
	public static final String MAX_SIZE  = "Max size";
	public static final String SCALE     = "Scale";
	public static final String IS_NORMALISE_CONTRAST   = "Normalise contrast";
	public static final String IS_USE_HOUGH   = "Use Hough";
	public static final String IS_USE_CANNY   = "Use Canny";
	
	private File folder;
	
	private Map<String, IDetectionSubOptions> subMap = new HashMap<String, IDetectionSubOptions>();
	
	/**
	 * Construct specifying a folder of images to be analysed
	 * @param folder
	 */
	public AbstractHashDetectionOptions(File folder){
		
		this.folder = folder;		
	}
	
	/**
	 * Construct from a template options
	 * @param template
	 */
	protected AbstractHashDetectionOptions(IDetectionOptions template){
		if(template==null){
			throw new IllegalArgumentException("Template options is null");
		}
		
		folder    = template.getFolder();
		intMap.put(THRESHOLD, template.getThreshold());
		intMap.put(CHANNEL, template.getChannel());

		dblMap.put(MIN_CIRC, template.getMinCirc());
		dblMap.put(MAX_CIRC, template.getMaxCirc());
		dblMap.put(MIN_SIZE, template.getMinSize());
		dblMap.put(MAX_SIZE, template.getMaxSize());
		dblMap.put(SCALE,    template.getScale());
		
		boolMap.put(IS_NORMALISE_CONTRAST, template.isNormaliseContrast());
		
		
		if(template.hasCannyOptions()){
			try {
				subMap.put(IDetectionSubOptions.CANNY_OPTIONS, template.getCannyOptions().duplicate());
			} catch (MissingOptionException e) {
				error("Missing Canny options", e);
			}
		} else {
			
			IMutableCannyOptions cannyOptions = OptionsFactory.makeCannyOptions();
			cannyOptions.setUseCanny(false);
			subMap.put(IDetectionSubOptions.CANNY_OPTIONS, cannyOptions);
		}
		

	}
	
	public AbstractHashDetectionOptions setSize(double min, double max){
		dblMap.put(MIN_SIZE, min);
		dblMap.put(MAX_SIZE, max);
		return this;
	}
	
	public AbstractHashDetectionOptions setCircularity(double min, double max){
		dblMap.put(MIN_CIRC, min);
		dblMap.put(MAX_CIRC, max);
		return this;
	}
	
	public IDetectionSubOptions getSubOptions(String s){
		return subMap.get(s);
	}
	
	public void setSubOptions(String s, IDetectionSubOptions op){
		subMap.put(s, op);
	}
		
	
	/* (non-Javadoc)
	 * @see analysis.signals.INuclearSignalOptions#getThreshold()
	 */
	@Override
	public int getThreshold() {
		return intMap.get(THRESHOLD);
	}

	/* (non-Javadoc)
	 * @see analysis.signals.INuclearSignalOptions#setThreshold(int)
	 */
	@Override
	public void setThreshold(int threshold) {
		intMap.put(THRESHOLD, threshold);
	}

	/* (non-Javadoc)
	 * @see analysis.signals.INuclearSignalOptions#getMinCirc()
	 */
	@Override
	public double getMinCirc() {
		return dblMap.get(MIN_CIRC);
	}

	/* (non-Javadoc)
	 * @see analysis.signals.INuclearSignalOptions#setMinCirc(double)
	 */
	@Override
	public void setMinCirc(double minCirc) {
		dblMap.put(MIN_CIRC, minCirc);
	}

	/* (non-Javadoc)
	 * @see analysis.signals.INuclearSignalOptions#getMaxCirc()
	 */
	@Override
	public double getMaxCirc() {
		return dblMap.get(MAX_CIRC);
	}

	/* (non-Javadoc)
	 * @see analysis.signals.INuclearSignalOptions#setMaxCirc(double)
	 */
	@Override
	public void setMaxCirc(double maxCirc) {
		dblMap.put(MAX_CIRC, maxCirc);
	}

	/* (non-Javadoc)
	 * @see analysis.signals.INuclearSignalOptions#getMinSize()
	 */
	@Override
	public double getMinSize() {
		return dblMap.get(MIN_SIZE);
	}

	/* (non-Javadoc)
	 * @see analysis.signals.INuclearSignalOptions#setMinSize(double)
	 */
	@Override
	public void setMinSize(double minSize) {
		dblMap.put(MIN_SIZE, minSize);
	}

	@Override
	public File getFolder() {
		return folder;
	}

	@Override
	public double getMaxSize() {
		return dblMap.get(MAX_SIZE);
	}

	@Override
	public double getScale() {
		return dblMap.get(SCALE);
	}

	@Override
	public int getChannel() {
		return intMap.get(CHANNEL);
	}

	@Override
	public boolean isNormaliseContrast() {
		return boolMap.get(IS_NORMALISE_CONTRAST);
	}

	@Override
	public boolean hasCannyOptions() {
		return subMap.containsKey(IDetectionSubOptions.CANNY_OPTIONS);
	}

	@Override
	public IMutableCannyOptions getCannyOptions() throws MissingOptionException {
		
		if(this.hasCannyOptions()){
			IDetectionSubOptions c = subMap.get(IDetectionSubOptions.CANNY_OPTIONS);
			if(c instanceof IMutableCannyOptions){
				return (IMutableCannyOptions) c;
			} else {
				throw new MissingOptionException("Sub options cannot be cast to canny");
			}
		} else {
			throw new MissingOptionException("Canny options not present");
		}
		
		
	}

	@Override
	public boolean isValid(CellularComponent c){
		
		if(c==null){
			return false;
		}
		if(c.getStatistic(NucleusStatistic.AREA) < this.getMinSize()){
			return false;
		}
		if(c.getStatistic(NucleusStatistic.AREA) > this.getMaxSize()){
			return false;
		}
		if(c.getStatistic(NucleusStatistic.CIRCULARITY) < this.getMinCirc()){
			return false;
		}
		
		if(c.getStatistic(NucleusStatistic.CIRCULARITY) > this.getMaxCirc()){
			return false;
		}
		return true;
		
	}

	@Override
	public void setChannel(int channel) {
		intMap.put(CHANNEL, channel);
	}

	@Override
	public void setScale(double scale) {
		dblMap.put(SCALE, scale);
	}

	@Override
	public void setMaxSize(double maxSize) {
		dblMap.put(MAX_SIZE, maxSize);
		
	}

	@Override
	public void setFolder(File folder) {
		this.folder = folder;
	}

	@Override
	public void setCannyOptions(IMutableCannyOptions canny) {
		subMap.put(IDetectionSubOptions.CANNY_OPTIONS, canny);	
	}
	
	@Override
	public void setHoughOptions(IHoughDetectionOptions hough) {
		subMap.put(IDetectionSubOptions.HOUGH_OPTIONS, hough);	
	}
	
	@Override
	public void setNormaliseContrast(boolean b) {
		boolMap.put(IS_NORMALISE_CONTRAST, b);
	}
	
	@Override
	public void set(IDetectionOptions options){
		
		try {
			this.setCannyOptions(options.getCannyOptions());
		} catch (MissingOptionException e) {
			fine("No canny options to copy");
		}
		this.setChannel(options.getChannel());
		this.setMaxCirc(options.getMaxCirc());
		this.setMinCirc(options.getMinCirc());
		this.setMaxSize(options.getMaxSize());
		this.setMinSize(options.getMinSize());
		this.setThreshold(options.getThreshold());
		this.setScale(options.getScale());
		this.setNormaliseContrast(options.isNormaliseContrast());

		folder = new File(options.getFolder().getAbsolutePath());

		
	}
	
	
	@Override
	public int hashCode(){
		
		final int prime = 31;
		int result = super.hashCode();
		
		result = prime * result + folder.hashCode();
		result = prime * result + intMap.hashCode();
		result = prime * result + dblMap.hashCode();
		result = prime * result + boolMap.hashCode();
		result = prime * result + subMap.hashCode();
		return result;
		
	}
	
	@Override
	public boolean equals(Object o){
		if (this == o)
			return true;
		
		if(o==null)
			return false;
	
		if( ! ( o instanceof IDetectionOptions))
			return false;
		
		
		IDetectionOptions other = (IDetectionOptions) o;
		
		
		if(getThreshold()!=other.getThreshold())
			return false;
		
		if(getChannel()!=other.getChannel())
			return false;
		
		if( Double.doubleToLongBits(getMinCirc())!= 
				Double.doubleToLongBits(other.getMinCirc()))
			return false;
		
		if( Double.doubleToLongBits(getMaxCirc())
				!=Double.doubleToLongBits(other.getMaxCirc()))
			return false;

		if(Double.doubleToLongBits(getMinSize())!=
				Double.doubleToLongBits(other.getMinSize()))
			return false;
		
		if(Double.doubleToLongBits(getMaxSize())	!=
				Double.doubleToLongBits(other.getMaxSize()))
			return false;
		
		if(Double.doubleToLongBits(getScale())!=
				Double.doubleToLongBits(other.getScale()))
			return false;
		
		if(isNormaliseContrast()!=other.isNormaliseContrast())
			return false;
		
		try {
			if(! getCannyOptions().equals(other.getCannyOptions())){
				return false;
			}
		} catch (MissingOptionException e) {
			error("Canny options missing in comparison", e);
		}
				
		return true;
		
	}

	@Override
	public IMutableDetectionOptions unlock() {
		return this;
	}
	

	@Override
	public boolean isUseHoughTransform() {
		return boolMap.get(IS_USE_HOUGH);
	}

	

	@Override
	public IDetectionOptions lock() {
		return this;
	}


}
