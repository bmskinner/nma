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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.bmskinner.nuclear_morphology.components.generic.BorderTag;
import com.bmskinner.nuclear_morphology.components.generic.BorderTagObject;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;

/**
 * This holds the rulesets for identifying each of the BorderTags in a profile.
 * Multiple RuleSets can be combined for each tag, allowing multiple
 * ProfileTypes to be used. Designed to be stored within a cell collection
 * 
 * @author bms41
 *
 */
public class RuleSetCollection implements Serializable {
	
	private static final Logger LOGGER = Logger.getLogger(RuleSetCollection.class.getName());

    private static final long serialVersionUID = 1L;

    private Map<Tag, List<RuleSet>> map = new HashMap<>();

    /**
     * Create a new empty collection
     */
    public RuleSetCollection() {
//        for (Tag tag : BorderTagObject.values()) {
//            clearRuleSets(tag);
//        }
//        clearRuleSets(Tag.CUSTOM_POINT);
    }

    /**
     * Remove all the RuleSets for the given tag
     * 
     * @param tag
     */
    public void clearRuleSets(Tag tag) {
        map.put(tag, new ArrayList<RuleSet>());
    }

    /**
     * Add a ruleset for the given tag
     * 
     * @param tag
     * @param r
     */
    public void addRuleSet(Tag tag, RuleSet r) {
    	if(!map.containsKey(tag)) {
    		map.put(tag, new ArrayList<>());
    	}
    	map.get(tag).add(r);
    }

    /**
     * Replace existing RuleSets for the given tag with the list
     * 
     * @param tag
     * @param list
     */
    public void setRuleSets(Tag tag, List<RuleSet> list) {
        map.put(tag, list);
    }

    /**
     * Get the rulesets for the given tag
     * 
     * @param tag
     * @param r
     */
    public List<RuleSet> getRuleSets(Tag tag) {
        return map.get(tag);
    }

    public Set<Tag> getTags() {
        return map.keySet();
    }

    public boolean hasRulesets(Tag tag) {
        return map.get(tag).size() > 0;
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("RuleSets:\n");
        for (Tag tag : map.keySet()) {
            b.append("\t" + tag + ":\n");
            List<RuleSet> l = map.get(tag);
            for (RuleSet r : l) {
                b.append("\t" + r.toString() + "\n");
            }
        }

        return b.toString();
    }
    
    

    /*
     * Static methods to create the default rulesets for a given NucleusType
     */

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((map == null) ? 0 : map.hashCode());
		return result;
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
		if (map == null) {
			if (other.map != null)
				return false;
		} else if (!map.equals(other.map))
			return false;
		return true;
	}

	public static RuleSetCollection createDefaultRuleSet(NucleusType type) {

        switch (type) {
        case PIG_SPERM:
            return createPigSpermRuleSets();
        case RODENT_SPERM:
            return createMouseSpermRuleSets();
        default:
            return createRoundRuleSets();

        }

    }

    /**
     * Create a RuleSetCollection for mouse sperm nuclei
     * 
     * @return
     */
    private static RuleSetCollection createMouseSpermRuleSets() {
        RuleSetCollection r = new RuleSetCollection();

        r.addRuleSet(Tag.REFERENCE_POINT, RuleSet.mouseSpermRPRuleSet());
        r.addRuleSet(Tag.ORIENTATION_POINT, RuleSet.mouseSpermOPRuleSet());
        r.addRuleSet(Tag.TOP_VERTICAL, RuleSet.mouseSpermTVRuleSet());
        r.addRuleSet(Tag.BOTTOM_VERTICAL, RuleSet.mouseSpermBVRuleSet());
        return r;
    }

    /**
     * Create a RuleSetCollection for pig sperm nuclei
     * 
     * @return
     */
    private static RuleSetCollection createPigSpermRuleSets() {
        RuleSetCollection r = new RuleSetCollection();

        r.addRuleSet(Tag.REFERENCE_POINT, RuleSet.pigSpermRPRuleSet());
        r.addRuleSet(Tag.ORIENTATION_POINT, RuleSet.pigSpermOPRuleSet());
        return r;
    }

    /**
     * Create a RuleSetCollection for round nuclei
     * 
     * @return
     */
    private static RuleSetCollection createRoundRuleSets() {
        RuleSetCollection r = new RuleSetCollection();

        r.addRuleSet(Tag.REFERENCE_POINT, RuleSet.roundRPRuleSet());
        r.addRuleSet(Tag.ORIENTATION_POINT, RuleSet.roundOPRuleSet());
        return r;
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        if (map != null) {

            Map<Tag, List<RuleSet>> newMap = new HashMap<>();

            Iterator<?> it = map.keySet().iterator();

            // We need to convert any old tags using enums to the newer objects
            while (it.hasNext()) {
                Object tag = it.next();
                if (tag instanceof BorderTag) {
                    LOGGER.finer("No BorderTagObject for " + tag.toString() + ": creating");
                    newMap.put(new BorderTagObject((BorderTag) tag), map.get(tag));
                }

            }

            if (!newMap.isEmpty()) {
                map = newMap;
            }

        }
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

}
