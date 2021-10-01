package com.bmskinner.nuclear_morphology.io.xml;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.components.rules.PriorityAxis;
import com.bmskinner.nuclear_morphology.components.rules.Rule;
import com.bmskinner.nuclear_morphology.components.rules.Rule.RuleType;
import com.bmskinner.nuclear_morphology.components.rules.RuleApplicationType;
import com.bmskinner.nuclear_morphology.components.rules.RuleSet;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;

public class RuleSetCollectionXMLReader extends XMLReader<RuleSetCollection>{
	
	private static final Logger LOGGER = Logger.getLogger(RuleSetCollectionXMLReader.class.getName());

	public RuleSetCollectionXMLReader(@NonNull Element e) {
		super(e);
	}
	
	private String getLandmarkName(String key) {
		if(rootElement.getChild(key)==null)
			return null;
		return rootElement.getChild(key).getValue();
	}

	@Override
	public RuleSetCollection read() throws XMLReadingException {

		Map<Landmark, RuleSet> map = new HashMap<>(); 
		
		// Get the orientation landmark names
		String ruleName   = getLandmarkName("Name");
		String ruleType   = getLandmarkName("Type");
		String topTagName = getLandmarkName("Top");
		String btmTagName = getLandmarkName("Bottom");
		String lftTagName = getLandmarkName("Left");
		String rgtTagName = getLandmarkName("Right");
		String scndXTagName = getLandmarkName("SecondaryX");
		String scndYTagName = getLandmarkName("SecondaryY");
		String priorityAxis = getLandmarkName("PriorityAxis");
		
				
		// Init the orientation landmarks
		Landmark topTag = null;
		Landmark btmTag = null;
		Landmark lftTag = null;
		Landmark rgtTag = null;
		Landmark scndX  = null;
		Landmark scndY  = null;
		
		// All tags are under Tagn names
		long tagCount = rootElement.getChildren()
				.stream()
				.filter(e->e.getName().startsWith(XMLCreator.TAG_KEY))
				.count();
		
		for(int tag=0; tag<tagCount; tag++) {
			
			Element tagElement = rootElement.getChild(XMLCreator.TAG_KEY+tag);
			Landmark tagObject = readTag(tagElement);
			LOGGER.finest("Found tag "+tagObject);
			
			
			// Get the rules for the landmark
			int rulesetCount = tagElement.getChildren().size()-2;
			for(int i=0; i<rulesetCount; i++) {
				LOGGER.finest("Creating ruleset "+i);
				Element ruleSetElement = tagElement.getChild(XMLCreator.RULESET_KEY+"_"+i);
				
				RuleSet rs = createRuleSet(ruleSetElement);
				map.put(tagObject, rs);
			}
			
			// Check the orientation rules
			if(tagObject.getName().equals(topTagName))
				topTag = tagObject;
			if(tagObject.getName().equals(btmTagName))
				btmTag = tagObject;
			if(tagObject.getName().equals(lftTagName))
				lftTag = tagObject;
			if(tagObject.getName().equals(rgtTagName))
				rgtTag = tagObject;
			if(tagObject.getName().equals(scndYTagName))
				scndY = tagObject;
			if(tagObject.getName().equals(scndXTagName))
				scndX = tagObject;
			
		}
		
		
		// Now build the collection
		RuleSetCollection rsc = new RuleSetCollection(ruleName, lftTag, rgtTag, topTag, btmTag,
				scndX, scndY, PriorityAxis.valueOf(priorityAxis), RuleApplicationType.valueOf(ruleType));
		for(Entry<Landmark, RuleSet> e : map.entrySet()) {
			rsc.addRuleSet(e.getKey(), e.getValue());
		}

		LOGGER.finest("Created ruleset collection");
		return rsc;
	}
	
	private RuleSet createRuleSet(Element tagElement) {
		
		ProfileType t = ProfileType.valueOf(tagElement.getChild(XMLCreator.PROFILE_TYPE_KEY).getValue());
		
		RuleSet rs = new RuleSet(t);
				
		List<Element> elements = tagElement.getChildren(XMLCreator.RULE_KEY);
		for(int i=0; i<elements.size(); i++) {
			Element rElement = elements.get(i);
			
			RuleType rt = RuleType.valueOf(rElement.getChild(XMLCreator.TYPE_KEY).getValue());
			double v0 = Double.valueOf(rElement.getChild(XMLCreator.VALUE_KEY+0).getValue().toString());
			Rule r = new Rule(rt, v0);
			LOGGER.finest("Created rule "+rt+" with value "+v0);
			
			for(int j=1; j<rElement.getChildren().size()-1; j++) {
				double v = Double.valueOf(rElement.getChild(XMLCreator.VALUE_KEY+j).getValue().toString());
				LOGGER.finest("Adding value "+j+": "+v);
				r.addValue(v);

			}
			rs.addRule(r);
		}
		
		return rs;
	}
}
	

