package com.bmskinner.nuclear_morphology.io.xml;

import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Document;
import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.rules.Rule;
import com.bmskinner.nuclear_morphology.components.rules.RuleSet;

/**
 * Create XML representation of Rulesets
 * @author ben
 * @since 1.18.3
 *
 */
public class RulesetXMLCreator extends XMLCreator<RuleSet> {
	
	private static final Logger LOGGER = Logger.getLogger(RulesetXMLCreator.class.getName());

	public RulesetXMLCreator(@NonNull final RuleSet template) {
		super(template);
	}

	@Override
	public Document create() {
		Element rootElement = new Element(RULESET_KEY);
		
		rootElement.addContent(createType(template.getType()));
		
		for(Rule r : template.getRules()) {
			rootElement.addContent(createRule(r));
		}
		
		return new Document(rootElement);
	}
	
	private Element createType(ProfileType type) {
		return createElement(PROFILE_TYPE_KEY, type.toString());
	}
	
	private Element createRule(Rule r) {
		Element e = new Element(RULE_KEY);
		
		e.addContent(createElement(TYPE_KEY, r.getType()));
		for(int i=0; i<r.valueCount(); i++) {
			e.addContent(createElement(VALUE_KEY+i, r.getValue(i)));
		}
		
		return e;
	}
}
