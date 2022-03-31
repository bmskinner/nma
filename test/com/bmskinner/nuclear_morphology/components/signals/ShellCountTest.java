package com.bmskinner.nuclear_morphology.components.signals;

import static org.junit.Assert.*;

import java.util.UUID;

import org.jdom2.Element;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.components.cells.ComponentCreationException;

public class ShellCountTest {

	@Test
	public void testXmlSerialisesWithOneElement() throws ComponentCreationException {
		UUID cellId = UUID.randomUUID();
		UUID compId = UUID.randomUUID();
		UUID sigId  = UUID.randomUUID();
		ShellKey k = new ShellKey(cellId, compId, sigId);
		
		ShellCount c = new ShellCount();
		c.putValues(k, new long[]{ 1L, 2L, 3L, 4L, 5L });
		
		Element e = c.toXmlElement();
		
		
		ShellCount r = new ShellCount(e);
		
		assertEquals("Shell keys should match", c, r);
		assertEquals(c.hashCode(), r.hashCode());
	}
	
	@Test
	public void testXmlSerialisesWithMultipleElements() throws ComponentCreationException {
		
		ShellCount c = new ShellCount();
				
		c.putValues(new ShellKey(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()), new long[]{ 1L, 2L, 3L, 4L, 5L });
		c.putValues(new ShellKey(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()), new long[]{ 6L, 7L, 8L, 9L, 10L });
		c.putValues(new ShellKey(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()), new long[]{ 4L, 8L, 12L, 1L, 9L });
		
		Element e = c.toXmlElement();
		
		
		ShellCount r = new ShellCount(e);
		
		assertEquals("Shell counts should match", c, r);
		assertEquals("Shell count hashes should match", c.hashCode(), r.hashCode());
	}
}
