package com.bmskinner.nuclear_morphology.components;

import java.io.File;
import java.util.Random;

import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetProfilingMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod.MorphologyAnalysisMode;
import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;

/**
 * Simplify the creation of test datasets using a builder pattern
 * @author bms41
 * @since 1.14.0
 *
 */
public class TestDatasetBuilder {
	
	public static final int DEFAULT_VARIATION   = 0;
	public static final int DEFAULT_BASE_WIDTH  = 40;
	public static final int DEFAULT_BASE_HEIGHT = 50;
	public static final int DEFAULT_X_BASE      = 100;
	public static final int DEFAULT_Y_BASE      = 100;
	public static final int DEFAULT_ROTATION    = 0;
	public static final int DEFAULT_BORDER_OFFSET = 0;
	public static final boolean DEFAULT_IS_RANDOM_OFFSET = true;
	
	private IAnalysisDataset d;
	private NucleusType type = NucleusType.ROUND;
	private int nCells = 1;
	private int maxVariation = DEFAULT_VARIATION;
	private int xBase = DEFAULT_X_BASE;
	private int yBase = DEFAULT_Y_BASE;
	private int w = DEFAULT_BASE_WIDTH;
	private int h = DEFAULT_BASE_HEIGHT;
	private int fixedOffset = DEFAULT_BORDER_OFFSET;
	private int maxRotation = DEFAULT_ROTATION;
	
	private boolean profile = false;
	private boolean segment = false;
	private boolean offset  = DEFAULT_IS_RANDOM_OFFSET;
	
	private TestComponentShape nucleusShape = TestComponentShape.SQUARE;
	
	private Random rng;
	
	public enum TestComponentShape {
		SQUARE, ROUND
	}
	
	/**
	 * Construct with a randomly chosen seed
	 */
	public TestDatasetBuilder() {
		this(new Random().nextLong());
	}
	
	/**
	 * Construct with a given seed for the random number
	 * generator
	 */
	public TestDatasetBuilder(long seed) {
		rng = new Random(seed);
	}
		
	public IAnalysisDataset build() throws Exception {
				
		switch(nucleusShape) {
		case SQUARE: 
		default: d = variableRectangularDataset(nCells, type, maxVariation, w, h, xBase, yBase, maxRotation, offset, fixedOffset);
		}
		d.setRoot(true);
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
	
	public TestDatasetBuilder withNucleusShape(TestComponentShape shape) throws Exception {
		nucleusShape = shape;
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
	
	public TestDatasetBuilder randomOffsetProfiles(boolean b) {
		offset = b;
		return this;
	}	
	
	public TestDatasetBuilder fixedProfileOffset(int i) {
		fixedOffset = i;
		return this;
	}	
	
	/**
	 * Create a dataset consisting of rectangular nuclei. Each nucleus has a random width and
	 * size constrained by the variation factor
	 * @param nCells the number of cells to create
	 * @param maxSizeVariation the maximum variation from the base width and height for a cell
	 * @param baseWidth the starting width for a cell, before variation
	 * @param baseHeight the starting heigth for a cell, before variation
	 * @param xBase the starting x position
	 * @param yBase the starting y position
	 * @param maxRotationDegrees the maximum rotation to be applied to a cell 
	 * @param randomOffsetStart should an offset be applied to the border array
	 * @param fixedStartOffset an offset to apply to the border array; has no effect if randomOffsetStart is true
	 * @return
	 * @throws ComponentCreationException
	 */
	private IAnalysisDataset variableRectangularDataset(int nCells, NucleusType type, int maxSizeVariation, int baseWidth, int baseHeight, int xBase, int yBase, int maxRotationDegrees, boolean randomOffsetStart, int fixedStartOffset) throws ComponentCreationException {
		
		ICellCollection collection = new DefaultCellCollection(new File("empty folder"), "Test", "Test", type);

		for(int i=0; i<nCells; i++) {
			
			int wVar = (int)(rng.nextDouble()*maxSizeVariation);
			int hVar = (int)(rng.nextDouble()*maxSizeVariation);
			int width = (rng.nextDouble()<0.5)?baseWidth-wVar:baseWidth+wVar;
			int height = (rng.nextDouble()<0.5)?baseHeight-hVar:baseHeight+hVar;
			double degreeRot = (rng.nextDouble()*maxRotationDegrees);
			
			int borderLength = w*2+h*2;
			int borderOffset = randomOffsetStart ? (int) (rng.nextDouble()*(double)borderLength) : fixedStartOffset;
			
			ICell cell = TestComponentFactory.rectangularCell(width, height, xBase, yBase, degreeRot, 
					borderOffset);
			
			Nucleus n = cell.getNucleus();
			
			collection.addCell(cell);
		}
		return new DefaultAnalysisDataset(collection);
	}
}
