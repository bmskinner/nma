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
package com.bmskinner.nuclear_morphology.visualisation.options;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.ChartPanel;

import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.measure.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.components.rules.OrientationMark;
import com.bmskinner.nuclear_morphology.components.signals.IShellResult.Aggregation;
import com.bmskinner.nuclear_morphology.components.signals.IShellResult.Normalisation;
import com.bmskinner.nuclear_morphology.components.signals.IShellResult.ShrinkType;
import com.bmskinner.nuclear_morphology.components.signals.IWarpedSignal;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.gui.RotationMode;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter.ColourSwatch;
import com.bmskinner.nuclear_morphology.gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;

/**
 * Builder for a ChartOptions object. This simplifies the creation of the
 * options when not all parameters need to be set. It also makes it easier to
 * remember which boolean option is which.
 * 
 * @author bms41
 *
 */
public class ChartOptionsBuilder {

	private DefaultChartOptions options;

	/**
	 * Create a builder. This has options initilised with the current global scale
	 * and swatch
	 */
	public ChartOptionsBuilder() {
		options = new DefaultChartOptions((List<IAnalysisDataset>) null);
		options.setScale(GlobalOptions.getInstance().getScale());
		options.setSwatch(GlobalOptions.getInstance().getSwatch());
	}

	public ChartOptionsBuilder setDatasets(List<IAnalysisDataset> list) {
		options.setDatasets(list);
		return this;
	}

	public ChartOptionsBuilder setDatasets(IAnalysisDataset dataset) {
		List<IAnalysisDataset> list = new ArrayList<IAnalysisDataset>();
		list.add(dataset);
		return this.setDatasets(list);
	}

	public ChartOptionsBuilder setSwatch(ColourSwatch swatch) {
		options.setSwatch(swatch);
		return this;
	}

	public ChartOptionsBuilder setTarget(ChartPanel target) {
		options.setTarget(target);
		return this;
	}

	public ChartOptionsBuilder setNormalised(boolean b) {
		options.setNormalised(b);
		return this;
	}

	public ChartOptionsBuilder setSegID(UUID id) {
		options.setSegID(id);
		return this;
	}

	public ChartOptionsBuilder setModalityPosition(double modalityPosition) {
		options.setModalityPosition(modalityPosition);
		return this;
	}

	public ChartOptionsBuilder setSegPosition(int segPosition) {
		options.setSegPosition(segPosition);
		return this;
	}

	public ChartOptionsBuilder setAlignment(ProfileAlignment alignment) {
		options.setAlignment(alignment);
		return this;
	}

	public ChartOptionsBuilder setLandmark(OrientationMark tag) {
		options.setTag(tag);
		return this;
	}

	public ChartOptionsBuilder setShowMarkers(boolean b) {
		options.setShowMarkers(b);
		return this;
	}

	public ChartOptionsBuilder setShowProfiles(boolean b) {
		options.setShowProfiles(b);
		return this;
	}

	public ChartOptionsBuilder setShowPoints(boolean b) {
		options.setShowPoints(b);
		return this;
	}

	public ChartOptionsBuilder setShowLines(boolean b) {
		options.setShowLines(b);
		return this;
	}

	public ChartOptionsBuilder setShowIQR(boolean b) {
		options.setShowIQR(b);
		return this;
	}

	public ChartOptionsBuilder setShowAnnotations(boolean showAnnotations) {
		options.setShowAnnotations(showAnnotations);
		return this;
	}

	public ChartOptionsBuilder setProfileType(ProfileType type) {
		options.setType(type);
		return this;
	}

	public ChartOptionsBuilder setSignalGroup(UUID group) {
		options.setSignalGroup(group);
		return this;
	}

	public ChartOptionsBuilder setUseDensity(boolean b) {
		options.setUseDensity(b);
		return this;
	}

	public ChartOptionsBuilder addStatistic(Measurement s) {
		options.addStat(s);
		return this;
	}

	public ChartOptionsBuilder setScale(MeasurementScale s) {
		options.setScale(s);
		return this;
	}

	public ChartOptionsBuilder setShowMesh(boolean b) {
		options.setShowMesh(b);
		return this;
	}

	public ChartOptionsBuilder setMeshSize(int i) {
		options.setMeshSize(i);
		return this;
	}

	public ChartOptionsBuilder setShowBounds(boolean b) {
		options.setShowBounds(b);
		return this;
	}

	public ChartOptionsBuilder setShowMeshVertices(boolean b) {
		options.setShowMeshVertices(b);
		return this;
	}

	public ChartOptionsBuilder setShowMeshEdges(boolean b) {
		options.setShowMeshEdges(b);
		return this;
	}

	public ChartOptionsBuilder setShowMeshFaces(boolean b) {
		options.setShowMeshFaces(b);
		return this;
	}

	public ChartOptionsBuilder setStraightenMesh(boolean b) {
		options.setStraightenMesh(b);
		return this;
	}

	public ChartOptionsBuilder setShowXAxis(boolean b) {
		options.setShowXAxis(b);
		return this;
	}

	public ChartOptionsBuilder setShowYAxis(boolean b) {
		options.setShowYAxis(b);
		return this;
	}

	public ChartOptionsBuilder setInvertXAxis(boolean b) {
		options.setInvertXAxis(b);
		return this;
	}

	public ChartOptionsBuilder setInvertYAxis(boolean b) {
		options.setInvertYAxis(b);
		return this;
	}

	public ChartOptionsBuilder setRotationMode(RotationMode r) {
		options.setRotateMode(r);
		return this;
	}

	public ChartOptionsBuilder setCell(ICell c) {
		options.setCell(c);
		return this;
	}

	public ChartOptionsBuilder addCellularComponent(CellularComponent c) {
		options.addComponent(c);
		return this;
	}

	public ChartOptionsBuilder setShowWarp(boolean b) {
		options.setShowWarp(b);
		return this;
	}

	public ChartOptionsBuilder setShowBorderTags(boolean b) {
		options.setShowBorderTags(b);
		return this;
	}

	public ChartOptionsBuilder setShowSignals(boolean b) {
		options.setShowSignals(b);
		return this;
	}

	public ChartOptionsBuilder setAggregation(Aggregation c) {
		options.setAggregation(c);
		return this;
	}

	public ChartOptionsBuilder setNormalisation(Normalisation c) {
		options.setNormalisation(c);
		return this;
	}

	public ChartOptionsBuilder setShrinkType(ShrinkType t) {
		options.setShrinkType(t);
		return this;
	}

	public ChartOptionsBuilder setWarpedSignals(List<IWarpedSignal> ws) {
		options.setWarpedSignals(ws);
		return this;
	}

	/**
	 * Create the options object
	 * 
	 * @return
	 */
	public @NonNull ChartOptions build() {
		return options;
	}

}
