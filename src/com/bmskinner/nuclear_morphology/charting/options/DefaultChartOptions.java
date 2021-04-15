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
package com.bmskinner.nuclear_morphology.charting.options;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jfree.chart.ChartPanel;

import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.gui.RotationMode;
import com.bmskinner.nuclear_morphology.gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;

/*
 * Hold the drawing options for a chart. Can store options for profile
 * charts, boxplots, histograms and signal charts. The appropriate options
 * are retrieved on chart generation.
 */
public class DefaultChartOptions extends AbstractOptions implements ChartOptions {

    private boolean          normalised       = false;
    private ProfileAlignment alignment        = ProfileAlignment.LEFT;
    private Tag              tag              = Tag.REFERENCE_POINT;
    private boolean          showMarkers      = false;
    private boolean          hideProfiles     = false;
    private boolean          isShowIQR        = true;
    private ProfileType      type             = ProfileType.ANGLE;
    private UUID             signalGroup      = null;
    private boolean          useDensity       = false;
    private double           modalityPosition = 0;
    private boolean          showPoints       = false;
    private boolean          showLines        = true;
    private boolean          showAnnotations  = true;
    private boolean          showBorderTags   = true;                 // used in
                                                                      // cell
                                                                      // outlines
    private boolean          showSignals      = true;                 // used in
                                                                      // cell
                                                                      // outlines

    // Options for nucleus mesh creation
    private boolean showMesh       = false;
    private boolean showMeshVertices  = false;
    private boolean showMeshEdges  = true;
    private boolean showMeshFaces  = false;
    private boolean straightenMesh = false;
    private int     meshSize       = 10;
    private boolean showBounds     = false;

    // Chart axis options
    private boolean showXAxis   = true;
    private boolean showYAxis   = true;
    private boolean invertXAxis = false;
    private boolean invertYAxis = false;

    private RotationMode      rotateMode = RotationMode.ACTUAL;
    private final List<CellularComponent> component  = new ArrayList<>();
    private boolean           showWarp   = false;

    private ChartPanel target = null;

    public DefaultChartOptions(final List<IAnalysisDataset> list) {
        super(list);
    }

    public DefaultChartOptions(final IAnalysisDataset d) {
        super(new ArrayList<IAnalysisDataset>() {

            private List<IAnalysisDataset> init() {
                this.add(d);
                return this;
            }

        }.init());
    }

    @Override
    public boolean isNormalised() {
        return normalised;
    }

    public void setNormalised(boolean normalised) {
        this.normalised = normalised;
    }

    @Override
    public ChartPanel getTarget() {
        return target;
    }

    @Override
    public boolean hasTarget() {
        return target != null;
    }

    public void setTarget(ChartPanel target) {
        this.target = target;
    }

    @Override
    public ProfileAlignment getAlignment() {
        return alignment;
    }

    public void setAlignment(ProfileAlignment alignment) {
        this.alignment = alignment;
    }

    @Override
    public double getModalityPosition() {
        return modalityPosition;
    }

    public void setModalityPosition(double modalityPosition) {
        this.modalityPosition = modalityPosition;
    }


    @Override
    public Tag getTag() {
        return tag;
    }


    public void setTag(Tag tag) {
        this.tag = tag;
    }

    @Override
    public boolean isShowMarkers() {
        return showMarkers;
    }

    public void setShowMarkers(boolean showMarkers) {
        this.showMarkers = showMarkers;
    }

    @Override
    public boolean isShowProfiles() {
        return hideProfiles;
    }

    public void setShowProfiles(boolean hideProfiles) {
        this.hideProfiles = hideProfiles;
    }

    @Override
    public boolean isShowPoints() {
        return showPoints;
    }

    public void setShowPoints(boolean showPoints) {
        this.showPoints = showPoints;
    }

    @Override
    public boolean isShowLines() {
        return showLines;
    }

    public void setShowLines(boolean showLines) {
        this.showLines = showLines;
    }
    
    @Override
    public boolean isShowIQR() {
        return isShowIQR;
    }

    public void setShowIQR(boolean b) {
        this.isShowIQR = b;
    }

    @Override
    public boolean isShowAnnotations() {
        return showAnnotations;
    }

    public void setShowAnnotations(boolean showAnnotations) {
        this.showAnnotations = showAnnotations;
    }

    @Override
    public ProfileType getType() {
        return type;
    }

    public void setType(ProfileType type) {
        this.type = type;
    }

    @Override
    public UUID getSignalGroup() {
        return signalGroup;
    }

    public void setSignalGroup(UUID signalGroup) {
        this.signalGroup = signalGroup;
    }

    @Override
    public boolean isUseDensity() {
        return useDensity;
    }

    public void setUseDensity(boolean useDensity) {
        this.useDensity = useDensity;
    }

    @Override
    public boolean isShowMesh() {
        return showMesh;
    }

    public void setShowMesh(boolean showMesh) {
        this.showMesh = showMesh;
    }

    @Override
    public boolean isShowMeshEdges() {
        return showMeshEdges;
    }
    
    @Override
    public boolean isShowMeshVertices() {
        return showMeshVertices;
    }
    
    public void setShowMeshVertices(boolean showMeshVertices) {
        this.showMeshVertices = showMeshVertices;
    }

    public void setShowMeshEdges(boolean showMeshEdges) {
        this.showMeshEdges = showMeshEdges;
    }

    @Override
    public boolean isShowMeshFaces() {
        return showMeshFaces;
    }

    public void setShowMeshFaces(boolean showMeshFaces) {
        this.showMeshFaces = showMeshFaces;
    }

    @Override
    public boolean isStraightenMesh() {
        return straightenMesh;
    }

    public void setStraightenMesh(boolean straightenMesh) {
        this.straightenMesh = straightenMesh;
    }

    @Override
    public int getMeshSize() {
        return meshSize;
    }

    public void setMeshSize(int meshSize) {
        this.meshSize = meshSize;
    }

    @Override
    public boolean isShowBounds() {
        return showBounds;
    }

    public void setShowBounds(boolean showBounds) {
        this.showBounds = showBounds;
    }

    @Override
    public boolean isShowXAxis() {
        return showXAxis;
    }

    public void setShowXAxis(boolean showXAxis) {
        this.showXAxis = showXAxis;
    }

    @Override
    public boolean isShowYAxis() {
        return showYAxis;
    }

    public void setShowYAxis(boolean showYAxis) {
        this.showYAxis = showYAxis;
    }

    @Override
    public boolean isInvertXAxis() {
        return invertXAxis;
    }

    public void setInvertXAxis(boolean invertXAxis) {
        this.invertXAxis = invertXAxis;
    }

    @Override
    public boolean isInvertYAxis() {
        return invertYAxis;
    }

    public void setInvertYAxis(boolean invertYAxis) {
        this.invertYAxis = invertYAxis;
    }

    @Override
    public RotationMode getRotateMode() {
        return rotateMode;
    }

    public void setRotateMode(RotationMode rotateMode) {
        this.rotateMode = rotateMode;
    }

    @Override
    public List<CellularComponent> getComponent() {
        return component;
    }

    public void addComponent(CellularComponent component) {
        this.component.add(component);
    }
    
    @Override
    public boolean hasComponent() {
        return !component.isEmpty();
    }

    @Override
    public boolean isShowWarp() {
        return showWarp;
    }

    public void setShowWarp(boolean showWarp) {
        this.showWarp = showWarp;
    }

    @Override
    public boolean isShowBorderTags() {
        return showBorderTags;
    }

    public void setShowBorderTags(boolean showBorderTags) {
        this.showBorderTags = showBorderTags;
    }

    @Override
    public boolean isShowSignals() {
        return showSignals;
    }

    public void setShowSignals(boolean showSignals) {
        this.showSignals = showSignals;
    }
    
    @Override
    public String toString() {
    	String newline = System.getProperty("line.separator");
    	return new StringBuilder("Options:"+newline)
    			.append("Datasets: "+this.datasetCount()+newline)
    			.append("Cell: "+this.hasCell()+newline)
    			.append("Normalised: "+normalised+newline)
    			.append("Alignment: "+alignment+newline)
    			.append("Hide profiles: "+hideProfiles+newline)
    			.append("Show points: "+showPoints+newline)
    			.append("Show lines: "+showLines+newline)
    			.append("Show IQR: "+isShowIQR+newline)
    			.append("Show annotations: "+showAnnotations+newline)
    			.append("Show markers: "+showMarkers+newline)
    			.append("Show mesh: "+showMesh+newline)
    			.append("Show x axis: "+showXAxis+newline)
    			.append("Show y axis: "+showYAxis+newline)
    			.append("Invert x axis: "+invertXAxis+newline)
    			.append("Invert y axis: "+invertYAxis+newline)
    			.append("Tag: "+tag+newline)
    			.append("Type: "+type+newline)
    			.append("Show border tags: "+showBorderTags+newline)
    			.append("Show signals: "+showSignals+newline)
    			.append("Signal group: "+signalGroup+newline)
    			.append("Rotate mode: "+rotateMode+newline)
    			.append("Use density: "+useDensity+newline)
    			.append("Show warp: "+showWarp+newline)
    			.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((alignment == null) ? 0 : alignment.hashCode());
        long temp;
        temp = Double.doubleToLongBits(modalityPosition);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + (normalised ? 1231 : 1237);
        result = prime * result + (showMarkers ? 1231 : 1237);
        result = prime * result + (hideProfiles ? 1231 : 1237);
        result = prime * result + (showPoints ? 1231 : 1237);
        result = prime * result + (showLines ? 1231 : 1237);
        result = prime * result + (isShowIQR ? 1231 : 1237);
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

        result = prime * result + ((rotateMode == null) ? 0 : rotateMode.hashCode());

        result = prime * result + ((component == null) ? 0 : component.hashCode());
        result = prime * result + (showWarp ? 1231 : 1237);

        result = prime * result + (showSignals ? 1231 : 1237);
        result = prime * result + (showBorderTags ? 1231 : 1237);

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see charting.options.ChartOptions#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;

        DefaultChartOptions other = (DefaultChartOptions) obj;
        if (alignment != other.alignment)
            return false;
        if (Double.doubleToLongBits(modalityPosition) != Double.doubleToLongBits(other.modalityPosition))
            return false;

        if (normalised != other.normalised)
            return false;
        if (showMarkers != other.showMarkers)
            return false;
        if (isShowIQR != other.isShowIQR)
            return false;
        if (hideProfiles != other.hideProfiles)
            return false;
        if (showPoints != other.showPoints)
            return false;
        if (showLines != other.showLines)
            return false;
        if (showAnnotations != other.showAnnotations)
            return false;
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
        if (showXAxis != other.showXAxis)
            return false;
        if (showYAxis != other.showYAxis)
            return false;
        if (invertXAxis != other.invertXAxis)
            return false;
        if (invertYAxis != other.invertYAxis)
            return false;
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
        if (component == null) {
            if (other.component != null)
                return false;
        } else if (!component.equals(other.component))
            return false;

        if (showWarp != other.showWarp)
            return false;
        if (showSignals != other.showSignals)
            return false;
        if (showBorderTags != other.showBorderTags)
            return false;
        return true;
    }

}
