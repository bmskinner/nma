package no.collections;

import no.nuclei.Nucleus;

import java.io.File;
import java.util.List;
import java.util.UUID;

import no.components.AnalysisOptions;
import no.components.NuclearSignal;
import no.components.ProfileCollection;
import no.components.ShellResult;

public interface NucleusCollection
{
	public static final int CHART_WINDOW_HEIGHT     = 400;
	public static final int CHART_WINDOW_WIDTH      = 500;
	public static final int CHART_TAIL_BOX_Y_MIN    = 325;
	public static final int CHART_TAIL_BOX_Y_MID    = 340;
	public static final int CHART_TAIL_BOX_Y_MAX    = 355;
	public static final int CHART_SIGNAL_Y_LINE_MIN = 275;
	public static final int CHART_SIGNAL_Y_LINE_MAX = 315;
	public String DEFAULT_REFERENCE_POINT = null;

	public void addNucleus(Nucleus r);
	public void addConsensusNucleus(Nucleus n);

//	public void annotateAndExportNuclei();

	/**
	 * Find the Nucleus with a profile most closely matching the median 
	 * of the population
	 * 
	 * @param pointType the median profile type to search
	 * @return the nucleus
	 */
	public Nucleus getNucleusMostSimilarToMedian(String pointType);

	
	public Nucleus getConsensusNucleus();
	
//	public AnalysisOptions getAnalysisOptions();
//	public void setAnalysisOptions(AnalysisOptions analysisOptions);
	/*
		-----------------------
		Getters for aggregate stats
		-----------------------
	*/
	
	public void setName(String s);
	public String getName();
	public UUID getID();
	
	public Nucleus getNucleus(UUID id);
	
	public String getReferencePoint();
	public String getOrientationPoint();

	public File getFolder();
	
	public File getOutputFolder();
	
	public String getOutputFolderName();

	public File getDebugFile();

	public String getType();

	public double[] getPerimeters();

	public double[] getAreas();

	public double[] getFerets();
	
	public double[] getMinFerets();

	public double[] getPathLengths();

	public double[] getArrayLengths();

	public double[] getMedianDistanceBetweenPoints();

	public String[] getNucleusPaths();

	public String[] getCleanNucleusPaths();

	public String[] getPositions();

	public int getNucleusCount();

	public List<Nucleus> getNuclei();

	public Nucleus getNucleus(int i);
	
	public List<Integer> getSignalChannels();
	public int getSignalCount(int channel);

	public int getRedSignalCount();

	public int getGreenSignalCount();
	
	public boolean hasSignals(int channel);
	public boolean hasSignals();
	public boolean hasConsensusNucleus();
	
//	public void addShellResult(int channel, ShellResult result);
//	public ShellResult getShellResult(int channel);
//	public boolean hasShellResult();

	// allow for refiltering of nuclei based on nuclear parameters after looking at the rest of the data
	public double getMedianNuclearArea();

	public double getMedianNuclearPerimeter();

	public double getMedianPathLength();

	public double getMedianArrayLength();

	public double getMedianFeretLength();

	public double getMaxProfileLength();
	
	public double getMedianSignalDistance(int channel);
	public double getMedianSignalFeret(int channel);
	public double getMedianSignalAngle(int channel);
	public double getMedianSignalArea(int channel);
	
	public int getProfileWindowSize();

	public List<Nucleus> getNucleiWithSignals(int channel);

	public double[] getDifferencesToMedianFromPoint(String pointType);

	public int[] getPointIndexes(String pointType);

	public double[] getPointToPointDistances(String pointTypeA, String pointTypeB);

	public String getLogFileName(String filename);


	public ProfileCollection getProfileCollection();
	public ProfileCollection getFrankenCollection();
	public void setFrankenCollection (ProfileCollection frankenCollection);
	
	
	public void findTailIndexInMedianCurve();
	
	public void calculateOffsets();
	
	public int getSignalCount();
	
	public List<NuclearSignal> getSignals(int channel);
	

}