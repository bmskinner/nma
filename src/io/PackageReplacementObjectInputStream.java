package io;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Specifies a map of new package names for classes being deserialised, so that 
 * the package structure can be updated without old datasets breaking.
 * From http://stackoverflow.com/questions/10936625/using-readclassdescriptor-and-maybe-resolveclass-to-permit-serialization-ver/14608062#14608062
 * @author ben
 * @since 1.13.3
 *
 */
public class PackageReplacementObjectInputStream extends ObjectInputStream {

    /**
     * Migration table. Holds old to new classes representation.
     */
    private static final Map<String, Class<?>> MIGRATION_MAP = new HashMap<String, Class<?>>();

    static
    {
//        MIGRATION_MAP.put("DBOBHandler", com.foo.valueobjects.BoardHandler.class);
//        MIGRATION_MAP.put("DBEndHandler", com.foo.valueobjects.EndHandler.class);
//        MIGRATION_MAP.put("DBStartHandler", com.foo.valueobjects.StartHandler.class);
    }

    /**
     * Constructor.
     * @param stream input stream
     * @throws IOException if io error
     */    
    public PackageReplacementObjectInputStream(final InputStream stream) throws IOException {
        super(stream);
    }

    @Override
    protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
        ObjectStreamClass resultClassDescriptor = super.readClassDescriptor();

        for (final String oldName : MIGRATION_MAP.keySet()){
            if (resultClassDescriptor.getName().equals(oldName))
            {
                String replacement = MIGRATION_MAP.get(oldName).getName();

                try {
                    Field f = resultClassDescriptor.getClass().getDeclaredField("name");
                    f.setAccessible(true);
                    f.set(resultClassDescriptor, replacement);
                } catch (Exception e) {
//                    LOGGER.severe("Error while replacing class name." + e.getMessage());
                }

            }
        }

        return resultClassDescriptor;
    }
}