package com.bmskinner.nuclear_morphology.io.xml;

import static org.junit.Assert.assertEquals;

import org.jdom2.Document;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.components.nuclei.NucleusType;
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
		RuleSetCollection rsc = RuleSetCollection.createDefaultRuleSet(NucleusType.RODENT_SPERM);
		testRulesetCollectionRead(rsc);
	}
	
	@Test
	public void testPigSpermRulesetRead() throws XMLReadingException {		
		RuleSetCollection rsc = RuleSetCollection.createDefaultRuleSet(NucleusType.PIG_SPERM);
		testRulesetCollectionRead(rsc);
	}
	
	@Test
	public void testRoundSpermRulesetRead() throws XMLReadingException {		
		RuleSetCollection rsc = RuleSetCollection.createDefaultRuleSet(NucleusType.ROUND);
		testRulesetCollectionRead(rsc);
	}
	
	private void testRulesetCollectionRead(RuleSetCollection rs) throws XMLReadingException {
		Document d = new RuleSetCollectionXMLCreator(rs).create();
		RuleSetCollection test = new RuleSetCollectionXMLReader(d.getRootElement()).read();
		assertEquals("Ruleset collections should match", rs, test);
	}
}
