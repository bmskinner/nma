/* 
  -----------------------
  RODENT SPERM NUCLEUS COLLECTION CLASS
  -----------------------
  This class enables filtering for the nucleus type
  It enables offsets to be calculated based on the median normalised curves
*/

package no.nuclei.sperm;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.gui.ProgressBar;
import ij.gui.TextRoi;
import ij.io.FileInfo;
import ij.io.FileOpener;
import ij.io.DirectoryChooser;
import ij.io.Opener;
import ij.io.OpenDialog;
import ij.io.RandomAccessStream;
import ij.measure.ResultsTable;
import ij.measure.SplineFitter;
import ij.plugin.ChannelSplitter;
import ij.plugin.PlugIn;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.FloatPolygon;
import ij.process.FloatProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.StackConverter;
import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.Color;
import java.awt.geom.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.Polygon;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.*;
import no.nuclei.*;
import no.nuclei.sperm.*;

public class RodentSpermNucleusCollection 
	extends no.nuclei.NucleusCollection
{

	// Chart drawing parameters
  private static final int CHART_WINDOW_HEIGHT     = 300;
  private static final int CHART_WINDOW_WIDTH      = 400;
  private static final int CHART_TAIL_BOX_Y_MIN    = 325;
  private static final int CHART_TAIL_BOX_Y_MID    = 340;
  private static final int CHART_TAIL_BOX_Y_MAX    = 355;
  private static final int CHART_SIGNAL_Y_LINE_MIN = 275;
  private static final int CHART_SIGNAL_Y_LINE_MAX = 315;

  // failure  codes
  public static final int FAILURE_TIP       = 1;
  public static final int FAILURE_TAIL      = 2;

  private static final double PROFILE_INCREMENT = 0.5;

	private String logMedianFromTipFile  = "logMediansFromTip"; // output medians
  private String logMedianFromTailFile = "logMediansFromTail"; // output medians

	// private ArrayList<RodentSpermNucleus> nucleiCollection = new ArrayList<RodentSpermNucleus>(0); // store all the nuclei analysed

  private double[] normalisedMedianLineFromTail; // this is an array of 200 angles

	private boolean differencesCalculated = false;

private Map<Double, Collection<Double>> normalisedProfilesFromTail = new HashMap<Double, Collection<Double>>();

	private int offsetCount = 20;
	private int medianLineTailIndex;

  private Plot  rawXFromTipPlot;
  private Plot normXFromTipPlot;
  private Plot  rawXFromTailPlot;
  private Plot normXFromTailPlot;

  private PlotWindow rawXFromTipWindow;
  private PlotWindow normXFromTipWindow;
  private PlotWindow rawXFromTailWindow;
  private PlotWindow normXFromTailWindow;

  private double maxDifferenceFromMedian = 1.5; // used to filter the nuclei, and remove those too small, large or irregular to be real
  private double maxWibblinessFromMedian = 1.2; // filter for the irregular borders more stringently

  public class PigSpermNucleusCollection
    extends NucleusCollection
  {

    public PigSpermNucleusCollection(File folder, String type){
      super(folder, type);
    }

    public PigSpermNucleus getNucleus(int i){
      return (PigSpermNucleus)this.getNuclei().get(i);
    }

  /*
    -----------------------
    Create and manipulate
    aggregate profiles. These
    can be centred on head or tail
    -----------------------
  */

  public void recalculateTailPositions(){

    // this.createProfileAggregateFromTail();
    // this.drawProfilePlots();
    // this.drawNormalisedMedianLineFromTail();
    // this.findTailIndexInMedianCurve();
    // this.calculateOffsets();
    // this.createNormalisedTailPositions();
    // this.createProfileAggregateFromTail();

    // this.drawRawPositionsFromTailChart();
    // this.drawNormalisedPositionsFromTailChart();

    // this.drawNormalisedMedianLineFromTail();
  }

  public void measureAndExportNuclei(){
    this.exportNuclearStats("logStats");
    this.annotateImagesOfNuclei();
    this.exportAnnotatedNuclei();
    this.exportCompositeImage("composite");
  }
  }