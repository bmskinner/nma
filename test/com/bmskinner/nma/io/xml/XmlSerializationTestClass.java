package com.bmskinner.nma.io.xml;

import static org.junit.Assert.assertEquals;

import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Test;

import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.components.rules.Rule;
import com.bmskinner.nma.components.rules.RuleSet;
import com.bmskinner.nma.components.rules.RuleSetCollection;

public class XmlSerializationTestClass {

	@Test
	public void testRuleSetCollection() throws Exception {
		RuleSetCollection rsc = RuleSetCollection.mouseSpermRuleSetCollection();
		Element e = rsc.toXmlElement();

		XMLOutputter xmlOutput = new XMLOutputter();
		xmlOutput.setFormat(Format.getPrettyFormat());

		RuleSetCollection test = new RuleSetCollection(e);
		assertEquals(rsc, test);
	}

	@Test
	public void testRuleSet() throws Exception {
		RuleSetCollection rsc = RuleSetCollection.mouseSpermRuleSetCollection();
		RuleSet rs = rsc.getRuleSets(OrientationMark.TOP).get(0);
		Element e = rs.toXmlElement();

		XMLOutputter xmlOutput = new XMLOutputter();
		xmlOutput.setFormat(Format.getPrettyFormat());

		RuleSet test = new RuleSet(e);
		assertEquals(rs, test);

	}

	@Test
	public void testRule() throws Exception {
		RuleSetCollection rsc = RuleSetCollection.mouseSpermRuleSetCollection();
		RuleSet rs = rsc.getRuleSets(OrientationMark.TOP).get(0);
		Rule r = rs.getRules().get(0);

		Element e = r.toXmlElement();

		XMLOutputter xmlOutput = new XMLOutputter();
		xmlOutput.setFormat(Format.getPrettyFormat());

		Rule test = new Rule(e);
		assertEquals(r, test);

	}

}
