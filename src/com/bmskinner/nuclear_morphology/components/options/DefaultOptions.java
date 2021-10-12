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
package com.bmskinner.nuclear_morphology.components.options;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.io.Io;
import com.bmskinner.nuclear_morphology.io.XmlSerializable;

/**
 * A base for all the options classes that need to store options as a key value
 * pair
 * 
 * @author ben
 * @since 1.13.4
 */
public class DefaultOptions implements Serializable, HashOptions {
	
    private static final long serialVersionUID = 1L;
    private final Map<String, Integer> intMap    = new HashMap<>();
    private final Map<String, Double>  dblMap    = new HashMap<>();
    private final Map<String, Float>   fltMap    = new HashMap<>();
    private final Map<String, Boolean> boolMap   = new HashMap<>();
    private final Map<String, String>  stringMap = new HashMap<>();
    
    private final Map<String, HashOptions> subMap = new HashMap<>();

    /**
     * Construct an empty options
     */
    public DefaultOptions() { }

    /**
     * Create by copying another options object
     * @param o
     */
    protected DefaultOptions(@NonNull HashOptions o) {
    	set(o);
    }
    
    /**
     * Construct from an XML element. Use for 
     * unmarshalling. The element should conform
     * to the specification in {@link XmlSerializable}.
     * @param e the XML element containing the data.
     */
    public DefaultOptions(Element e) {
		
		// Add each map
		for(Element i : e.getChildren("Integer"))
			intMap.put(i.getAttributeValue("name"), Integer.parseInt(i.getText()));
		
		for(Element i : e.getChildren("Float"))
			fltMap.put(i.getAttributeValue("name"), Float.parseFloat(i.getText()));
		
		for(Element i : e.getChildren("Double"))
			dblMap.put(i.getAttributeValue("name"), Double.parseDouble(i.getText()));
		
		for(Element i : e.getChildren("Boolean"))
			boolMap.put(i.getAttributeValue("name"), Boolean.parseBoolean(i.getText()));
		
		for(Element i : e.getChildren("String"))
			stringMap.put(i.getAttributeValue("name"), i.getText());
		
		
		for(Element i : e.getChildren("Suboption"))
			subMap.put(i.getAttributeValue("name"), new DefaultOptions(i.getChild("Options")));
		
    }
    
    @Override
	public HashOptions duplicate() {
    	DefaultOptions d = new DefaultOptions();
    	d.set(this);
		return d;
	}

	@Override
	public void set(HashOptions o) {
    	for(String s : o.getBooleanKeys())
    		boolMap.put(s, o.getBoolean(s));    
    	for(String s : o.getIntegerKeys())
    		intMap.put(s, o.getInt(s));
    	for(String s : o.getDoubleKeys())
    		dblMap.put(s, o.getDouble(s));
    	for(String s : o.getFloatKeys())
    		fltMap.put(s, o.getFloat(s));
    	for(String s : o.getStringKeys())
    		stringMap.put(s, o.getString(s));
    	
    	for(String s : o.getSubOptionKeys()) {
    		subMap.put(s, o.getSubOptions(s).duplicate());
    	}
    }
    
    @Override
	public boolean hasSubOptions(String s) {
		return subMap.containsKey(s);
	}

	@Override
	public HashOptions getSubOptions(String s) {
		return subMap.get(s);
	}

	@Override
	public void setSubOptions(String s, HashOptions o) {
		subMap.put(s, o);
	}

	/**
     * Get the double value with the given key
     * 
     * @param s
     * @return
     */
    @Override
	public double getDouble(String s) {
        return dblMap.get(s).doubleValue();
    }

    /**
     * Get the int value with the given key
     * 
     * @param s
     * @return
     */
    @Override
	public int getInt(String s) {
        return intMap.get(s).intValue();
    }

    /**
     * Get the boolean value with the given key
     * 
     * @param s
     * @return
     */
    @Override
	public boolean getBoolean(String s) {
    	if(!boolMap.containsKey(s))
    		return false;
        return boolMap.get(s).booleanValue();
    }

    /**
     * Set the double value with the given key
     * 
     * @param s
     * @param d
     */
    @Override
	public void setDouble(String s, double d) {
        dblMap.put(s, d);
    }

    /**
     * ] Set the int value with the given key
     * 
     * @param s
     * @param i
     */
    @Override
	public void setInt(String s, int i) {
        intMap.put(s, i);
    }

    /**
     * Set the boolean value with the given key
     * 
     * @param s
     * @param b
     */
    @Override
	public void setBoolean(String s, boolean b) {
        boolMap.put(s, b);
    }


    @Override
	public float getFloat(String s) {
        return fltMap.get(s).floatValue();
    }

    @Override
	public void setFloat(String s, float f) {
        fltMap.put(s, f);
    }
    
    @Override
	public String getString(String s) {
        return stringMap.get(s);
    }

    @Override
	public void setString(String k, String v) {
    	stringMap.put(k, v);
    }
    
    
    
    @Override
	public File getFile(String s) {
		return new File(stringMap.get(s));
	}

	@Override
	public void setFile(String s, File f) {
		stringMap.put(s, f.getAbsolutePath());
	}

	@Override
	public boolean hasBoolean(String s) {
		return boolMap.containsKey(s);
	}

	@Override
	public boolean hasFloat(String s) {
		return fltMap.containsKey(s);
	}

	@Override
	public boolean hasDouble(String s) {
		return dblMap.containsKey(s);
	}

	@Override
	public boolean hasInt(String s) {
		return intMap.containsKey(s);
	}

	@Override
	public boolean hasString(String s) {
		return stringMap.containsKey(s);
	}

	@Override
	public List<String> getBooleanKeys() {
    	return sortedKeyList(boolMap);
    }
    
    @Override
	public List<String> getIntegerKeys() {
    	return sortedKeyList(intMap);
    }
    
    @Override
	public List<String> getDoubleKeys() {
    	return sortedKeyList(dblMap);
    }
    
    @Override
	public List<String> getFloatKeys() {
    	return sortedKeyList(fltMap);
    }
    
    @Override
	public List<String> getStringKeys() {
    	return sortedKeyList(stringMap);
    }
    
    @Override
   	public List<String> getSubOptionKeys() {
       	return sortedKeyList(subMap);
       }
    
    private List<String> sortedKeyList(Map<String, ?> map){
    	return map.keySet().stream().sorted().collect(Collectors.toList());
    }

    @Override
	public List<String> getKeys() {
        List<String> list = new ArrayList<>();
        
        list.addAll(getBooleanKeys());
        list.addAll(getIntegerKeys());
        list.addAll(getDoubleKeys());
        list.addAll(getFloatKeys());
        list.addAll(getStringKeys());
        list.addAll(getSubOptionKeys());

        Collections.sort(list);
        return list;
    }

    @Override
	public Object getValue(String key) {

        if (intMap.containsKey(key))
            return intMap.get(key);
        if (dblMap.containsKey(key))
            return dblMap.get(key);
        if (boolMap.containsKey(key))
            return boolMap.get(key);
        if (fltMap.containsKey(key))
            return fltMap.get(key);
        if(stringMap.containsKey(key))
        	 return stringMap.get(key);
        if(subMap.containsKey(key))
        	return subMap.get(key);
        return "N/A";
    }
    
    @Override
	public Map<String, Object> getEntries(){
    	Map<String, Object> result = new HashMap<>();
    	addEntries(intMap, result);
    	addEntries(dblMap, result);
    	addEntries(boolMap, result);
    	addEntries(fltMap, result);
    	addEntries(stringMap, result);
    	addEntries(subMap, result);
    	return result;
    }
    
    private void addEntries(Map<String, ?> source, Map<String, Object> target){
    	for(Entry<String, ?> e : source.entrySet())
    		target.put(e.getKey(), e.getValue());
    }

    @Override
	public void remove(String s) {
    	intMap.remove(s);
    	dblMap.remove(s);
    	boolMap.remove(s);
    	fltMap.remove(s);
    	stringMap.remove(s);
    	subMap.remove(s);
    }
    
    
    
    
    @Override
	public void remove(HashOptions o) {
		for(String s : o.getKeys())
			remove(s);
	}

	@Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Map<String, Object> map = getEntries();
        for(Entry<String, Object> e : map.entrySet())
            sb.append("\t" + e.getKey() + ": " + e.getValue() + Io.NEWLINE);
        return sb.toString();
    }

	@Override
	public Element toXmlElement() {
		Element e = new Element("Options");
		
		// Add each map
		for(Entry<String, Integer> entry : intMap.entrySet()) {
			e.addContent(new Element("Integer").setAttribute("name", entry.getKey()).setText(entry.getValue().toString()));
		}
		

		for(Entry<String, Float> entry : fltMap.entrySet()) {
			e.addContent(new Element("Float").setAttribute("name", entry.getKey()).setText(entry.getValue().toString()));
		}
		
		for(Entry<String, Double> entry : dblMap.entrySet()) {
			e.addContent(new Element("Double").setAttribute("name", entry.getKey()).setText(entry.getValue().toString()));
		}
		
		for(Entry<String, Boolean> entry : boolMap.entrySet()) {
			e.addContent(new Element("Boolean").setAttribute("name", entry.getKey()).setText(entry.getValue().toString()));
		}
		
		for(Entry<String, String> entry : stringMap.entrySet()) {
			e.addContent(new Element("String").setAttribute("name", entry.getKey()).setText(entry.getValue().toString()));
		}
		
		// Add the sub-options
		for(Entry<String, HashOptions> entry : subMap.entrySet()) {
			e.addContent(new Element("Suboption").setAttribute("name", entry.getKey())
					.addContent(entry.getValue().toXmlElement() ));
		}
		return e;
	}

	@Override
	public int hashCode() {
		return Objects.hash(boolMap, dblMap, fltMap, intMap, stringMap, subMap);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DefaultOptions other = (DefaultOptions) obj;
		return Objects.equals(boolMap, other.boolMap) && Objects.equals(dblMap, other.dblMap)
				&& Objects.equals(fltMap, other.fltMap) && Objects.equals(intMap, other.intMap)
				&& Objects.equals(stringMap, other.stringMap) && Objects.equals(subMap, other.subMap);
	}
}
