package com.bmskinner.nuclear_morphology.io.xml;

import static org.junit.Assert.assertEquals;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jdom2.Document;
import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;
import com.bmskinner.nuclear_morphology.io.xml.XMLReader.XMLReadingException;
import com.bmskinner.nuclear_morphology.logging.ConsoleFormatter;
import com.bmskinner.nuclear_morphology.logging.ConsoleHandler;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Test reading of ruleset collections from XML
 * @author ben
 * @since 1.18.3
 *
 */
public class RuleSetCollectionXMLReaderTest {
	
	private static final Logger LOGGER = Logger.getLogger(Loggable.PROJECT_LOGGER);

	@Before
	public void setUp() throws Exception {
		LOGGER.setLevel(Level.FINEST);

		boolean hasHandler = false;
		for(Handler h : LOGGER.getHandlers()) {
			if(h instanceof ConsoleHandler)
				hasHandler = true;
		}
		if(!hasHandler)
			LOGGER.addHandler(new ConsoleHandler(new ConsoleFormatter()));
	}

	@Test
	public void testMouseSpermRulesetRead() throws XMLReadingException {		
		LOGGER.fine("Testing mouse");
		RuleSetCollection rsc = RuleSetCollection.createDefaultRuleSet(NucleusType.RODENT_SPERM);
		testRulesetCollectionRead(rsc);
	}
	
	@Test
	public void testPigSpermRulesetRead() throws XMLReadingException {		
		LOGGER.fine("Testing mouse");
		RuleSetCollection rsc = RuleSetCollection.createDefaultRuleSet(NucleusType.PIG_SPERM);
		testRulesetCollectionRead(rsc);
	}
	
	@Test
	public void testRoundSpermRulesetRead() throws XMLReadingException {		
		LOGGER.fine("Testing mouse");
		RuleSetCollection rsc = RuleSetCollection.createDefaultRuleSet(NucleusType.ROUND);
		testRulesetCollectionRead(rsc);
	}
	
	private void testRulesetCollectionRead(RuleSetCollection rs) throws XMLReadingException {
		Document d = new RuleSetCollectionXMLCreator(rs).create();
		RuleSetCollection test = new RuleSetCollectionXMLReader(d.getRootElement()).read();
		assertEquals("Ruleset collections should match", rs, test);
	}
}
