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
package charting.options;

import gui.RotationMode;
import gui.components.ColourSelecter.ColourSwatch;
import gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import stats.PlottableStatistic;
import components.Cell;
import components.CellularComponent;
import components.generic.BorderTag;
import components.generic.MeasurementScale;
import components.generic.ProfileType;
import analysis.AnalysisDataset;

/**
 * Builder for a ChartOptions object. This simplifies the creation
 * of the options when not all parameters need to be set. It also makes
 * it easier to remember which boolean option is which.
 * @author bms41
 *
 */
public class ChartOptionsBuilder {
	private List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
	private ColourSwatch swatch        = ColourSwatch.REGULAR_SWATCH;
	private boolean normalised         = false;
	private ProfileAlignment alignment = ProfileAlignment.LEFT;
	private BorderTag tag              = BorderTag.REFERENCE_POINT;
	private boolean showMarkers        = false;
	private boolean hideProfiles       = false;
	private ProfileType type           = ProfileType.ANGLE;
	private UUID signalGroup           = null;
	private boolean useDensity         = false;
	private List<PlottableStatistic> stats    = new ArrayList<PlottableStatistic>();
	private MeasurementScale scale     = MeasurementScale.PIXELS;
	private UUID segID                 = null;
	private int segPosition            = 0;
	private double modalityPosition    = 0;
	private boolean showPoints         = false;
	private boolean showLines          = true;
	private boolean showAnnotations    = true;
	
	// Options for nucleus mesh creation
	private boolean showMesh           = false;
	private boolean showMeshEdges      = true;
	private boolean showMeshFaces      = false;
	private int     meshSize           = 10;
	private boolean showBounds         = false; // Bounding box for meshes and warped images
	
	// Chart axis options
	private boolean showXAxis           = true;
	private boolean showYAxis           = true;
	private boolean invertXAxis         = false;
	private boolean invertYAxis         = false;
	
	// Cells tab
	private RotationMode rotateMode     = RotationMode.ACTUAL;
	private Cell cell                   = null;
	private CellularComponent component = null;
	private boolean showWarp            = false;
	
	
	public ChartOptionsBuilder(){
		
	}
	
	public ChartOptionsBuilder setDatasets(List<AnalysisDataset> list){
		this.list = list;
		if(list!=null && !list.isEmpty()){
			if(list.get(0).getSwatch()!=null){
				this.swatch = list.get(0).getSwatch();
			} else {
				this.swatch = ColourSwatch.REGULAR_SWATCH;
				list.get(0).setSwatch(swatch);
			}
		} else {
			this.swatch = ColourSwatch.REGULAR_SWATCH;
		}
		return this;
	}
	
	public ChartOptionsBuilder setDatasets(AnalysisDataset dataset){
		List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
		list.add(dataset);
		return this.setDatasets(list);
	}
	
	public ChartOptionsBuilder setSwatch(ColourSwatch swatch){
		this.swatch = swatch;
		return this;
	}
		
	public ChartOptionsBuilder setNormalised(boolean b){
		this.normalised = b;
		return this;
	}
	
	public ChartOptionsBuilder setSegID(UUID id){
		this.segID = id;
		return this;
	}
	
	public ChartOptionsBuilder setModalityPosition(double modalityPosition) {
		this.modalityPosition = modalityPosition;
		return this;
	}
	
	public ChartOptionsBuilder setSegPosition(int segPosition) {
		this.segPosition = segPosition;
		return this;
	}
	
	public ChartOptionsBuilder setAlignment(ProfileAlignment alignment){
		this.alignment = alignment;
		return this;
	}
	
	public ChartOptionsBuilder setTag(BorderTag tag){
		this.tag = tag;
		return this;
	}
	
	public ChartOptionsBuilder setShowMarkers(boolean b){
		this.showMarkers = b;
		return this;
	}
	
	public ChartOptionsBuilder setHideProfiles(boolean b){
		this.hideProfiles = b;
		return this;
	}
	
	public ChartOptionsBuilder setShowPoints(boolean b){
		this.showPoints = b;
		return this;
	}
	
	public ChartOptionsBuilder setShowLines(boolean b){
		this.showLines = b;
		return this;
	}
	
	public ChartOptionsBuilder setShowAnnotations(boolean showAnnotations) {
		this.showAnnotations = showAnnotations;
		return this;
	}
	
	public ChartOptionsBuilder setProfileType(ProfileType type){
		this.type = type;
		return this;
	}
	
	public ChartOptionsBuilder setSignalGroup(UUID group){
		this.signalGroup = group;
		return this;
	}
	
	public ChartOptionsBuilder setUseDensity(boolean b){
		this.useDensity = b;
		return this;
	}
	
	public ChartOptionsBuilder addStatistic(PlottableStatistic s){
		this.stats.add(s);
		return this;
	}
	
	public ChartOptionsBuilder setScale(MeasurementScale s){
		this.scale = s;
		return this;
	}
	
	public ChartOptionsBuilder setShowMesh(boolean b){
		this.showMesh = b;
		return this;
	}
	
	public ChartOptionsBuilder setMeshSize(int i){
		this.meshSize = i;
		return this;
	}
	
	public ChartOptionsBuilder setShowBounds(boolean b){
		this.showBounds = b;
		return this;
	}
	
	public ChartOptionsBuilder setShowMeshEdges(boolean b){
		this.showMeshEdges = b;
		return this;
	}
	
	public ChartOptionsBuilder setShowMeshFaces(boolean b){
		this.showMeshFaces = b;
		return this;
	}
	
	public ChartOptionsBuilder setShowXAxis(boolean b){
		this.showXAxis = b;
		return this;
	}
	
	public ChartOptionsBuilder setShowYAxis(boolean b){
		this.showYAxis = b;
		return this;
	}
	
	public ChartOptionsBuilder setInvertXAxis(boolean b){
		this.invertXAxis = b;
		return this;
	}
	
	public ChartOptionsBuilder setInvertYAxis(boolean b){
		this.invertYAxis = b;
		return this;
	}
	
	public ChartOptionsBuilder setRotationMode(RotationMode r){
		this.rotateMode = r;
		return this;
	}
	
	public ChartOptionsBuilder setCell(Cell c){
		this.cell = c;
		return this;
	}
	
	public ChartOptionsBuilder setCellularComponent(CellularComponent c){
		this.component = c;
		return this;
	}
	
	public ChartOptionsBuilder setShowWarp(boolean b){
		this.showWarp = b;
		return this;
	}
	
	
	public ChartOptions build(){
		ChartOptions result =  new ChartOptions(list);
		result.setSwatch(swatch);
		result.setAlignment(alignment);
		result.setNormalised(normalised);
		result.setScale(scale);
		result.setShowMarkers(showMarkers);
		result.setHideProfiles(hideProfiles);
		result.setSignalGroup(signalGroup);
		result.setStats(stats);
		result.setTag(tag);
		result.setType(type);
		result.setUseDensity(useDensity);
		result.setSegID(segID);
		result.setSegPosition(segPosition);
		result.setModalityPosition(modalityPosition);
		result.setShowLines(showLines);
		result.setShowPoints(showPoints);
		result.setShowAnnotations(showAnnotations);
		
		result.setShowMesh(showMesh);
		result.setShowMeshEdges(showMeshEdges);
		result.setShowMeshFaces(showMeshFaces);
		result.setMeshSize(meshSize);
		result.setShowBounds(showBounds);
		
		result.setShowXAxis(showXAxis);
		result.setShowYAxis(showYAxis);
		result.setInvertXAxis(invertXAxis);
		result.setInvertYAxis(invertYAxis);
		
		result.setRotateMode(rotateMode);
		result.setCell(cell);
		result.setComponent(component);
		result.setShowWarp(showWarp);
		return result;
	}
	
}
