package com.bmskinner.nuclear_morphology;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.junit.Before;

import com.bmskinner.nuclear_morphology.components.generic.IPoint;

/**
 * Base class for the component tests
 * @author bms41
 * @since 1.14.0
 *
 */
public abstract class ComponentTester extends FloatArrayTester {
	
	protected static final long RNG_SEED = 1234;
	protected static final int N_CELLS = 10;
	protected static final int N_CHILD_DATASETS = 2;
	protected static final Logger LOGGER = Logger.getLogger(ComponentTester.class.getName());

	@Before
	public void setUp() throws Exception{

	}
	
	/**
	 * Test if the two given points are vertically aligned
	 * @param topPoint the top point
	 * @param bottomPoint the bottom point
	 * @return
	 */
	protected boolean areVertical(@NonNull IPoint topPoint, @NonNull IPoint bottomPoint) {
		double err = bottomPoint.getX()-topPoint.getX();
		LOGGER.fine("Error = "+err);
		boolean xEqual = (Math.abs(bottomPoint.getX()- topPoint.getX())<0.0001);
		boolean yAbove = topPoint.getY()>bottomPoint.getY();
		return xEqual & yAbove;
	}
	
	/**
	 * Get all the private fields for the class, including superclass fields
	 * @param type
	 * @return
	 */
	private List<Field> getInheritedPrivateFields(Class<?> type) {
	    List<Field> result = new ArrayList<>();

	    Class<?> i = type;
	    while (i != null && i != Object.class) {
	        Collections.addAll(result, i.getDeclaredFields());
	        i = i.getSuperclass();
	    }

	    return result;
	}
	
	/**
	 * Get the inherited field for the given class
	 * @param type the class to fetch
	 * @param name the inherited field name
	 * @return
	 */
	protected Field getInheritedField(Class<?> type, String name) {
		List<Field> fields = getInheritedPrivateFields(type);
		for(Field f : fields)
			if(f.getName().equals(name)) {
				f.setAccessible(true);
				return f;
			}
		return null;
	}
	
	/**
	 * Test if the fields of two objects have the same hashcodes.
	 * Skips cache classes which are not used in hashcode methods. 
	 * @param original
	 * @param dup
	 * @param fieldsToSkip skip fields in the object with these names
	 * @throws Exception
	 */
	protected void testDuplicatesByField(Object original, Object dup, List<String> fieldsToSkip) throws Exception {
		for(Field f : getInheritedPrivateFields(dup.getClass())) {
			
			if(fieldsToSkip.contains(f.getName()))
				continue;
			f.setAccessible(true);	

			if(f.getType().equals(SoftReference.class))
				continue;
			if(f.getType().equals(WeakReference.class))
				continue;
			if(f.getType().equals(Class.forName("com.bmskinner.nuclear_morphology.components.cells.DefaultCellularComponent$ShapeCache")))
				continue;
			if(f.getType().equals(Class.forName("com.bmskinner.nuclear_morphology.components.profiles.DefaultProfileCollection$ProfileCache")))
				continue;	
			if(f.getType().equals(Class.forName("com.bmskinner.nuclear_morphology.components.measure.StatsCache")))
				continue;
			if(f.getType().equals(Class.forName("com.bmskinner.nuclear_morphology.components.measure.VennCache")))
				continue;
			if(f.getType().equals(Class.forName("com.bmskinner.nuclear_morphology.components.signals.SignalManager")))
				continue;
			if(f.getType().equals(Class.forName("com.bmskinner.nuclear_morphology.components.profiles.ProfileManager")))
				continue;
			Object oValue = f.get(original);
			Object dValue  = f.get(dup);
			
			// ignore transient fields
			if(!Modifier.isTransient(f.getModifiers())) {
				if(oValue!=null && !oValue.equals(dValue)) {
					
					if(oValue.getClass().equals(float[].class)) {
						System.out.println(Arrays.toString((float[])oValue));
						System.out.println(Arrays.toString((float[])dValue));
					}
					if(oValue.getClass().equals(int[].class)) {
						System.out.println(Arrays.toString((int[])oValue));
						System.out.println(Arrays.toString((int[])dValue));
					}
					if(oValue.getClass().equals(double[].class)) {
						System.out.println(Arrays.toString((double[])oValue));
						System.out.println(Arrays.toString((double[])dValue));
					}
					if(oValue.getClass().equals(byte[].class)) {
						System.out.println(Arrays.toString((byte[])oValue));
						System.out.println(Arrays.toString((byte[])dValue));
					}
					
					// Issue with arrays in hashmaps: Object.hashcode()
					// depends on reference, so is not equal between two
					// arrays. Need to use Arrays.hashcode().
					if(oValue.getClass().equals(HashMap.class)) {

						int oHash = 0;
						int dHash = 0;
						HashMap o = (HashMap)oValue;
						HashMap d = (HashMap)dValue;
						
						for(Object e : o.keySet()) {
							Object v0 = o.get(e);
							Object v1 = d.get(e);
							if(v0.getClass().equals(byte[].class)) {
								oHash += Arrays.hashCode((byte[])v0);
								dHash += Arrays.hashCode((byte[])v1);
							}
							
						}
						oValue = oHash;
						dValue = dHash;
					
					}
					if(oValue.getClass().equals(HashSet.class)) {
						int oHash = 0;
						int dHash = 0;
						HashSet o = (HashSet)oValue;
						HashSet d = (HashSet)dValue;
						if(!o.containsAll(d)) {
							System.out.println("Elements not shared in set");
							for(Object v0 : o) {
								System.out.println(d.contains(v0));
							}
							
						}
					}
					
				}
				
				String msg = "Field '"+f.getName()+"' in "+original.getClass().getSimpleName()+" does not match";
				assertThat(msg, dValue, equalTo(oValue));
			}
		}

		assertEquals("Equals method in "+original.getClass().getSimpleName(), original, dup);
	}
	
	/**
	 * Test if the fields of two objects have the same hashcodes.
	 * Skips cache classes which are not used in hashcode methods. 
	 * @param original
	 * @param dup
	 * @throws Exception
	 */
	protected void testDuplicatesByField(Object original, Object dup) throws Exception {
		List<String> fieldsToSkip = new ArrayList<>();
		testDuplicatesByField(original, dup, fieldsToSkip);
	}

}
