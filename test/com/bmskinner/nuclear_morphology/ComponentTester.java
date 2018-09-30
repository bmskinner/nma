package com.bmskinner.nuclear_morphology;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;

import com.bmskinner.nuclear_morphology.logging.ConsoleHandler;
import com.bmskinner.nuclear_morphology.logging.LogPanelFormatter;
import com.bmskinner.nuclear_morphology.logging.Loggable;

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
	protected Logger logger;
	
	@Before
	public void setUp() throws Exception{
		logger = Logger.getLogger(Loggable.CONSOLE_LOGGER);
		logger.setLevel(Level.FINE);

		boolean hasHandler = false;
		for(Handler h : logger.getHandlers()) {
			if(h instanceof ConsoleHandler)
				hasHandler = true;
		}
		if(!hasHandler)
			logger.addHandler(new ConsoleHandler(new LogPanelFormatter()));
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
	 * Test if the fields of two objects are identical
	 * @param original
	 * @param dup
	 * @throws Exception
	 */
	protected void testDuplicatesByField(Object original, Object dup) throws Exception {
		for(Field f : getInheritedPrivateFields(dup.getClass())) {
			f.setAccessible(true);	

			if(f.getType().equals(SoftReference.class))
				continue;
			if(f.getType().equals(Class.forName("com.bmskinner.nuclear_morphology.components.DefaultCellularComponent$ShapeCache")))
				continue;
			if(f.getType().equals(Class.forName("com.bmskinner.nuclear_morphology.components.generic.DefaultProfileCollection$ProfileCache")))
				continue;	
			Object oValue = f.get(original);
			Object dValue  = f.get(dup);
			
			int oHash = oValue==null?-1:oValue.hashCode();
			int dHash = dValue==null?-1:dValue.hashCode();
			
			assertThat(f.getName()+": hashcodes: original "+oHash+" | dup "+dHash, oValue, equalTo(dValue));
		}

		assertEquals("Equals method", original, dup);
	}

}
