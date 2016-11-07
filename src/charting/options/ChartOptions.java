package charting.options;

import java.util.UUID;

import org.jfree.chart.ChartPanel;

import components.CellularComponent;
import components.generic.ProfileType;
import components.generic.Tag;
import gui.RotationMode;
import gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;

/**
 * This interface describes the values that should be checkable
 * by ChartFactories. Implementing classes must provide sensible 
 * defaults. 
 * @author bms41
 *
 */
public interface ChartOptions extends DisplayOptions {

	/**
	 * Should a profile chart be normalised?
	 * @return
	 */
	boolean isNormalised();

	/**
	 * Get the target panel the chart with the given options should be placed
	 * in once complete. This is designed to be used with a ChartFactoryWorker
	 * in a DetailPanel.
	 * @return
	 */
	ChartPanel getTarget();
	
	/**
	 * Check if a target ChartPanel has been set
	 * @return
	 */
	boolean hasTarget();

	/**
	 * Get the alignment for profiles in a profile panel. 
	 * @return
	 */
	ProfileAlignment getAlignment();

	/**
	 * Get the position within a profile to display modality 
	 * probabilities for
	 * @return
	 */
	double getModalityPosition();

	/**
	 * Get the tag
	 * @return
	 */
	Tag getTag();

	/**
	 * Check if markers should be displayed on profile panels
	 * at border tag positions
	 * @return
	 */
	boolean isShowMarkers();


	/**
	 * Check if individual profiles should be displayed on a single
	 * dataset profile chart, or if profiles should be hidden in favour
	 * of median and IQR range shaded in the dataset colour
	 * @return
	 */
	boolean isHideProfiles();


	/**
	 * Check if individual points in an XY chart should be displayed
	 * @return
	 */
	boolean isShowPoints();


	/**
	 * Check if lines in an XY chart should be displayed
	 * @return
	 */
	boolean isShowLines();


	/**
	 * Check if dataset annotations such as segment names, 
	 * segment boundary lines
	 * @return
	 */
	boolean isShowAnnotations();


	/**
	 * Get the profile type from a profile chart
	 * @return
	 */
	ProfileType getType();


	/**
	 * Get the id of the signal group to display in this chart
	 * @return
	 */
	UUID getSignalGroup();


	/**
	 * Check if a histogram plot should show counts or
	 * a probability density function
	 * @return
	 */
	boolean isUseDensity();


	/**
	 * Check if an outline chart should display a mesh
	 * @return
	 */
	boolean isShowMesh();


	/**
	 * Check if a mesh chart should annotate the mesh edges
	 * @return
	 */
	boolean isShowMeshEdges();


	/**
	 * Check if a mesh chart should annotate the mesh faces
	 * @return
	 */
	boolean isShowMeshFaces();


	/**
	 * Check if a mesh chart should straighten the mesh into a rectangle
	 * @return
	 */
	boolean isStraightenMesh();


	/**
	 * Get the size of the displayed mesh
	 * @return
	 */
	int getMeshSize();


	/**
	 * Check if the bounds of an object should be shown on an 
	 * outline chart
	 * @return
	 */
	boolean isShowBounds();


	/**
	 * Check if the x-axis should be displayed on the chart
	 * @return
	 */
	boolean isShowXAxis();


	/**
	 * Check if the y-axis should be displayed on the chart
	 * @return
	 */
	boolean isShowYAxis();


	/**
	 * Check if the x-axis should be inverted on the chart
	 * @return
	 */
	boolean isInvertXAxis();

	/**
	 * Check if the y-axis should be inverted on the chart
	 * @return
	 */
	boolean isInvertYAxis();


	/**
	 * Get the rotation of the object in an outline chart
	 * @return
	 */
	RotationMode getRotateMode();

	/**
	 * Get the component displayed in an outline chart
	 * @return
	 */
	CellularComponent getComponent();

	/**
	 * Check if a component is set to display in an outline chart
	 * @return
	 */
	boolean hasComponent();

	/**
	 * Check if an outline chart should show a warp to a consensus object
	 * @return
	 */
	boolean isShowWarp();


	/**
	 * Check if border tags should be displayed as marker lines
	 * on a profile chart
	 * @return
	 */
	boolean isShowBorderTags();


	/**
	 * Check if nuclear signals should be shown on an outline chart
	 * @return
	 */
	boolean isShowSignals();

	int hashCode();

	boolean equals(Object obj);

}