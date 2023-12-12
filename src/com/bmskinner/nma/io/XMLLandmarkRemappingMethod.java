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
import com.bmskinner.nma.components.XMLNames;
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
				.getChild(XMLNames.XML_CELL_COLLECTION)
				.getChildren(XMLNames.XML_CELL)
				.stream()
				.flatMap(c -> c.getChildren(XMLNames.XML_NUCLEUS).stream())
				.toList();

		// Find all the nuclei in the target document
		List<Element> targetCells = landmarkTarget.getRootElement()
				.getChild(XMLNames.XML_CELL_COLLECTION)
				.getChildren(XMLNames.XML_CELL)
				.stream()
				.flatMap(c -> c.getChildren(XMLNames.XML_NUCLEUS).stream())
				.toList();

		this.fireUpdateProgressTotalLength(targetCells.size());

		for (Element nucleusElement : targetCells) {
			updateLandmarkLocation(nucleusElement, sourceCells);

			this.fireProgressEvent();
		}

		this.fireIndeterminateState();

		// Write the updated XML to a new file
		XMLWriter.writeXML(landmarkTarget, outputFile);

		LOGGER.info(() -> "Remapped " + remappedCells + " of " + totalCells + " nuclei");
		LOGGER.info(() -> "Remapped data saved to " + outputFile.getAbsolutePath());

		return new DefaultAnalysisResult((IAnalysisDataset) null);
	}

	private void updateLandmarkLocation(Element targetNucleus, List<Element> sourceNuclei) {

		totalCells++;

		Element com = targetNucleus.getChild(XMLNames.XML_COM);
		Element xpoints = targetNucleus.getChild(XMLNames.XML_XPOINTS);
		Element ypoints = targetNucleus.getChild(XMLNames.XML_YPOINTS);

		// Check every source nucleus for a match
		for (Element sourceNucleus : sourceNuclei) {
			Element scom = sourceNucleus.getChild(XMLNames.XML_COM);
			Element sxpoints = sourceNucleus.getChild(XMLNames.XML_XPOINTS);
			Element sypoints = sourceNucleus.getChild(XMLNames.XML_YPOINTS);

			Optional<Element> rpElement = sourceNucleus.getChildren(XMLNames.XML_ORIENTATION)
					.stream()
					.filter(e -> e.getAttributeValue(XMLNames.XML_NAME).equals("REFERENCE"))
					.findFirst();

			if (rpElement.isEmpty()) // skip cells without defined RP, can't copy segments
				continue;

			String rpName = rpElement.get().getAttributeValue(XMLNames.XML_VALUE);

			// When a match is found, go through the landmarks
			if (scom.getAttributeValue(XMLNames.XML_X).equals(com.getAttributeValue(XMLNames.XML_X))
					&& scom.getAttributeValue(XMLNames.XML_Y)
							.equals(com.getAttributeValue(XMLNames.XML_Y))
					&& sxpoints.getText().equals(xpoints.getText())
					&& sypoints.getText().equals(ypoints.getText())) {

				// The cell coordinates match, proceed

				List<Element> targetSegments = targetNucleus.getChildren(XMLNames.XML_SEGMENT);
				List<Element> sourceLandmarks = sourceNucleus.getChildren(XMLNames.XML_LANDMARK);
				List<Element> sourceSegments = sourceNucleus.getChildren(XMLNames.XML_SEGMENT);

				LOGGER.finer("Match found for target nucleus "
						+ targetNucleus.getAttributeValue(XMLNames.XML_ID) + " with source "
						+ sourceNucleus.getAttributeValue(XMLNames.XML_ID));

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

				List<Element> landmarks = targetNucleus.getChildren(XMLNames.XML_LANDMARK);

				// Find the equivalent landmark in the target nucleus
				for (Element lmElement : sourceLandmarks) {

					Optional<Element> match = landmarks.stream()
							.filter(lm -> lm.getAttributeValue(XMLNames.XML_NAME)
									.equals(lmElement.getAttributeValue(XMLNames.XML_NAME)))
							.findFirst();

					// Update the landmark to match the index in the source nucleus
					if (match.isPresent()) {

						String lmName = match.get().getAttributeValue(XMLNames.XML_NAME);
						String targetValue = match.get().getAttributeValue(XMLNames.XML_INDEX);
						String sourceValue = lmElement.getAttributeValue(XMLNames.XML_INDEX);

						// update the landmark index
						if (!targetValue.equals(sourceValue)) {
							match.get().setAttribute(XMLNames.XML_INDEX, sourceValue);
							LOGGER.finer(
									"Updated landmark "
											+ match.get().getAttributeValue(XMLNames.XML_NAME)
											+ " from " + targetValue + " to " + sourceValue);
						}

					} else {
						LOGGER.finer(
								"Landmark " + lmElement.getAttributeValue(XMLNames.XML_NAME)
										+ " not found");
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

		List<Element> targetLandmarks = targetNucleus.getChildren(XMLNames.XML_LANDMARK);
		List<Element> sourceLandmarks = sourceNucleus.getChildren(XMLNames.XML_LANDMARK);

		// Find the RPs
		Optional<Element> targetRpElement = targetLandmarks.stream()
				.filter(e -> e.getAttributeValue(XMLNames.XML_NAME).equals(rpName))
				.findFirst();

		Optional<Element> sourceRpElement = sourceLandmarks.stream()
				.filter(e -> e.getAttributeValue(XMLNames.XML_NAME).equals(rpName))
				.findFirst();

		// Note the indexes of the RPs - used to find the equivalent segments
		String targetRpValue = targetRpElement.get().getAttributeValue(XMLNames.XML_INDEX);
		String sourceRpValue = sourceRpElement.get().getAttributeValue(XMLNames.XML_INDEX);

		// Create the map of segment ids
		Map<String, String> targetIds = new HashMap<>();
		List<Element> targetSegments = targetNucleus.getChildren(XMLNames.XML_SEGMENT);
		List<Element> sourceSegments = sourceNucleus.getChildren(XMLNames.XML_SEGMENT);

		Optional<Element> rpSegTarget = targetSegments.stream()
				.filter(s -> s.getAttributeValue(XMLNames.XML_SEGMENT_START).equals(targetRpValue))
				.findFirst();
		Optional<Element> rpSegSource = sourceSegments.stream()
				.filter(s -> s.getAttributeValue(XMLNames.XML_SEGMENT_START).equals(sourceRpValue))
				.findFirst();

		LOGGER.finer(() -> "Looking for segments matching target index " + targetRpValue
				+ " to source index "
				+ sourceRpValue);

		// Put the RP into the map - key is target, value is source
		targetIds.put(rpSegTarget.get().getAttributeValue(XMLNames.XML_ID),
				rpSegSource.get().getAttributeValue(XMLNames.XML_ID));

		// Now iterate through all of them, following the links
		String targetPrevEndIndex = rpSegTarget.get().getAttributeValue(XMLNames.XML_SEGMENT_END);
		String sourcePrevEndIndex = rpSegSource.get().getAttributeValue(XMLNames.XML_SEGMENT_END);

		// continue until all segments added
		while (targetIds.size() < targetSegments.size()) {

			final String tpi = targetPrevEndIndex;
			final String spi = sourcePrevEndIndex;

			// Find the segment whose start matches the previous end
			Optional<Element> segTarget = targetSegments.stream()
					.filter(s -> s.getAttributeValue(XMLNames.XML_SEGMENT_START).equals(tpi))
					.findFirst();
			Optional<Element> segSource = sourceSegments.stream()
					.filter(s -> s.getAttributeValue(XMLNames.XML_SEGMENT_START).equals(spi))
					.findFirst();

			targetIds.put(segTarget.get().getAttributeValue(XMLNames.XML_ID),
					segSource.get().getAttributeValue(XMLNames.XML_ID));

			targetPrevEndIndex = segTarget.get().getAttributeValue(XMLNames.XML_SEGMENT_END);
			sourcePrevEndIndex = segSource.get().getAttributeValue(XMLNames.XML_SEGMENT_END);
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
		List<Element> targetSegments = targetNucleus.getChildren(XMLNames.XML_SEGMENT);
		List<Element> sourceSegments = sourceNucleus.getChildren(XMLNames.XML_SEGMENT);

		for (Element targetSeg : targetSegments) {
			String targetId = targetSeg.getAttributeValue(XMLNames.XML_ID);
			String sourceId = segmentMap.get(targetId);

			Optional<Element> sourceSeg = sourceSegments.stream()
					.filter(s -> s.getAttributeValue(XMLNames.XML_ID).equals(sourceId))
					.findFirst();

			if (sourceSeg.isPresent()) {
				String start = sourceSeg.get().getAttributeValue(XMLNames.XML_SEGMENT_START);
				String end = sourceSeg.get().getAttributeValue(XMLNames.XML_SEGMENT_END);
				targetSeg.setAttribute(XMLNames.XML_SEGMENT_START, start);
				targetSeg.setAttribute(XMLNames.XML_SEGMENT_END, end);

				LOGGER.finer(
						() -> "Setting target segment " + sourceId + " to " + start + " - " + end);

			}

		}
	}

}
