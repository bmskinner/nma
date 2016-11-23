package analysis.signals;

import java.io.File;

import analysis.IMutableCannyOptions;
import analysis.IMutableDetectionOptions;
import components.CellularComponent;

@Deprecated
public class NuclearSignalOptions implements IMutableNuclearSignalOptions {
	
	private static final long serialVersionUID = 1L;
	
	public int threshold;
	public double minCirc, maxCirc, minSize, maxFraction;
	public int detectionMode;

	public NuclearSignalOptions(){
		this(   DEFAULT_SIGNAL_THRESHOLD, 
				DEFAULT_MIN_CIRC,
				DEFAULT_MAX_CIRC,
				DEFAULT_MIN_SIGNAL_SIZE,
				DEFAULT_MAX_SIGNAL_FRACTION,
				SignalDetectionMode.FORWARD);
		
	}
	
	public NuclearSignalOptions(int threshold, double minCirc, double maxCirc,
			double minSize, double maxFraction, SignalDetectionMode detectionMode) {
		this.threshold = threshold;
		this.minCirc = minCirc;
		this.maxCirc = maxCirc;
		this.minSize = minSize;
		this.maxFraction = maxFraction;
		
		switch(detectionMode){
			case FORWARD: this.detectionMode = 0;
				break;
			case REVERSE: this.detectionMode = 1;
				break;
			case ADAPTIVE: this.detectionMode = 2;
				break;
		}
	}
	
	/**
	 * Construct from a template object
	 * @param template
	 */
	protected NuclearSignalOptions(INuclearSignalOptions template){
		threshold = template.getThreshold();
		
		minCirc = template.getMinCirc();
		maxCirc = template.getMaxCirc();
		minSize = template.getMinSize();
		maxFraction = template.getMaxFraction();
		
		SignalDetectionMode	mode = template.getDetectionMode();
		switch(mode){
			case FORWARD: this.detectionMode = 0;
				break;
			case REVERSE: this.detectionMode = 1;
				break;
			case ADAPTIVE: this.detectionMode = 2;
				break;
		}

				
	}

	/* (non-Javadoc)
	 * @see analysis.signals.INuclearSignalOptions#getThreshold()
	 */
	@Override
	public int getThreshold() {
		return threshold;
	}

	/* (non-Javadoc)
	 * @see analysis.signals.INuclearSignalOptions#setThreshold(int)
	 */
	@Override
	public void setThreshold(int threshold) {
		this.threshold = threshold;
	}

	/* (non-Javadoc)
	 * @see analysis.signals.INuclearSignalOptions#getMinCirc()
	 */
	@Override
	public double getMinCirc() {
		return minCirc;
	}

	/* (non-Javadoc)
	 * @see analysis.signals.INuclearSignalOptions#setMinCirc(double)
	 */
	@Override
	public void setMinCirc(double minCirc) {
		this.minCirc = minCirc;
	}

	/* (non-Javadoc)
	 * @see analysis.signals.INuclearSignalOptions#getMaxCirc()
	 */
	@Override
	public double getMaxCirc() {
		return maxCirc;
	}

	/* (non-Javadoc)
	 * @see analysis.signals.INuclearSignalOptions#setMaxCirc(double)
	 */
	@Override
	public void setMaxCirc(double maxCirc) {
		this.maxCirc = maxCirc;
	}

	/* (non-Javadoc)
	 * @see analysis.signals.INuclearSignalOptions#getMinSize()
	 */
	@Override
	public double getMinSize() {
		return minSize;
	}

	/* (non-Javadoc)
	 * @see analysis.signals.INuclearSignalOptions#setMinSize(double)
	 */
	@Override
	public void setMinSize(double minSize) {
		this.minSize = minSize;
	}

	/* (non-Javadoc)
	 * @see analysis.signals.INuclearSignalOptions#getMaxFraction()
	 */
	@Override
	public double getMaxFraction() {
		return maxFraction;
	}

	/* (non-Javadoc)
	 * @see analysis.signals.INuclearSignalOptions#setMaxFraction(double)
	 */
	@Override
	public void setMaxFraction(double maxFraction) {
		this.maxFraction = maxFraction;
	}

	/* (non-Javadoc)
	 * @see analysis.signals.INuclearSignalOptions#getDetectionMode()
	 */
	@Override
	public SignalDetectionMode getDetectionMode() {
		
		switch(detectionMode){
			case 0: return SignalDetectionMode.FORWARD;
	
			case 1: return SignalDetectionMode.REVERSE;
	
			case 2: return SignalDetectionMode.ADAPTIVE;
			
			default: return SignalDetectionMode.FORWARD;
		}

	}


	/* (non-Javadoc)
	 * @see analysis.signals.INuclearSignalOptions#setDetectionMode(int)
	 */
	@Override
	public void setDetectionMode(SignalDetectionMode detectionMode) {
		switch(detectionMode){
		case FORWARD: this.detectionMode = 0;
			break;
		case REVERSE: this.detectionMode = 1;
			break;
		case ADAPTIVE: this.detectionMode = 2;
			break;
	}
	}

	@Override
	public File getFolder() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getMaxSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getScale() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getChannel() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isNormaliseContrast() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasCannyOptions() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public IMutableCannyOptions getCannyOptions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isValid(CellularComponent c) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setChannel(int channel) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setScale(double scale) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setMaxSize(double maxNucleusSize) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setFolder(File folder) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setCannyOptions(IMutableCannyOptions canny) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IMutableDetectionOptions duplicate() {
		return new NuclearSignalOptions(this);
	}

	@Override
	public void setNormaliseContrast(boolean b) {
		// TODO Auto-generated method stub
		
	}
	
	
}