package charting.options;

import java.util.UUID;

import org.jfree.chart.ChartPanel;

import components.CellularComponent;
import components.ICell;
import components.generic.ProfileType;
import components.generic.Tag;
import gui.RotationMode;
import gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;

public interface ChartOptions extends DisplayOptions {

	boolean isNormalised();

//	void setNormalised(boolean normalised);

	ChartPanel getTarget();
	
	boolean hasTarget();
	
//	void setTarget(ChartPanel target);

	ProfileAlignment getAlignment();

//	void setAlignment(ProfileAlignment alignment);

	double getModalityPosition();

//	void setModalityPosition(double modalityPosition);

	Tag getTag();

//	void setTag(Tag tag);

	boolean isShowMarkers();

//	void setShowMarkers(boolean showMarkers);

	boolean isHideProfiles();

//	void setHideProfiles(boolean hideProfiles);

	boolean isShowPoints();

//	void setShowPoints(boolean showPoints);

	boolean isShowLines();

//	void setShowLines(boolean showLines);

	boolean isShowAnnotations();

//	void setShowAnnotations(boolean showAnnotations);

	ProfileType getType();

//	void setType(ProfileType type);

	UUID getSignalGroup();

//	void setSignalGroup(UUID signalGroup);

	boolean isUseDensity();

//	void setUseDensity(boolean useDensity);

	boolean isShowMesh();

//	void setShowMesh(boolean showMesh);

	boolean isShowMeshEdges();

//	void setShowMeshEdges(boolean showMeshEdges);

	boolean isShowMeshFaces();

//	void setShowMeshFaces(boolean showMeshFaces);

	boolean isStraightenMesh();

//	void setStraightenMesh(boolean straightenMesh);

	int getMeshSize();

//	void setMeshSize(int meshSize);

	boolean isShowBounds();

//	void setShowBounds(boolean showBounds);

	boolean isShowXAxis();

//	void setShowXAxis(boolean showXAxis);

	boolean isShowYAxis();

//	void setShowYAxis(boolean showYAxis);

	boolean isInvertXAxis();

//	void setInvertXAxis(boolean invertXAxis);

	boolean isInvertYAxis();

//	void setInvertYAxis(boolean invertYAxis);

	RotationMode getRotateMode();

//	void setRotateMode(RotationMode rotateMode);

	ICell getCell();

//	void setCell(ICell cell);

	boolean hasCell();

	CellularComponent getComponent();

//	void setComponent(CellularComponent component);

	boolean hasComponent();

	boolean isShowWarp();

//	void setShowWarp(boolean showWarp);

	boolean isShowBorderTags();

//	void setShowBorderTags(boolean showBorderTags);

	boolean isShowSignals();

//	void setShowSignals(boolean showSignals);

	int hashCode();

	boolean equals(Object obj);

}