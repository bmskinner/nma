package com.bmskinner.nuclear_morphology.io.xml;

import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.rules.Rule;
import com.bmskinner.nuclear_morphology.components.rules.Rule.RuleType;
import com.bmskinner.nuclear_morphology.components.rules.RuleSet;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;

public class RuleSetCollectionXMLReader extends XMLReader<RuleSetCollection>{
	
	private static final Logger LOGGER = Logger.getLogger(RuleSetCollectionXMLReader.class.getName());

	public RuleSetCollectionXMLReader(@NonNull Element e) {
		super(e);
	}

	@Override
	public RuleSetCollection read() throws XMLReadingException {

		RuleSetCollection rsc = new RuleSetCollection();

		
		int tagCount = rootElement.getChildren().size();
		
		for(int tag=0; tag<tagCount; tag++) {
			
			Element tagElement = rootElement.getChild(XMLCreator.TAG_KEY+tag);
			
			String tagName = tagElement.getChildText(XMLCreator.NAME_KEY);
			String tagType = tagElement.getChildText(XMLCreator.TYPE_KEY);
			Tag tagObject = Tag.of(tagType);
			LOGGER.finest("Found tag "+tagName+" ("+tagType+") as "+tagObject);
			int rulesetCount = tagElement.getChildren().size()-2;
			for(int i=0; i<rulesetCount; i++) {
				LOGGER.finest("Creating ruleset "+i);
				Element ruleSetElement = tagElement.getChild(XMLCreator.RULESET_KEY+"_"+i);
				
				RuleSet rs = createRuleSet(ruleSetElement);
				rsc.addRuleSet(tagObject, rs);
			}			
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
	

