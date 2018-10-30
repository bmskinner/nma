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
package com.bmskinner.nuclear_morphology.components.nuclear;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bmskinner.nuclear_morphology.components.generic.BorderTag;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.nuclei.DefaultLobedNucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.DefaultNucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.sperm.DefaultPigSpermNucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.sperm.DefaultRodentSpermNucleus;

/**
 * The types of nuclei we are able to analyse, with the reference and
 * orientation points to be used. The reference point is the best identifiable
 * point on the nucleus when aligning profiles. The orientation point is the
 * point placed at the bottom when rotating a consensus nucleus.
 *
 */
public enum NucleusType {
    ROUND("Round nucleus", "Head", "Tail", DefaultNucleus.class), 
    RODENT_SPERM("Rodent sperm nucleus", "Tip", "Tail", DefaultRodentSpermNucleus.class), 
    PIG_SPERM("Pig sperm nucleus", "Tail", "Tail", DefaultPigSpermNucleus.class), 
    NEUTROPHIL("Lobed nucleus", "Head", "Tail", DefaultLobedNucleus.class);

    private final String   name;
    private final Class<?> nucleusClass;

    private final Map<Tag, String> map = new HashMap<Tag, String>();

    NucleusType(String name, String referencePoint, String orientationPoint, Class<?> nucleusClass) {
        this.name = name;
        this.nucleusClass = nucleusClass;
        this.map.put(Tag.REFERENCE_POINT, referencePoint);
        this.map.put(Tag.ORIENTATION_POINT, orientationPoint);
    }

    public String toString() {
        return this.name;
    }

    /**
     * Get the name of the given border tag, if present. For example, the name
     * of the RP in mouse sperm is the tip. The name of the RP in pig sperm is
     * the head.
     * 
     * @param point
     * @return
     */
    public String getPoint(Tag point) {
        return this.map.get(point);
    }

    public Class<?> getNucleusClass() {
        return this.nucleusClass;
    }

    /**
     * Get the simple names of the border tags in the nucleus
     * 
     * @return
     */
    public String[] pointNames() {
        List<String> list = new ArrayList<String>();
        for (Tag tag : map.keySet()) {
            list.add(map.get(tag));
        }
        return list.toArray(new String[0]);
    }

    /**
     * Get the border tag with the given name, or null if the name is not found
     * 
     * @param name
     * @return
     */
    public Tag getTagFromName(String name) {
        for (Tag tag : map.keySet()) {
            if (map.get(tag).equals(name)) {
                return tag;
            }
        }
        return null;
    }

    /**
     * Given a nucleus, find the appropriate NucleusType
     * 
     * @param n
     * @return
     */
    public static NucleusType getNucleusType(Nucleus n) {
        Class<?> nucleusClass = n.getClass();
        for (NucleusType type : NucleusType.values()) {
            if (type.getNucleusClass().equals(nucleusClass)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Test if a given name is a tag name
     * 
     * @param s
     * @return
     */
    public static boolean isBorderTag(String s) {
        for (BorderTag tag : BorderTag.values()) {
            if (tag.toString().equals(s)) {
                return true;
            }
        }
        return false;
    }
}
