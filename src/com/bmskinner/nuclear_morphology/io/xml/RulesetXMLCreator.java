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
	
	private String name;

	/**
	 * Create with a ruleset and the name to give the ruleset in the XML document.
	 * This will usually be a tag name, but can be custom
	 * @param template the ruleset
	 * @param name the name to give the ruleset
	 */
	public RulesetXMLCreator(@NonNull final RuleSet template, @NonNull final String name) {
		super(template);
		this.name = name;
	}

	@Override
	public Document create() {
		Element rootElement = new Element(RULESET_KEY);
		rootElement.addContent(createElement(NAME_KEY, name));
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
