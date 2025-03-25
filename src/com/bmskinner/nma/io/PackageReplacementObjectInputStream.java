/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nma.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Specifies a map of new package names for classes being deserialised, so that
 * the package structure can be updated without old datasets breaking. From
 * http://stackoverflow.com/questions/10936625/using-readclassdescriptor-and-maybe-resolveclass-to-permit-serialization-ver/14608062#14608062
 * 
 * @author Ben Skinner
 * @since 1.13.3
 *
 */
@SuppressWarnings("deprecation")
public class PackageReplacementObjectInputStream extends ObjectInputStream {
	
	private static final Logger LOGGER = Logger.getLogger(PackageReplacementObjectInputStream.class.getName());

    /**
     * Migration table. Holds old to new classes representation.
     */
    protected static final Map<String, Class<?>> MIGRATION_MAP = new HashMap<String, Class<?>>();

    /**
     * Constructor.
     * 
     * @param stream input stream
     * @throws IOException if io error
     */
    public PackageReplacementObjectInputStream(final InputStream stream) throws IOException {
        super(stream);
    }


    @Override
    protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
        ObjectStreamClass resultClassDescriptor = super.readClassDescriptor();

        for (final String oldName : MIGRATION_MAP.keySet()) {
            if (resultClassDescriptor.getName().equals(oldName)) {

                String replacement = MIGRATION_MAP.get(oldName).getName();

                try {
                    Field f = resultClassDescriptor.getClass().getDeclaredField("name");
                    f.setAccessible(true);
                    f.set(resultClassDescriptor, replacement);  
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error while replacing class name: " + e.getMessage(), e);
                    throw new ClassNotFoundException("Package replacement of class "+oldName+" was unsuccessful", e);
                }
            }
        }

        return resultClassDescriptor;
    }
}
