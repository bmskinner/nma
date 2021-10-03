package com.bmskinner.nuclear_morphology.io.xml;

import static org.junit.Assert.assertEquals;

import java.io.PrintWriter;

import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.rules.Rule;
import com.bmskinner.nuclear_morphology.components.rules.RuleSet;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;

public class XmlSerializationTestClass {

	@Test
	public void testRuleSetCollection() throws Exception {
		RuleSetCollection rsc = RuleSetCollection.mouseSpermRuleSetCollection();		
		Element e = rsc.toXmlElement();
		
		XMLOutputter xmlOutput = new XMLOutputter();
		xmlOutput.setFormat(Format.getPrettyFormat());
		xmlOutput.output(e, new PrintWriter( System.out ));
		
		RuleSetCollection test = new RuleSetCollection(e);
		assertEquals(rsc, test);
	}
	
	@Test
	public void testRuleSet() throws Exception {
		RuleSetCollection rsc = RuleSetCollection.mouseSpermRuleSetCollection();	
		RuleSet rs = rsc.getRuleSets(Landmark.TOP_VERTICAL).get(0);
		Element e = rs.toXmlElement();
		
		XMLOutputter xmlOutput = new XMLOutputter();
		xmlOutput.setFormat(Format.getPrettyFormat());
		xmlOutput.output(e, new PrintWriter( System.out ));
		
		RuleSet test = new RuleSet(e);
		assertEquals(rs, test);
	    
	}
	
	@Test
	public void testRule() throws Exception {
		RuleSetCollection rsc = RuleSetCollection.mouseSpermRuleSetCollection();	
		RuleSet rs = rsc.getRuleSets(Landmark.TOP_VERTICAL).get(0);
		Rule r = rs.getRules().get(0);
		
		Element e = r.toXmlElement();
		
		XMLOutputter xmlOutput = new XMLOutputter();
		xmlOutput.setFormat(Format.getPrettyFormat());
		xmlOutput.output(e, new PrintWriter( System.out ));
		
		Rule test = new Rule(e);
		assertEquals(r, test);
	    
	}

}
