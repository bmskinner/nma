package com.bmskinner.nuclear_morphology.components.profiles;

import com.bmskinner.nuclear_morphology.components.profiles.BorderTagObject.BorderTagType;

/**
 * A tag using only a name, rather than the BorderTag enum
 * traditionally provided. Allows for unique tags outside the
 * core set
 * @author ben
 * @since 1.18.3
 *
 */
public class DefaultTag implements Tag {
	
	private static final long serialVersionUID = 1L;
	
	private String name;
	
	public DefaultTag(String name) {
		this.name = name;
	}

	@Override
	public int compareTo(Tag tag) {
		return name.compareTo(tag.getName());
	}

	@Override
	public String getName() {
		return name;
	}


	@Override
	public BorderTagType type() {
		return BorderTagType.EXTENDED;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		DefaultTag other = (DefaultTag) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return name;
	}
}
