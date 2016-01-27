/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package utility;

import java.io.Serializable;

/**
 * Hold version information, and parsing methods
 * @author bms41
 *
 */
public class Version implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private final int major;
	private final int minor;
	private final int revision;
	
	private static final String SEPARATOR = "."; 
	
	public Version(final int major, final int minor, final int revision ){
		this.major = major;
		this.minor = minor;
		this.revision = revision;
	}
	
	/**
	 * Parse the given string to a version. The string should have
	 * three integers separated by dots - e.g. 1.11.5
	 * @param s
	 * @return
	 */
	public static Version parseString(final String s){
		if(s==null){
			throw new IllegalArgumentException("Input string is null");
		}
		
		String[] parts = s.split("\\"+SEPARATOR);
		if(parts.length==3){
			return new Version(Integer.valueOf(parts[0]), Integer.valueOf(parts[1]), Integer.valueOf(parts[2]));
		} else {
			throw new IllegalArgumentException("Input string is not a version format");
		}
	}
	
	/**
	 * Test if the given version is older than this version
	 * @param v
	 * @return
	 */
	public boolean isOlderThan(final Version v){
		if(this.major<v.getMajor()){
			return true;
		}
		
		if(this.major==v.getMajor() && this.minor<v.getMinor()){
			return true;
		}
		
		if(this.major==v.getMajor() && this.minor==v.getMinor() && this.revision<v.getRevision()){
			return true;
		}
		return false;
	}
	
	public boolean isNewerThan(final Version v){
		return v.isOlderThan(this);
	}
	
	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + major;
		result = prime * result + minor;
		result = prime * result + revision;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Version other = (Version) obj;
		if (major != other.major)
			return false;
		if (minor != other.minor)
			return false;
		if (revision != other.revision)
			return false;
		return true;
	}

	public int getMajor() {
		return major;
	}

	public int getMinor() {
		return minor;
	}

	public int getRevision() {
		return revision;
	}
	
	public String toString(){
		return major + SEPARATOR + minor + SEPARATOR + revision;
	}

}
