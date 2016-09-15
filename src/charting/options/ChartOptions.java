/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
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
import gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;

import java.util.List;
import java.util.UUID;
import org.jfree.chart.ChartPanel;

import components.Cell;
import components.CellularComponent;
import components.generic.BorderTagObject;
import components.generic.ProfileType;
import analysis.AnalysisDataset;

/*
 * Hold the drawing options for a chart. Can store options for profile
 * charts, boxplots, histograms and signal charts. The appropriate options
 * are retrieved on chart generation.
 */
public class ChartOptions extends AbstractOptions {
	
	private boolean normalised         = false;
	private ProfileAlignment alignment = ProfileAlignment.LEFT;
	private BorderTagObject tag              = BorderTagObject.REFERENCE_POINT;
	private boolean showMarkers        = false;
	private boolean hideProfiles       = false;
	private ProfileType type           = ProfileType.ANGLE;
	private UUID signalGroup            = null;
	private boolean useDensity         = false;
	private double modalityPosition    = 0;
	private boolean showPoints         = false;
	private boolean showLines          = true;
	private boolean showAnnotations    = true;
	
	// Options for nucleus mesh creation
	private boolean showMesh           = false;
	private boolean showMeshEdges      = true;
	private boolean showMeshFaces      = false;
	private boolean straightenMesh     = false;
	private int     meshSize           = 10;
	private boolean showBounds         = false;
	
	// Chart axis options
	private boolean showXAxis           = true;
	private boolean showYAxis           = true;
	private boolean invertXAxis         = false;
	private boolean invertYAxis         = false;
	
	private RotationMode rotateMode     = RotationMode.ACTUAL;
	private Cell cell                   = null;
	private CellularComponent component = null;
	private boolean showWarp            = false;
	
	private ChartPanel target           = null;
	

	
	public ChartOptions(List<AnalysisDataset> list){
		super(list);
	}
			
	public boolean isNormalised() {
		return normalised;
	}

	public void setNormalised(boolean normalised) {
		this.normalised = normalised;
	}

	public ChartPanel getTarget() {
		return target;
	}

	public void setTarget(ChartPanel target) {
		this.target = target;
	}

	public ProfileAlignment getAlignment() {
		return alignment;
	}

	public void setAlignment(ProfileAlignment alignment) {
		this.alignment = alignment;
	}
	
	

	public double getModalityPosition() {
		return modalityPosition;
	}

	public void setModalityPosition(double modalityPosition) {
		this.modalityPosition = modalityPosition;
	}

	public BorderTagObject getTag() {
		return tag;
	}

	public void setTag(BorderTagObject tag) {
		this.tag = tag;
	}

	public boolean isShowMarkers() {
		return showMarkers;
	}

	public void setShowMarkers(boolean showMarkers) {
		this.showMarkers = showMarkers;
	}
	
	public boolean isHideProfiles() {
		return hideProfiles;
	}

	public void setHideProfiles(boolean hideProfiles) {
		this.hideProfiles = hideProfiles;
	}
	

	public boolean isShowPoints() {
		return showPoints;
	}

	public void setShowPoints(boolean showPoints) {
		this.showPoints = showPoints;
	}

	public boolean isShowLines() {
		return showLines;
	}

	public void setShowLines(boolean showLines) {
		this.showLines = showLines;
	}
	
	

	public boolean isShowAnnotations() {
		return showAnnotations;
	}

	public void setShowAnnotations(boolean showAnnotations) {
		this.showAnnotations = showAnnotations;
	}

	public ProfileType getType() {
		return type;
	}

	public void setType(ProfileType type) {
		this.type = type;
	}

	public UUID getSignalGroup() {
		return signalGroup;
	}

	public void setSignalGroup(UUID signalGroup) {
		this.signalGroup = signalGroup;
	}

	public boolean isUseDensity() {
		return useDensity;
	}

	public void setUseDensity(boolean useDensity) {
		this.useDensity = useDensity;
	}
	
	

	public boolean isShowMesh() {
		return showMesh;
	}

	public void setShowMesh(boolean showMesh) {
		this.showMesh = showMesh;
	}

	public boolean isShowMeshEdges() {
		return showMeshEdges;
	}

	public void setShowMeshEdges(boolean showMeshEdges) {
		this.showMeshEdges = showMeshEdges;
	}

	public boolean isShowMeshFaces() {
		return showMeshFaces;
	}

	public void setShowMeshFaces(boolean showMeshFaces) {
		this.showMeshFaces = showMeshFaces;
	}
	
	public boolean isStraightenMesh() {
		return straightenMesh;
	}

	public void setStraightenMesh(boolean straightenMesh) {
		this.straightenMesh = straightenMesh;
	}

	public int getMeshSize() {
		return meshSize;
	}

	public void setMeshSize(int meshSize) {
		this.meshSize = meshSize;
	}
	

	public boolean isShowBounds() {
		return showBounds;
	}

	public void setShowBounds(boolean showBounds) {
		this.showBounds = showBounds;
	}

	public boolean isShowXAxis() {
		return showXAxis;
	}

	public void setShowXAxis(boolean showXAxis) {
		this.showXAxis = showXAxis;
	}

	public boolean isShowYAxis() {
		return showYAxis;
	}

	public void setShowYAxis(boolean showYAxis) {
		this.showYAxis = showYAxis;
	}
	
	

	public boolean isInvertXAxis() {
		return invertXAxis;
	}

	public void setInvertXAxis(boolean invertXAxis) {
		this.invertXAxis = invertXAxis;
	}

	public boolean isInvertYAxis() {
		return invertYAxis;
	}

	public void setInvertYAxis(boolean invertYAxis) {
		this.invertYAxis = invertYAxis;
	}
	
	
	public RotationMode getRotateMode() {
		return rotateMode;
	}

	public void setRotateMode(RotationMode rotateMode) {
		this.rotateMode = rotateMode;
	}
	
	public Cell getCell() {
		return cell;
	}

	public void setCell(Cell cell) {
		this.cell = cell;
	}
	
	public boolean hasCell(){
		return this.cell!=null;
	}

	public CellularComponent getComponent() {
		return component;
	}

	public void setComponent(CellularComponent component) {
		this.component = component;
	}
	
	public boolean hasComponent(){
		return this.component!=null;
	}
	
	
	public boolean isShowWarp() {
		return showWarp;
	}

	public void setShowWarp(boolean showWarp) {
		this.showWarp = showWarp;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((alignment == null) ? 0 : alignment.hashCode());
		long temp;
		temp = Double.doubleToLongBits(modalityPosition);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + (normalised ? 1231 : 1237);
		result = prime * result + (showMarkers ? 1231 : 1237);
		result = prime * result + (hideProfiles ? 1231 : 1237);
		result = prime * result + (showPoints ? 1231 : 1237);
		result = prime * result + (showLines ? 1231 : 1237);
		result = prime * result + (showAnnotations ? 1231 : 1237);
		
		result = prime * result + (showMesh ? 1231 : 1237);
		result = prime * result + (showMeshEdges ? 1231 : 1237);
		result = prime * result + (showMeshFaces ? 1231 : 1237);
		result = prime * result + meshSize;
		result = prime * result + (showBounds ? 1231 : 1237);
		result = prime * result + (straightenMesh ? 1231 : 1237);
		
		result = prime * result + (showXAxis ? 1231 : 1237);
		result = prime * result + (showYAxis ? 1231 : 1237);
		result = prime * result + (invertXAxis ? 1231 : 1237);
		result = prime * result + (invertYAxis ? 1231 : 1237);
		
		result = prime * result + ((signalGroup == null) ? 0 : signalGroup.hashCode());
		
		result = prime * result + ((tag == null) ? 0 : tag.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + (useDensity ? 1231 : 1237);
		
		result = prime * result
				+ ((rotateMode == null) ? 0 : rotateMode.hashCode());
		result = prime * result
				+ ((cell == null) ? 0 : cell.hashCode());
		result = prime * result
				+ ((component == null) ? 0 : component.hashCode());
		result = prime * result + (showWarp ? 1231 : 1237);
		
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		
//		log("Equals reached level 0");
		
		if (!super.equals(obj))
			return false;
		
		if (getClass() != obj.getClass())
			return false;
		
		ChartOptions other = (ChartOptions) obj;
//		log("Equals reached level 0.5");
		
		if (alignment != other.alignment)
			return false;
		
		if (Double.doubleToLongBits(modalityPosition) != Double
				.doubleToLongBits(other.modalityPosition))
			return false;
		
		if (normalised != other.normalised)
			return false;
		
//		log("Equals reached level 0.6");
		
		if (showMarkers != other.showMarkers)
			return false;
		
		if (hideProfiles != other.hideProfiles)
			return false;
		
//		log("Equals reached level 0.7");
		
		if (showPoints != other.showPoints)
			return false;
		if (showLines != other.showLines)
			return false;
		
//		log("Equals reached level 0.8");
		
		if (showAnnotations != other.showAnnotations)
			return false;
		
//		log("Equals reached level 1");
		
		if (showMesh != other.showMesh)
			return false;
		if (showMeshEdges != other.showMeshEdges)
			return false;
		if (showMeshFaces != other.showMeshFaces)
			return false;
		if (meshSize != other.meshSize)
			return false;
		if (showBounds != other.showBounds)
			return false;
		if (straightenMesh != other.straightenMesh)
			return false;
		
//		log("Equals reached level 2");
		
		if (showXAxis != other.showXAxis)
			return false;
		if (showYAxis != other.showYAxis)
			return false;
		if (invertXAxis != other.invertXAxis)
			return false;
		if (invertYAxis != other.invertYAxis)
			return false;
		
//		log("Equals reached level 3");

		if (signalGroup != other.signalGroup)
			return false;
		
		if (tag == null) {
			if (other.tag != null)
				return false;
		} else if (!tag.equals(other.tag))
			return false;
		
		if (type != other.type)
			return false;
		if (useDensity != other.useDensity)
			return false;
		
		if (rotateMode != other.rotateMode)
			return false;
		
//		log("Equals reached level 4");

		if (cell == null) {
			if (other.cell != null)
				return false;
		} else if (!cell.equals(other.cell))
			return false;
		
//		log("Equals reached level 5");

		
		if (component == null) {
			if (other.component != null)
				return false;
		} else if (!component.equals(other.component))
			return false;
//		log("Equals reached level 6");

		
		if (showWarp != other.showWarp)
			return false;
		
		
		return true;
	}
	
	
	
}
