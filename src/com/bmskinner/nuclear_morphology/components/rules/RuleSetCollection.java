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
package com.bmskinner.nuclear_morphology.components.rules;

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

import com.bmskinner.nuclear_morphology.components.cells.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.profiles.DefaultLandmark;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.LandmarkType;
import com.bmskinner.nuclear_morphology.io.XMLReader.XMLReadingException;
import com.bmskinner.nuclear_morphology.io.XmlSerializable;

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

	private static final String XML_PRIORITY_AXIS = "PriorityAxis";

	private static final String XML_RULE_APPLICATION_TYPE = "RuleApplicationType";

	private static final String XML_RULE_SET_COLLECTION = "RuleSetCollection";
	private static final String XML_RULESET = "Ruleset";

	private static final String XML_TYPE = "type";

	private static final String XML_LANDMARK = "Landmark";

	private static final String XML_NAME = "name";

	private static final Logger LOGGER = Logger.getLogger(RuleSetCollection.class.getName());

	private final String name;

	private final Map<Landmark, List<RuleSet>> map = new HashMap<>();

	/** Store the landmarks to be used for orientation */
	private final Map<OrientationMark, Landmark> orientationMarks = new EnumMap<>(OrientationMark.class);

	/** Track which measurements should be performed for these nuclei */
	private final Set<Measurement> validMeasurements = new HashSet<>();

	private final PriorityAxis priorityAxis;

	private final RuleApplicationType ruleApplicationType;

	/**
	 * Create a new empty collection. Specify the landmarks that should be used
	 * preferentially by nuclei for orientation
	 */
	public RuleSetCollection(@NonNull String name, @NonNull Landmark rp, @Nullable Landmark left,
			@Nullable Landmark right, @Nullable Landmark top, @Nullable Landmark bottom, @Nullable Landmark seondaryX,
			@Nullable Landmark seondaryY, @Nullable PriorityAxis priorityAxis, RuleApplicationType type) {
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
		ruleApplicationType = RuleApplicationType.valueOf(e.getAttributeValue(XML_RULE_APPLICATION_TYPE));

		// Add the rulesets
		for (Element t : e.getChildren(XML_LANDMARK)) {

			String lmName = t.getAttributeValue(XML_NAME);
			LandmarkType lmType = LandmarkType.valueOf(t.getAttributeValue(XML_TYPE));
			Landmark l = Landmark.of(lmName, lmType);

			List<RuleSet> rules = new ArrayList<>();
			for (Element r : t.getChildren(XML_RULESET)) {
				rules.add(new RuleSet(r));
			}
			map.put(l, rules);
		}

		// Add the orientation landmarks
		for (Element om : e.getChildren("Orient")) {
			orientationMarks.put(OrientationMark.valueOf(om.getAttributeValue("name")), map.keySet().stream()
					.filter(l -> l.getName().equals(om.getAttributeValue("value"))).findFirst().orElse(null));
		}

		for (Element m : e.getChildren("Measurement")) {
			validMeasurements.add(Measurement.of(m.getText()));
		}

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
		return map.containsKey(OrientationMark.LEFT) || map.containsKey(OrientationMark.RIGHT);
	}

	/**
	 * Test if the rules specify a preferred landmark above or below the CoM
	 * 
	 * @return
	 */
	public boolean isAsymmetricY() {
		return map.containsKey(OrientationMark.TOP) || map.containsKey(OrientationMark.BOTTOM);
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
	public void clearRuleSets(@NonNull Landmark tag) {
		map.put(tag, new ArrayList<>());
	}

	public List<RuleSet> removeRuleSets(@NonNull Landmark tag) {
		return map.remove(tag);
	}

	/**
	 * Add a ruleset for the given tag
	 * 
	 * @param tag
	 * @param r
	 */
	public void addRuleSet(@NonNull Landmark tag, @NonNull RuleSet r) {
		map.computeIfAbsent(tag, k -> new ArrayList<>());
		map.get(tag).add(r);
	}

	/**
	 * Replace existing RuleSets for the given tag with the list
	 * 
	 * @param tag
	 * @param list
	 */
	public void setRuleSets(@NonNull Landmark tag, @NonNull List<RuleSet> list) {
		map.put(tag, new ArrayList<>());
		map.get(tag).addAll(list);
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
	 * @param tag
	 * @param r
	 */
	public List<RuleSet> getRuleSets(@NonNull OrientationMark tag) {
		if (orientationMarks.containsKey(tag)) {
			Landmark l = orientationMarks.get(tag);
			return map.get(tag);
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
	public boolean hasRulesets(@NonNull Landmark tag) {
		if (map.containsKey(tag)) {
			return !map.get(tag).isEmpty();
		}
		return false;
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

		Element rootElement = new Element(XML_RULE_SET_COLLECTION).setAttribute(XML_NAME, getName())
				.setAttribute(XML_RULE_APPLICATION_TYPE, getApplicationType().toString());

		if (priorityAxis != null)
			rootElement.setAttribute(XML_PRIORITY_AXIS, priorityAxis.toString());

		// Add the landmark rule definitions
//		for (Landmark t : getLandmarks()) {
//			Element tagElement = new Element(XML_LANDMARK).setAttribute(XML_NAME, t.getName()).setAttribute(XML_TYPE,
//					t.type().toString());
//
//			for (RuleSet rs : getRuleSets(t)) {
//				tagElement.addContent(rs.toXmlElement());
//			}
//			rootElement.addContent(tagElement);
//		}

		// Add any orientation landmarks
		for (Entry<OrientationMark, Landmark> entry : orientationMarks.entrySet()) {

			rootElement.addContent(new Element("Orient").setAttribute("name", entry.getKey().name())
					.setAttribute("value", entry.getValue().toString()));

			Element tagElement = new Element(XML_LANDMARK).setAttribute(XML_NAME, entry.getValue().getName())
					.setAttribute(XML_TYPE, entry.getValue().type().toString());

			for (RuleSet rs : getRuleSets(entry.getKey())) {
				tagElement.addContent(rs.toXmlElement());
			}

			rootElement.addContent(tagElement);
		}

		for (Measurement m : validMeasurements) {
			rootElement.addContent(new Element("Measurement").setText(m.name()));
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
				&& Objects.equals(orientationMarks, other.orientationMarks) && priorityAxis == other.priorityAxis
				&& ruleApplicationType == other.ruleApplicationType;
	}

	/**
	 * Create a RuleSetCollection for mouse sperm nuclei
	 * 
	 * @return
	 */
	public static RuleSetCollection mouseSpermRuleSetCollection() {

		Landmark rp = new DefaultLandmark("Tip of hook", LandmarkType.CORE);
		Landmark tv = new DefaultLandmark("Ventral upper", LandmarkType.EXTENDED);
		Landmark bv = new DefaultLandmark("Ventral lower", LandmarkType.EXTENDED);
		Landmark op = new DefaultLandmark("Tail socket", LandmarkType.EXTENDED);

		RuleSetCollection r = new RuleSetCollection("Mouse sperm", rp, rp, null, tv, bv, null, op, PriorityAxis.Y,
				RuleApplicationType.VIA_MEDIAN);

		r.addRuleSet(rp, RuleSet.mouseSpermRPRuleSet());
		r.addRuleSet(op, RuleSet.mouseSpermOPRuleSet());
		r.addRuleSet(tv, RuleSet.mouseSpermTVRuleSet());
		r.addRuleSet(bv, RuleSet.mouseSpermBVRuleSet());

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

		Landmark rp = new DefaultLandmark("Tail socket", LandmarkType.CORE);
		Landmark lh = new DefaultLandmark("Body left", LandmarkType.EXTENDED);
		Landmark rh = new DefaultLandmark("Body right", LandmarkType.EXTENDED);

		RuleSetCollection r = new RuleSetCollection("Pig sperm", rp, lh, rh, null, null, null, rp, PriorityAxis.X,
				RuleApplicationType.VIA_MEDIAN);

		r.addRuleSet(rp, RuleSet.pigSpermRPRuleSet());
		r.addRuleSet(lh, RuleSet.pigSpermLHRuleSet());
		r.addRuleSet(rh, RuleSet.pigSpermRHRuleSet());

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

		Landmark rp = new DefaultLandmark("Longest axis", LandmarkType.CORE);

		RuleSetCollection r = new RuleSetCollection("Round", rp, null, null, null, rp, null, rp, PriorityAxis.Y,
				RuleApplicationType.VIA_MEDIAN);

		r.addRuleSet(rp, RuleSet.roundRPRuleSet());

		for (Measurement m : Measurement.getRoundNucleusStats())
			r.addMeasurableValue(m);
		return r;
	}
}
