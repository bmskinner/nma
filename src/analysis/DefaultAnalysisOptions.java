package analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import analysis.signals.INuclearSignalOptions;
import components.nuclear.NucleusType;

/**
 * The default implementation of the IAnalysisOptions interface
 * @author ben
 * @since 1.13.3
 *
 */
public class DefaultAnalysisOptions implements IMutableAnalysisOptions {
	
	private static final long serialVersionUID = 1L;
	
	private Map<String, IMutableDetectionOptions> detectionOptions = new HashMap<String, IMutableDetectionOptions>();
	
	private double profileWindowProportion;
	
	private NucleusType type;
	
	private boolean isRefoldNucleus, isKeepFailed;
	
	/**
	 * The default constructor
	 */
	public DefaultAnalysisOptions(){	
		
	}
	
	/**
	 * The default constructor
	 */
	public DefaultAnalysisOptions(IAnalysisOptions options){	
		for(String key : options.getDetectionOptionTypes()){
			
			IMutableDetectionOptions op = options.getDetectionOptions(key);
			this.setDetectionOptions(key, op.duplicate());
		}
		
	}

	@Override
	public IMutableDetectionOptions getDetectionOptions(String key) {
		return detectionOptions.get(key);
	}

	@Override
	public Set<String> getDetectionOptionTypes() {
		 return detectionOptions.keySet();
	}

	@Override
	public boolean hasDetectionOptions(String type) {
		return detectionOptions.containsKey(type);
	}

	@Override
	public double getProfileWindowProportion() {
		return profileWindowProportion;
	}

	@Override
	public NucleusType getNucleusType() {
		return type;
	}

	@Override
	public boolean refoldNucleus() {
		return isRefoldNucleus;
	}

	@Override
	public Set<UUID> getNuclearSignalGroups() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasSignalDetectionOptions(UUID signalGroup) {
		String key = signalGroup.toString();
		return hasDetectionOptions(key);
	}

	@Override
	public boolean isKeepFailedCollections() {
		return isKeepFailed;
	}

	@Override
	public void setDetectionOptions(String key, IMutableDetectionOptions options) {
		detectionOptions.put(key, options);
		
	}

	@Override
	public void setAngleWindowProportion(double proportion) {
		profileWindowProportion = proportion;
		
	}

	@Override
	public void setNucleusType(NucleusType nucleusType) {
		type = nucleusType;
		
	}

	@Override
	public void setRefoldNucleus(boolean refoldNucleus) {
		isRefoldNucleus = refoldNucleus;
		
	}

	@Override
	public void setKeepFailedCollections(boolean keepFailedCollections) {
		isKeepFailed = keepFailedCollections;
		
	}

	@Override
	public INuclearSignalOptions getNuclearSignalOptions(UUID signalGroup) {
		if(hasSignalDetectionOptions(signalGroup)){
			return (INuclearSignalOptions) getDetectionOptions(signalGroup.toString());
		}
		return null;
	}

	
	
	
}
