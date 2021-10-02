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
package com.bmskinner.nuclear_morphology.components.rules;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.components.profiles.Landmark;

/**
 * This holds the rulesets for identifying each of the landmarks in a profile.
 * Multiple RuleSets can be combined for each tag, allowing multiple
 * profile types to be used. Designed to be stored within a cell collection.
 * 
 * It also stores which landmarks should be used for orientation of the nucleus.
 * This replaces the v1.x.x series of dedicated classes for each type of nucleus
 * to be analysed.
 * 
 * @author ben
 *
 */
public class RuleSetCollection implements Serializable {
	
	private static final Logger LOGGER = Logger.getLogger(RuleSetCollection.class.getName());

    private static final long serialVersionUID = 1L;
    
    private final String name;

    private final Map<Landmark, List<RuleSet>> map = new HashMap<>();
        
    /** Track which landmarks to use for X axis
     *  Can both be null for no preference **/
    private final Landmark leftCoM;
    private final Landmark rightCoM;
    
    /** Track which landmarks to use for Y axis
     *  Can both be null for no preference **/
    private final Landmark topVertical;
    private final Landmark btmVertical;

    private final Landmark seondaryX;
    private final Landmark seondaryY;
    
    private final PriorityAxis priorityAxis;
    
    private final RuleApplicationType ruleApplicationType;
    
    /**
     * Create a new empty collection. Specify the landmarks that should
     * be used preferentially by nuclei for orientation
     */
    public RuleSetCollection(@NonNull String name, @Nullable Landmark left, @Nullable Landmark right, 
    		@Nullable Landmark top, @Nullable Landmark bottom, @Nullable Landmark seondaryX,
    		@Nullable Landmark seondaryY, @Nullable PriorityAxis priorityAxis, RuleApplicationType type) {
    	this.name = name;
    	leftCoM = left;
    	rightCoM = right;
    	topVertical = top;
    	btmVertical = bottom;
    	this.seondaryX = seondaryX;
    	this.seondaryY = seondaryY;
    	this.priorityAxis = priorityAxis;
    	this.ruleApplicationType = type;
    }
    
    public String getName() {
    	return name;
    }
    
    public RuleApplicationType getApplicationType() {
    	return ruleApplicationType;
    }
    
    /**
     * Test if the rules specify a preferred landmark
     * to the left or right of the CoM
     * @return
     */
    public boolean isAsymmetricX() {
    	return leftCoM!=null || rightCoM!=null;
    }
    
    /**
     * Test if the rules specify a preferred landmark
     * above or below the CoM
     * @return
     */
    public boolean isAsymmetricY() {
    	return topVertical!=null || btmVertical!=null;
    }
    
    /**
     * Test if there is asymmetry in either axis
     * @return
     */
    public boolean isAsymmetric() {
    	return isAsymmetricX() || isAsymmetricY();
    }

    public Optional<Landmark> getSecondaryX() {
		return Optional.ofNullable(seondaryX);
	}

	public Optional<Landmark> getSecondaryY() {
		return Optional.ofNullable(seondaryY);
	}

	public Optional<PriorityAxis> getPriorityAxis() {
		return Optional.ofNullable(priorityAxis);
	}

	public Optional<Landmark> getLeftLandmark() {
    	return Optional.ofNullable(leftCoM);
    }

    public Optional<Landmark> getRightLandmark() {
    	return Optional.ofNullable(rightCoM);
    }

    public Optional<Landmark> getTopLandmark() {
    	return Optional.ofNullable(topVertical);
    }
    
    public Optional<Landmark> getBottomLandmark() {
    	return Optional.ofNullable(btmVertical);
    }

    /**
     * Remove all the RuleSets for the given tag
     * 
     * @param tag
     */
    public void clearRuleSets(@NonNull Landmark tag) {
        map.put(tag, new ArrayList<>());
    }
    
    public List<RuleSet> removeRuleSets(@NonNull Landmark tag) {
    	return map.remove(tag);
    }

    /**
     * Add a ruleset for the given tag
     * 
     * @param tag
     * @param r
     */
    public void addRuleSet(@NonNull Landmark tag, @NonNull RuleSet r) {
    	map.computeIfAbsent(tag, k -> new ArrayList<>());
    	map.get(tag).add(r);
    }

    /**
     * Replace existing RuleSets for the given tag with the list
     * 
     * @param tag
     * @param list
     */
    public void setRuleSets(@NonNull Landmark tag, @NonNull List<RuleSet> list) {
        map.put(tag, new ArrayList<>());
        map.get(tag).addAll(list);
    }

    /**
     * Get the rulesets for the given tag
     * 
     * @param tag
     * @param r
     */
    public List<RuleSet> getRuleSets(@NonNull Landmark tag) {
        return map.get(tag);
    }

    public Set<Landmark> getTags() {
        return map.keySet();
    }

    /**
     * Test if the collection has rulesets for the given tag
     * @param tag the tag to check
     * @return true if rulesets are present, false otherwise
     */
    public boolean hasRulesets(@NonNull Landmark tag) {
    	if(map.containsKey(tag)) {
    		return !map.get(tag).isEmpty();
    	}
    	return false;
    }

    /**
     * Test if the collection is empty
     * @return
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
	public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("RuleSets:\n");
        for(Entry<Landmark, List<RuleSet>> entry : map.entrySet()) {
            b.append(entry.getKey() + ":\n");
            for (RuleSet r : entry.getValue()) {
                b.append(r.toString() + "\n");
            }
        }

        return b.toString();
    }

	@Override
	public int hashCode() {
		return Objects.hash(btmVertical, leftCoM, map, priorityAxis, 
				rightCoM, seondaryX, seondaryY, topVertical);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RuleSetCollection other = (RuleSetCollection) obj;
		return Objects.equals(btmVertical, other.btmVertical) && Objects.equals(leftCoM, other.leftCoM)
				&& Objects.equals(map, other.map) && Objects.equals(priorityAxis, other.priorityAxis)
				&& Objects.equals(rightCoM, other.rightCoM) && Objects.equals(seondaryX, other.seondaryX)
				&& Objects.equals(seondaryY, other.seondaryY) && Objects.equals(topVertical, other.topVertical)
				&& Objects.equals(ruleApplicationType, other.ruleApplicationType);
	}

    /**
     * Create a RuleSetCollection for mouse sperm nuclei
     * 
     * @return
     */
    public static RuleSetCollection mouseSpermRuleSetCollection() {
        RuleSetCollection r = new RuleSetCollection("Mouse sperm", Landmark.REFERENCE_POINT, null,
        		Landmark.TOP_VERTICAL, Landmark.BOTTOM_VERTICAL,
        		null, Landmark.ORIENTATION_POINT, PriorityAxis.Y, RuleApplicationType.VIA_MEDIAN);

        r.addRuleSet(Landmark.REFERENCE_POINT, RuleSet.mouseSpermRPRuleSet());
        r.addRuleSet(Landmark.ORIENTATION_POINT, RuleSet.mouseSpermOPRuleSet());
        r.addRuleSet(Landmark.TOP_VERTICAL, RuleSet.mouseSpermTVRuleSet());
        r.addRuleSet(Landmark.BOTTOM_VERTICAL, RuleSet.mouseSpermBVRuleSet());
        return r;
    }

    /**
     * Create a RuleSetCollection for pig sperm nuclei
     * 
     * @return
     */
    public static RuleSetCollection pigSpermRuleSetCollection() {
        RuleSetCollection r = new RuleSetCollection("Pig sperm", null, null, null, Landmark.REFERENCE_POINT,
        		null, Landmark.ORIENTATION_POINT, PriorityAxis.Y, RuleApplicationType.VIA_MEDIAN);

        r.addRuleSet(Landmark.REFERENCE_POINT, RuleSet.pigSpermRPRuleSet());
        r.addRuleSet(Landmark.ORIENTATION_POINT, RuleSet.pigSpermOPRuleSet());
        return r;
    }

    /**
     * Create a RuleSetCollection for round nuclei
     * 
     * @return
     */
    public static RuleSetCollection roundRuleSetCollection() {
        RuleSetCollection r = new RuleSetCollection("Round", null, null, null, Landmark.REFERENCE_POINT,
        		null, Landmark.REFERENCE_POINT, PriorityAxis.Y, RuleApplicationType.VIA_MEDIAN);

        r.addRuleSet(Landmark.REFERENCE_POINT, RuleSet.roundRPRuleSet());
        return r;
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

}
