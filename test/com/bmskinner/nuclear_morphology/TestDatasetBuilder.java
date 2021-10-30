package com.bmskinner.nuclear_morphology;

import java.awt.Color;
import java.io.File;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;

import com.bmskinner.nuclear_morphology.analysis.classification.NucleusClusteringMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetProfilingMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod.MorphologyAnalysisMode;
import com.bmskinner.nuclear_morphology.components.TestComponentFactory;
import com.bmskinner.nuclear_morphology.components.cells.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.datasets.DefaultAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.datasets.DefaultCellCollection;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;
import com.bmskinner.nuclear_morphology.components.signals.DefaultSignalGroup;
import com.bmskinner.nuclear_morphology.components.signals.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.signals.ISignalGroup;

/**
 * Simplify the creation of test datasets using a builder pattern
 * @author bms41
 * @since 1.14.0
 *
 */
public class TestDatasetBuilder {
	
	private static final Logger LOGGER = Logger.getLogger(TestDatasetBuilder.class.getName());
	
	public static final String TEST_DATASET_NAME = "Test";
	public static final String TEST_DATASET_IMAGE_FOLDER = "Image folder";
	public static final UUID   TEST_DATASET_UUID = UUID.fromString("99998888-0000-6666-1111-444433332222");

	public static final int DEFAULT_VARIATION   = 0;
	public static final int DEFAULT_BASE_WIDTH  = 40;
	public static final int DEFAULT_BASE_HEIGHT = 50;
	public static final int DEFAULT_X_BASE      = 100;
	public static final int DEFAULT_Y_BASE      = 100;
	public static final int DEFAULT_ROTATION    = 0;
	public static final int DEFAULT_BORDER_OFFSET = 0;
	public static final int DEFAULT_CHILD_CLUSTERS = 0;
	public static final int DEFAULT_N_CELLS     = 1;
	
	/** Should the start index of the border list be randomly offset? */
	public static final boolean DEFAULT_IS_RANDOM_OFFSET = true;
	
	public static final boolean DEFAULT_RED_SIGNALS = false;
	public static final boolean DEFAULT_GREEN_SIGNALS = false;
	
	/** The default shape for nuclei; a square */
	public static final TestComponentShape DEFAULT_NUCLEUS_SHAPE = TestComponentShape.SQUARE;
	
	private IAnalysisDataset d;
	private RuleSetCollection rsc = RuleSetCollection.roundRuleSetCollection();
	private int nCells = DEFAULT_N_CELLS;
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
	
	private boolean redSignals = DEFAULT_RED_SIGNALS;
	private boolean greenSignals = DEFAULT_GREEN_SIGNALS;
	
	private int nClusters = DEFAULT_CHILD_CLUSTERS;
	
	public static final UUID RED_SIGNAL_GROUP = UUID.fromString("99998888-7777-6666-5555-444433332222");
	public static final UUID GREEN_SIGNAL_GROUP = UUID.fromString("88887777-6666-5555-4444-333322221111");
	
	public static final String RED_SIGNAL_GROUP_NAME = "Red";
	public static final String GREEN_SIGNAL_GROUP_NAME = "Green";
	
	
	private TestComponentShape nucleusShape = DEFAULT_NUCLEUS_SHAPE;
	
	private Random rng;
	
	public enum TestComponentShape {
		/** A rectangle */
		SQUARE, 
		
		/** An ellipse */
		ROUND
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
	 * @param seed the seed value
	 */
	public TestDatasetBuilder(long seed) {
		rng = new Random(seed);
	}
		
	/**
	 * Construct a new dataset based on the parameters in this builder.
	 * @return a new dataset
	 * @throws Exception
	 */
	public IAnalysisDataset build() throws Exception {
		LOGGER.finest("Building dataset");		
		switch(nucleusShape) {
		case SQUARE: 
		default: d = createRectangularDataset(nCells, rsc, maxVariation, w, h, xBase, 
				yBase, maxRotation, offset, fixedOffset);
		}

		if(segment || profile)
			new DatasetProfilingMethod(d).call();
		if(segment)
			new DatasetSegmentationMethod(d, MorphologyAnalysisMode.NEW).call();
		
		if(nClusters>0) {
			LOGGER.finest("Clustering dataset");		
			HashOptions o = OptionsFactory.makeDefaultClusteringOptions()
					.withValue(HashOptions.CLUSTER_MANUAL_CLUSTER_NUMBER_KEY, nClusters)
					.build();
			new NucleusClusteringMethod(d, o).call();
		}
				
		return d;
	}
	
	/**
	 * The new dataset should have a profiling method applied.
	 * @return this builder
	 * @throws Exception
	 */
	public TestDatasetBuilder profiled() throws Exception {
		profile = true;
		return this;
	}
	
	/**
	 * The new dataset should have a segmentation method applied. This
	 * option sets profiling automatically; i.e. {@code builder.segmented()} is
	 * equivalent to {@code builder.profiled().segmented()}.
	 * @return this builder
	 * @throws Exception
	 */
	public TestDatasetBuilder segmented() throws Exception {
		segment = true;
		return this;
	}
	
	/**
	 * The shape for the nuclei. The default shape is {@link #DEFAULT_NUCLEUS_SHAPE}.
	 * @param shape the shape to create.
	 * @return this builder
	 * @throws Exception
	 */
	public TestDatasetBuilder withNucleusShape(TestComponentShape shape) throws Exception {
		nucleusShape = shape;
		return this;
	}
	
	public TestDatasetBuilder ofType(RuleSetCollection rsc) {
		this.rsc = rsc;
		return this;
	}
	
	/**
	 * Set the number of cells in the  dataset. 
	 * Default value {@link #DEFAULT_N_CELLS}.
	 * @param i the number of cells
	 * @return this builder
	 */
	public TestDatasetBuilder cellCount(int i) {
		nCells = i;
		return this;
	}
	
	/**
	 * Set the number of child datasets to create
	 * by clustering
	 * Default value {@link #DEFAULT_CHILD_CLUSTERS}.
	 * @param i the number of clusters
	 * @return this builder
	 */
	public TestDatasetBuilder numberOfClusters(int i) {
		nClusters = i;
		return this;
	}
	
	/**
	 * Set the maximum amount of variation in size of 
	 * the nuclei
	 * Default value {@link #DEFAULT_VARIATION}.
	 * @param i the maximum variation in size
	 * @return this builder
	 */
	public TestDatasetBuilder withMaxSizeVariation(int i) {
		maxVariation = i;
		return this;
	}
	
	/**
	 * Set the minimum x coordinate for nuclei
	 * Default value {@link #DEFAULT_X_BASE}.
	 * @param i the base x
	 * @return this builder
	 */
	public TestDatasetBuilder xBase(int i) {
		xBase = i;
		return this;
	}
	
	/**
	 * Set the minimum y coordinate for nuclei
	 * Default value {@link #DEFAULT_Y_BASE}.
	 * @param i the base x
	 * @return this builder
	 */
	public TestDatasetBuilder yBase(int i) {
		yBase = i;
		return this;
	}
	
	/**
	 * Set the starting width for nuclei before any variations are added.
	 * Default value {@link #DEFAULT_BASE_WIDTH}.
	 * @param i the base width
	 * @return this builder
	 */
	public TestDatasetBuilder baseWidth(int i) {
		w = i;
		return this;
	}
	
	/**
	 * Set the starting height for nuclei before any variations are added.
	 * Default value {@link #DEFAULT_BASE_HEIGHT}.
	 * @param i the base height
	 * @return this builder
	 */
	public TestDatasetBuilder baseHeight(int i) {
		h = i;
		return this;
	}
	
	/**
	 * The maximum random rotation to be applied to a cell after creation.
	 * This simulates the random orientation of cells in an image. Each cell
	 * will be rotated by a random value between zero and the given value. If this
	 * is set to zero, no rotations will be applied. The default value is {@link #DEFAULT_ROTATION}.
	 * @param i the maximum angle of rotation in degrees. 
	 * @return this builder
	 */
	public TestDatasetBuilder maxRotation(int i) {
		maxRotation = i;
		return this;
	}	
	
	/**
	 * Should the start index of the border list be randomly offset?
	 * This simulates detected objects, which may not have their borders
	 * starting in a neat position for segmentation or profiling. The
	 * default value is {@link #DEFAULT_IS_RANDOM_OFFSET}.
	 * @param b should a random offset be applied
	 * @return this builder
	 */
	public TestDatasetBuilder randomOffsetProfiles(boolean b) {
		offset = b;
		return this;
	}	
	
	/**
	 * Set a fixed offset value for the start index of the border list
	 * in each cell. The default value is {@link #DEFAULT_BORDER_OFFSET}.
	 * Setting this paramter disables random offsets in profiles.
	 * @param i
	 * @see #randomOffsetProfiles(boolean)
	 * @return this builder
	 */
	public TestDatasetBuilder fixedProfileOffset(int i) {
		offset=false;
		fixedOffset = i;
		return this;
	}	
	
	/**
	 * Create nuclear signals in the given red or green channel.
	 * @param i the RGB channel to add signals - 0 or 1
	 * @return this builder
	 */
	public TestDatasetBuilder addSignalsInChannel(int i) {
		if(i==0)
			redSignals = true;
		if(i==1)
			greenSignals = true;
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
	 * @param fixedStartOffset the offset to apply to the border array if randomOffsetStart is false
	 * @return
	 * @throws ComponentCreationException
	 */
	private IAnalysisDataset createRectangularDataset(int nCells, RuleSetCollection rsc, int maxSizeVariation, int baseWidth, int baseHeight, int xBase, int yBase, int maxRotationDegrees, boolean randomOffsetStart, int fixedStartOffset) throws ComponentCreationException {
		
		ICellCollection collection = new DefaultCellCollection(rsc, TEST_DATASET_NAME, TEST_DATASET_UUID);
		
		IAnalysisOptions o =  OptionsFactory.makeDefaultRoundAnalysisOptions(new File(TEST_DATASET_IMAGE_FOLDER).getAbsoluteFile());
		o.getNucleusDetectionOptions().get().setInt(HashOptions.MIN_SIZE_PIXELS, (baseWidth-maxSizeVariation)*(baseHeight-maxSizeVariation) );
		o.getNucleusDetectionOptions().get().setInt(HashOptions.MAX_SIZE_PIXELS, (baseWidth+maxSizeVariation)*(baseHeight+maxSizeVariation) );

		if(redSignals) {
			ISignalGroup g = new DefaultSignalGroup(RED_SIGNAL_GROUP_NAME, RED_SIGNAL_GROUP);
			g.setGroupColour(Color.red);
			collection.addSignalGroup(g);
			HashOptions n = OptionsFactory.makeNuclearSignalOptions(new File(TEST_DATASET_IMAGE_FOLDER))
					.withValue(HashOptions.SIGNAL_GROUP_ID, RED_SIGNAL_GROUP.toString())
					.withValue(HashOptions.SIGNAL_GROUP_NAME, RED_SIGNAL_GROUP_NAME)
					.build();
			o.setNuclearSignalDetectionOptions(n);
		}
		
		if(greenSignals) {
			ISignalGroup g = new DefaultSignalGroup(GREEN_SIGNAL_GROUP_NAME, GREEN_SIGNAL_GROUP);
			g.setGroupColour(Color.GREEN);
			collection.addSignalGroup(g);
			HashOptions n = OptionsFactory.makeNuclearSignalOptions(new File(TEST_DATASET_IMAGE_FOLDER))
					.withValue(HashOptions.SIGNAL_GROUP_ID, GREEN_SIGNAL_GROUP.toString())
					.withValue(HashOptions.SIGNAL_GROUP_NAME, GREEN_SIGNAL_GROUP_NAME)
					.build();
			o.setNuclearSignalDetectionOptions(n);
		}

		for(int i=0; i<nCells; i++) {
			
			int wVar = (int)(rng.nextDouble()*maxSizeVariation);
			int hVar = (int)(rng.nextDouble()*maxSizeVariation);
			int width = (rng.nextDouble()<0.5)?baseWidth-wVar:baseWidth+wVar;
			int height = (rng.nextDouble()<0.5)?baseHeight-hVar:baseHeight+hVar;
			double degreeRot = (rng.nextDouble()*maxRotationDegrees);
			
			int borderLength = (width+height)*2;
			int borderOffset = randomOffsetStart ? (int) (rng.nextDouble()*borderLength) : fixedStartOffset;
			
			ICell cell = createCell(width, height, degreeRot, borderOffset, rsc);	
			cell.getPrimaryNucleus().setStatistic(Measurement.AREA, width*height);
			collection.addCell(cell);
			
			
			if(redSignals) {
				INuclearSignal s = TestComponentFactory.createSignal(cell.getPrimaryNucleus(), 0.2, 0);
				cell.getPrimaryNucleus().getSignalCollection().addSignal(s, RED_SIGNAL_GROUP);
			}
			
			if(greenSignals) {
				INuclearSignal s = TestComponentFactory.createSignal(cell.getPrimaryNucleus(), 0.2, 1);
				cell.getPrimaryNucleus().getSignalCollection().addSignal(s, GREEN_SIGNAL_GROUP);
			}
			
		}
		
		IAnalysisDataset d = new DefaultAnalysisDataset(collection, new File(TEST_DATASET_IMAGE_FOLDER).getAbsoluteFile());
		d.setAnalysisOptions(o);

		return d;
	}

	private ICell createCell(int width, int height, double degreeRot, int borderOffset, RuleSetCollection rsc) throws ComponentCreationException {
		switch(nucleusShape) {
			case SQUARE: return  TestComponentFactory.rectangularCell(width, height, xBase, yBase, degreeRot, 
					borderOffset, rsc);
			case ROUND:
			default: return  TestComponentFactory.roundCell(width, height, xBase, yBase, degreeRot, 
					borderOffset, rsc);

		}
	}

}
