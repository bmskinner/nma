package com.bmskinner.nuclear_morphology.io.conversion;

import static org.junit.Assert.*;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.OptionalDataException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.ComponentTester;
import com.bmskinner.nuclear_morphology.FloatArrayTester;
import com.bmskinner.nuclear_morphology.TestResources;
import com.bmskinner.nuclear_morphology.components.DefaultCellCollection;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.generic.DefaultProfileAggregate;
import com.bmskinner.nuclear_morphology.components.generic.DefaultProfileCollection;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.IProfileAggregate;
import com.bmskinner.nuclear_morphology.components.generic.IProfileCollection;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.Version;
import com.bmskinner.nuclear_morphology.components.generic.Version.UnsupportedVersionException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderPoint;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.io.CountedInputStream;
import com.bmskinner.nuclear_morphology.io.PackageReplacementObjectInputStream;
import com.bmskinner.nuclear_morphology.io.SampleDatasetReader;
import com.bmskinner.nuclear_morphology.io.DatasetImportMethod.UnloadableDatasetException;
import com.bmskinner.nuclear_morphology.stats.Stats;

/**
 * This class tests the conversions between component
 * classes
 * @author bms41
 * @since 1.14.0
 *
 */
public class CellularComponentConversionTest extends ComponentTester {


	@Test
	public void testConvertNucleusToSegmentedCellularComponent() throws Exception {
		
		// Version 1.13.8 is the last version before the SegmentedCellualarComponent
		// was added. When the dataset is opened, profiles are converted on the
		// first getProfile() invokation.
		
		File f = new File(TestResources.DATASET_FOLDER+OldFormatConverterTest.DIR_1_13_8, MouseFormatConverterTest.MOUSE_BACKUP_FILE);
		IAnalysisDataset d = deserialiseDataset(f);
		
		if(d==null) {
			fail("Dataset could not be opened");
			return; // stop the null parameter checker complaining
		}
		
		assertEquals(Version.v_1_13_8, d.getVersion());
		
		// Check the profile aggregate
		
				
		Field colField = getInheritedField(d.getClass(), "cellCollection");		
		DefaultCellCollection col = (DefaultCellCollection) colField.get(d);
		
		Field cellsField = getInheritedField(col.getClass(), "cells");		
		
		for(ICell cell : col) {
			Nucleus n = cell.getNucleus();
			
			Field xField = getInheritedField(n.getClass(), "xpoints");	
			Field yField = getInheritedField(n.getClass(), "ypoints");		
			int[] xpoints = (int[]) xField.get(n);
			int[] ypoints = (int[]) yField.get(n);
			
			Field tagField = getInheritedField(n.getClass(), "borderTags");		
			Map<Tag, Integer> idx = (Map<Tag, Integer>) tagField.get(n);
			
			for(Tag t : idx.keySet())
				System.out.println(t.toString()+": "+idx.get(t));
			
			
			Field borderField = getInheritedField(n.getClass(), "borderList");
			List<IBorderPoint> borderList = (List<IBorderPoint>) borderField.get(n);
			System.out.println(n.getNameAndNumber()+" Border: "+borderList.size());
		}


		

		// Now check the individual nuclei. Their RPs should not change index 
		
		
		
		// Get the profiles following restoration of the profile aggregates
		IProfileCollection pc = d.getCollection().getProfileCollection();
		pc.createAndRestoreProfileAggregate(d.getCollection());
//
//		assertEquals(pro.length(), pc.length());
//		for(Tag t : idx.keySet())
//			assertEquals(t.toString(), idx.get(t).intValue(), pc.getIndex(t));
//		
//		IProfile profile = pc.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);
//		ISegmentedProfile median = pc.getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);
//		System.out.println("After conversions");
//		float[] firstProfile = (float[]) m.invoke(map.get(ProfileType.ANGLE), 0);
//		
//		System.out.println(Arrays.toString(firstProfile));
//
//		equals(profile.toFloatArray(), median.toFloatArray());
	}
	
	
	@Test
	public void testProfileCollectionsHaveSameIndexesOnDatasetDeserialisation() throws Exception {
		// Version 1.13.8 is the last version before the SegmentedCellualarComponent
		// was added. When the dataset is opened, profiles are converted on the
		// first getProfile() invokation.

		File f = new File(TestResources.DATASET_FOLDER+OldFormatConverterTest.DIR_1_13_8, MouseFormatConverterTest.MOUSE_BACKUP_FILE);
		IAnalysisDataset d = deserialiseDataset(f);

		if(d==null) {
			fail("Dataset could not be opened");
			return; // stop the null parameter checker complaining
		}
		assertEquals(Version.v_1_13_8, d.getVersion());

		// Check the profile aggregate

		Field colField = getInheritedField(d.getClass(), "cellCollection");		
		DefaultCellCollection col = (DefaultCellCollection) colField.get(d);

		Field pcField = getInheritedField(col.getClass(), "profileCollection");
		DefaultProfileCollection pro = (DefaultProfileCollection) pcField.get(col);

		Field mapField = getInheritedField(pro.getClass(), "map");
		Field idxField = getInheritedField(pro.getClass(), "indexes");

		Map<ProfileType, IProfileAggregate> map = (Map<ProfileType, IProfileAggregate>) mapField.get(pro);
		Map<Tag, Integer> idx = (Map<Tag, Integer>) idxField.get(pro);

		Field aggField = getInheritedField(DefaultProfileAggregate.class, "aggregate");
		Method m = DefaultProfileAggregate.class.getDeclaredMethod("getValuesForNucleus", int.class);
		m.setAccessible(true);

		// Get the profiles following restoration of the profile aggregates
		IProfileCollection pc = d.getCollection().getProfileCollection();
		pc.createAndRestoreProfileAggregate(d.getCollection());

		assertEquals(pro.length(), pc.length());
		for(Tag t : idx.keySet())
			assertEquals(t.toString(), idx.get(t).intValue(), pc.getIndex(t));

		IProfile profile = pc.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);
		ISegmentedProfile median = pc.getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);
//		System.out.println("After conversions");
		float[] firstProfile = (float[]) m.invoke(map.get(ProfileType.ANGLE), 0);

//		System.out.println(Arrays.toString(firstProfile));

		equals(profile.toFloatArray(), median.toFloatArray());
	}
	
	
	/**
	 * Deserialise the given file to a dataset. Mimics the dataset importer, but
	 * without validation or monitoring the input stream.
	 * @param inputFile the file to import
	 * @return
	 * @throws UnloadableDatasetException
	 */
	private IAnalysisDataset deserialiseDataset(@NonNull File inputFile) {
		IAnalysisDataset dataset = null;

		try(FileInputStream fis     = new FileInputStream(inputFile.getAbsolutePath());
			BufferedInputStream bis = new BufferedInputStream(fis);	
			ObjectInputStream ois   = new PackageReplacementObjectInputStream(bis);) {

			dataset = (IAnalysisDataset) ois.readObject();

		} catch(Exception e) {
			e.printStackTrace();
		}
		return dataset;
	}

}
