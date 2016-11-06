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

import components.CellularComponent;
import components.ICell;
import components.generic.BorderTagObject;
import components.generic.ProfileType;
import components.generic.Tag;
import analysis.AnalysisDataset;
import analysis.IAnalysisDataset;

/*
 * Hold the drawing options for a chart. Can store options for profile
 * charts, boxplots, histograms and signal charts. The appropriate options
 * are retrieved on chart generation.
 */
public class DefaultChartOptions extends AbstractOptions implements ChartOptions {
	
	private boolean normalised         = false;
	private ProfileAlignment alignment = ProfileAlignment.LEFT;
	private Tag tag              = Tag.REFERENCE_POINT;
	private boolean showMarkers        = false;
	private boolean hideProfiles       = false;
	private ProfileType type           = ProfileType.ANGLE;
	private UUID signalGroup            = null;
	private boolean useDensity         = false;
	private double modalityPosition    = 0;
	private boolean showPoints         = false;
	private boolean showLines          = true;
	private boolean showAnnotations    = true;
	private boolean showBorderTags     = true; // used in cell outlines
	private boolean showSignals        = true; // used in cell outlines
	
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
	private ICell cell                   = null;
	private CellularComponent component = null;
	private boolean showWarp            = false;
	
	private ChartPanel target           = null;
	

	
	public DefaultChartOptions(List<IAnalysisDataset> list){
		super(list);
	}
			
	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#isNormalised()
	 */
	@Override
	public boolean isNormalised() {
		return normalised;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#setNormalised(boolean)
	 */
	public void setNormalised(boolean normalised) {
		this.normalised = normalised;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#getTarget()
	 */
	@Override
	public ChartPanel getTarget() {
		return target;
	}
	
	@Override
	public boolean hasTarget(){
		return target!=null;
	}


	public void setTarget(ChartPanel target) {
		this.target = target;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#getAlignment()
	 */
	@Override
	public ProfileAlignment getAlignment() {
		return alignment;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#setAlignment(gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment)
	 */
	public void setAlignment(ProfileAlignment alignment) {
		this.alignment = alignment;
	}
	
	

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#getModalityPosition()
	 */
	@Override
	public double getModalityPosition() {
		return modalityPosition;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#setModalityPosition(double)
	 */
	public void setModalityPosition(double modalityPosition) {
		this.modalityPosition = modalityPosition;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#getTag()
	 */
	@Override
	public Tag getTag() {
		return tag;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#setTag(components.generic.Tag)
	 */
	public void setTag(Tag tag) {
		this.tag = tag;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#isShowMarkers()
	 */
	@Override
	public boolean isShowMarkers() {
		return showMarkers;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#setShowMarkers(boolean)
	 */
	public void setShowMarkers(boolean showMarkers) {
		this.showMarkers = showMarkers;
	}
	
	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#isHideProfiles()
	 */
	@Override
	public boolean isHideProfiles() {
		return hideProfiles;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#setHideProfiles(boolean)
	 */
	public void setHideProfiles(boolean hideProfiles) {
		this.hideProfiles = hideProfiles;
	}
	

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#isShowPoints()
	 */
	@Override
	public boolean isShowPoints() {
		return showPoints;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#setShowPoints(boolean)
	 */
	public void setShowPoints(boolean showPoints) {
		this.showPoints = showPoints;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#isShowLines()
	 */
	@Override
	public boolean isShowLines() {
		return showLines;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#setShowLines(boolean)
	 */
	public void setShowLines(boolean showLines) {
		this.showLines = showLines;
	}
	
	

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#isShowAnnotations()
	 */
	@Override
	public boolean isShowAnnotations() {
		return showAnnotations;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#setShowAnnotations(boolean)
	 */
	public void setShowAnnotations(boolean showAnnotations) {
		this.showAnnotations = showAnnotations;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#getType()
	 */
	@Override
	public ProfileType getType() {
		return type;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#setType(components.generic.ProfileType)
	 */
	public void setType(ProfileType type) {
		this.type = type;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#getSignalGroup()
	 */
	@Override
	public UUID getSignalGroup() {
		return signalGroup;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#setSignalGroup(java.util.UUID)
	 */
	public void setSignalGroup(UUID signalGroup) {
		this.signalGroup = signalGroup;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#isUseDensity()
	 */
	@Override
	public boolean isUseDensity() {
		return useDensity;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#setUseDensity(boolean)
	 */
	public void setUseDensity(boolean useDensity) {
		this.useDensity = useDensity;
	}
	
	

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#isShowMesh()
	 */
	@Override
	public boolean isShowMesh() {
		return showMesh;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#setShowMesh(boolean)
	 */
	public void setShowMesh(boolean showMesh) {
		this.showMesh = showMesh;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#isShowMeshEdges()
	 */
	@Override
	public boolean isShowMeshEdges() {
		return showMeshEdges;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#setShowMeshEdges(boolean)
	 */
	public void setShowMeshEdges(boolean showMeshEdges) {
		this.showMeshEdges = showMeshEdges;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#isShowMeshFaces()
	 */
	@Override
	public boolean isShowMeshFaces() {
		return showMeshFaces;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#setShowMeshFaces(boolean)
	 */
	public void setShowMeshFaces(boolean showMeshFaces) {
		this.showMeshFaces = showMeshFaces;
	}
	
	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#isStraightenMesh()
	 */
	@Override
	public boolean isStraightenMesh() {
		return straightenMesh;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#setStraightenMesh(boolean)
	 */
	public void setStraightenMesh(boolean straightenMesh) {
		this.straightenMesh = straightenMesh;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#getMeshSize()
	 */
	@Override
	public int getMeshSize() {
		return meshSize;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#setMeshSize(int)
	 */
	public void setMeshSize(int meshSize) {
		this.meshSize = meshSize;
	}
	

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#isShowBounds()
	 */
	@Override
	public boolean isShowBounds() {
		return showBounds;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#setShowBounds(boolean)
	 */
	public void setShowBounds(boolean showBounds) {
		this.showBounds = showBounds;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#isShowXAxis()
	 */
	@Override
	public boolean isShowXAxis() {
		return showXAxis;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#setShowXAxis(boolean)
	 */
	public void setShowXAxis(boolean showXAxis) {
		this.showXAxis = showXAxis;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#isShowYAxis()
	 */
	@Override
	public boolean isShowYAxis() {
		return showYAxis;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#setShowYAxis(boolean)
	 */
	public void setShowYAxis(boolean showYAxis) {
		this.showYAxis = showYAxis;
	}
	
	

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#isInvertXAxis()
	 */
	@Override
	public boolean isInvertXAxis() {
		return invertXAxis;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#setInvertXAxis(boolean)
	 */
	public void setInvertXAxis(boolean invertXAxis) {
		this.invertXAxis = invertXAxis;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#isInvertYAxis()
	 */
	@Override
	public boolean isInvertYAxis() {
		return invertYAxis;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#setInvertYAxis(boolean)
	 */
	public void setInvertYAxis(boolean invertYAxis) {
		this.invertYAxis = invertYAxis;
	}
	
	
	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#getRotateMode()
	 */
	@Override
	public RotationMode getRotateMode() {
		return rotateMode;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#setRotateMode(gui.RotationMode)
	 */
	public void setRotateMode(RotationMode rotateMode) {
		this.rotateMode = rotateMode;
	}
	
	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#getCell()
	 */
	@Override
	public ICell getCell() {
		return cell;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#setCell(components.ICell)
	 */
	public void setCell(ICell cell) {
		this.cell = cell;
	}
	
	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#hasCell()
	 */
	@Override
	public boolean hasCell(){
		return this.cell!=null;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#getComponent()
	 */
	@Override
	public CellularComponent getComponent() {
		return component;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#setComponent(components.CellularComponent)
	 */
	public void setComponent(CellularComponent component) {
		this.component = component;
	}
	
	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#hasComponent()
	 */
	@Override
	public boolean hasComponent(){
		return this.component!=null;
	}
	
	
	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#isShowWarp()
	 */
	@Override
	public boolean isShowWarp() {
		return showWarp;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#setShowWarp(boolean)
	 */
	public void setShowWarp(boolean showWarp) {
		this.showWarp = showWarp;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#isShowBorderTags()
	 */
	@Override
	public boolean isShowBorderTags() {
		return showBorderTags;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#setShowBorderTags(boolean)
	 */
	public void setShowBorderTags(boolean showBorderTags) {
		this.showBorderTags = showBorderTags;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#isShowSignals()
	 */
	@Override
	public boolean isShowSignals() {
		return showSignals;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#setShowSignals(boolean)
	 */
	public void setShowSignals(boolean showSignals) {
		this.showSignals = showSignals;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#hashCode()
	 */
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
		
		result = prime * result + (showSignals    ? 1231 : 1237);
		result = prime * result + (showBorderTags ? 1231 : 1237);
		
		return result;
	}

	/* (non-Javadoc)
	 * @see charting.options.ChartOptions#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		
//		log("Equals reached level 0");
		
		if (!super.equals(obj))
			return false;
		
		if (getClass() != obj.getClass())
			return false;
		
		DefaultChartOptions other = (DefaultChartOptions) obj;
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
		
		if (showSignals != other.showSignals)
			return false;
		
		if (showBorderTags != other.showBorderTags)
			return false;
		
		return true;
	}
	
	
	
}
