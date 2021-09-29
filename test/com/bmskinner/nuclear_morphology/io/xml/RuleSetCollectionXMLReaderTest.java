package com.bmskinner.nuclear_morphology.io.xml;

import static org.junit.Assert.assertEquals;

import org.jdom2.Document;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;
import com.bmskinner.nuclear_morphology.io.xml.XMLReader.XMLReadingException;

/**
 * Test reading of ruleset collections from XML
 * @author ben
 * @since 1.18.3
 *
 */
public class RuleSetCollectionXMLReaderTest {
	
	@Test
	public void testMouseSpermRulesetRead() throws XMLReadingException {		
		testRulesetCollectionRead(RuleSetCollection.mouseSpermRuleSetCollection());
	}
	
	@Test
	public void testPigSpermRulesetRead() throws XMLReadingException {		
		testRulesetCollectionRead(RuleSetCollection.pigSpermRuleSetCollection());
	}
	
	@Test
	public void testRoundSpermRulesetRead() throws XMLReadingException {		
		testRulesetCollectionRead(RuleSetCollection.roundRuleSetCollection());
	}
	
	/**
	 * Given a ruleset collection, create an XML represntation, and try to 
	 * marshal the XML back to a Java object. The two should match.
	 * @param rs
	 * @throws XMLReadingException
	 */
	private void testRulesetCollectionRead(RuleSetCollection rs) throws XMLReadingException {
		// Make an XML representation of the collection
		Document d = new RuleSetCollectionXMLCreator(rs).create();
		
		// Convert the XML back to a collection
		RuleSetCollection test = new RuleSetCollectionXMLReader(d.getRootElement()).read();
		
		assertEquals("Ruleset collections should match", rs, test);
	}
}
