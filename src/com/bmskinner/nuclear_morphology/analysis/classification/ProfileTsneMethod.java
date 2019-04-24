package com.bmskinner.nuclear_morphology.analysis.classification;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.jujutsu.tsne.TSneConfiguration;
import com.jujutsu.tsne.barneshut.BarnesHutTSne;
import com.jujutsu.tsne.barneshut.ParallelBHTsne;
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
	
	public static final String PROFILE_TYPE_KEY   = "Profile type";
	public static final String MAX_ITERATIONS_KEY = "Max iterations";
	public static final String PERPLEXITY_KEY     = "Perplexity";
	public static final String INITIAL_DIMS_KEY   = "Initial dimensions";

	private final HashOptions tSneOptions;
	
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
		
		fine("Running tSNE with p"+perplexity+" and i"+maxIterations);
		
		// We know initial dimensions from the profile - it's 100
		int initialDims = 100;
		
		// Create the matrix for profile values with consistent cell order
		List<Nucleus> nuclei = new ArrayList<>(dataset.getCollection().getNuclei());
		
		double[][] profileMatrix = makeProfileMatrix(nuclei);

	    TSneConfiguration config = TSneUtils.buildConfig(profileMatrix, OUTPUT_DIMENSIONS, initialDims, perplexity, maxIterations);
	    BarnesHutTSne tsne = new ParallelBHTsne(); // may not play well with the thread manager. If single thread, use BHTSne()
		double [][] tSneResult = tsne.tsne(config); 
		
		// store this in the cell collection, attached to each cell
		for(int i=0; i<nuclei.size(); i++) {
			Nucleus n = nuclei.get(i);	
			n.setStatistic(PlottableStatistic.TSNE_1, tSneResult[i][0]);
			n.setStatistic(PlottableStatistic.TSNE_2, tSneResult[i][1]);
		}
		
		Optional<IAnalysisOptions> analysisOptions = dataset.getAnalysisOptions();
		if(analysisOptions.isPresent())
			analysisOptions.get().setSecondaryOptions(IAnalysisOptions.TSNE, tSneOptions);
		
		return new DefaultAnalysisResult(dataset);
	}

	private double[][] makeProfileMatrix(List<Nucleus> nuclei) throws UnavailableBorderTagException, UnavailableProfileTypeException, ProfileException {
		double[][] matrix = new double[nuclei.size()][100];
		
		ProfileType profileType = ProfileType.fromString(tSneOptions.getString(PROFILE_TYPE_KEY));
		if(profileType==null)
			throw new UnavailableProfileTypeException("Cannot find profile type "+profileType);
		
		for(int i=0; i<nuclei.size(); i++) {
			Nucleus n = nuclei.get(i);	
			IProfile p = n.getProfile(profileType, Tag.REFERENCE_POINT);

			for (int j = 0; j < 100; j++) {
				double idx = ((double) j) / 100d;
				matrix[i][j] = p.get(idx);
			}
		}
		return matrix;
	}

}
