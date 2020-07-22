package com.bmskinner.nuclear_morphology.io.xml;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.components.rules.Rule;
import com.bmskinner.nuclear_morphology.components.rules.RuleSet;

/**
 * Test XML documents are created correctly describing 
 * Rulesets
 * @author ben
 * @since 1.18.3
 *
 */
public class RulesetXMLCreatorTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testRulesetMouseSpermRulesetsCreated() throws IOException {
		Map<String, RuleSet> list = new HashMap<>();
		list.put("RP", RuleSet.mouseSpermRPRuleSet());
		list.put("OP", RuleSet.mouseSpermOPRuleSet());
		list.put("TV", RuleSet.mouseSpermTVRuleSet());
		list.put("BV", RuleSet.mouseSpermBVRuleSet());
		
		for(Entry<String, RuleSet> e : list.entrySet()) {
			testRuleSetCreated(e.getValue(), e.getKey());
		}
	}
	
	@Test
	public void testRulesetPigSpermRulesetsCreated() throws IOException {
		Map<String, RuleSet> list = new HashMap<>();
		list.put("RP", RuleSet.pigSpermRPRuleSet());
		list.put("RP", RuleSet.pigSpermRPBackupRuleSet());
		list.put("OP", RuleSet.pigSpermOPRuleSet());
		
		for(Entry<String, RuleSet> e : list.entrySet()) {
			testRuleSetCreated(e.getValue(), e.getKey());
		}
	}
	
	/**
	 * Given a ruleset, create an XML representation
	 * and test if it matches
	 * @param rs the ruleset to test
	 * @throws IOException
	 */
	private void testRuleSetCreated(RuleSet rs, String name) throws IOException {
		RulesetXMLCreator rxc = new RulesetXMLCreator(rs, name);		
		Document d = rxc.create();
		printXML(d);
		testRuleSetMatches(rs, d,name);
	}
	
	/**
	 * Print the given document to console using a standard formatter
	 * @param d
	 * @throws IOException
	 */
	private void printXML(Document d) throws IOException {
		XMLOutputter xmlOutput = new XMLOutputter();
		xmlOutput.setFormat(Format.getPrettyFormat());
		xmlOutput.output(d, System.out);
	}
	
	/**
	 * Given a ruleset and an XML document, test if
	 * the document contains the elements in the
	 * ruleset
	 * @param rs
	 * @param d
	 */
	private void testRuleSetMatches(RuleSet rs, Document d, String name) {
				
		assertEquals(name, 
				d.getRootElement().getChild(XMLCreator.NAME_KEY).getValue());
		

		assertEquals(rs.getType().name(), 
				d.getRootElement().getChild(XMLCreator.PROFILE_TYPE_KEY).getValue());
				
		List<Element> elements = d.getRootElement().getChildren(XMLCreator.RULE_KEY);
		for(int i=0; i<elements.size(); i++) {
			Element rElement = elements.get(i);
			Rule r = rs.getRules().get(i);
			assertEquals(r.getType().toString(),
					rElement.getChild(XMLCreator.TYPE_KEY).getValue());
			for(int j=0; j<r.valueCount(); j++) {
				assertEquals(Double.valueOf(r.getValue(j)),
						Double.valueOf(rElement.getChild(XMLCreator.VALUE_KEY+j).getValue().toString()));
			}
		}
	}

}
