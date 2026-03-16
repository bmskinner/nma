package com.bmskinner.nma.components.profiles;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;

/**
 * A cache for profiles to avoid recalculating from individual nuclei
 * 
 * @author Ben Skinner
 * @since 1.13.4
 *
 */
public class ProfileCache {
	/**
	 * The key used to store values in the cache
	 * 
	 * @author Ben Skinner
	 * @since 2.4.1
	 *
	 */
	record ProfileKey(ProfileType type,
			double quartile,
			Landmark tag) {

		public boolean has(Landmark t) {
			return tag.equals(t);
		}
	}

	private final Map<ProfileKey, IProfile> map = new ConcurrentHashMap<>();

	public ProfileCache() { // no default data
	}

	public ProfileCache duplicate() throws SegmentUpdateException {
		final ProfileCache result = new ProfileCache();
		for (final ProfileKey k : map.keySet()) {
			final IProfile p = map.get(k);
			if (p != null) {
				result.map.put(k, p.duplicate());
			}
		}
		return result;
	}

	/**
	 * Add a profile with the given keys
	 * 
	 * @param type     the profile type
	 * @param quartile the quartile of the dataset
	 * @param tag      the tag
	 * @param profile  the profile to save
	 */
	public void addProfile(final ProfileType type, final double quartile,
			final Landmark tag,
			IProfile profile) {
		final ProfileKey key = new ProfileKey(type, quartile, tag);
		map.put(key, profile);
	}

	public boolean hasProfile(final ProfileType type, final double quartile,
			final Landmark tag) {
		final ProfileKey key = new ProfileKey(type, quartile, tag);
		return map.containsKey(key);
	}

	public IProfile getProfile(final ProfileType type, final double quartile,
			final Landmark tag) {
		final ProfileKey key = new ProfileKey(type, quartile, tag);
		return map.get(key);
	}

	/**
	 * Remove all profiles from the cache
	 */
	public void clear() {
		map.clear();
	}

	public void remove(final Landmark t) {

		final Iterator<ProfileKey> it = map.keySet().iterator();
		while (it.hasNext()) {
			final ProfileKey k = it.next();
			if (k.has(t)) {
				it.remove();
			}
		}

	}

	@Override
	public int hashCode() {
		return Objects.hash(map);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final ProfileCache other = (ProfileCache) obj;
		return Objects.equals(map, other.map);
	}

}
