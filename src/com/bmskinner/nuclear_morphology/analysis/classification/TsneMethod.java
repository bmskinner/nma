package com.bmskinner.nuclear_morphology.analysis.classification;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

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
import com.bmskinner.nuclear_morphology.components.generic.UnavailableComponentException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.jujutsu.tsne.TSneConfiguration;
import com.jujutsu.tsne.barneshut.BarnesHutTSne;
import com.jujutsu.tsne.barneshut.ParallelBHTsne;
import com.jujutsu.utils.TSneUtils;

/**
 * This method takes a dataset and generates a t-SNE
 * from cell stats. The downside of this over exporting the data
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
public class TsneMethod  extends SingleDatasetAnalysisMethod {
	
	private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	
	private static final int OUTPUT_DIMENSIONS = 2;
	
	public static final String PROFILE_TYPE_KEY   = "Profile type";
	public static final String MAX_ITERATIONS_KEY = "Max iterations";
	public static final String PERPLEXITY_KEY     = "Perplexity";
	public static final String INITIAL_DIMS_KEY   = "Initial dimensions";

	private final HashOptions options;
	
	/**
	 * Create the t-SNE method with a dataset to analyse, and appropriate options
	 * @param dataset the dataset which tSNE should be run on 
	 * @param tSneOptions the tSNE options
	 */
	public TsneMethod(@NonNull final IAnalysisDataset dataset, @NonNull HashOptions tSneOptions) {
		super(dataset);
		this.options = tSneOptions;
	}

	@Override
	public IAnalysisResult call() throws Exception {
		
		int maxIterations = options.getInt(MAX_ITERATIONS_KEY);
		double perplexity = options.getDouble(PERPLEXITY_KEY);
		
		LOGGER.fine("Running tSNE with p"+perplexity+" and i"+maxIterations);
		
		// Calculate from options
		int initialDims = calculateNumberOfDimensions();
		
		// Create the matrix for profile values with consistent cell order
		List<Nucleus> nuclei = new ArrayList<>(dataset.getCollection().getNuclei());
		
		double[][] profileMatrix = makeProfileMatrix(nuclei, initialDims);

	    TSneConfiguration config = TSneUtils.buildConfig(profileMatrix, OUTPUT_DIMENSIONS, initialDims, perplexity, maxIterations);
	    BarnesHutTSne tsne = new ParallelBHTsne(); // may not play well with the thread manager. If single thread, use BHTSne()
		double [][] tSneResult = tsne.tsne(config); 
		
		// store this in the cell collection, attached to each cell. This is a temporary store - 
		// if used for clustering, it should be attached to the cluster id
		for(int i=0; i<nuclei.size(); i++) {
			Nucleus n = nuclei.get(i);	
			n.setStatistic(PlottableStatistic.TSNE_1, tSneResult[i][0]);
			n.setStatistic(PlottableStatistic.TSNE_2, tSneResult[i][1]);
		}
		
		Optional<IAnalysisOptions> analysisOptions = dataset.getAnalysisOptions();
		if(analysisOptions.isPresent())
			analysisOptions.get().setSecondaryOptions(IAnalysisOptions.TSNE, options);
		
		return new DefaultAnalysisResult(dataset);
	}

	/**
	 * Create the matrix of values for dimensional reduction
	 * @param nuclei
	 * @return
	 * @throws ProfileException
	 * @throws UnavailableComponentException
	 */
	private double[][] makeProfileMatrix(List<Nucleus> nuclei, int initialDims) throws ProfileException, UnavailableComponentException {
		double[][] matrix = new double[nuclei.size()][initialDims];		
				
		for(int i=0; i<nuclei.size(); i++) {
			int j = 0;
			Nucleus n = nuclei.get(i);	
			for(ProfileType t : ProfileType.displayValues()) {
				if(!options.getBoolean(t.toString()))
					continue;
				
				IProfile p = n.getProfile(t, Tag.REFERENCE_POINT);
				for (int k = 0; k < 100; k++) {
					double idx = ((double) k) / 100d;
					matrix[i][j++] = p.get(idx);
				}
			}
			
			
			for (PlottableStatistic stat : PlottableStatistic.getNucleusStats((dataset.getCollection().getNucleusType()))) {
				if(!options.getBoolean(stat.toString()))
					continue;
				matrix[i][j++] = n.getStatistic(stat);
			}
			
			for (IBorderSegment s : dataset.getCollection().getProfileCollection().getSegments(Tag.REFERENCE_POINT)) {
				if(!options.getBoolean(s.getID().toString()))
					continue;
				
				IBorderSegment seg = n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT).getSegment(s.getID());
                double proportionPerimeter = (double) seg.length() / (double) seg.getProfileLength();
				matrix[i][j++] = n.getStatistic(PlottableStatistic.PERIMETER) * proportionPerimeter;
			}
		}
		return matrix;
	}
	
	/**
	 * Count the number of dimensions to be reduced, based on the input options
	 * @return
	 */
	private int calculateNumberOfDimensions() {
		int dimensions = 0;
		for (PlottableStatistic stat : PlottableStatistic.getNucleusStats((dataset.getCollection().getNucleusType())))
			if(options.getBoolean(stat.toString()))
				dimensions++;
		
		for(ProfileType t : ProfileType.displayValues())
			if(options.getBoolean(t.toString()))
				dimensions+=100;
		
		try {
			for (IBorderSegment s : dataset.getCollection().getProfileCollection().getSegments(Tag.REFERENCE_POINT))
				if(options.getBoolean(s.getID().toString()))
					dimensions++;
			
		} catch(ProfileException | UnavailableBorderTagException e) {
			LOGGER.log(Loggable.STACK, "Unable to get segments", e);
		}
		
		
		return dimensions;

	}

	
}
