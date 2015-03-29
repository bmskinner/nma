package no.collections;

import no.nuclei.INuclearFunctions;

import java.io.File;
import java.util.Map;
import java.util.List;

import ij.gui.Plot;
import no.components.Profile;
import no.components.ProfileAggregate;
import no.components.ProfileCollection;

public interface INuclearCollection
{
	 
	public void addNucleus(INuclearFunctions r);

	public void exportStatsFiles();

	public void annotateAndExportNuclei();

	public void measureProfilePositions();

	/**
	 * Find the Nucleus with a profile most closely matching the median 
	 * of the population
	 * 
	 * @param pointType the median profile type to search
	 * @return the nucleus
	 */
	public INuclearFunctions getNucleusMostSimilarToMedian(String pointType);

	/*
		-----------------------
		Getters for aggregate stats
		-----------------------
	*/

	public File getFolder();
	
	public String getOutputFolder();

	public File getDebugFile();

	public String getType();

	public double[] getPerimeters();

	public double[] getAreas();

	public double[] getFerets();

	public double[] getPathLengths();

	public double[] getArrayLengths();

	public double[] getMedianDistanceBetweenPoints();

	public String[] getNucleusPaths();

	public String[] getCleanNucleusPaths();

	public String[] getPositions();

	public int getNucleusCount();

	public List<INuclearFunctions> getNuclei();

	public INuclearFunctions getNucleus(int i);

	public int getRedSignalCount();

	public int getGreenSignalCount();

	// allow for refiltering of nuclei based on nuclear parameters after looking at the rest of the data
	public double getMedianNuclearArea();

	public double getMedianNuclearPerimeter();

	public double getMedianPathLength();

	public double getMedianArrayLength();

	public double getMedianFeretLength();

	public double getMaxProfileLength();

	public List<INuclearFunctions> getNucleiWithSignals(int channel);

	public double[] getDifferencesToMedianFromPoint(String pointType);

	// get the plot from the collection corresponding to the given pointType of interest
//	public Plot getPlot(String pointType, String plotType);

	public int[] getPointIndexes(String pointType);

	public double[] getPointToPointDistances(String pointTypeA, String pointTypeB);

	/*
		-----------------
		Basic filtering of population
		-----------------
	*/

	/*
		The filters needed to separate out objects from nuclei
		Filter on: nuclear area, perimeter and array length to find
		conjoined nuclei and blobs too small to be nuclei
		Use path length to remove poorly thresholded nuclei
	*/
	public void refilterNuclei(INuclearCollection failedCollection);

	/*
		-----------------
		Profile functions
		-----------------
	*/

//	protected void createProfileAggregateFromPoint(String pointType);

	public void createProfileAggregates();

	/*
		-----------------
		Annotate images
		-----------------
	*/

	public void measureNuclearOrganisation();

	/*
		-----------------
		Export functions
		-----------------
	*/

	public String getLogFileName(String filename);
	/*
		Export the signal parameters of the nucleus to the designated log file
	*/
	public void exportSignalStats();

	public void exportAngleProfiles();

	public void exportDistancesBetweenSingleSignals();

	public void exportAnnotatedNuclei();

	public void exportMediansOfProfile(Profile profile, String filename);

	public void exportMediansAndQuartilesOfProfile(ProfileAggregate profileAggregate, String filename);
	/*
		To hold the nuclear stats (and any stats), we want a structure that can 
		hold: a column of data. Any arbitrary other numbers of columns of data.
	*/
//	public Map<String, List<String>> calculateNuclearStats();

//	public void exportStats(Map<String, List<String>> stats, String filename);
	
	// this is for the mapping of image to path for 
	// identifying FISHed nuclei in prefish images
	public void exportImagePaths(String filename);

	public void exportNuclearStats(String filename);

	public void exportFilterStats();

	public void exportCompositeImage(String filename);

//	public void exportProfilePlot(Plot plot, String name);
	
	/*
		Draw the charts of the profiles of the nuclei within this collecion.
	*/
	public void drawProfilePlots();

	/*
		Draw a median profile on the normalised plots.
	*/
//	public void drawMedianLine(String pointType, Plot plot);
//
//	public void drawNormalisedMedianLines();

	public void addSignalsToProfileCharts();

	public void addSignalsToProfileChartFromPoint(String pointType, Plot plot);

//	public void exportProfilePlots();

	public ProfileCollection getProfileCollection();

}