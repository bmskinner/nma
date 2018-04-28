package com.bmskinner.nuclear_morphology.components;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests for the static methods of analysis datasets
 * @author ben
 *
 */
public class IAnalysisDatasetTest {
	
	
	private List<IAnalysisDataset> l = new ArrayList<>();
	
	@Before
	public void setUp() {
		l = new ArrayList<>();
	}
	
	private IAnalysisDataset createMock(boolean hasConsensus, NucleusType type, File path) {
		IAnalysisDataset d1 = mock(IAnalysisDataset.class);
		ICellCollection c1 = mock(ICellCollection.class);
		when(d1.getCollection()).thenReturn(c1);
		when(d1.getSavePath()).thenReturn(path);
		when(c1.hasConsensus()).thenReturn(hasConsensus);
		when(c1.getNucleusType()).thenReturn(type);
		return d1;
	}

	@Test
	public void testHaveConsensusNucleiReturnsCorrectly() {	
		IAnalysisDataset d1 = createMock(true, NucleusType.RODENT_SPERM, null);
		l.add(d1);
		assertTrue(IAnalysisDataset.haveConsensusNuclei(l));
		
		IAnalysisDataset d2 = createMock(false, NucleusType.RODENT_SPERM, null);
		l.add(d2);
		
		assertFalse(IAnalysisDataset.haveConsensusNuclei(l));
	}
	
	@Test
	public void testAreSameNucleusType() {
		IAnalysisDataset d1 = createMock(true, NucleusType.RODENT_SPERM, null);
		IAnalysisDataset d2 = createMock(false, NucleusType.RODENT_SPERM, null);
		l.add(d1);
		l.add(d2);
		
		assertTrue(IAnalysisDataset.areSameNucleusType(l));
		
		IAnalysisDataset d3 = createMock(false, NucleusType.PIG_SPERM, null);
		l.add(d3);
		
		assertFalse(IAnalysisDataset.areSameNucleusType(l));
	}

	@Test
	public void testMergedSourceOptionsAreSame() {
		fail("Not yet implemented");
	}
	
	@Test
	public void testGetBroadestNucleusTypeWhenSame() {	
		IAnalysisDataset d1 = createMock(true, NucleusType.RODENT_SPERM, null);
		IAnalysisDataset d2 = createMock(false, NucleusType.RODENT_SPERM, null);
		
		l.add(d1);
		l.add(d2);
		
		assertEquals(NucleusType.RODENT_SPERM, IAnalysisDataset.getBroadestNucleusType(l));
	}
	
	@Test
	public void testGetBroadestNucleusTypeWhenDifferent() {		
		IAnalysisDataset d1 = createMock(true, NucleusType.RODENT_SPERM, null);
		IAnalysisDataset d2 = createMock(false, NucleusType.PIG_SPERM, null);
		
		l.add(d1);
		l.add(d2);
		
		assertEquals(NucleusType.ROUND, IAnalysisDataset.getBroadestNucleusType(l));
	}
	
	@Test
	public void testCommonPathOfFilesInSameSubfolder() {
		
		File path1 = new File("/path/to/folder/one/one.nmd");
		File path2 = new File("/path/to/folder/two/two.nmd");
		
		IAnalysisDataset d1 = createMock(true, NucleusType.RODENT_SPERM, path1);
		IAnalysisDataset d2 = createMock(false, NucleusType.RODENT_SPERM, path2);
		
		l.add(d1);
		l.add(d2);
		
		File common = new File("/path/to/folder/");
		
		assertEquals(common, IAnalysisDataset.commonPathOfFiles(l));
	}
	
	@Test
	public void testCommonPathOfFilesInDifferentSubfolders() {
		
		File path1 = new File("/path/to/folder/one/one.nmd");
		File path2 = new File("/different/path/to/folder/two/two.nmd");
		
		IAnalysisDataset d1 = createMock(true, NucleusType.RODENT_SPERM, path1);
		IAnalysisDataset d2 = createMock(false, NucleusType.RODENT_SPERM, path2);
		
		l.add(d1);
		l.add(d2);
		
		File common = new File("/");
		
		assertEquals(common, IAnalysisDataset.commonPathOfFiles(l));
	}
	
	@Test
	public void testCommonPathOfMultipleFilesInDifferentSubfolders() {
		
		File path1 = new File("/path/to/folder/one/one.nmd");
		File path2 = new File("/path/with/path/to/folder/two/two.nmd");
		File path3 = new File("/path/going/to/folder/three/three.nmd");
		
		IAnalysisDataset d1 = createMock(true, NucleusType.RODENT_SPERM, path1);
		IAnalysisDataset d2 = createMock(false, NucleusType.RODENT_SPERM, path2);
		IAnalysisDataset d3 = createMock(false, NucleusType.RODENT_SPERM, path3);
		
		l.add(d1);
		l.add(d2);
		l.add(d3);
		
		File common = new File("/path/");
		
		assertEquals(common, IAnalysisDataset.commonPathOfFiles(l));
	}
	
	@Test
	public void testCommonPathOfFilesWindowsStyleRoot() {
		
		File path1 = new File("C:/path/to/folder/one/one.nmd");
		File path2 = new File("D:/different/path/to/folder/two/two.nmd");
		
		IAnalysisDataset d1 = createMock(true, NucleusType.RODENT_SPERM, path1);
		IAnalysisDataset d2 = createMock(false, NucleusType.RODENT_SPERM, path2);
		
		l.add(d1);
		l.add(d2);
		
		File common = new File("");
		
		assertEquals(common, IAnalysisDataset.commonPathOfFiles(l));
	}
	
}
