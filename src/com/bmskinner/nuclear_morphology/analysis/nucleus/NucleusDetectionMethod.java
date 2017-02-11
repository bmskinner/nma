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
import com.bmskinner.nuclear_morphology.components.DefaultAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.DefaultCellCollection;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.io.ImageImporter;

public class NucleusDetectionMethod extends AbstractAnalysisMethod {

	private static final String spacerString = "---------";

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
			processFolder(analysisOptions.getDetectionOptions(IAnalysisOptions.NUCLEUS).getFolder());

			fine("Detected nuclei in "+analysisOptions.getDetectionOptions(IAnalysisOptions.NUCLEUS).getFolder().getAbsolutePath());

			fine( "Creating cell collections");

			List<ICellCollection> folderCollection = this.getNucleiCollections();

			// Run the analysis pipeline

			fine("Analysing collections");

			datasets = analysePopulations(folderCollection);		

			fine( "Analysis complete; return collections");

		} catch(Exception e){
			stack("Error in processing folder", e);
		}

	}


	private void getTotalImagesToAnalyse(){
		log("Calculating number of images to analyse");
		int totalImages = countSuitableImages(analysisOptions.getDetectionOptions(IAnalysisOptions.NUCLEUS).getFolder());
		fireProgressEvent(new ProgressEvent(this, ProgressEvent.SET_TOTAL_PROGRESS, totalImages));
		log("Analysing "+totalImages+" images");
	}



	public List<IAnalysisDataset> getDatasets(){
		return this.datasets;
	}

	public List<IAnalysisDataset> analysePopulations(List<ICellCollection> folderCollection){

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
				boolean ok = new CollectionFilterer().run(collection, failedNuclei); // put fails into failedNuclei, remove from r
				if(ok){
					log("Filtered OK");
				} else {
					log("Filtering error");
				}

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

				log(spacerString);

				log("Population: "+collection.getName());
				log("Passed: "+collection.size()+" nuclei");
				log("Failed: "+failedNuclei.size()+" nuclei");

				log(spacerString);


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
	public void addNucleusCollection(File file, ICellCollection collection){
		this.collectionGroup.put(file, collection);
	}


	/**
	 * Get the Map of NucleusCollections to the folder from
	 * which they came. Any folders with no nuclei are removed
	 * before returning.
	 *
	 *  @return a Map of a folder to its nuclei
	 */
	public List<ICellCollection> getNucleiCollections(){
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
	public static int countSuitableImages(File folder){

		File[] listOfFiles = folder.listFiles();

		int result = 0;

		for (File file : listOfFiles) {

			boolean ok = ImageImporter.checkFile(file);

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

		File[] listOfFiles = folder.listFiles();

		ICellCollection folderCollection = new DefaultCellCollection(folder, 
				outputFolder, 
				folder.getName(), 
				analysisOptions.getNucleusType());

		this.collectionGroup.put(folder, folderCollection);


		FileProcessingTask task = new FileProcessingTask(folder, listOfFiles, folderCollection, outputFolder, analysisOptions);
		task.addProgressListener(this);
		task.invoke();
//		mainPool.invoke(task);

		for(File f : listOfFiles){
			if(f.isDirectory()){
				processFolder(f); // recurse over each folder
			}
		}


	} // end function

}
