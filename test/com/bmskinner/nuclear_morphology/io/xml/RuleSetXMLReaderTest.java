package com.bmskinner.nuclear_morphology.io.xml;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jdom2.Document;
import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.components.rules.RuleSet;
import com.bmskinner.nuclear_morphology.io.xml.XMLReader.XMLReadingException;
import com.bmskinner.nuclear_morphology.logging.ConsoleFormatter;
import com.bmskinner.nuclear_morphology.logging.ConsoleHandler;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/** 
 * Test cases for reading XML documents with
 * Rulesets
 * @author ben
 * @since 1.18.3
 *
 */
public class RuleSetXMLReaderTest {
	
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
		Map<String, RuleSet> list = new HashMap<>();
		list.put("RP", RuleSet.mouseSpermRPRuleSet());
		list.put("OP", RuleSet.mouseSpermOPRuleSet());
		list.put("TV", RuleSet.mouseSpermTVRuleSet());
		list.put("BV", RuleSet.mouseSpermBVRuleSet());
		
		for(Entry<String, RuleSet> e : list.entrySet()) {
			testRulesetRead(e.getValue());
		}
	}
	
	@Test
	public void testRulesetPigSpermRulesetsRead() throws XMLReadingException {
		Map<String, RuleSet> list = new HashMap<>();
		list.put("RP", RuleSet.pigSpermRPRuleSet());
		list.put("RP", RuleSet.pigSpermRPBackupRuleSet());
		list.put("OP", RuleSet.pigSpermOPRuleSet());
		
		for(Entry<String, RuleSet> e : list.entrySet()) {
			testRulesetRead(e.getValue());
		}
	}

	
	private void testRulesetRead(RuleSet rs) throws XMLReadingException {
		Document d = new RulesetXMLCreator(rs, "OP").create();
		
		RuleSet test = new RuleSetXMLReader(d.getRootElement()).read();
		
		assertEquals("Rulesets should match", rs, test);
	}
}
