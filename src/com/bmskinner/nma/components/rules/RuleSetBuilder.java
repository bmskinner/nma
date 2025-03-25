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
import java.util.logging.Logger;

import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.Rule.RuleType;

/**
 * Use readable instructions to create a ruleset describing how to find a
 * feature in a profile
 * 
 * @author Ben Skinner
 *
 */
public class RuleSetBuilder {
	
	private static final Logger LOGGER = Logger.getLogger(RuleSetBuilder.class.getName());

	private List<Rule>  rules;
	private ProfileType type;

    /**
     * Construct a builder for the given profile type
     * 
     * @param type
     */
    public RuleSetBuilder(final ProfileType type) {
        rules = new ArrayList<>();
        this.type = type;
    }

    /**
     * Add an arbitrary rule
     * 
     * @param r
     * @return
     */
    public RuleSetBuilder addRule(final Rule r) {
        rules.add(r);
        return this;
    }
    
    /**
     * Get the first index in the test profile. This is used to match
     * the reference point; the input profile is assumed to have the RP
     * at index zero.
     * 
     * @return
     */
    public RuleSetBuilder isZeroIndex() {
        Rule r = new Rule(RuleType.IS_ZERO_INDEX, true);
        rules.add(r);
        return this;
    }

    /**
     * Will find the lowest remaining value in the profile after previous rules
     * have been applied
     * 
     * @return
     */
    public RuleSetBuilder isMinimum() {
        Rule r = new Rule(RuleType.IS_MINIMUM, true);
        rules.add(r);
        return this;
    }

    /**
     * Will find the highest remaining value in the profile after previous rules
     * have been applied
     * 
     * @return
     */
    public RuleSetBuilder isMaximum() {
        Rule r = new Rule(RuleType.IS_MAXIMUM, true);
        rules.add(r);
        return this;
    }

    /**
     * The index must be a local minimum. Uses a default smoothing window size
     * of 5
     * 
     * @return
     */
    public RuleSetBuilder isLocalMinimum() {
        return isLocalMinimum(5);
    }

    /**
     * The index must be a local minimum
     * 
     * @param window
     *            the smoothing window size
     * @return
     */
    public RuleSetBuilder isLocalMinimum(double window) {
        Rule r = new Rule(RuleType.IS_LOCAL_MINIMUM, true);
        r.addValue(window);
        rules.add(r);
        return this;
    }

    /**
     * The index must not be a local minimum
     * 
     * @return
     */
    public RuleSetBuilder isNotLocalMinimum() {
        return isNotLocalMinimum(5);
    }

    /**
     * The index must not be a local minimum
     * 
     * @return
     */
    public RuleSetBuilder isNotLocalMinimum(double window) {
        Rule r = new Rule(RuleType.IS_LOCAL_MINIMUM, false);
        r.addValue(window);
        rules.add(r);
        return this;
    }

    /**
     * The index must be a local maximum
     * 
     * @return
     */
    public RuleSetBuilder isLocalMaximum() {
        return isLocalMaximum(5);
    }

    /**
     * The index must be a local maximum
     * 
     * @param window
     *            the smoothing window size
     * @return
     */
    public RuleSetBuilder isLocalMaximum(double window) {
        Rule r = new Rule(RuleType.IS_LOCAL_MAXIMUM, true);
        r.addValue(window);
        rules.add(r);
        return this;
    }

    /**
     * The index must not be a local maximum
     * 
     * @return
     */
    public RuleSetBuilder isNotLocalMaximum() {
        Rule r = new Rule(RuleType.IS_LOCAL_MAXIMUM, false);
        rules.add(r);
        return isNotLocalMaximum(5);
    }

    /**
     * The index must not be a local maximum
     * 
     * @return
     */
    public RuleSetBuilder isNotLocalMaximum(double window) {
        Rule r = new Rule(RuleType.IS_LOCAL_MAXIMUM, false);
        r.addValue(window);
        rules.add(r);
        return this;
    }

    /**
     * The index found must be less than the given proportional distance through
     * the profile (0-1)
     * 
     * @param d
     * @return
     */
    public RuleSetBuilder indexIsLessThan(final double d) {
        if (d < 0 || d > 1) {
            LOGGER.warning("Invalid rule: proportion " + d);
        }
        Rule r = new Rule(RuleType.INDEX_IS_LESS_THAN, d);
        rules.add(r);
        return this;
    }

    /**
     * The index found must be less than the given proportional distance through
     * the profile (0-1)
     * 
     * @param d
     * @return
     */
    public RuleSetBuilder indexIsMoreThan(final double d) {
        if (d < 0 || d > 1) {
            LOGGER.warning("Invalid rule: proportion " + d);
        }
        Rule r = new Rule(RuleType.INDEX_IS_MORE_THAN, d);
        rules.add(r);
        return this;
    }

    /**
     * The value found must be less than the given value
     * 
     * @param d
     * @return
     */
    public RuleSetBuilder valueIsLessThan(final double d) {

        Rule r = new Rule(RuleType.VALUE_IS_LESS_THAN, d);
        rules.add(r);
        return this;
    }

    /**
     * The value found must be less than the given value
     * 
     * @param d
     * @return
     */
    public RuleSetBuilder valueIsMoreThan(final double d) {

        Rule r = new Rule(RuleType.VALUE_IS_MORE_THAN, d);
        rules.add(r);
        return this;
    }

    /**
     * Detects a region of constant value
     * 
     * @param value
     *            the value to remain at
     * @param window
     *            the minimum length of the region
     * @param epsilon
     *            the maximum variation from the value
     * @return
     */
    public RuleSetBuilder isConstantRegionAtValue(final double value, final double window, final double epsilon) {

        Rule r = new Rule(RuleType.IS_CONSTANT_REGION, value);
        r.addValue(window);
        r.addValue(epsilon);
        rules.add(r);
        return this;
    }

    /**
     * Limit values to indexes within a certain profile fraction of valid
     * indexes
     * 
     * @param fraction
     *            the fraction of the profile valid indices lie within on either
     *            side (0-1)
     * @return the builder
     */
    public RuleSetBuilder indexIsWithinFractionOf(final double fraction) {
        Rule r = new Rule(RuleType.INDEX_IS_WITHIN_FRACTION_OF, fraction);
        rules.add(r);
        return this;
    }

    /**
     * Limit values to indexes beyond a certain profile fraction of valid
     * indexes
     * 
     * @param fraction
     *            the fraction of the profile valid indices lie outside on
     *            either side (0-1)
     * @return the builder
     */
    public RuleSetBuilder indexIsOutsideFractionOf(final double fraction) {
        Rule r = new Rule(RuleType.INDEX_IS_OUTSIDE_FRACTION_OF, fraction);
        rules.add(r);
        return this;
    }

    /**
     * For searching boolean profiles
     * 
     * @param d
     * @return
     */
    public RuleSetBuilder isFirstIndexInRegion() {

        Rule r = new Rule(RuleType.FIRST_TRUE, 1d);
        rules.add(r);
        return this;
    }

    /**
     * For searching boolean profiles
     * 
     * @param d
     * @return
     */
    public RuleSetBuilder isLastIndexInRegion() {

        Rule r = new Rule(RuleType.LAST_TRUE, 1d);
        rules.add(r);
        return this;
    }

    /**
     * Invert the currently selected indexes
     * 
     * @return
     */
    public RuleSetBuilder invert() {

        Rule r = new Rule(RuleType.INVERT, 1d);
        rules.add(r);
        return this;
    }

    /**
     * Create the RuleSet
     * 
     * @return
     */
    public RuleSet build() {
        RuleSet r = new RuleSet(type);
        for (Rule rule : rules) {
            r.addRule(rule);
        }
        return r;
    }

}
