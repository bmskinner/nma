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

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.rules.RuleApplicationType;

/**
 * The default implementation of the IAnalysisOptions interface
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public class DefaultAnalysisOptions implements IAnalysisOptions {

    private static final long serialVersionUID = 1L;

    private Map<String, IDetectionOptions> detectionOptions = new HashMap<>();

    private double profileWindowProportion;

    private NucleusType type;

    @Deprecated
    private boolean isRefoldNucleus, isKeepFailed;
    
    private final long analysisTime; 
    
    /* Store options that are not detection options. For example, clustering or tSNE options */
     private Map<String, HashOptions> secondaryOptions = new HashMap<>();
     
     /** How border tags should be detected in this dataset */
     private RuleApplicationType ruleApplicationType = RuleApplicationType.VIA_MEDIAN;


    /**
     * The default constructor, which sets default options specified in
     * IAnalysisOptions
     */
    public DefaultAnalysisOptions() {
        profileWindowProportion = DEFAULT_WINDOW_PROPORTION;
        type = DEFAULT_TYPE;
        isRefoldNucleus = DEFAULT_REFOLD;
        isKeepFailed = DEFAULT_KEEP_FAILED;
        analysisTime = System.currentTimeMillis();
    }

    /**
     * Construct from a template options
     * 
     * @param template the options to use as a template
     */
    public DefaultAnalysisOptions(@NonNull IAnalysisOptions template) {
        set(template);
        analysisTime = template.getAnalysisTime();
    }
    
    @Override
	public IAnalysisOptions duplicate() {
		return new DefaultAnalysisOptions(this);
	}

    @Override
    public Optional<IDetectionOptions> getDetectionOptions(String key){
        if (detectionOptions.containsKey(key)) {
            return Optional.of(detectionOptions.get(key));
        }
		return Optional.empty();

    }

    @Override
    public Set<String> getDetectionOptionTypes() {
        return detectionOptions.keySet();
    }
    
	@Override
	public Optional<IDetectionOptions> getNuclusDetectionOptions() {
		return getDetectionOptions(CellularComponent.NUCLEUS);
	}

    @Override
    public boolean hasDetectionOptions(String type) {
        return detectionOptions.containsKey(type);
    }
    

	@Override
	public Optional<HashOptions> getSecondaryOptions(String key) {
		if (secondaryOptions.containsKey(key)) {
            return Optional.of(secondaryOptions.get(key));
        }
		return Optional.empty();
	}

	@Override
	public Set<String> getSecondaryOptionKeys() {
		return secondaryOptions.keySet();
	}

	@Override
	public boolean hasSecondaryOptions(String key) {
		return secondaryOptions.containsKey(key);
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
    	Set<UUID> result = new HashSet<>();
    	for(String s : detectionOptions.keySet()) {
    		if(s.equals(CellularComponent.NUCLEUS) || s.equals(CellularComponent.CYTOPLASM) || s.equalsIgnoreCase(CellularComponent.SPERM_TAIL))
    			continue;
    		
    		
    		try {
    			if(s.startsWith(SIGNAL_GROUP)) {
    				UUID id = UUID.fromString(s.replace(SIGNAL_GROUP, ""));
    				result.add(id);
    			}
    		} catch(IllegalArgumentException e) {
    			// not a UUID
    		}
    	}
        return result;
    }

    @Override
    public boolean hasSignalDetectionOptions(@NonNull UUID signalGroup) {
        String key = SIGNAL_GROUP+signalGroup.toString();
        return hasDetectionOptions(key);
    }

    @Override
    public boolean isKeepFailedCollections() {
        return isKeepFailed;
    }
    
    @Override
    public long getAnalysisTime() {
    	return analysisTime;
    }

    @Override
    public void setDetectionOptions(String key, IDetectionOptions options) {
        detectionOptions.put(key, options);
    }
    
    @Override
    public void setSecondaryOptions(String key, HashOptions options) {
    	secondaryOptions.put(key, options);
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
    public INuclearSignalOptions getNuclearSignalOptions(@NonNull UUID signalGroup) {

    	Optional<IDetectionOptions> op = getDetectionOptions(SIGNAL_GROUP+signalGroup.toString());
    	
    	if(op.isPresent())
    		return (INuclearSignalOptions) op.get();
    	
    	// If is is the old format
    	op = getDetectionOptions(signalGroup.toString());
    	if(op.isPresent())
    		return (INuclearSignalOptions) op.get();

        return null;
    }
    
	@Override
	public RuleApplicationType getRuleApplicationType() {
		return ruleApplicationType;
	}

	@Override
	public void setRuleApplicationType(RuleApplicationType type) {
		ruleApplicationType = type;
	}
    
    @Override
    public void set(@NonNull IAnalysisOptions template) {
    	for (String key : template.getDetectionOptionTypes()) {
            Optional<IDetectionOptions> op  = template.getDetectionOptions(key);
            if(op.isPresent())
	            setDetectionOptions(key, op.get().duplicate());
        }

        profileWindowProportion = template.getProfileWindowProportion();
        type = template.getNucleusType();
        ruleApplicationType = template.getRuleApplicationType();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 0;

        result = prime * result + detectionOptions.hashCode();

        long temp = Double.doubleToLongBits(profileWindowProportion);
        result = prime * result + (int) (temp ^ (temp >>> 32));

        result = prime * result + type.hashCode();
        result = prime * result + ruleApplicationType.hashCode();

        result = prime * result + (isRefoldNucleus ? 1231 : 1237);
        result = prime * result + (isKeepFailed ? 1231 : 1237);

        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null)
            return false;
        if (!(o instanceof IAnalysisOptions))
            return false;
        IAnalysisOptions other = (IAnalysisOptions) o;
        
        Set<String> thisKeys  =  detectionOptions.keySet();
        Set<String> otherKeys =  other.getDetectionOptionTypes();
        
        if(!thisKeys.equals(otherKeys))
        	return false;

        for (String key : thisKeys) {
            IDetectionOptions subOptions = detectionOptions.get(key);
            if(!other.hasDetectionOptions(key))
            	return false;
            Optional<IDetectionOptions> otherSubOp = other.getDetectionOptions(key);
            if(!otherSubOp.isPresent())
            	return false;
            IDetectionOptions otherSub = otherSubOp.get();
            if (!subOptions.equals(otherSub))
            	return false;
        }

        if (Double.doubleToLongBits(profileWindowProportion) != Double
                .doubleToLongBits(other.getProfileWindowProportion()))
            return false;

        if (type != other.getNucleusType())
            return false;
        
        if(ruleApplicationType != other.getRuleApplicationType())
        	return false;
        return true;
    }
    
    
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("Analysis options"+IDetectionOptions.NEWLINE);
        b.append("Run at: "+analysisTime+IDetectionOptions.NEWLINE);
        for (String s : detectionOptions.keySet()) {
            b.append(s+IDetectionOptions.NEWLINE);
            IDetectionOptions d = detectionOptions.get(s);
            b.append(d.toString());
        }
        b.append(IDetectionOptions.NEWLINE+profileWindowProportion);
        b.append(IDetectionOptions.NEWLINE+type);
        b.append(IDetectionOptions.NEWLINE+ruleApplicationType);
        return b.toString();
    }  
    
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        
        //  Changes from 1.15.2 to 1.16.0
        if(secondaryOptions==null)
        	secondaryOptions = new HashMap<>();
        
        // Changes from 1.18.2 to 1.18.3/1.19.0
        if(ruleApplicationType==null)
        	ruleApplicationType = RuleApplicationType.VIA_MEDIAN;
    }
}
