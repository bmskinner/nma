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
 * @author Ben Skinner
 *
 */
public class ColourSelecter {

	public static final Color DEFAULT_CELL_OUTLINE = Color.CYAN;
	public static final Color DEFAULT_LOBE_OUTLINE = Color.GREEN;

	protected static final Color[] DEFAULT_PALETTE = {

			Color.BLUE, Color.ORANGE, Color.GREEN, Color.MAGENTA, Color.DARK_GRAY, Color.CYAN,
			Color.RED, Color.YELLOW,
			Color.PINK, new Color(0, 153, 0), // lime green
			new Color(135, 206, 235) // sky blue
	};

	// Colours for FISH signals in nuclei
	protected static final Color[] FISH_SIGNAL_PALETTE = { Color.RED, Color.GREEN, Color.CYAN,
			Color.MAGENTA,
			Color.YELLOW,
			Color.LIGHT_GRAY };

	// Color blind friendly swatch
	// See http://optional.is/required/2011/06/20/accessible-color-swatches/
	protected static final Color[] ACCESSIBLE_PALETTE = { Color.decode("#fff200"),
			Color.decode("#006f45"),
			Color.decode("#f7941e"), Color.decode("#008fd5"), Color.decode("#abd69c"),
			Color.decode("#741472") };

	/**
	 * Colour palette DARK2 from RColorBrewer
	 * https://cran.r-project.org/web/packages/RColorBrewer/index.html
	 */
	protected static final Color[] DARK2_PALETTE = {
			Color.decode("#1B9E77"),
			Color.decode("#D95F02"),
			Color.decode("#7570B3"),
			Color.decode("#E7298A"),
			Color.decode("#66A61E"),
			Color.decode("#E6AB02"),
			Color.decode("#A6761D"),
			Color.decode("#666666")
	};

	protected static final Color[] BLACK_PALETTE = { Color.BLACK };

	/**
	 * The available colour choices, used for default dataset, segment and signal
	 * colours
	 * 
	 * @author Ben Skinner
	 *
	 */
	public enum ColourSwatch {
		REGULAR_SWATCH("Regular"),
		NO_SWATCH("No colours"),
		ACCESSIBLE_SWATCH("Accessible colours"),
		DARK2_SWATCH("RColorBrewer Dark2");

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
			return getAccessibleColor(i);
		case NO_SWATCH:
			return Color.BLACK;
		case REGULAR_SWATCH:
			return getRegularColor(i);
		case DARK2_SWATCH:
			return getDark2Color(i);
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
		return ColourSelecter.DEFAULT_PALETTE[i % ColourSelecter.DEFAULT_PALETTE.length];
	}

	/**
	 * Get an appropriate colour for the given number. Loops through 6 colours that
	 * have been chosen to be distinguishable in three major types of color
	 * blindness, Deutranopia, Protanopia and Tritanopia.
	 * 
	 * @param i the number of the colour to return
	 * @return a colour
	 */
	private static Color getAccessibleColor(int i) {
		return ColourSelecter.ACCESSIBLE_PALETTE[i
				% ColourSelecter.ACCESSIBLE_PALETTE.length];
	}

	private static Color getDark2Color(int i) {
		return ColourSelecter.DARK2_PALETTE[i
				% ColourSelecter.DARK2_PALETTE.length];
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
		Color color = ColourSelecter.FISH_SIGNAL_PALETTE[channel
				% ColourSelecter.FISH_SIGNAL_PALETTE.length];
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
	 * @param c     the colour to make transparent
	 * @param alpha the alpha level in the range 0-255
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
