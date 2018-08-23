package com.bmskinner.nuclear_morphology.components;

import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetProfilingMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod.MorphologyAnalysisMode;
import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;

/**
 * Simplify the creation of test datasets using a builder pattern
 * @author bms41
 * @since 1.14.0
 *
 */
public class TestDatasetBuilder {
	
	private IAnalysisDataset d;
	private NucleusType type = NucleusType.ROUND;
	private int nCells = 1;
	private int maxVariation = TestDatasetFactory.DEFAULT_VARIATION;
	private int xBase = TestDatasetFactory.DEFAULT_X_BASE;
	private int yBase = TestDatasetFactory.DEFAULT_Y_BASE;
	private int w = TestDatasetFactory.DEFAULT_BASE_WIDTH;
	private int h = TestDatasetFactory.DEFAULT_BASE_HEIGHT;
	private int fixedOffset = TestDatasetFactory.DEFAULT_BORDER_OFFSET;
	private int maxRotation = TestDatasetFactory.DEFAULT_ROTATION;
	
	private boolean profile = false;
	private boolean segment = false;
	private boolean offset  = TestDatasetFactory.DEFAULT_IS_BORDER_OFFSET;
	
	public TestDatasetBuilder() {}
	
		
	public IAnalysisDataset build() throws Exception {
				
		d = TestDatasetFactory.variableRectangularDataset(nCells, type, maxVariation, w, h, xBase, yBase, maxRotation, offset, fixedOffset);
		
		if(segment || profile)
			new DatasetProfilingMethod(d).call();
		if(segment)
			new DatasetSegmentationMethod(d, MorphologyAnalysisMode.NEW).call();
		return d;
	}
	
	public TestDatasetBuilder profiled() throws Exception {
		profile = true;
		return this;
	}
	
	public TestDatasetBuilder segmented() throws Exception {
		segment = true;
		return this;
	}
	
	public TestDatasetBuilder ofType(NucleusType type) {
		this.type = type;
		return this;
	}
	
	public TestDatasetBuilder cellCount(int i) {
		nCells = i;
		return this;
	}
	
	public TestDatasetBuilder withMaxSizeVariation(int i) {
		maxVariation = i;
		return this;
	}
	
	public TestDatasetBuilder xBase(int i) {
		xBase = i;
		return this;
	}
	
	public TestDatasetBuilder yBase(int i) {
		yBase = i;
		return this;
	}
	
	public TestDatasetBuilder baseWidth(int i) {
		w = i;
		return this;
	}
	
	public TestDatasetBuilder baseHeight(int i) {
		h = i;
		return this;
	}
	
	public TestDatasetBuilder maxRotation(int i) {
		maxRotation = i;
		return this;
	}	
	
	public TestDatasetBuilder offsetProfiles(boolean b) {
		offset = b;
		return this;
	}	
	
	public TestDatasetBuilder fixedProfileOffset(int i) {
		fixedOffset = i;
		return this;
	}	

}
