package com.bmskinner.nma.io;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import org.jdom2.Document;
import org.jdom2.Element;

import com.bmskinner.nma.analysis.AbstractAnalysisMethod;
import com.bmskinner.nma.analysis.DefaultAnalysisResult;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.io.Io.Importer;

/**
 * Remap landmarks in XML nmd files
 * 
 * @author bs19022
 *
 */
public class XMLLandmarkRemappingMethod extends AbstractAnalysisMethod implements Importer {

	private static final Logger LOGGER = Logger
			.getLogger(XMLLandmarkRemappingMethod.class.getName());

	private final Document landmarkSource;
	private final Document landmarkTarget;
	private final File outputFile;

	private int totalCells = 0;
	private int remappedCells = 0;

	/**
	 * Construct with XMl documents containing equivalent datasets
	 * 
	 * @param landmarkSource the source XML document with correct landmarks
	 * @param landmarkTarget the target XML document with incorrect landmarks
	 * @param outputFile     where the updated target document should be saved
	 */
	public XMLLandmarkRemappingMethod(final Document landmarkSource,
			final Document landmarkTarget, File outputFile) {
		super();

		this.landmarkSource = landmarkSource;
		this.landmarkTarget = landmarkTarget;
		this.outputFile = outputFile;
	}

	@Override
	public IAnalysisResult call() throws Exception {

		// Find all the nuclei in the source landmarks
		List<Element> sourceCells = landmarkSource.getRootElement()
				.getChild("CellCollection")
				.getChildren("Cell")
				.stream()
				.flatMap(c -> c.getChildren("Nucleus").stream())
				.toList();

		// Find all the nuclei in the target document
		List<Element> targetCells = landmarkTarget.getRootElement()
				.getChild("CellCollection")
				.getChildren("Cell")
				.stream()
				.flatMap(c -> c.getChildren("Nucleus").stream())
				.toList();

		for (Element nucleusElement : targetCells) {
			updateLandmarkLocation(nucleusElement, sourceCells);
		}

		// Write the updated XML to a new file
		XMLWriter.writeXML(landmarkTarget, outputFile);

		LOGGER.fine(() -> "Ramapped " + remappedCells + " of " + totalCells + " nuclei");

		return new DefaultAnalysisResult((IAnalysisDataset) null);
	}

	private void updateLandmarkLocation(Element targetNucleus, List<Element> sourceNuclei) {

		totalCells++;

		Element com = targetNucleus.getChild("CoM");
		Element xpoints = targetNucleus.getChild("xpoints");
		Element ypoints = targetNucleus.getChild("ypoints");

		// Check every source nucleus for a match
		for (Element sourceNucleus : sourceNuclei) {
			Element scom = sourceNucleus.getChild("CoM");
			Element sxpoints = sourceNucleus.getChild("xpoints");
			Element sypoints = sourceNucleus.getChild("ypoints");

			Optional<Element> rpElement = sourceNucleus.getChildren("Orientation")
					.stream()
					.filter(e -> e.getAttributeValue("name").equals("REFERENCE"))
					.findFirst();

			if (rpElement.isEmpty()) // skip cells without defined RP, can't copy segments
				continue;

			String rpName = rpElement.get().getAttributeValue("value");

			// When a match is found, go through the landmarks
			if (scom.getAttributeValue("x").equals(com.getAttributeValue("x"))
					&& scom.getAttributeValue("y").equals(com.getAttributeValue("y"))
					&& sxpoints.getText().equals(xpoints.getText())
					&& sypoints.getText().equals(ypoints.getText())) {

				// The cell coordinates match, proceed

				List<Element> targetSegments = targetNucleus.getChildren("Segment");
				List<Element> sourceLandmarks = sourceNucleus.getChildren("Landmark");
				List<Element> sourceSegments = sourceNucleus.getChildren("Segment");

				LOGGER.finer("Match found for target nucleus "
						+ targetNucleus.getAttributeValue("id") + " with source "
						+ sourceNucleus.getAttributeValue("id"));

				// Match the segment IDs between cells. We start from the RP where we
				// know the index
				Map<String, String> targetIds = matchSegmentIds(targetNucleus,
						sourceNucleus, rpName);

				// Copy the segmentation pattern so that the RP matches a
				// segment boundary. Ensure this is before the landmarks are changed
				if (targetSegments.size() != sourceSegments.size()) {
					LOGGER.warning(
							"Cannot update segment indexes, different number between nmds");
					continue;
				}
				// Now use this map to update all the segment indexes
				updateSegmentIds(targetNucleus, sourceNucleus, targetIds);

				List<Element> landmarks = targetNucleus.getChildren("Landmark");

				// Find the equivalent landmark in the target nucleus
				for (Element lmElement : sourceLandmarks) {

					Optional<Element> match = landmarks.stream()
							.filter(lm -> lm.getAttributeValue("name")
									.equals(lmElement.getAttributeValue("name")))
							.findFirst();

					// Update the landmark to match the index in the source nucleus
					if (match.isPresent()) {

						String lmName = match.get().getAttributeValue("name");
						String targetValue = match.get().getAttributeValue("index");
						String sourceValue = lmElement.getAttributeValue("index");

						// update the landmark index
						if (!targetValue.equals(sourceValue)) {
							match.get().setAttribute("index", sourceValue);
							LOGGER.finer(
									"Updated landmark " + match.get().getAttributeValue("name")
											+ " from " + targetValue + " to " + sourceValue);
						}

					} else {
						LOGGER.finer(
								"Landmark " + lmElement.getAttributeValue("name") + " not found");
					}
				}

				remappedCells++;
				// Nucleus is done, stop
				return;
			}
		}
	}

	private Map<String, String> matchSegmentIds(Element targetNucleus, Element sourceNucleus,
			String rpName) {

		List<Element> targetLandmarks = targetNucleus.getChildren("Landmark");
		List<Element> sourceLandmarks = sourceNucleus.getChildren("Landmark");

		// Find the RPs
		Optional<Element> targetRpElement = targetLandmarks.stream()
				.filter(e -> e.getAttributeValue("name").equals(rpName))
				.findFirst();

		Optional<Element> sourceRpElement = sourceLandmarks.stream()
				.filter(e -> e.getAttributeValue("name").equals(rpName))
				.findFirst();

		// Note the indexes of the RPs - used to find the equivalent segments
		String targetRpValue = targetRpElement.get().getAttributeValue("index");
		String sourceRpValue = sourceRpElement.get().getAttributeValue("index");

		// Create the map of segment ids
		Map<String, String> targetIds = new HashMap<>();
		List<Element> targetSegments = targetNucleus.getChildren("Segment");
		List<Element> sourceSegments = sourceNucleus.getChildren("Segment");

		Optional<Element> rpSegTarget = targetSegments.stream()
				.filter(s -> s.getAttributeValue("start").equals(targetRpValue))
				.findFirst();
		Optional<Element> rpSegSource = sourceSegments.stream()
				.filter(s -> s.getAttributeValue("start").equals(sourceRpValue))
				.findFirst();

		LOGGER.finer(() -> "Looking for segments matching target index " + targetRpValue
				+ " to source index "
				+ sourceRpValue);

		// Put the RP into the map - key is target, value is source
		targetIds.put(rpSegTarget.get().getAttributeValue("id"),
				rpSegSource.get().getAttributeValue("id"));

		// Now iterate through all of them, following the links
		String targetPrevEndIndex = rpSegTarget.get().getAttributeValue("end");
		String sourcePrevEndIndex = rpSegSource.get().getAttributeValue("end");

		// continue until all segments added
		while (targetIds.size() < targetSegments.size()) {

			final String tpi = targetPrevEndIndex;
			final String spi = sourcePrevEndIndex;

			// Find the segment whose start matches the previous end
			Optional<Element> segTarget = targetSegments.stream()
					.filter(s -> s.getAttributeValue("start").equals(tpi))
					.findFirst();
			Optional<Element> segSource = sourceSegments.stream()
					.filter(s -> s.getAttributeValue("start").equals(spi))
					.findFirst();

			targetIds.put(segTarget.get().getAttributeValue("id"),
					segSource.get().getAttributeValue("id"));

			targetPrevEndIndex = segTarget.get().getAttributeValue("end");
			sourcePrevEndIndex = segSource.get().getAttributeValue("end");
		}

		return targetIds;
	}

	/**
	 * Given two nucleus elements, and a map of which segments match, update the
	 * segment boundaries in the target to match the source
	 * 
	 * @param targetNucleus
	 * @param sourceNucleus
	 * @param segmentMap
	 */
	private void updateSegmentIds(Element targetNucleus, Element sourceNucleus,
			Map<String, String> segmentMap) {
		List<Element> targetSegments = targetNucleus.getChildren("Segment");
		List<Element> sourceSegments = sourceNucleus.getChildren("Segment");

		for (Element targetSeg : targetSegments) {
			String targetId = targetSeg.getAttributeValue("id");
			String sourceId = segmentMap.get(targetId);

			Optional<Element> sourceSeg = sourceSegments.stream()
					.filter(s -> s.getAttributeValue("id").equals(sourceId))
					.findFirst();

			if (sourceSeg.isPresent()) {
				String start = sourceSeg.get().getAttributeValue("start");
				String end = sourceSeg.get().getAttributeValue("end");
				targetSeg.setAttribute("start", start);
				targetSeg.setAttribute("end", end);

				LOGGER.finer(
						() -> "Setting target segment " + sourceId + " to " + start + " - " + end);

			}

		}
	}

}
