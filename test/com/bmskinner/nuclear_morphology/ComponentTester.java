package com.bmskinner.nuclear_morphology;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
			if(f.getType().equals(Class.forName("com.bmskinner.nuclear_morphology.components.DefaultCellularComponent$ShapeCache")))
				continue;
			if(f.getType().equals(Class.forName("com.bmskinner.nuclear_morphology.components.generic.DefaultProfileCollection$ProfileCache")))
				continue;	
			if(f.getType().equals(Class.forName("com.bmskinner.nuclear_morphology.components.stats.StatsCache")))
				continue;
			if(f.getType().equals(Class.forName("com.bmskinner.nuclear_morphology.components.stats.VennCache")))
				continue;
			if(f.getType().equals(Class.forName("com.bmskinner.nuclear_morphology.analysis.signals.SignalManager")))
				continue;
			if(f.getType().equals(Class.forName("com.bmskinner.nuclear_morphology.components.generic.ProfileManager")))
				continue;
			Object oValue = f.get(original);
			Object dValue  = f.get(dup);
			
			int oHash = oValue==null?-1:oValue.hashCode();
			int dHash = dValue==null?-1:dValue.hashCode();
			if(oValue!=null&&oValue.getClass().equals(float[].class)) {
				oHash = Arrays.hashCode((float[])oValue);
				dHash = Arrays.hashCode((float[])dValue);
			}
			if(oValue!=null&&oValue.getClass().equals(int[].class)) {
				oHash = Arrays.hashCode((int[])oValue);
				dHash = Arrays.hashCode((int[])dValue);
			}
			if(oValue!=null&&oValue.getClass().equals(double[].class)) {
				oHash = Arrays.hashCode((double[])oValue);
				dHash = Arrays.hashCode((double[])dValue);
			}
			
			// ignore transient fields
			if(!Modifier.isTransient(f.getModifiers()))
				assertThat("Field "+f.getName()+" in "+original.getClass().getSimpleName()+": hashcodes: original "+oHash+" | dup "+dHash, dValue, equalTo(oValue));
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
