package com.bmskinner.nuclear_morphology.io.xml;

import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.rules.Rule;
import com.bmskinner.nuclear_morphology.components.rules.Rule.RuleType;
import com.bmskinner.nuclear_morphology.components.rules.RuleSet;

/**
 * Reader for XML documents containing a ruleset
 * @author ben
 *
 */
public class RuleSetXMLReader extends XMLReader<RuleSet>{
	
	private static final Logger LOGGER = Logger.getLogger(RuleSetXMLReader.class.getName());

	public RuleSetXMLReader(@NonNull Element e) {
		super(e);
	}

	@Override
	public RuleSet read() throws XMLReadingException {

		ProfileType t = ProfileType.valueOf(rootElement.getChild(XMLCreator.PROFILE_TYPE_KEY).getValue());
		
		RuleSet rs = new RuleSet(t);
				
		List<Element> elements = rootElement.getChildren(XMLCreator.RULE_KEY);
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
		LOGGER.finest("Created ruleset");
		return rs;
	}
	

}
