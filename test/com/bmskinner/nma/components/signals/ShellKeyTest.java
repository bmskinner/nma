package com.bmskinner.nma.components.signals;

import static org.junit.Assert.*;

import java.util.UUID;

import org.jdom2.Element;
import org.junit.Test;

import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.signals.ShellKey;

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
