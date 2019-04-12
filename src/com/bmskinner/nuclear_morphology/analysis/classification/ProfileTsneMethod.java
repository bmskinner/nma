package com.bmskinner.nuclear_morphology.analysis.classification;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.jujutsu.tsne.TSneConfiguration;
import com.jujutsu.tsne.barneshut.BHTSne;
import com.jujutsu.tsne.barneshut.BarnesHutTSne;
import com.jujutsu.tsne.barneshut.ParallelBHTsne;
import com.jujutsu.utils.MatrixOps;
import com.jujutsu.utils.MatrixUtils;
import com.jujutsu.utils.TSneUtils;

/**
 * This method takes a dataset and generates a t-SNE
 * from angle profiles. The downside of this over exporting the data
 * into R is that the Barnes-Hut t-SNE implementation uses 
 * ThreadLocalRandom random number generation, so there is no 
 * opportunity to set a seed. This means every run will give a different
 * result. Consequently, it will be better to use this for first pass
 * analysis, and decide what to export, rather than relying on this for
 * full analysis. It also does not easily allow for testing hyperparameters
 * to choose the best options.
 * @author bms41
 * @since 1.16.0
 *
 */
public class ProfileTsneMethod  extends SingleDatasetAnalysisMethod {
	
	private static final int OUTPUT_DIMENSIONS = 2;
	
	public static final String MAX_ITERATIONS_KEY = "Max iterations";
	public static final String PERPLEXITY_KEY     = "Perplexity";
	public static final String INITIAL_DIMS_KEY   = "Initial dimensions";

	private final HashOptions tSneOptions;
	
	/**
	 * Analysis result for tSNE data. Contains the raw tSNE output
	 * and the map of which cell is in each row 
	 * @author bms41
	 * @since 1.16.0
	 *
	 */
	public class TsneResult extends DefaultAnalysisResult {
		
		public final double[][] tSneOutput;
		public final Map<Integer, UUID> cellIds;

		public TsneResult(IAnalysisDataset d, double[][] matrix, Map<Integer, UUID> ids) {
			super(d);
			tSneOutput = matrix;
			cellIds = ids;
		}
		
	}

	/**
	 * Create the t-SNE method with a dataset to analyse, and appropriate options
	 * @param dataset the dataset which tSNE should be run on 
	 * @param tSneOptions the tSNE options
	 */
	public ProfileTsneMethod(@NonNull final IAnalysisDataset dataset, @NonNull HashOptions tSneOptions) {
		super(dataset);
		this.tSneOptions = tSneOptions;
	}

	@Override
	public IAnalysisResult call() throws Exception {
		
		int maxIterations = tSneOptions.getInt(MAX_ITERATIONS_KEY);
		double perplexity = tSneOptions.getDouble(PERPLEXITY_KEY);
		
		log("Running tSNE with p"+perplexity+" and i"+maxIterations);
		
		// We can calculate initial dimensions from the profile - it's 100
		int initialDims   = 100;
		
		// Create the matrix for profile values with consistent cell order
		List<ICell> cells = new ArrayList<>(dataset.getCollection().getCells());
		Map<Integer, UUID> cellIds = new HashMap<>();
		for(int i=0; i<cells.size(); i++)
			cellIds.put(i, cells.get(i).getId());
		
		double[][] profileMatrix = makeProfileMatrix(cells);

//	    double [][] X = MatrixUtils.simpleRead2DMatrix(new File("src/main/resources/datasets/mnist2500_X.txt"), "   ");
//	    System.out.println(MatrixOps.doubleArrayToPrintString(X, ", ", 50,10));
		
	    TSneConfiguration config = TSneUtils.buildConfig(profileMatrix, OUTPUT_DIMENSIONS, initialDims, perplexity, maxIterations);
	    BarnesHutTSne tsne = new ParallelBHTsne(); // may not play well with the thread manager
		double [][] tSneResult = tsne.tsne(config); 
		
		// Do we store this in the cell collection, attached to each cell, or return the matrix directly?
		
//	    boolean parallel = false;
//		if(parallel) {			
//			tsne = new ParallelBHTsne();
//		} else {
//			tsne = new BHTSne();
//		}
	    
		return new TsneResult(dataset, tSneResult, cellIds);
	}

	private double[][] makeProfileMatrix(List<ICell> cells) throws UnavailableBorderTagException, UnavailableProfileTypeException, ProfileException{
		double[][] matrix = new double[cells.size()][100];

		for(int i=0; i<cells.size(); i++) {
			ICell c = cells.get(i);	
			Nucleus n = c.getNucleus();
			IProfile p = n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);

			for (int j = 0; j < 100; j++) {
				double idx = ((double) j) / 100d;
				matrix[i][j] = p.get(idx);
			}
		}
		return matrix;
	}

}
