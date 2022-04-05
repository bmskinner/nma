package com.bmskinner.nuclear_morphology.components.signals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.components.cells.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.io.XmlSerializable;
import com.bmskinner.nuclear_morphology.utility.StringUtils;

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
	
	
    /**
     * Construct from an XML element. Use for 
     * unmarshalling. The element should conform
     * to the specification in {@link XmlSerializable}.
     * @param e the XML element containing the data.
     * @throws ComponentCreationException 
     */
	public ShortWarpedSignal(@NonNull Element e) throws ComponentCreationException {
		id = UUID.fromString(e.getAttributeValue("id"));
		
		for(Element el : e.getChildren("Image")) {
			WarpedSignalKey k = new WarpedSignalKey(el.getChild("WarpedSignalKey"));
			
			byte[] b = StringUtils.hexToBytes(el.getChildText("Bytes"));			
			images.put(k, b);
			
			targetNames.put(k, el.getChildText("Name"));
			
			widths.put(k, Integer.parseInt(el.getChildText("Width")));
		}
	}
	
	@Override
	public Element toXmlElement() {
		Element e = new Element("WarpedSignal").setAttribute("id", id.toString());

		for(Entry<WarpedSignalKey, byte[]> entry : images.entrySet()) {
			Element el = new Element("Image");
			el.addContent(entry.getKey().toXmlElement());
			el.addContent(new Element("Bytes").setText(StringUtils.bytesToHex(entry.getValue())));
			el.addContent(new Element("Name").setText(targetNames.get(entry.getKey())));
			el.addContent(new Element("Width").setText( String.valueOf(widths.get(entry.getKey()))));
			e.addContent(el);
		}		
		return e;
	}
	
	@Override
	public IWarpedSignal duplicate() {
		ShortWarpedSignal w = new ShortWarpedSignal(id);
		for(Entry<WarpedSignalKey, byte[]> entry : images.entrySet()) {
			w.images.put(entry.getKey(), entry.getValue());
			w.targetNames.put(entry.getKey(), targetNames.get(entry.getKey()));
			w.widths.put(entry.getKey(), widths.get(entry.getKey()));
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
	public void addWarpedImage(@NonNull Nucleus template, 
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
	public Optional<ImageProcessor> getWarpedImage(@NonNull Nucleus template,
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
			sb.append("Key: "+k)
			.append(" Image: "+images.containsKey(k))
			.append(" Target: "+targetNames.containsKey(k))
			.append(" Widths: "+widths.containsKey(k))
			.append("\n");
		}
		
		sb.append(images.hashCode());
		return sb.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, images, targetNames, widths);
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
		
		// Issue with arrays in hashmaps: Object.hashcode()
		// depends on reference, so is not equal between two
		// arrays. Need to use Arrays.hashcode().
		Set<WarpedSignalKey> allKeys = new HashSet<>();
		allKeys.addAll(getWarpedSignalKeys());
		allKeys.addAll(other.getWarpedSignalKeys());
		
		for(WarpedSignalKey k : allKeys) {
			if(!images.containsKey(k))
					return false;
			if(!other.images.containsKey(k))
				return false;
			byte[] b0 = images.get(k);
			byte[] b1 = other.images.get(k);
			
			if(Arrays.hashCode(b0)!=Arrays.hashCode(b1))
				return false;
		}
		
		return Objects.equals(id, other.id) 
				&& Objects.equals(targetNames, other.targetNames) && Objects.equals(widths, other.widths);
	}

	
}
