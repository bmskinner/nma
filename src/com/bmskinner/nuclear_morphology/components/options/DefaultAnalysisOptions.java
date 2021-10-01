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
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;
import com.bmskinner.nuclear_morphology.io.Io;

/**
 * The default implementation of the IAnalysisOptions interface
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public class DefaultAnalysisOptions implements IAnalysisOptions {

    private static final long serialVersionUID = 1L;

    private Map<String, HashOptions> detectionOptions = new HashMap<>();

    private double profileWindowProportion;

    private RuleSetCollection rulesets;
    
    private final long analysisTime; 
    
    /* Store options that are not detection options. For example, clustering or tSNE options */
     private Map<String, HashOptions> secondaryOptions = new HashMap<>();
     

    /**
     * The default constructor, which sets default options specified in
     * IAnalysisOptions
     */
    public DefaultAnalysisOptions() {
        profileWindowProportion = DEFAULT_WINDOW_PROPORTION;
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
    public Optional<HashOptions> getDetectionOptions(String key){
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
	public RuleSetCollection getRuleSetCollection() {
		return rulesets;
	}

	@Override
	public void setRuleSetCollection(RuleSetCollection rsc) {
		rulesets = rsc;
	}
    
	@Override
	public Optional<HashOptions> getNuclusDetectionOptions() {
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
    public long getAnalysisTime() {
    	return analysisTime;
    }

    @Override
    public void setDetectionOptions(String key, HashOptions options) {
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
    public HashOptions getNuclearSignalOptions(@NonNull UUID signalGroup) {
    	//TODO Ugly
    	Optional<HashOptions> op = getDetectionOptions(SIGNAL_GROUP+signalGroup.toString());
    	
    	if(op.isPresent())
    		return op.get();
    	
        return null;
    }
        
    @Override
    public void set(@NonNull IAnalysisOptions template) {
    	for (String key : template.getDetectionOptionTypes()) {
            Optional<HashOptions> op  = template.getDetectionOptions(key);
            if(op.isPresent())
	            setDetectionOptions(key, op.get().duplicate());
        }

        profileWindowProportion = template.getProfileWindowProportion();
        rulesets = template.getRuleSetCollection();
    }
    
    @Override
	public int hashCode() {
		return Objects.hash(analysisTime, detectionOptions, profileWindowProportion, rulesets, secondaryOptions);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DefaultAnalysisOptions other = (DefaultAnalysisOptions) obj;
		return analysisTime == other.analysisTime && Objects.equals(detectionOptions, other.detectionOptions)
				&& Double.doubleToLongBits(profileWindowProportion) == Double
						.doubleToLongBits(other.profileWindowProportion)
				&& Objects.equals(rulesets, other.rulesets) && Objects.equals(secondaryOptions, other.secondaryOptions);
	}

	@Override
    public String toString() {
        StringBuilder b = new StringBuilder("Analysis options"+Io.NEWLINE);
        b.append("Run at: "+analysisTime+Io.NEWLINE);
        for(Entry<String,HashOptions> e : detectionOptions.entrySet()) {
            b.append(e.getKey()+Io.NEWLINE);
            b.append(e.getValue().toString());
        }
        b.append(Io.NEWLINE+profileWindowProportion);
        b.append(Io.NEWLINE+rulesets.getName());
        return b.toString();
    }  
    
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }
}
