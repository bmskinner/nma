package com.bmskinner.nuclear_morphology.io.xml;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Document;
import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
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
		for(Landmark t : template.getTags()) {
			Element tagElement = new Element(TAG_KEY+tagCounter);
			tagElement.addContent(createElement(NAME_KEY, t.getName()));
			tagElement.addContent(createElement(TYPE_KEY, t.type()));
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
		
		
		
		try {
			
			// Add values known not to be nullable
			rootElement.addContent(new Element("Name").addContent(template.getName()));
			rootElement.addContent(new Element("Type").addContent(template.getApplicationType().toString()));
			
			// Add the orientation landmark names
			addOrientationElements(RuleSetCollection.class.getMethod("getTopLandmark"), rootElement);
			addOrientationElements(RuleSetCollection.class.getMethod("getBottomLandmark"), rootElement);
			addOrientationElements(RuleSetCollection.class.getMethod("getLeftLandmark"), rootElement);
			addOrientationElements(RuleSetCollection.class.getMethod("getRightLandmark"), rootElement);	
			
			addOrientationElements(RuleSetCollection.class.getMethod("getSecondaryX"), rootElement);
			addOrientationElements(RuleSetCollection.class.getMethod("getSecondaryY"), rootElement);
			
			// Add priority axis separately since it's a string not a landmark
			if(template.getPriorityAxis().isPresent()) {
				rootElement.addContent(new Element("PriorityAxis")
						.addContent(template.getPriorityAxis().get().toString()));
			}
			

		} catch (NoSuchMethodException | SecurityException e) {
			LOGGER.log(Level.SEVERE, "Unable to reflect ruleset collection", e);
		}		
		return new Document(rootElement);
	}
	
	/**
	 * Simplify creation of landmark elements by reflecting on the method
	 * names.
	 * @param f the method to create an element for
	 * @param rootElement the element to add any created element to
	 */
	private void addOrientationElements(Method f, Element rootElement) {
		try {
			String lmName = f.getName().replaceAll("get","").replaceAll("Landmark", "");			
			Optional<Landmark> lm = (Optional<Landmark>)f.invoke(template);
			
			if(lm.isPresent()) {
				Element element = new Element(lmName);
				element.addContent(lm.get().toString());
				rootElement.addContent(element);
			}
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			LOGGER.log(Level.SEVERE, "Unable to reflect ruleset collection", e);
		}
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
