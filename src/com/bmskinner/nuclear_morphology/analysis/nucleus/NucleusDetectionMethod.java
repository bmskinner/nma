package com.bmskinner.nuclear_morphology.analysis.nucleus;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bmskinner.nuclear_morphology.analysis.AbstractAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.ProgressEvent;
import com.bmskinner.nuclear_morphology.analysis.detection.pipelines.Finder;
import com.bmskinner.nuclear_morphology.analysis.detection.pipelines.FluorescentNucleusFinder;
import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.DefaultAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.DefaultCellCollection;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.MissingOptionException;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;

/**
 * The method for finding nuclei in fluorescence images
 * @author bms41
 * @since 1.13.4
 *
 */
public class NucleusDetectionMethod extends AbstractAnalysisMethod {

	private static final String SPACER = "---------";
	private static final double DEFAULT_FILTERING_DELTA = 1.6;

	private final String outputFolder;

	private final IMutableAnalysisOptions analysisOptions;

	private Map<File, ICellCollection> collectionGroup = new HashMap<File, ICellCollection>();

	List<IAnalysisDataset> datasets;

	/**
	 * Construct a detector on the given folder, and output the results to 
	 * the given output folder
	 * @param outputFolder the name of the folder for results
	 * @param programLogger the logger to the log panel
	 * @param debugFile the dataset log file
	 * @param options the options to detect with
	 */
	public NucleusDetectionMethod(String outputFolder, File debugFile, IMutableAnalysisOptions options){
		super(null);
		this.outputFolder 	= outputFolder;
		this.analysisOptions 	= options;
	}



	@Override
	public IAnalysisResult call() throws Exception {

		run();		
		IAnalysisResult r = new DefaultAnalysisResult(datasets);
		return r;
	}

	public void run(){
		

		try{

			getTotalImagesToAnalyse();

			log("Running nucleus detector");
			
			// Detect the nuclei in the folders selected
			processFolder(analysisOptions.getDetectionOptions(IAnalysisOptions.NUCLEUS).getFolder());

			fine("Detected nuclei in "+analysisOptions.getDetectionOptions(IAnalysisOptions.NUCLEUS).getFolder().getAbsolutePath());

			fine( "Creating cell collections");

			// Get the collections containing nuclei
			List<ICellCollection> folderCollection = this.getNucleiCollections();

			// Run the analysis pipeline

			fine("Analysing collections");

			// Filter the datasets
			datasets = analysePopulations(folderCollection);		

			fine( "Analysis complete; return collections");

		} catch(Exception e){
			stack("Error in processing folder", e);
		}

	}


	private void getTotalImagesToAnalyse(){
		
		log("Counting images to analyse");
		try {
			
			File folder = analysisOptions.getDetectionOptions(IAnalysisOptions.NUCLEUS).getFolder();
			int totalImages = countSuitableImages(folder);
			fireProgressEvent(new ProgressEvent(this, ProgressEvent.SET_TOTAL_PROGRESS, totalImages));
			log("Analysing "+totalImages+" images");
			
		} catch (MissingOptionException e) {
			warn("No folder to analyse");
			stack(e.getMessage(), e);
		}

	}



	public List<IAnalysisDataset> getDatasets(){
		return this.datasets;
	}

	private List<IAnalysisDataset> analysePopulations(List<ICellCollection> folderCollection){

		log("Creating cell collections");

		List<IAnalysisDataset> result = new ArrayList<IAnalysisDataset>();

		for(ICellCollection collection : folderCollection){

			IAnalysisDataset dataset = new DefaultAnalysisDataset(collection);
			dataset.setAnalysisOptions(analysisOptions);
			dataset.setRoot(true);

			File folder = collection.getFolder();
			log("Analysing: "+folder.getName());

			try{

				ICellCollection failedNuclei = new DefaultCellCollection(folder, 
						collection.getOutputFolderName(), 
						collection.getName()+" - failed", 
						collection.getNucleusType());


				log("Filtering collection...");
				
				Filterer<ICellCollection> filter = new CellCollectionFilterer();
				filter.removeOutliers(collection, failedNuclei, DEFAULT_FILTERING_DELTA);
				log("Filtered OK");
				
//				boolean ok = new CollectionFilterer().run(collection, failedNuclei); // put fails into failedNuclei, remove from r
//				if(ok){
//					log("Filtered OK");
//				} else {
//					log("Filtering error");
//				}

				/*
				 * Keep the failed nuclei - they can be manually assessed later
				 */

				if(analysisOptions.isKeepFailedCollections()){
					log("Keeping failed nuclei as new collection");
					IAnalysisDataset failed = new DefaultAnalysisDataset(failedNuclei);
					IMutableAnalysisOptions failedOptions = OptionsFactory.makeAnalysisOptions(analysisOptions);
					failedOptions.setNucleusType(NucleusType.ROUND);
					failed.setAnalysisOptions(failedOptions);
					failed.setRoot(true);
					result.add(failed);
				}

				log(SPACER);

				log("Population: "+collection.getName());
				log("Passed: "+collection.size()+" nuclei");
//				log("Failed: "+failedNuclei.size()+" nuclei");

				log(SPACER);


				result.add(dataset);


			} catch(Exception e){
				warn("Cannot create collection: "+e.getMessage());
				stack("Error in nucleus detection", e);
			}

			//			

		}
		return result;
	}

	/**
	 * Add a NucleusCollection to the group, using the source folder
	 * name as a key.
	 *
	 *  @param file a folder to be analysed
	 *  @param collection the collection of nuclei found
	 */
//	private void addNucleusCollection(File file, ICellCollection collection){
//		this.collectionGroup.put(file, collection);
//	}


	/**
	 * Get the Map of NucleusCollections to the folder from
	 * which they came. Any folders with no nuclei are removed
	 * before returning.
	 *
	 *  @return a Map of a folder to its nuclei
	 */
	private List<ICellCollection> getNucleiCollections(){
		// remove any empty collections before returning

		fine( "Getting all collections");

		List<File> toRemove = new ArrayList<File>(0);

		fine( "Testing nucleus counts");

		Set<File> keys = collectionGroup.keySet();
		for (File key : keys) {
			ICellCollection collection = collectionGroup.get(key);
			if(collection.size()==0){
				fine( "Removing collection "+key.toString());
				toRemove.add(key);
			}    
		}

		fine( "Got collections to remove");

		Iterator<File> iter = toRemove.iterator();
		while(iter.hasNext()){
			collectionGroup.remove(iter.next());
		}

		fine( "Removed collections");

		List<ICellCollection> result = new ArrayList<ICellCollection>();
		for(ICellCollection c : collectionGroup.values()){
			result.add(c);
		}
		return result;

	}

	/**
	 * Count the number of images in the given folder
	 * that are suitable for analysis. Rcursive over 
	 * subfolders.
	 * @param folder the folder to count
	 * @return the number of analysable image files
	 */
	private static int countSuitableImages(File folder){
		
		if(folder==null){
			throw new IllegalArgumentException("Folder cannot be null");
		}

		File[] listOfFiles = folder.listFiles();
		
		if(listOfFiles==null){
			return 0;
		}

		int result = 0;

		for (File file : listOfFiles) {

			boolean ok = ImageImporter.fileIsImportable(file);

			if(ok){
				result++;

			} else { 
				if(file.isDirectory()){ // recurse over any sub folders
					result += countSuitableImages(file);
				}
			} 
		} 
		return result;
	}

	/**
	 * Go through the input folder. Check if each file is
	 * suitable for analysis, and if so, call the analyser.
	 *
	 * @param folder the folder of images to be analysed
	 */
	protected void processFolder(File folder){
		
		if(folder==null){
			throw new IllegalArgumentException("Folder cannot be null");
		}
		
		// Recurse over all folders in the supplied folder
		for(File f : folder.listFiles()){
			if(f.isDirectory()){
				processFolder(f);
			}
		}

		ICellCollection folderCollection = new DefaultCellCollection(folder, 
				outputFolder, 
				folder.getName(), 
				analysisOptions.getNucleusType());

		this.collectionGroup.put(folder, folderCollection);
		
		/*
		 * NEW METHOD - appears to be working
		 */
		
		Finder<List<ICell>> finder =  new FluorescentNucleusFinder(analysisOptions);
		finder.addProgressListener(this);
				
		try {
			List<ICell> cells = finder.findInFolder(folder);
			for(ICell cell : cells){
				folderCollection.addCell(cell);
			}
			
			if( ! cells.isEmpty()){
				makeFolder(folder);
			}
		} catch (ImageImportException | ComponentCreationException e) {
			stack("Error searching folder", e);
		}
		
		
		/*
		 * OLD METHOD
		 */
//		File[] listOfFiles = folder.listFiles();
//
//		NucleusDetectionTask task = new NucleusDetectionTask(folder, listOfFiles, folderCollection, outputFolder, analysisOptions);
//		task.addProgressListener(this);
//		task.invoke();
//
//		for(File f : listOfFiles){
//			if(f.isDirectory()){
//				processFolder(f); // recurse over each folder
//			}
//		}


	} // end function
	
	  /**
	  * Create the output folder for the analysis if required
	  *
	  * @param folder the folder in which to create the analysis folder
	  * @return a File containing the created folder
	  */
	protected File makeFolder(File folder){
	    File output = new File(folder.getAbsolutePath()+File.separator+this.outputFolder);
	    if(!output.exists()){
	      try{
	        output.mkdir();
	      } catch(Exception e) {
	    	  error("Failed to create directory", e);
	      }
	    }
	    return output;
	  }

}
