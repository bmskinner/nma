package com.bmskinner.nuclear_morphology.components.nuclear;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.CellularComponent;

import ij.process.ImageProcessor;

/**
 * Implementation of a warped signal using shorts. Replaces the DefaultWarpedSignal,
 * which was limited to saving 8-bit images. Note that the internal save state is still
 * 8-bit - any 16-bit images will be down-sampled when saved
 * @author ben
 * @since 1.16.0
 *
 */
public class ShortWarpedSignal implements IWarpedSignal {
	
	private static final Logger LOGGER = Logger.getLogger(ShortWarpedSignal.class.getName());

	private static final long serialVersionUID = 1L;
	private final UUID id;

	/** ImageProcessors are not serializable, so store the byte array and convert back as needed.
	 * Note this provides only 8-bit images */
	private final Map<WarpedSignalKey, byte[]> images = new HashMap<>();
	
	/** The names of the target shapes */
	private final Map<WarpedSignalKey, String> targetNames = new HashMap<>();
	
	/** The image widths, to reconstruct from byte arrays */
	private final Map<WarpedSignalKey, Integer> widths = new HashMap<>();
	
	/**
	 * Construct with the signal group id
	 * @param signalGroupId
	 */
	public ShortWarpedSignal(@NonNull UUID signalGroupId) {
		id = signalGroupId;
	}
	
	@Override
	public IWarpedSignal duplicate() {
		ShortWarpedSignal w = new ShortWarpedSignal(id);
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
	public void addWarpedImage(@NonNull CellularComponent template, 
			@NonNull UUID templateId,  
			@NonNull String name, 
			boolean isCellWithSignalsOnly, 
			int threshold, 
			boolean isBinarised,
			boolean isNormalised,
			@NonNull ImageProcessor image) {

		byte[] arr = IWarpedSignal.toArray(image);
		
		WarpedSignalKey k = new WarpedSignalKey(template, 
				templateId, 
				isCellWithSignalsOnly, 
				threshold,
				isBinarised,
				isNormalised);
		images.put(k, arr);
		targetNames.put(k, name);
		widths.put(k, image.getWidth());
		LOGGER.fine("Added warped image: "+k);
	}
	
	@Override
	public void removeWarpedImage(@NonNull WarpedSignalKey key) {
		images.remove(key);
		targetNames.remove(key);
	}
	
	@Override
	public Optional<ImageProcessor> getWarpedImage(@NonNull WarpedSignalKey k){
		if(!images.containsKey(k)) {
			LOGGER.fine("Image for requested key not present: "+k);
			LOGGER.fine(toString());
			return Optional.empty();
		}
		
		byte[] arr = images.get(k);
		
		short[] shortArray = IWarpedSignal.byteToshortArray(arr);
		int w = widths.get(k);
		LOGGER.fine("Image retrieved: "+k);
		return Optional.of(IWarpedSignal.toImageProcessor(shortArray, w));
	}

	@Override
	public Optional<ImageProcessor> getWarpedImage(@NonNull CellularComponent template,
			@NonNull UUID templateId, 
			boolean isCellWithSignalsOnly, 
			int threshold,
			boolean isBinarised,
			boolean isNormalised) {
		WarpedSignalKey k = new WarpedSignalKey(template, 
				templateId, 
				isCellWithSignalsOnly, 
				threshold,
				isBinarised,
				isNormalised);
		return getWarpedImage(k);
	}
	
	@Override
	public String getTargetName(@NonNull WarpedSignalKey key) {
		if(targetNames.containsKey(key))
			return targetNames.get(key);
		return targetNames.get(new WarpedSignalKey(key.getTargetShape(), 
				key.getTemplateId(), 
				key.isCellWithSignalsOnly(), 
				key.getThreshold(), 
				key.isBinarised(),
				key.isNormalised()));
	}
	

	@Override
	public String toString() {
		Set<WarpedSignalKey> allKeys = new HashSet<>();
		allKeys.addAll(images.keySet());
		allKeys.addAll(targetNames.keySet());
		allKeys.addAll(widths.keySet());
		StringBuilder sb = new StringBuilder();
		for(WarpedSignalKey k : allKeys ) {
			sb.append(k)
			.append(" Image: "+images.containsKey(k))
			.append("Target: "+targetNames.containsKey(k))
			.append("Widths: "+widths.containsKey(k))
			.append("\n");
		}
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((images == null) ? 0 : images.hashCode());
		result = prime * result + ((targetNames == null) ? 0 : targetNames.hashCode());
		result = prime * result + ((widths == null) ? 0 : widths.hashCode());
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
		ShortWarpedSignal other = (ShortWarpedSignal) obj;
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
		if(widths == null) {
			if (other.widths != null)
				return false;
		} else if (!widths.equals(other.widths))
			return false;
		return true;
	}

}
