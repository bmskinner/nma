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
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Element;

import com.bmskinner.nma.components.XMLNames;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.io.XmlSerializable;

/**
 * A collection of rules that can be followed to identify a feature in a
 * profile. Rules will be applied sequentially: if rule n limits indexes, rule
 * n+1 will only be applied to those remaining indexes
 * 
 * @author Ben Skinner
 *
 */
public class RuleSet implements XmlSerializable {

	/** the type of profile to which the rules apply */
	private final ProfileType type;

	private final List<Rule> rules = new ArrayList<>();

	public RuleSet(final ProfileType type) {
		this.type = type;
	}

	protected RuleSet(RuleSet rs) {
		type = rs.type;
		for (Rule r : rs.rules) {
			rules.add(r.duplicate());
		}
	}

	/**
	 * Construct from an XML element. Use for unmarshalling. The element should
	 * conform to the specification in {@link XmlSerializable}.
	 * 
	 * @param e the XML element containing the data.
	 */
	public RuleSet(@NonNull Element e) {
		type = ProfileType.fromString(e.getAttributeValue(XMLNames.XML_RULE_TYPE));

		for (Element r : e.getChildren(XMLNames.XML_RULE)) {
			addRule(new Rule(r));
		}
	}

	public RuleSet duplicate() {
		return new RuleSet(this);
	}

	public void addRule(@NonNull final Rule r) {
		rules.add(r);
	}

	public @NonNull List<Rule> getRules() {
		return rules;
	}

	public ProfileType getType() {
		return type;
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append(type + System.getProperty("line.separator"));
		for (Rule r : rules) {
			b.append(r.toString() + System.getProperty("line.separator"));
		}
		return b.toString();
	}

	@Override
	@NonNull public Element toXmlElement() {
		Element e = new Element(XMLNames.XML_RULESET)
				.setAttribute(XMLNames.XML_RULE_TYPE, getType().toString());

		for (Rule r : getRules()) {
			e.addContent(r.toXmlElement());
		}
		return e;
	}

	@Override
	public int hashCode() {
		return Objects.hash(rules, type);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RuleSet other = (RuleSet) obj;
		return Objects.equals(rules, other.rules) && type == other.type;
	}

	/**
	 * Create a RuleSet that describes how to find the RP in mouse sperm nuclear
	 * profiles
	 * 
	 * @return
	 */
	public static RuleSet mouseSpermRPRuleSet() {
		return new RuleSetBuilder(ProfileType.ANGLE).isMinimum().build();
	}

	/**
	 * Create a RuleSet that describes how to find the OP in mouse sperm nuclear
	 * profiles
	 * 
	 * @return
	 */
	public static RuleSet mouseSpermOPRuleSet() {
		return new RuleSetBuilder(ProfileType.ANGLE)
				.isLocalMinimum()
				.indexIsMoreThan(0.2) // assumes the profile is indexed on the RP
				.indexIsLessThan(0.6)
				.isMinimum()
				.build();
	}

	/**
	 * Create a RuleSet that describes how to find the top vertical in mouse sperm
	 * nuclear profiles
	 * 
	 * @return
	 */
	public static RuleSet mouseSpermTVRuleSet() {
		return new RuleSetBuilder(ProfileType.ANGLE).isConstantRegionAtValue(180, 10, 10)
				.isFirstIndexInRegion().build();
	}

	/**
	 * Create a RuleSet that describes how to find the bottom vertical in mouse
	 * sperm nuclear profiles
	 * 
	 * @return
	 */
	public static RuleSet mouseSpermBVRuleSet() {
		return new RuleSetBuilder(ProfileType.ANGLE).isConstantRegionAtValue(180, 10, 10)
				.isLastIndexInRegion().build();
	}

	/**
	 * Create a RuleSet that describes how to find the RP in pig sperm nuclear
	 * profiles with poorly identifiable tails
	 * 
	 * @return
	 */
	public static RuleSet pigSpermRPRuleSet() {
		return new RuleSetBuilder(ProfileType.ANGLE)
				.isMinimum() // This will find one of the tail dimples
				.indexIsWithinFractionOf(0.07) // Expand to include indexes around the dimple
				.isLocalMaximum() // Select the first local max point to avoid shoulders
				.build();
	}

	/**
	 * Create a RuleSet that describes how to find the min diameter left point in
	 * pig sperm
	 * 
	 * @return
	 */
	public static RuleSet pigSpermLHRuleSet() {
		return new RuleSetBuilder(ProfileType.DIAMETER)
				.indexIsLessThan(0.5)
				.isMinimum()
				.build();
	}

	/**
	 * Create a RuleSet that describes how to find the min diameter right point in
	 * pig sperm
	 * 
	 * @return
	 */
	public static RuleSet pigSpermRHRuleSet() {
		return new RuleSetBuilder(ProfileType.DIAMETER)
				.indexIsMoreThan(0.5)
				.isMinimum()
				.build();
	}

	/**
	 * Create a RuleSet that describes how to find the RP in round nucleus profiles
	 * 
	 * @return
	 */
	public static RuleSet roundRPRuleSet() {
		return new RuleSetBuilder(ProfileType.DIAMETER).isMaximum().build();
	}

	/**
	 * Create a RuleSet that describes how to find the RP in round nucleus profiles
	 * 
	 * @return
	 */
	public static RuleSet roundOPRuleSet() {
		return new RuleSetBuilder(ProfileType.DIAMETER).isMaximum().build();
	}

}
