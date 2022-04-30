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
package com.bmskinner.nma.components.rules;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.jdom2.Element;

import com.bmskinner.nma.components.Version;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.profiles.DefaultLandmark;
import com.bmskinner.nma.components.profiles.Landmark;
import com.bmskinner.nma.io.XMLReader.XMLReadingException;
import com.bmskinner.nma.io.XmlSerializable;

/**
 * This holds the rulesets for identifying each of the landmarks in a profile.
 * Multiple RuleSets can be combined for each tag, allowing multiple profile
 * types to be used. Designed to be stored within a cell collection.
 * 
 * It also stores which landmarks should be used for orientation of the nucleus.
 * This replaces the v1.x.x series of dedicated classes for each type of nucleus
 * to be analysed.
 * 
 * @author ben
 *
 */
public class RuleSetCollection implements XmlSerializable {

	private static final String XML_VALUE = "value";

	private static final String XML_MEASUREMENT = "Measurement";

	private static final String XML_ORIENT = "Orient";

	private static final String XML_PRIORITY_AXIS = "axis";

	private static final String XML_RULE_APPLICATION_TYPE = "application";
	private static final String XML_RULE_VERSION = "version";

	private static final String XML_RULE_SET_COLLECTION = "RuleSetCollection";
	private static final String XML_RULESET = "Ruleset";

	private static final String XML_LANDMARK = "Landmark";

	private static final String XML_NAME = "name";

	private static final Logger LOGGER = Logger.getLogger(RuleSetCollection.class.getName());

	private final String name;

	private final Map<Landmark, List<RuleSet>> map = new HashMap<>();

	/** Store the landmarks to be used for orientation */
	private final Map<@NonNull OrientationMark, Landmark> orientationMarks = new EnumMap<>(
			OrientationMark.class);

	/** Track which measurements should be performed for these nuclei */
	private final Set<Measurement> validMeasurements = new HashSet<>();

	private final PriorityAxis priorityAxis;

	private final RuleApplicationType ruleApplicationType;

	/**
	 * Create a new empty collection. Specify the landmarks that should be used
	 * preferentially by nuclei for orientation
	 */
	public RuleSetCollection(@NonNull String name, @NonNull Landmark rp, @Nullable Landmark left,
			@Nullable Landmark right, @Nullable Landmark top, @Nullable Landmark bottom,
			@Nullable Landmark seondaryX,
			@Nullable Landmark seondaryY, @Nullable PriorityAxis priorityAxis,
			RuleApplicationType type) {
		this.name = name;

		orientationMarks.put(OrientationMark.REFERENCE, rp);

		if (left != null)
			orientationMarks.put(OrientationMark.LEFT, left);

		if (right != null)
			orientationMarks.put(OrientationMark.RIGHT, right);

		if (top != null)
			orientationMarks.put(OrientationMark.TOP, top);

		if (bottom != null)
			orientationMarks.put(OrientationMark.BOTTOM, bottom);

		if (seondaryX != null)
			orientationMarks.put(OrientationMark.X, seondaryX);

		if (seondaryY != null)
			orientationMarks.put(OrientationMark.Y, seondaryY);

		this.priorityAxis = priorityAxis;
		this.ruleApplicationType = type;
	}

	protected RuleSetCollection(RuleSetCollection rsc) {
		name = rsc.name;

		for (Entry<Landmark, List<RuleSet>> entry : rsc.map.entrySet()) {
			map.computeIfAbsent(entry.getKey(), v -> new ArrayList<>());
			for (RuleSet rs : entry.getValue())
				map.get(entry.getKey()).add(rs.duplicate());
		}

		for (Entry<OrientationMark, Landmark> entry : rsc.orientationMarks.entrySet()) {
			orientationMarks.put(entry.getKey(), entry.getValue());
		}

		for (Measurement m : rsc.validMeasurements) {
			validMeasurements.add(m);
		}

		priorityAxis = rsc.priorityAxis;
		ruleApplicationType = rsc.ruleApplicationType;
	}

	/**
	 * Construct from an XML element. Use for unmarshalling. The element should
	 * conform to the specification in {@link XmlSerializable}.
	 * 
	 * @param e the XML element containing the data.
	 * @throws XMLReadingException
	 * @throws ComponentCreationException
	 */
	public RuleSetCollection(Element e) throws ComponentCreationException {
		name = e.getAttributeValue(XML_NAME);
		priorityAxis = PriorityAxis.valueOf(e.getAttributeValue(XML_PRIORITY_AXIS));
		ruleApplicationType = RuleApplicationType
				.valueOf(e.getAttributeValue(XML_RULE_APPLICATION_TYPE));

		// Add the rulesets
		for (Element t : e.getChildren(XML_LANDMARK)) {

			String lmName = t.getAttributeValue(XML_NAME);
			Landmark l = new DefaultLandmark(lmName);

			List<RuleSet> rules = new ArrayList<>();
			for (Element r : t.getChildren(XML_RULESET)) {
				rules.add(new RuleSet(r));
			}
			map.put(l, rules);
		}

		// Add the orientation landmarks
		for (Element om : e.getChildren(XML_ORIENT)) {
			orientationMarks.put(OrientationMark.valueOf(om.getAttributeValue(XML_NAME)),
					map.keySet().stream()
							.filter(l -> l.getName().equals(om.getAttributeValue(XML_VALUE)))
							.findFirst().orElse(null));
		}

		for (Element m : e.getChildren(XML_MEASUREMENT)) {
			validMeasurements.add(Measurement.of(m.getText()));
		}

	}

	public RuleSetCollection duplicate() {
		return new RuleSetCollection(this);
	}

	public String getName() {
		return name;
	}

	public Set<Measurement> getMeasurableValues() {
		return validMeasurements;
	}

	public void addMeasurableValue(Measurement m) {
		validMeasurements.add(m);
	}

	public RuleApplicationType getApplicationType() {
		return ruleApplicationType;
	}

	/**
	 * Test if the rules specify a preferred landmark to the left or right of the
	 * CoM
	 * 
	 * @return
	 */
	public boolean isAsymmetricX() {
		return orientationMarks.containsKey(OrientationMark.LEFT)
				|| orientationMarks.containsKey(OrientationMark.RIGHT);
	}

	/**
	 * Test if the rules specify a preferred landmark above or below the CoM
	 * 
	 * @return
	 */
	public boolean isAsymmetricY() {
		return orientationMarks.containsKey(OrientationMark.TOP)
				|| orientationMarks.containsKey(OrientationMark.BOTTOM);
	}

	/**
	 * Test if there is asymmetry in either axis
	 * 
	 * @return
	 */
	public boolean isAsymmetric() {
		return isAsymmetricX() || isAsymmetricY();
	}

	public Optional<Landmark> getLandmark(OrientationMark s) {
		return Optional.ofNullable(orientationMarks.get(s));
	}

	public Optional<PriorityAxis> getPriorityAxis() {
		return Optional.ofNullable(priorityAxis);
	}

	/**
	 * Remove all the RuleSets for the given tag
	 * 
	 * @param tag
	 */
	public void clearRuleSets(@NonNull OrientationMark tag) {
		Landmark lm = orientationMarks.get(tag);
		map.put(lm, new ArrayList<>());
	}

	public List<RuleSet> removeRuleSets(@NonNull OrientationMark tag) {
		Landmark lm = orientationMarks.get(tag);
		return map.remove(lm);
	}

	/**
	 * Add a ruleset for the given tag
	 * 
	 * @param tag
	 * @param r
	 */
	public void addRuleSet(@NonNull OrientationMark tag, @NonNull RuleSet r) {
		Landmark lm = orientationMarks.get(tag);
		map.computeIfAbsent(lm, k -> new ArrayList<>());
		map.get(lm).add(r);
	}

	/**
	 * Replace existing RuleSets for the given tag with the list
	 * 
	 * @param tag
	 * @param list
	 */
	public void setRuleSets(@NonNull OrientationMark tag, @NonNull List<RuleSet> list) {
		Landmark lm = orientationMarks.get(tag);
		map.put(lm, new ArrayList<>());
		map.get(lm).addAll(list);
	}

	/**
	 * Get the rulesets for the given tag
	 * 
	 * @param om
	 * @param r
	 */
	public List<RuleSet> getRuleSets(@NonNull Landmark l) {
		return map.get(l);

	}

	/**
	 * Get the rulesets for the given tag
	 * 
	 * @param om
	 * @param r
	 */
	public List<RuleSet> getRuleSets(@NonNull OrientationMark om) {
		if (orientationMarks.containsKey(om)) {
			Landmark l = orientationMarks.get(om);
			return map.get(l);
		}
		return new ArrayList<>();
	}

	/**
	 * Get the landmarks that are defined in this rule set collection
	 * 
	 * @return
	 */
	public Set<Landmark> getLandmarks() {
		return map.keySet();
	}

	public Set<OrientationMark> getOrientionMarks() {
		return orientationMarks.keySet();
	}

	/**
	 * Test if the collection has rulesets for the given tag
	 * 
	 * @param tag the tag to check
	 * @return true if rulesets are present, false otherwise
	 */
	public boolean hasRulesets(@NonNull OrientationMark om) {
		if (!orientationMarks.containsKey(om))
			return false;

		if (map.containsKey(orientationMarks.get(om)))
			return false;

		return !map.get(orientationMarks.get(om)).isEmpty();
	}

	/**
	 * Test if the collection is empty
	 * 
	 * @return
	 */
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("RuleSets:\n");
		for (Entry<Landmark, List<RuleSet>> entry : map.entrySet()) {
			b.append(entry.getKey() + ":\n");
			for (RuleSet r : entry.getValue()) {
				b.append(r.toString() + "\n");
			}
		}

		return b.toString();
	}

	@Override
	public Element toXmlElement() {

		Element rootElement = new Element(XML_RULE_SET_COLLECTION)
				.setAttribute(XML_NAME, getName())
				.setAttribute(XML_RULE_APPLICATION_TYPE, getApplicationType().toString())
				.setAttribute(XML_RULE_VERSION, Version.currentVersion().toString());

		if (priorityAxis != null)
			rootElement.setAttribute(XML_PRIORITY_AXIS, priorityAxis.toString());

		// Add any orientation landmarks
		for (Entry<OrientationMark, Landmark> entry : orientationMarks.entrySet()) {

			rootElement
					.addContent(
							new Element(XML_ORIENT)
									.setAttribute(XML_NAME, entry.getKey().name())
									.setAttribute(XML_VALUE, entry.getValue().toString()));
		}

		for (Entry<Landmark, List<RuleSet>> entry : map.entrySet()) {
			Element tagElement = new Element(XML_LANDMARK).setAttribute(XML_NAME,
					entry.getKey().getName());
			for (RuleSet rs : entry.getValue())
				tagElement.addContent(rs.toXmlElement());
			rootElement.addContent(tagElement);
		}

		for (Measurement m : validMeasurements) {
			rootElement.addContent(new Element(XML_MEASUREMENT).setText(m.name()));
		}

		return rootElement;
	}

	@Override
	public int hashCode() {
		return Objects.hash(map, name, orientationMarks, priorityAxis, ruleApplicationType);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RuleSetCollection other = (RuleSetCollection) obj;
		return Objects.equals(map, other.map) && Objects.equals(name, other.name)
				&& Objects.equals(orientationMarks, other.orientationMarks)
				&& priorityAxis == other.priorityAxis
				&& ruleApplicationType == other.ruleApplicationType;
	}

	/**
	 * Create a RuleSetCollection for mouse sperm nuclei
	 * 
	 * @return
	 */
	public static RuleSetCollection mouseSpermRuleSetCollection() {

		Landmark rp = new DefaultLandmark("Tip of hook");
		Landmark tv = new DefaultLandmark("Ventral upper");
		Landmark bv = new DefaultLandmark("Ventral lower");
		Landmark op = new DefaultLandmark("Tail socket");

		RuleSetCollection r = new RuleSetCollection("Mouse sperm", rp, rp, null, tv, bv, null, op,
				PriorityAxis.Y,
				RuleApplicationType.VIA_MEDIAN);

		r.addRuleSet(OrientationMark.REFERENCE, RuleSet.mouseSpermRPRuleSet());
		r.addRuleSet(OrientationMark.Y, RuleSet.mouseSpermOPRuleSet());
		r.addRuleSet(OrientationMark.TOP, RuleSet.mouseSpermTVRuleSet());
		r.addRuleSet(OrientationMark.BOTTOM, RuleSet.mouseSpermBVRuleSet());

		for (Measurement m : Measurement.getRoundNucleusStats())
			r.addMeasurableValue(m);

		r.addMeasurableValue(Measurement.BODY_WIDTH);
		r.addMeasurableValue(Measurement.HOOK_LENGTH);
		return r;
	}

	/**
	 * Create a RuleSetCollection for pig sperm nuclei
	 * 
	 * @return
	 */
	public static RuleSetCollection pigSpermRuleSetCollection() {

		Landmark rp = new DefaultLandmark("Tail socket");
		Landmark lh = new DefaultLandmark("Body left");
		Landmark rh = new DefaultLandmark("Body right");

		RuleSetCollection r = new RuleSetCollection("Pig sperm", rp, lh, rh, null, null, null, rp,
				PriorityAxis.X,
				RuleApplicationType.VIA_MEDIAN);

		r.addRuleSet(OrientationMark.REFERENCE, RuleSet.pigSpermRPRuleSet());
		r.addRuleSet(OrientationMark.LEFT, RuleSet.pigSpermLHRuleSet());
		r.addRuleSet(OrientationMark.RIGHT, RuleSet.pigSpermRHRuleSet());

		for (Measurement m : Measurement.getRoundNucleusStats())
			r.addMeasurableValue(m);

		return r;
	}

	/**
	 * Create a RuleSetCollection for round nuclei
	 * 
	 * @return
	 */
	public static RuleSetCollection roundRuleSetCollection() {

		Landmark rp = new DefaultLandmark("Longest axis");

		RuleSetCollection r = new RuleSetCollection("Round", rp, null, null, null, rp, null, rp,
				PriorityAxis.Y,
				RuleApplicationType.VIA_MEDIAN);

		r.addRuleSet(OrientationMark.REFERENCE, RuleSet.roundRPRuleSet());

		for (Measurement m : Measurement.getRoundNucleusStats())
			r.addMeasurableValue(m);
		return r;
	}
}
