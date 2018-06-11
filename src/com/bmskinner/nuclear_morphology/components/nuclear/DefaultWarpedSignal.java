package com.bmskinner.nuclear_morphology.components.nuclear;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.options.INuclearSignalOptions;

import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

public class DefaultWarpedSignal implements IWarpedSignal {
	
	private final UUID id;
	
	// ImageProcessors are not serializable, so store the byte array and convert back as needed
	private final Map<CellularComponent, byte[][]> templates = new HashMap<>();
	
	/**
	 * Construct with the signal group id
	 * @param signalGroupId
	 */
	public DefaultWarpedSignal(@NonNull UUID signalGroupId) {
		id = signalGroupId;
	}

	@Override
	public @NonNull UUID getSignalGroupId() {
		return id;
	}

	@Override
	public @NonNull Set<CellularComponent> getTemplates() {
		return templates.keySet();
	}

	@Override
	public void addWarpedImage(@NonNull CellularComponent template, @NonNull ByteProcessor image) {

		byte[][] arr = new byte[image.getWidth()][image.getHeight()];
		
		for(int w=0; w<image.getWidth(); w++) {
			for(int h=0; h<image.getHeight(); h++) {
				arr[w][h] = (byte) image.get(w, h);
			}
		}
		templates.put(template, arr);
	}

	@Override
	public Optional<ImageProcessor> getWarpedImage(@NonNull CellularComponent template) {
		if(!templates.containsKey(template))
			return Optional.empty();
		
		byte[][] arr = templates.get(template);
		ByteProcessor image = new ByteProcessor(arr.length, arr[0].length);
		for(int w=0; w<image.getWidth(); w++) {
			for(int h=0; h<image.getHeight(); h++) {
				image.set(w, h, arr[w][h]);
			}
		}
		return Optional.of(image);
	}

	@Override
	public Optional<INuclearSignal> getWarpedSignal(@NonNull CellularComponent template, @NonNull INuclearSignalOptions options) {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((templates == null) ? 0 : templates.hashCode());
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
		if (templates == null) {
			if (other.templates != null)
				return false;
		} else if (!templates.equals(other.templates))
			return false;
		return true;
	}
	
	

}
