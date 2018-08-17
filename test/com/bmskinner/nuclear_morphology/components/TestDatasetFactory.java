package com.bmskinner.nuclear_morphology.components;

import java.io.File;

import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetProfilingMethod;
import com.bmskinner.nuclear_morphology.components.DefaultAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.DefaultCellCollection;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;

/**
 * Create test datasets for unit testing
 * @author bms41
 * @since 1.14.0
 *
 */
public class TestDatasetFactory {
	
	public static final int DEFAULT_BASE_WIDTH = 40;
	public static final int DEFAULT_BASE_HEIGHT = 50;
	public static final int DEFAULT_X_BASE = 100;
	public static final int DEFAULT_Y_BASE = 100;
	public static final int DEFAULT_ROTATION = 0;
	public static final int DEFAULT_BORDER_OFFSET = 20;
	public static final boolean DEFAULT_IS_BORDER_OFFSET = true;
	
	/**
	 * Run profiling on the given dataset and return the same dataset
	 * @param d
	 * @return
	 * @throws Exception
	 */
	public static IAnalysisDataset profileDataset(IAnalysisDataset d) throws Exception {
		DatasetProfilingMethod m = new DatasetProfilingMethod(d);
		m.call();
		return d;
	}
	
	/**
	 * Create a dataset consisting of square nuclei.
	 * @return
	 * @throws ComponentCreationException 
	 */
	public static IAnalysisDataset squareDataset(int nCells) throws ComponentCreationException {
		
		ICellCollection collection = new DefaultCellCollection(new File("empty folder"), "Test", "Test", NucleusType.ROUND);
		
		for(int i=0; i<nCells; i++) {
			collection.addCell(TestComponentFactory.squareCell(40));
		}
		
		
		return new DefaultAnalysisDataset(collection);
		
	}
	
	/**
	 * Create a dataset consisting of identical rectangular nuclei. Values are set to the factory
	 * defaults.
	 * @param nCells the number of cells in the dataset
	 * @return
	 * @throws ComponentCreationException
	 */
	public static IAnalysisDataset identicalRectangularDataset(int nCells) throws ComponentCreationException {
		ICellCollection collection = new DefaultCellCollection(new File("empty folder"), "Test", "Test", NucleusType.ROUND);

		for(int i=0; i<nCells; i++) {
			ICell cell = TestComponentFactory.rectangularCell(DEFAULT_BASE_WIDTH, DEFAULT_BASE_HEIGHT, 
					DEFAULT_X_BASE, DEFAULT_Y_BASE, DEFAULT_ROTATION, false, DEFAULT_BORDER_OFFSET);
			collection.addCell(cell);
		}
		return new DefaultAnalysisDataset(collection);
	}
	
	/**
	 * Create a dataset consisting of rectangular nuclei. Each nucleus has a random width and
	 * size constrained by the variation factor
	 * @param nCells
	 * @param maxVariation
	 * @return
	 * @throws ComponentCreationException
	 */
	public static IAnalysisDataset variableRectangularDataset(int nCells, int maxVariation) throws ComponentCreationException {
		return variableRectangularDataset(nCells, maxVariation, DEFAULT_BASE_WIDTH, DEFAULT_BASE_HEIGHT,
				DEFAULT_X_BASE, DEFAULT_Y_BASE, DEFAULT_ROTATION, DEFAULT_IS_BORDER_OFFSET, DEFAULT_BORDER_OFFSET);
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
	public static IAnalysisDataset variableRectangularDataset(int nCells, int maxSizeVariation, int baseWidth, int baseHeight, int xBase, int yBase, int maxRotationDegrees, boolean randomOffsetStart, int fixedStartOffset) throws ComponentCreationException {
		
		ICellCollection collection = new DefaultCellCollection(new File("empty folder"), "Test", "Test", NucleusType.ROUND);

		for(int i=0; i<nCells; i++) {
			
			int wVar = (int)(Math.random()*maxSizeVariation);
			int hVar = (int)(Math.random()*maxSizeVariation);
			int width = (Math.random()<0.5)?baseWidth-wVar:baseWidth+wVar;
			int height = (Math.random()<0.5)?baseHeight-hVar:baseHeight+hVar;
			double degreeRot = (Math.random()*maxRotationDegrees);
			
			ICell cell = TestComponentFactory.rectangularCell(width, height, xBase, yBase, degreeRot, 
					randomOffsetStart, fixedStartOffset);
			
			Nucleus n = cell.getNucleus();
			
			collection.addCell(cell);
			System.out.println("Nucleus "+i);
//			for(IBorderPoint b : n.getBorderList()) {
//				System.out.println(b.toString());
//			}
		}
		
		
		return new DefaultAnalysisDataset(collection);
		
	}

}
