package com.bmskinner.nuclear_morphology.components.nuclear;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.CellularComponent;

import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

/**
 * Default implementation of a warped signal
 * @author ben
 * @since 1.14.0
 *
 */
public class DefaultWarpedSignal implements IWarpedSignal {

	private static final long serialVersionUID = 1L;
	private final UUID id;

	/** ImageProcessors are not serializable, so store the byte array and convert back as needed */
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
	public void addWarpedImage(@NonNull CellularComponent template, @NonNull String name, boolean isCellWithSignalsOnly, @NonNull ByteProcessor image) {

		byte[][] arr = new byte[image.getWidth()][image.getHeight()];
		
		for(int w=0; w<image.getWidth(); w++) {
			for(int h=0; h<image.getHeight(); h++) {
				arr[w][h] = (byte) image.get(w, h);
			}
		}
		
		WarpedSignalKey k = new WarpedSignalKey(template, isCellWithSignalsOnly);
		
		images.put(k, arr);
		targetNames.put(k, name);
	}
	
	@Override
	public Optional<ImageProcessor> getWarpedImage(@NonNull WarpedSignalKey k){
		if(!images.containsKey(k))
			return Optional.empty();
		
		byte[][] arr = images.get(k);
		ByteProcessor image = new ByteProcessor(arr.length, arr[0].length);
		for(int w=0; w<image.getWidth(); w++) {
			for(int h=0; h<image.getHeight(); h++) {
				image.set(w, h, arr[w][h]);
			}
		}
		return Optional.of(image);
	}

	@Override
	public Optional<ImageProcessor> getWarpedImage(@NonNull CellularComponent template, boolean isCellWithSignalsOnly) {
		WarpedSignalKey k = new WarpedSignalKey(template, isCellWithSignalsOnly);
		return getWarpedImage(k);
	}
	
	@Override
	public String getTargetName(@NonNull WarpedSignalKey key) {
		if(targetNames.containsKey(key))
			return targetNames.get(key);
		return targetNames.get(new WarpedSignalKey(key.getTargetShape(), !key.isCellWithSignalsOnly()));
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
