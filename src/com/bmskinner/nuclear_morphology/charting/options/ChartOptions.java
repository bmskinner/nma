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

import java.util.List;
import java.util.UUID;

import org.jfree.chart.ChartPanel;

import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.gui.RotationMode;
import com.bmskinner.nuclear_morphology.gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;

/**
 * This interface describes the values that should be checkable by
 * ChartFactories. Implementing classes must provide sensible defaults.
 * 
 * @author bms41
 *
 */
public interface ChartOptions extends DisplayOptions {

    /**
     * Should a profile chart be normalised?
     * 
     * @return
     */
    boolean isNormalised();

    /**
     * Get the target panel the chart with the given options should be placed in
     * once complete. This is designed to be used with a ChartFactoryWorker in a
     * DetailPanel.
     * 
     * @return
     */
    ChartPanel getTarget();

    /**
     * Check if a target ChartPanel has been set
     * 
     * @return
     */
    boolean hasTarget();

    /**
     * Get the alignment for profiles in a profile panel.
     * 
     * @return
     */
    ProfileAlignment getAlignment();

    /**
     * Get the position within a profile to display modality probabilities for
     * 
     * @return
     */
    double getModalityPosition();

    /**
     * Get the tag
     * 
     * @return
     */
    Landmark getTag();

    /**
     * Check if markers should be displayed on profile panels at border tag
     * positions
     * 
     * @return
     */
    boolean isShowMarkers();

    /**
     * Check if individual profiles should be displayed on a single dataset
     * profile chart, or if profiles should be hidden in favour of median and
     * IQR range shaded in the dataset colour
     * 
     * @return
     */
    boolean isShowProfiles();

    /**
     * Check if individual points in an XY chart should be displayed
     * 
     * @return
     */
    boolean isShowPoints();
    
    /**
     * Should the interquartile range be displayed in a profile chart?
     * @return
     */
    boolean isShowIQR();

    /**
     * Check if lines in an XY chart should be displayed
     * 
     * @return
     */
    boolean isShowLines();

    /**
     * Check if dataset annotations such as segment names, segment boundary
     * lines
     * 
     * @return
     */
    boolean isShowAnnotations();

    /**
     * Get the profile type from a profile chart
     * 
     * @return
     */
    ProfileType getType();

    /**
     * Get the id of the signal group to display in this chart
     * 
     * @return
     */
    UUID getSignalGroup();

    /**
     * Check if a histogram plot should show counts or a probability density
     * function
     * 
     * @return
     */
    boolean isUseDensity();

    /**
     * Check if an outline chart should display a mesh
     * 
     * @return
     */
    boolean isShowMesh();
    
    /**
     * Check if a mesh chart should annotate the mesh vertices
     * 
     * @return
     */
    boolean isShowMeshVertices();

    /**
     * Check if a mesh chart should annotate the mesh edges
     * 
     * @return
     */
    boolean isShowMeshEdges();

    /**
     * Check if a mesh chart should annotate the mesh faces
     * 
     * @return
     */
    boolean isShowMeshFaces();

    /**
     * Check if a mesh chart should straighten the mesh into a rectangle
     * 
     * @return
     */
    boolean isStraightenMesh();

    /**
     * Get the size of the displayed mesh
     * 
     * @return
     */
    int getMeshSize();

    /**
     * Check if the bounds of an object should be shown on an outline chart
     * 
     * @return
     */
    boolean isShowBounds();

    /**
     * Check if the x-axis should be displayed on the chart
     * 
     * @return
     */
    boolean isShowXAxis();

    /**
     * Check if the y-axis should be displayed on the chart
     * 
     * @return
     */
    boolean isShowYAxis();

    /**
     * Check if the x-axis should be inverted on the chart
     * 
     * @return
     */
    boolean isInvertXAxis();

    /**
     * Check if the y-axis should be inverted on the chart
     * 
     * @return
     */
    boolean isInvertYAxis();

    /**
     * Get the rotation of the object in an outline chart
     * 
     * @return
     */
    RotationMode getRotateMode();

    /**
     * Get the components displayed in an outline chart
     * 
     * @return
     */
    List<CellularComponent> getComponent();

    /**
     * Check if a component is set to display in an outline chart
     * 
     * @return
     */
    boolean hasComponent();

    /**
     * Check if an outline chart should show a warp to a consensus object
     * 
     * @return
     */
    boolean isShowWarp();

    /**
     * Check if border tags should be displayed as marker lines on a profile
     * chart
     * 
     * @return
     */
    boolean isShowBorderTags();

    /**
     * Check if nuclear signals should be shown on an outline chart
     * 
     * @return
     */
    boolean isShowSignals();

}
