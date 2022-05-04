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
package com.bmskinner.nma.components;

import java.awt.Rectangle;
import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.profiles.ProfileIndexFinder;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.DefaultNucleus;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.generic.FloatPoint;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.components.rules.RuleSetCollection;
import com.bmskinner.nma.components.signals.DefaultNuclearSignal;
import com.bmskinner.nma.components.signals.INuclearSignal;

import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.FloatPolygon;

/**
 * Constructs factories for components.
 * 
 * @author ben
 * @since 2.0.0
 *
 */
public class ComponentBuilderFactory {

	private static final Logger LOGGER = Logger.getLogger(ComponentBuilderFactory.class.getName());

	private ComponentBuilderFactory() {
	}

	/**
	 * Factory for nucleus builders. This can be initialised with rulesets and
	 * profile window proportions that are common across all nuclei to be created to
	 * simplify the building code.
	 * 
	 * @author ben
	 * @since 2.0.0
	 *
	 */
	public static class NucleusBuilderFactory {

		private int nucleusCount = 0; // store the number of nuclei created by this factory

		private final RuleSetCollection rsc;
		private final double windowProp;
		private final double scale;

		public NucleusBuilderFactory(@NonNull RuleSetCollection rsc, double prop, double scale) {
			this.rsc = rsc;
			this.windowProp = prop;
			this.scale = scale;
		}

		/**
		 * Create a new builder for a nucleus
		 * 
		 * @return
		 */
		public NucleusBuilder newBuilder() {
			return new NucleusBuilder();
		}

		/**
		 * Builder for nuclei, using global parameters from the enclosing factory
		 * 
		 * @author ben
		 * @since 2.0.0
		 *
		 */
		public class NucleusBuilder {
			private Roi roi = null;
			private File file = null;
			private int channel = -1;
			private IPoint com = null;
			private UUID id = null;
			private int[] original = null;
			private boolean isOffset = false;

			private NucleusBuilder() {
			}

			public NucleusBuilder fromRoi(Roi r) {
				roi = r;
				return this;
			}

			public NucleusBuilder fromPoints(List<IPoint> points) {
				roi = toRoi(points);
				return this;
			}

			public NucleusBuilder withFile(File f) {
				file = f;
				return this;
			}

			public NucleusBuilder withChannel(int i) {
				channel = i;
				return this;
			}

			public NucleusBuilder withCoM(IPoint i) {
				com = i;
				return this;
			}

			public NucleusBuilder withId(UUID u) {
				id = u;
				return this;
			}

			public NucleusBuilder withOriginalPos(int[] pos) {
				original = pos;
				return this;
			}

			public NucleusBuilder offsetToOrigin() {
				isOffset = true;
				return this;
			}

			public Nucleus build() throws ComponentCreationException {
				Rectangle bounds = roi.getBounds();

				if (original == null)
					original = new int[] { (int) roi.getXBase(), (int) roi.getYBase(),
							(int) bounds.getWidth(),
							(int) bounds.getHeight() };

				if (id == null)
					id = UUID.randomUUID();

				int number = nucleusCount++;

				Nucleus n = new DefaultNucleus(roi, com, file, channel, number, rsc);

				if (isOffset) {
					IPoint offsetCoM = new FloatPoint(com.getX() - (int) roi.getXBase(),
							com.getY() - (int) roi.getYBase());
					n.moveCentreOfMass(offsetCoM);
				}

				n.setScale(scale);
				n.createProfiles(windowProp);

				try {
					ProfileIndexFinder.assignLandmarks(n, rsc);

					if (ProfileIndexFinder.shouldReverseProfile(n)) {
						n.reverse();
						n.clearMeasurements();
						n.createProfiles(windowProp); // ensure all profiles match - rare case
						ProfileIndexFinder.assignLandmarks(n, rsc);

					}
					LOGGER.finer(n.getNameAndNumber() + ": Assigned landmarks");
				} catch (MissingComponentException | ProfileException e) {
					LOGGER.fine(() -> "Unable to reverse profile in nucleus");
					throw new ComponentCreationException(e);
				}

				return n;
			}

			private Roi toRoi(List<IPoint> list) {
				float[] xpoints = new float[list.size()];
				float[] ypoints = new float[list.size()];

				for (int i = 0; i < list.size(); i++) {
					IPoint p = list.get(i);
					xpoints[i] = (float) p.getX();
					ypoints[i] = (float) p.getY();
				}

				// If the points are closer than 1 pixel, the float polygon smoothing
				// during object creation may disrupt the border. Ensure the spacing
				// is corrected to something larger. This is the reverse of the
				// smoothing carried out in component creation.
				Roi r = new PolygonRoi(xpoints, ypoints, Roi.POLYGON);
				FloatPolygon smoothed = r.getInterpolatedPolygon(2, false);
				return new PolygonRoi(smoothed.xpoints, smoothed.ypoints, Roi.POLYGON);
			}

		}

	}

	/**
	 * Factory for signal builders. This can be initialised with fields that are
	 * common across all signals to be created to simplify the building code.
	 * 
	 * @author ben
	 * @since 2.0.0
	 *
	 */
	public static class SignalBuilderFactory {

		public SignalBuilderFactory() {
		}

		/**
		 * Create a new builder for a signal
		 * 
		 * @return
		 */
		public SignalBuilder newBuilder() {
			return new SignalBuilder();
		}

		/**
		 * Builder for nuclei, using global parameters from the enclosing factory
		 * 
		 * @author ben
		 * @since 2.0.0
		 *
		 */
		public class SignalBuilder {
			private Roi roi = null;
			private File file = null;
			private int channel = -1;
			private IPoint com = null;
			private UUID id = null;
			private double scale;

			private SignalBuilder() {
			}

			public SignalBuilder fromRoi(Roi r) {
				roi = r;
				return this;
			}

			public SignalBuilder withFile(File f) {
				file = f;
				return this;
			}

			public SignalBuilder withChannel(int i) {
				channel = i;
				return this;
			}

			public SignalBuilder withCoM(IPoint i) {
				com = i;
				return this;
			}

			public SignalBuilder withId(UUID u) {
				id = u;
				return this;
			}

			public SignalBuilder withScale(double d) {
				this.scale = d;
				return this;
			}

			public INuclearSignal build() {
				if (id == null)
					id = UUID.randomUUID();

				INuclearSignal s = new DefaultNuclearSignal(roi, com, file, channel, id);

				s.setScale(scale);

				return s;
			}

		}
	}

	/**
	 * Create a factory for nuclei of the given type
	 * 
	 * @param rsc   the rulesets to use
	 * @param prop  the window proportion
	 * @param scale the scale
	 */
	public static NucleusBuilderFactory createNucleusBuilderFactory(@NonNull RuleSetCollection rsc,
			double prop,
			double scale) {
		return new NucleusBuilderFactory(rsc, prop, scale);
	}

	/**
	 * Create a factory for nuclear signals
	 * 
	 * @param scale the scale
	 */
	public static SignalBuilderFactory createSignalBuilderFactory() {
		return new SignalBuilderFactory();
	}
}
