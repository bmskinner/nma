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
import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.LandmarkType;
import com.bmskinner.nuclear_morphology.io.XmlSerializable;

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
public class RuleSetCollection implements XmlSerializable {
	
	private static final String XML_PRIORITY_AXIS = "PriorityAxis";

	private static final String XML_RULE_APPLICATION_TYPE = "RuleApplicationType";

	private static final String XML_RULE_SET_COLLECTION = "RuleSetCollection";

	private static final String XML_TYPE = "Type";

	private static final String XML_TAG = "Tag";

	private static final String XML_NAME = "Name";

	private static final Logger LOGGER = Logger.getLogger(RuleSetCollection.class.getName());
    
    private final String name;

    private final Map<Landmark, List<RuleSet>> map = new HashMap<>();
    
    /** Store the landmarks to be used for orientation */
    private Map<OrientationMark, Landmark> orientationMarks = new HashMap<>();
            
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
    	
    	if(left!=null)
    		orientationMarks.put(OrientationMark.LEFT, left);

    	if(right!=null)
    		orientationMarks.put(OrientationMark.RIGHT, right);

    	if(top!=null)
    		orientationMarks.put(OrientationMark.TOP, top);

    	if(bottom!=null)
    		orientationMarks.put(OrientationMark.BOTTOM, bottom);

    	if(seondaryX!=null)
    		orientationMarks.put(OrientationMark.X, seondaryX);

    	if(seondaryY!=null)
    		orientationMarks.put(OrientationMark.Y, seondaryY);

    	this.priorityAxis = priorityAxis;
    	this.ruleApplicationType = type;
    }
    
    /**
     * Construct from an XML element. Use for 
     * unmarshalling. The element should conform
     * to the specification in {@link XmlSerializable}.
     * @param e the XML element containing the data.
     */
    public RuleSetCollection(Element e) {
    	name = e.getChildText(XML_NAME);
    	
    	// Add the rulesets
    	for(Element t : e.getChildren(XML_TAG)) {
    		
    		String lmName = t.getChildText(XML_NAME);
    		LandmarkType lmType = LandmarkType.valueOf(t.getChildText(XML_TYPE));
    		Landmark l = Landmark.of(lmName, lmType);
    		
    		List<RuleSet> rules = new ArrayList<>();
    		for(Element r : t.getChildren("Rule")) {
    			rules.add(new RuleSet(r));
    		}
    		map.put(l, rules);
    	}
    	
    	// Add other rules
    	priorityAxis = PriorityAxis.valueOf(e.getChildText(XML_PRIORITY_AXIS));
    	ruleApplicationType = RuleApplicationType.valueOf(e.getChildText(XML_RULE_APPLICATION_TYPE));
    	    
    	// Add the orientation landmarks
    	for(OrientationMark s : OrientationMark.values()) {
    		if(e.getChild(s.name())!=null) {
        		orientationMarks.put(s, map.keySet().stream()
        				.filter(l->l.getName().equals(e.getChildText(s.name())))
        				.findFirst().orElse(null));
        	}
    	}
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
    	return map.containsKey(OrientationMark.LEFT) || map.containsKey(OrientationMark.RIGHT);
    }
    
    /**
     * Test if the rules specify a preferred landmark
     * above or below the CoM
     * @return
     */
    public boolean isAsymmetricY() {
    	return map.containsKey(OrientationMark.TOP) || map.containsKey(OrientationMark.BOTTOM);
    }
    
    /**
     * Test if there is asymmetry in either axis
     * @return
     */
    public boolean isAsymmetric() {
    	return isAsymmetricX() || isAsymmetricY();
    }
    
    public Optional<Landmark> getLandmark(OrientationMark s) {
		return Optional.ofNullable(orientationMarks.get(s));
	}

	public Optional<PriorityAxis> getPriorityAxis() {
		return Optional.ofNullable(priorityAxis);
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
    
    public Element toXmlElement() {
    	
    	Element rootElement = new Element(XML_RULE_SET_COLLECTION);
    	
    	rootElement.addContent(new Element(XML_NAME).addContent(getName()));
		
    	// Add the landmark rule definitions 
		for(Landmark t : getTags()) {
			Element tagElement = new Element(XML_TAG);
			tagElement.addContent(new Element(XML_NAME).setText(t.getName()));
			tagElement.addContent(new Element(XML_TYPE).setText(t.type().toString()));
			
			for(RuleSet rs : getRuleSets(t)) {
				tagElement.addContent(rs.toXmlElement());
			}
			rootElement.addContent(tagElement);
		}
		
		// Add rule application type
		rootElement.addContent(new Element(XML_RULE_APPLICATION_TYPE).addContent(getApplicationType().toString()));
		
		// Add any orientation landmarks
    	for(Entry<OrientationMark, Landmark> entry : orientationMarks.entrySet()) {
    		rootElement.addContent(new Element(entry.getKey().name()).addContent(entry.getValue().toString()));
    	}
		
		if(priorityAxis!=null)
			rootElement.addContent(new Element(XML_PRIORITY_AXIS).addContent(priorityAxis.toString()));
			
		return rootElement;
    }

	

    @Override
	public int hashCode() {
		return Objects.hash(map, name, orientationMarks, priorityAxis, ruleApplicationType);
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
		return Objects.equals(map, other.map) && Objects.equals(name, other.name)
				&& Objects.equals(orientationMarks, other.orientationMarks) && priorityAxis == other.priorityAxis
				&& ruleApplicationType == other.ruleApplicationType;
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
        RuleSetCollection r = new RuleSetCollection("Pig sperm", Landmark.LEFT_HORIZONTAL, 
        		Landmark.RIGHT_HORIZONTAL, null, null,
        		null, Landmark.REFERENCE_POINT, PriorityAxis.X, RuleApplicationType.VIA_MEDIAN);

        r.addRuleSet(Landmark.REFERENCE_POINT, RuleSet.pigSpermRPRuleSet());
        r.addRuleSet(Landmark.ORIENTATION_POINT, RuleSet.pigSpermOPRuleSet());
        r.addRuleSet(Landmark.LEFT_HORIZONTAL, RuleSet.pigSpermLHRuleSet());
        r.addRuleSet(Landmark.RIGHT_HORIZONTAL, RuleSet.pigSpermRHRuleSet());
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
}
