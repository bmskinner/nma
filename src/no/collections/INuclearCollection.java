package no.collections;

import no.nuclei.INuclearFunctions;

import java.io.File;
import java.util.List;

import no.components.Profile;
import no.components.ProfileAggregate;
import no.components.ProfileCollection;

public interface INuclearCollection
{
	public static final int CHART_WINDOW_HEIGHT     = 400;
	public static final int CHART_WINDOW_WIDTH      = 500;
	public static final int CHART_TAIL_BOX_Y_MIN    = 325;
	public static final int CHART_TAIL_BOX_Y_MID    = 340;
	public static final int CHART_TAIL_BOX_Y_MAX    = 355;
	public static final int CHART_SIGNAL_Y_LINE_MIN = 275;
	public static final int CHART_SIGNAL_Y_LINE_MAX = 315;
	public String DEFAULT_REFERENCE_POINT = null;

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
	
	public String getReferencePoint();

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
	
	public List<Integer> getSignalChannels();

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
	
	// this is for the mapping of image to path for 
	// identifying FISHed nuclei in prefish images
	public void exportImagePaths(String filename);

	public void exportNuclearStats(String filename);

	public void exportFilterStats();

	public void exportCompositeImage(String filename);
	
	/*
		Draw the charts of the profiles of the nuclei within this collecion.
	*/
	public void drawProfilePlots();

	public void drawBoxplots();

	public void addSignalsToProfileCharts();

	public ProfileCollection getProfileCollection();
	
	public void exportProfiles();
	
	public void findTailIndexInMedianCurve();
	
	public void calculateOffsets();

}