/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package com.bmskinner.nuclear_morphology.charting.options;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jfree.chart.ChartPanel;

import com.bmskinner.nuclear_morphology.analysis.signals.ShellCounter.CountType;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.gui.RotationMode;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter.ColourSwatch;
import com.bmskinner.nuclear_morphology.gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;

/**
 * Builder for a ChartOptions object. This simplifies the creation
 * of the options when not all parameters need to be set. It also makes
 * it easier to remember which boolean option is which.
 * @author bms41
 *
 */
public class ChartOptionsBuilder {
	
	private DefaultChartOptions options;
//	private List<IAnalysisDataset> list = new ArrayList<IAnalysisDataset>();
//	private ColourSwatch swatch        = GlobalOptions.getInstance().getSwatch();
//	private boolean normalised         = false;
//	private ProfileAlignment alignment = ProfileAlignment.LEFT;
//	private Tag tag              = Tag.REFERENCE_POINT;
//	private boolean showMarkers        = false;
//	private boolean hideProfiles       = false;
//	private ProfileType type           = ProfileType.ANGLE;
//	private UUID signalGroup           = null;
//	private boolean useDensity         = false;
//	private List<PlottableStatistic> stats    = new ArrayList<PlottableStatistic>();
//	private MeasurementScale scale     = MeasurementScale.PIXELS;
//	private UUID segID                 = null;
//	private int segPosition            = 0;
//	private double modalityPosition    = 0;
//	private boolean showPoints         = false;
//	private boolean showLines          = true;
//	private boolean showAnnotations    = true;
//	private boolean showBorderTags     = true; // used in cell outlines
//	private boolean showSignals        = true; // used in cell outlines
//	
//	// Options for nucleus mesh creation
//	private boolean showMesh           = false;
//	private boolean showMeshEdges      = true;
//	private boolean showMeshFaces      = false;
//	private boolean straightenMesh     = false;
//	private int     meshSize           = 10;
//	private boolean showBounds         = false; // Bounding box for meshes and warped images
//	
//	// Chart axis options
//	private boolean showXAxis           = true;
//	private boolean showYAxis           = true;
//	private boolean invertXAxis         = false;
//	private boolean invertYAxis         = false;
//	
//	// Cells tab
//	private RotationMode rotateMode     = RotationMode.ACTUAL;
//	private ICell cell                   = null;
//	private CellularComponent component = null;
//	private boolean showWarp            = false;
//	
//	private ChartPanel target           = null;
	
	public ChartOptionsBuilder(){
		options = new DefaultChartOptions(null);
	}
	
	public ChartOptionsBuilder setDatasets(List<IAnalysisDataset> list){
		options.setDatasets(list);
//		this.list = list;
		return this;
	}
	
	public ChartOptionsBuilder setDatasets(IAnalysisDataset dataset){
		List<IAnalysisDataset> list = new ArrayList<IAnalysisDataset>();
		list.add(dataset);
		return this.setDatasets(list);
	}
	
	public ChartOptionsBuilder setSwatch(ColourSwatch swatch){
//		this.swatch = swatch;
		options.setSwatch(swatch);
		return this;
	}
	
	public ChartOptionsBuilder setTarget(ChartPanel target) {
		options.setTarget(target);
//		this.target = target;
		return this;
	}
	
		
	public ChartOptionsBuilder setNormalised(boolean b){
		options.setNormalised(b);
//		this.normalised = b;
		return this;
	}
	
	public ChartOptionsBuilder setSegID(UUID id){
		options.setSegID(id);
//		this.segID = id;
		return this;
	}
	
	public ChartOptionsBuilder setModalityPosition(double modalityPosition) {
		options.setModalityPosition(modalityPosition);
//		this.modalityPosition = modalityPosition;
		return this;
	}
	
	public ChartOptionsBuilder setSegPosition(int segPosition) {
		options.setSegPosition(segPosition);
//		this.segPosition = segPosition;
		return this;
	}
	
	public ChartOptionsBuilder setAlignment(ProfileAlignment alignment){
		options.setAlignment(alignment);
//		this.alignment = alignment;
		return this;
	}
	
	public ChartOptionsBuilder setTag(Tag tag){
		options.setTag(tag);
//		this.tag = tag;
		return this;
	}
	
	public ChartOptionsBuilder setShowMarkers(boolean b){
		options.setShowMarkers(b);
//		this.showMarkers = b;
		return this;
	}
	
	public ChartOptionsBuilder setHideProfiles(boolean b){
		options.setHideProfiles(b);
//		this.hideProfiles = b;
		return this;
	}
	
	public ChartOptionsBuilder setShowPoints(boolean b){
		options.setShowPoints(b);
//		this.showPoints = b;
		return this;
	}
	
	public ChartOptionsBuilder setShowLines(boolean b){
		options.setShowLines(b);
//		this.showLines = b;
		return this;
	}
	
	public ChartOptionsBuilder setShowAnnotations(boolean showAnnotations) {
		options.setShowAnnotations(showAnnotations);
//		this.showAnnotations = showAnnotations;
		return this;
	}
	
	public ChartOptionsBuilder setProfileType(ProfileType type){
		options.setType(type);
//		this.type = type;
		return this;
	}
	
	public ChartOptionsBuilder setSignalGroup(UUID group){
		options.setSignalGroup(group);
//		this.signalGroup = group;
		return this;
	}
	
	public ChartOptionsBuilder setUseDensity(boolean b){
		options.setUseDensity(b);
//		this.useDensity = b;
		return this;
	}
	
	public ChartOptionsBuilder addStatistic(PlottableStatistic s){
		options.addStat(s);
//		this.stats.add(s);
		return this;
	}
	
	public ChartOptionsBuilder setScale(MeasurementScale s){
		options.setScale(s);
		return this;
	}
	
	public ChartOptionsBuilder setShowMesh(boolean b){
		options.setShowMesh(b);
		return this;
	}
	
	public ChartOptionsBuilder setMeshSize(int i){
		options.setMeshSize(i);
//		this.meshSize = i;
		return this;
	}
	
	public ChartOptionsBuilder setShowBounds(boolean b){
		options.setShowBounds(b);
//		this.showBounds = b;
		return this;
	}
	
	public ChartOptionsBuilder setShowMeshEdges(boolean b){
		options.setShowMeshEdges(b);
//		this.showMeshEdges = b;
		return this;
	}
	
	public ChartOptionsBuilder setShowMeshFaces(boolean b){
		options.setShowMeshFaces(b);
//		this.showMeshFaces = b;
		return this;
	}
	
	public ChartOptionsBuilder setStraightenMesh(boolean b){
		options.setStraightenMesh(b);
//		this.straightenMesh = b;
		return this;
	}
	
	public ChartOptionsBuilder setShowXAxis(boolean b){
		options.setShowXAxis(b);
//		this.showXAxis = b;
		return this;
	}
	
	public ChartOptionsBuilder setShowYAxis(boolean b){
		options.setShowYAxis(b);
//		this.showYAxis = b;
		return this;
	}
	
	public ChartOptionsBuilder setInvertXAxis(boolean b){
		options.setInvertXAxis(b);
//		this.invertXAxis = b;
		return this;
	}
	
	public ChartOptionsBuilder setInvertYAxis(boolean b){
		options.setInvertYAxis(b);
//		this.invertYAxis = b;
		return this;
	}
	
	public ChartOptionsBuilder setRotationMode(RotationMode r){
		options.setRotateMode(r);
//		this.rotateMode = r;
		return this;
	}
	
	public ChartOptionsBuilder setCell(ICell c){
		options.setCell(c);
//		this.cell = c;
		return this;
	}
	
	public ChartOptionsBuilder setCellularComponent(CellularComponent c){
		options.setComponent(c);
//		this.component = c;
		return this;
	}
	
	public ChartOptionsBuilder setShowWarp(boolean b){
		options.setShowWarp(b);
//		this.showWarp = b;
		return this;
	}
	
	public ChartOptionsBuilder setShowBorderTags(boolean b){
		options.setShowBorderTags(b);
//		this.showBorderTags = b;
		return this;
	}
	
	public ChartOptionsBuilder setShowSignals(boolean b){
		options.setShowSignals(b);
//		this.showSignals = b;
		return this;
	}
	
	public ChartOptionsBuilder setCountType(CountType c){
		options.setCountType(c);
		return this;
	}
	
	
	public ChartOptions build(){
		return options;
	}
	
}
