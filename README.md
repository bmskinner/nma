-------------------------------------------------
MOUSE SPERM CARTOGRAPHY IMAGEJ PLUGIN
-------------------------------------------------
Copyright (C) Ben Skinner 2015

This plugin allows for automated detection of FISH
signals in a mouse sperm nucleus, and measurement of
the signal position relative to the nuclear centre of
mass (CoM) and sperm tip. Works with both red and green channels.
It also generates a profile of the nuclear shape, allowing
morphology comparisons

  ---------------
  PLOT AND IMAGE FILES
  ---------------

  plotConsensus.tiff: The consensus nucleus with measured signal centres of mass displayed
  plotTailNorm.tiff: The normalised profiles centred on the tail, with median, IQR and signal positions.
  plotTipNorm.tiff: The normalised profiles centred on the tip, with median, IQR, signal positions and initial estimated tail positions.

  composite.tiff: All nuclei passing filters aggregated and rotated to put the tail at the bottom. Yellow line is tail-CoM-intersection.
                  This line divides the hook and hump ROIs (regions of interest).
                  Grey dots are initial tail estimates by 3 methods. Cyan dot is consensus tail position based on initial estimates.
                  Yellow dot is sperm tip. Pink line is the narrowest width through the nuclear CoM. Pink dot is the nuclear CoM.
                  Red and green dots are measured red and green signal CoMs. Red and green lines outline the signal ROIs.
                  The text annotation above and left of the nucleus corresponds to the image and log files in the directory.

  compositeFailed.tiff: As above, for nuclei that failed to pass filters.


  ---------------
  LOG FILES
  ---------------
  
  logProfiles: The normalised position in the array, interiorAngle and raw X position from the tail. No header row. Designed for R cut.

  logStats: The following fields for each nucleus passing filters:
      AREA            - nuclear area
      PERIMETER       - nuclear perimeter
      FERET           - longest distance across the nucleus
      PATH_LENGTH     - measure of wibbliness. Affected by thresholding.
      NORM_TAIL_INDEX - the position in the profile array normalised to 100
      DIFFERENCE      - the difference between the profile for this nucleus and the median profile of the collection of nuclei
      FAILURE_CODE    - will be 0 for all nuclei in this file
      PATH            - the path to the source image

  logFailed: The same fields for each nucleus failing filters. Failure codes are a sum of the following:
      FAILURE_TIP       = 1
      FAILURE_TAIL      = 2
      FAILURE_THRESHOLD = 4
      FAILURE_FERET     = 8
      FAILURE_ARRAY     = 16
      FAILURE_AREA      = 32
      FAILURE_PERIM     = 64
      FAILURE_OTHER     = 128

  logGreenSignals:
  logRedSignals:
    NUCLEUS_NUMBER      - the nucleus in the image. 
    SIGNAL_AREA         - area of the signal 
    SIGNAL_ANGLE        - angle of the signal CoM to nuclear CoM to the tail
    SIGNAL_FERET        - longest diameter of the signal 
    SIGNAL_DISTANCE     - distance in pixels of the signal from the nuclear CoM
    FRACTIONAL_DISTANCE - signal distance as a fraction of the distance to the nuclear border at the given angle. 0 = at CoM, 1 = at border
    SIGNAL_PERIMETER    - perimeter of the signal 
    SIGNAL_RADIUS       - radius of a circle with the same area as the signal.
    PATH                - the path to the source image

  logTailMedians: The medians centred on the tail
  logTipMedians: The medians centred on the tip
    X_POSITION       - normalised position along the profile. 0-100. Series of bins created from the normalised nuclei
    ANGLE_MEDIAN     - median angle in this bin
    Q25              - lowwer quartile
    Q75              - upper quartile
    Q10              - 10%ile
    Q90              - 90%ile
    NUMBER_OF_POINTS - the number of angles within the bin, from which the median angle was calculated             

  logConsensusNucleus: As per individual nuclei logs, but created for the consensus nucleus. Only SX, SY, FX, FY, IA are relevant.
    For each point in the nuclear boundary:
    SX - int x position
    SY - int y position
    FX - double x position
    FY - double y position
    IA - interior angle

    Remaining fields are for debugging only
    SX  SY  FX  FY  IA  MA  I_NORM  I_DELTA I_DELTA_S BLOCK_POSITION  BLOCK_NUMBER  L_MIN L_MAX IS_MIDPOINT IS_BLOCK  PROFILE_X DISTANCE_PROFILE

  ---------------
  FEATURES TO ADD
  ---------------
    Fix bug in signal drawing on tail profile
    Adaptive thresholding
    Measure DAPI propotions in each x-degree segment around CoM for normalisation.
      Relevant measurement code:  getResult("IntDen", 0);
    Alter filters to be more permissive of extreme Yqdel
    Clustering of profiles before median tail fitting and exclusion
    Add smoothing to consensus nucleus outline
    Rescale consensus image plot to rotated nucleus dimensions
    Add signal areas to consensus image
    Get measure of consistency in tail predictions
    Better profile orientation detector based on area above 180
    Confirm area of consenus nucleus matches median area, to allow overlay of different genotypes