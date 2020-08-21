package com.bmskinner.nuclear_morphology.components.generic;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests for tag creation
 * @author ben
 * @since 1.18.3
 *
 */
public class TagTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testOfCreatesCorrectCustomTagType() {
		String customTag = "Custom tag";
		Tag t = Tag.of(customTag);
		assertEquals(BorderTag.CUSTOM, t.getTag());
	}
	
	@Test
	public void testOfCreatesFixedTags() {
		
		for(BorderTag t : BorderTag.values()) {
			Tag tag = Tag.of(t.name());
			assertEquals(t, tag.getTag());
		}
		
		for(BorderTag t : BorderTag.values()) {
			Tag tag = Tag.of(t.toString());
			assertEquals(t, tag.getTag());
		}
	}

}
