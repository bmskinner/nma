package com.bmskinner.nuclear_morphology.components.signals;

import static org.junit.Assert.*;

import java.util.UUID;

import org.jdom2.Element;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.components.cells.ComponentCreationException;

public class ShellKeyTest {

	@Test
	public void testXmlSerialisesWithSignalGroup() throws ComponentCreationException {
		UUID cellId = UUID.randomUUID();
		UUID compId = UUID.randomUUID();
		UUID sigId  = UUID.randomUUID();
		
		ShellKey k = new ShellKey(cellId, compId, sigId);
		
		Element e = k.toXmlElement();
		
		ShellKey r = new ShellKey(e);
		
		assertEquals("Shell keys should match", k, r);
		assertEquals(k.hashCode(), r.hashCode());
	}
	
	@Test
	public void testXmlSerialisesWithoutSignalGroup() throws ComponentCreationException {
		UUID cellId = UUID.randomUUID();
		UUID compId = UUID.randomUUID();
		
		ShellKey k = new ShellKey(cellId, compId);
		
		Element e = k.toXmlElement();
		
		ShellKey r = new ShellKey(e);
		
		assertEquals("Shell keys should match", k, r);
		assertEquals(k.hashCode(), r.hashCode());
	}

}
