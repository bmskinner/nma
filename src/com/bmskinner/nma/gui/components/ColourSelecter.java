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
package com.bmskinner.nma.gui.components;

import java.awt.Color;

import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.core.GlobalOptions;

/**
 * Generate colour swatches for display. Uses the accessibility global option to
 * choose swatch.
 * 
 * @author ben
 *
 */
public class ColourSelecter {

	public static final Color DEFAULT_CELL_OUTLINE = Color.CYAN;
	public static final Color DEFAULT_LOBE_OUTLINE = Color.GREEN;

	protected static Color[] segmentColourList = {

			Color.BLUE, Color.ORANGE, Color.GREEN, Color.MAGENTA, Color.DARK_GRAY, Color.CYAN,
			Color.RED, Color.YELLOW,
			Color.PINK, new Color(0, 153, 0), // lime green
			new Color(135, 206, 235) // sky blue
	};

	// Colours for FISH signals in nuclei
	protected static Color[] signalColourList = { Color.RED, Color.GREEN, Color.CYAN, Color.MAGENTA,
			Color.YELLOW,
			Color.LIGHT_GRAY };

	// Color blind friendly swatch
	// See http://optional.is/required/2011/06/20/accessible-color-swatches/
	protected static Color[] optimisedSwatchList = { Color.decode("#fff200"),
			Color.decode("#006f45"),
			Color.decode("#f7941e"), Color.decode("#008fd5"), Color.decode("#abd69c"),
			Color.decode("#741472") };

	// AI generated colour names from a neural network:
	// http://lewisandquark.tumblr.com/post/160776374467/new-paint-colors-invented-by-neural-network
	protected static Color[] aiSwatchList = { new Color(48, 94, 83), // Grade Bat
			new Color(112, 113, 84), // Clardic Fug
			new Color(216, 200, 185), // Stummy Beige
			new Color(61, 63, 66), // Dorkwood
			new Color(176, 99, 108), // Grass Bat
			new Color(204, 205, 194), // Sindis Poop
			new Color(190, 164, 116), // Turdly
			new Color(201, 199, 165), // Snowbonk
			new Color(197, 162, 171) // Stanky Bean

	};

	protected static Color[] blackList = { Color.BLACK };

	/**
	 * The available colour choices, used for default dataset, segment and signal
	 * colours
	 * 
	 * @author bms41
	 *
	 */
	public enum ColourSwatch {
		REGULAR_SWATCH("Regular"), NO_SWATCH("No colours"), ACCESSIBLE_SWATCH("Accessible colours");

		private final String name;

		ColourSwatch(String value) {
			this.name = value;
		}

		@Override
		public String toString() {
			return this.name;
		}
	}

	/**
	 * Get an appropriate segment colour for the given number, and the current
	 * global swatch
	 * 
	 * @param i the number of the colour to return
	 * @return a colour
	 */
	public static Color getColor(int i) {
		return getColor(i, GlobalOptions.getInstance().getSwatch());
	}

	public static Color getColor(int i, ColourSwatch swatch) {
		switch (swatch) {
		case ACCESSIBLE_SWATCH:
			return getOptimisedColor(i);
		case NO_SWATCH:
			return Color.BLACK;
		case REGULAR_SWATCH:
			return getRegularColor(i);
		default:
			return getRegularColor(i);
		}
	}

	/**
	 * Get the appropriate colour for the given tag
	 * 
	 * @param tag the tag to get a color for
	 * @return the colour
	 */
	public static Color getColour(OrientationMark tag) {

		if (OrientationMark.BOTTOM.equals(tag) || OrientationMark.TOP.equals(tag)
				|| OrientationMark.LEFT.equals(tag)
				|| OrientationMark.RIGHT.equals(tag))
			return (Color.GREEN);

		if (OrientationMark.REFERENCE.equals(tag))
			return Color.ORANGE;

		if (OrientationMark.X.equals(tag) || OrientationMark.Y.equals(tag))
			return Color.BLUE;

		return Color.BLACK;
	}

	/**
	 * Get an appropriate colour for the given number from the regular colour set.
	 * Loops through 8 colours.
	 * 
	 * @param i the number of the colour to return
	 * @return a colour
	 */
	private static Color getRegularColor(int i) {
		return ColourSelecter.segmentColourList[i % ColourSelecter.segmentColourList.length];
	}

	/**
	 * Get an appropriate colour for the given number. Loops through 6 colours that
	 * have been chosen to be distinguishable in three major types of color
	 * blindness, Deutranopia, Protanopia and Tritanopia.
	 * 
	 * @param i the number of the colour to return
	 * @return a colour
	 */
	private static Color getOptimisedColor(int i) {
		return ColourSelecter.optimisedSwatchList[i
				% ColourSelecter.optimisedSwatchList.length];
	}

	/**
	 * Get a colour for displaying the given channel specifying transparency and
	 * alpha options
	 * 
	 * @param channel      the channel to display
	 * @param transparent  is the colour transparent
	 * @param defaultAlpha the transparency level
	 * @return a colour
	 */
	public static Color getSignalColour(int channel, boolean transparent, int defaultAlpha) {
		Color result;
		Color color = ColourSelecter.signalColourList[channel
				% ColourSelecter.signalColourList.length];
		result = transparent
				? new Color(color.getRed(), color.getGreen(), color.getBlue(), defaultAlpha)
				: color;
		return result;
	}

	/**
	 * Get a colour for displaying the given channel specifying transparency
	 * 
	 * @param channel     the channel to display
	 * @param transparent is the colour transparent
	 * @return a colour with the default transparency
	 */
	public static Color getSignalColour(int channel, boolean transparent) {
		return getSignalColour(channel, transparent, 10);
	}

	/**
	 * Get a colour for displaying the given channel without transparency
	 * 
	 * @param channel the channel to display
	 * @return a solid colour
	 */
	public static Color getSignalColour(int channel) {
		return getSignalColour(channel, false);
	}

	/**
	 * Get the highlight colour for FISH remapping groups. Takes a group number from
	 * 0 and returns the appropriate colour based on the selected swatch
	 * 
	 * @param group the group, zero indexed
	 * @return the colour for the group
	 */
	public static Color getRemappingColour(int group) {
		ColourSwatch swatch = GlobalOptions.getInstance().getSwatch();

		if (ColourSwatch.ACCESSIBLE_SWATCH.equals(swatch)) {
			if (group == 0)
				return Color.CYAN;
			return Color.ORANGE;

		}
		if (group == 0)
			return Color.GREEN;
		return Color.RED;

	}

	/**
	 * Make the given colour transparent
	 * 
	 * @param c
	 * @param alpha
	 * @return
	 */
	public static Color makeTransparent(Color c, int alpha) {
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
	}

	/**
	 * Get a transparent version of a paint
	 * 
	 * @param c            the input colour
	 * @param transparent  flag
	 * @param defaultAlpha the alpha level
	 * @return the new colour
	 */
	public static Color getTransparentColour(Color c, boolean transparent, int defaultAlpha) {
		return transparent ? new Color(c.getRed(), c.getGreen(), c.getBlue(), defaultAlpha)
				: c;
	}

	/**
	 * Get a transparent version of a colour using default transparency (10)
	 * 
	 * @param c           the input colour
	 * @param transparent flag
	 * @return a colour with the default transparency
	 */
	public static Color getTransparentColour(Color c, boolean transparent) {
		return getTransparentColour(c, transparent, 10);
	}
}
