package com.bmskinner.nuclear_morphology.io.xml;

import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Document;
import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.rules.Rule;
import com.bmskinner.nuclear_morphology.components.rules.RuleSet;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;

/**
 * Create XML representation of ruleset collections
 * @author ben
 * @since 1.18.3
 *
 */
public class RuleSetCollectionXMLCreator extends XMLCreator<RuleSetCollection> {
	
	public RuleSetCollectionXMLCreator(@NonNull final RuleSetCollection template) {
		super(template);
	}

	private static final Logger LOGGER = Logger.getLogger(RuleSetCollectionXMLCreator.class.getName());

	@Override
	public Document create() {
		Element rootElement = new Element(RULESET_COLLECTION_KEY);
		
		int tagCounter = 0;
		for(Tag t : template.getTags()) {
			Element tagElement = new Element(TAG_KEY+tagCounter);
			tagElement.addContent(createElement(NAME_KEY, t.getName()));
			tagElement.addContent(createElement(TYPE_KEY, t.getTag().name()));
			List<RuleSet> rList = template.getRuleSets(t);
			int i=0;
			for(RuleSet rs : rList) {
				Element e = createRuleSet(rs, RULESET_KEY+"_"+i);
				tagElement.addContent(e);
				i++;
			}
			tagCounter++;
			rootElement.addContent(tagElement);
		}
		return new Document(rootElement);
	}
	
	private Element createRuleSet(RuleSet rs, String name) {
		Element rsElement = new Element(name);
		rsElement.addContent(createType(rs.getType()));
		
		for(Rule r : rs.getRules()) {
			rsElement.addContent(createRule(r));
		}
		return rsElement;
	}
	
	private Element createType(ProfileType type) {
		return createElement(PROFILE_TYPE_KEY, type.name());
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
