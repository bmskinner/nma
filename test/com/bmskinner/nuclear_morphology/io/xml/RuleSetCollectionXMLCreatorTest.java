package com.bmskinner.nuclear_morphology.io.xml;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.rules.Rule;
import com.bmskinner.nuclear_morphology.components.rules.RuleSet;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;

/**
 * Tests for creating XML from ruleset collections
 * @author ben
 *
 */
public class RuleSetCollectionXMLCreatorTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testMouseSpermRulesetsCreated() throws IOException {
		RuleSetCollection rsc = RuleSetCollection.createDefaultRuleSet(NucleusType.RODENT_SPERM);
		testRuleSetCreated(rsc);
	}
	
	@Test
	public void testPigSpermRulesetsCreated() throws IOException {
		RuleSetCollection rsc = RuleSetCollection.createDefaultRuleSet(NucleusType.PIG_SPERM);
		testRuleSetCreated(rsc);
	}
	
	@Test
	public void testRoundRulesetsCreated() throws IOException {
		RuleSetCollection rsc = RuleSetCollection.createDefaultRuleSet(NucleusType.ROUND);
		testRuleSetCreated(rsc);
	}
	
	
	/**
	 * Given a ruleset, create an XML representation
	 * and test if it matches
	 * @param rs the ruleset to test
	 * @throws IOException
	 */
	private void testRuleSetCreated(RuleSetCollection rsc) throws IOException {
		RuleSetCollectionXMLCreator rxc = new RuleSetCollectionXMLCreator(rsc);		
		Document d = rxc.create();
		testRuleSetCollectionMatches(rsc, d);
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
	
	private void testRuleSetCollectionMatches(RuleSetCollection rsc, Document d) {
		
		List<Tag> tags = new ArrayList<>(rsc.getTags());
		for(int i=0; i<rsc.getTags().size(); i++) {
			Tag t = tags.get(i);
			Element rElements = d.getRootElement().getChild("Tag"+i);
			List<RuleSet> rList = rsc.getRuleSets(t);
			
			assertEquals(t.getName(), 
					rElements.getChild(XMLCreator.NAME_KEY).getValue());
			
			assertEquals(t.getTag().name(), 
					rElements.getChild(XMLCreator.TYPE_KEY).getValue());
			
			for(int j=0; j<rList.size(); j++) {
				testRuleSetMatches(rList.get(j), 
						rElements.getChild(XMLCreator.RULESET_KEY+"_"+j));
			}
		}		
	}
	
	
	/**
	 * Given a ruleset and an XML document, test if
	 * the document contains the elements in the
	 * ruleset
	 * @param rs
	 * @param d
	 */
	private void testRuleSetMatches(RuleSet rs, Element e) {
						

		assertEquals(rs.getType().name(), 
				e.getChild(XMLCreator.PROFILE_TYPE_KEY).getValue());
				
		List<Element> elements = e.getChildren(XMLCreator.RULE_KEY);
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
