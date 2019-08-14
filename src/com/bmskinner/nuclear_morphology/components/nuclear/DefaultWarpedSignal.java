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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.CellularComponent;

import ij.process.ImageProcessor;

/**
 * Default implementation of a warped signal
 * @author ben
 * @since 1.14.0
 * @deprecated since 1.16.0 - it saves 8-bit images, not 16-bit. Use a ShortWarpedSignal instead
 *
 */
@Deprecated
public class DefaultWarpedSignal implements IWarpedSignal {

	private static final long serialVersionUID = 1L;
	private final UUID id;

	/** ImageProcessors are not serializable, so store the byte array and convert back as needed.
	 * Note this provides only 8-bit images */
	private final Map<WarpedSignalKey, byte[][]> images = new HashMap<>();
	
	private final Map<WarpedSignalKey, String> targetNames = new HashMap<>();
	
	/**
	 * Construct with the signal group id
	 * @param signalGroupId
	 */
	public DefaultWarpedSignal(@NonNull UUID signalGroupId) {
		id = signalGroupId;
	}
	
	@Override
	public IWarpedSignal duplicate() {
		DefaultWarpedSignal w = new DefaultWarpedSignal(id);
		for(WarpedSignalKey k : images.keySet()) {
			w.images.put(k, images.get(k));
			w.targetNames.put(k, targetNames.get(k));
		}
		return w;
	}

	@Override
	public @NonNull UUID getSignalGroupId() {
		return id;
	}

	@Override
	public @NonNull Set<WarpedSignalKey> getWarpedSignalKeys() {
		return images.keySet();
	}

	@Override
	public void addWarpedImage(@NonNull CellularComponent template, @NonNull UUID templateId,  @NonNull String name, boolean isCellWithSignalsOnly, int threshold, @NonNull ImageProcessor image) {

		// TODO - reenable once this is working for 16-bit images
//		byte[][] arr = IWarpedSignal.toArrayArray(image);
//		
//		WarpedSignalKey k = new WarpedSignalKey(template, templateId, isCellWithSignalsOnly, threshold);
//		
//		images.put(k, arr);
//		targetNames.put(k, name);
	}
	
	@Override
	public void removeWarpedImage(@NonNull WarpedSignalKey key) {
		images.remove(key);
		targetNames.remove(key);
	}
	
	@Override
	public Optional<ImageProcessor> getWarpedImage(@NonNull WarpedSignalKey k){
//		if(!images.containsKey(k))
			return Optional.empty();
		
//		byte[][] arr = images.get(k);
//		return Optional.of(IWarpedSignal.toImageProcessor(arr));
	}

	@Override
	public Optional<ImageProcessor> getWarpedImage(@NonNull CellularComponent template, @NonNull UUID templateId,  boolean isCellWithSignalsOnly, int threshold) {
		WarpedSignalKey k = new WarpedSignalKey(template, templateId, isCellWithSignalsOnly, threshold);
		return getWarpedImage(k);
	}
	
	@Override
	public String getTargetName(@NonNull WarpedSignalKey key) {
		if(targetNames.containsKey(key))
			return targetNames.get(key);
		return targetNames.get(new WarpedSignalKey(key.getTargetShape(), key.getTemplateId(), !key.isCellWithSignalsOnly(), key.getThreshold()));
	}
	


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((images == null) ? 0 : images.hashCode());
		result = prime * result + ((targetNames == null) ? 0 : targetNames.hashCode());
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
		DefaultWarpedSignal other = (DefaultWarpedSignal) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (images == null) {
			if (other.images != null)
				return false;
		} else if (!images.equals(other.images))
			return false;
		if (targetNames == null) {
			if (other.targetNames != null)
				return false;
		} else if (!targetNames.equals(other.targetNames))
			return false;
		return true;
	}
}
