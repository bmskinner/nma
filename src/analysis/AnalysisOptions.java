/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package analysis;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import analysis.signals.NuclearSignalOptions;
import stats.NucleusStatistic;
import components.nuclear.NucleusType;
import components.nuclei.Nucleus;

public class AnalysisOptions implements IAnalysisOptions {

	private static final long serialVersionUID = 1L;
	private  int    nucleusThreshold;
	private  double minNucleusSize;
	private  double maxNucleusSize;
	private  double minNucleusCirc;
	private  double maxNucleusCirc;
	
	private Map<String, ICannyOptions> edgeDetection = new HashMap<String, ICannyOptions>(0);
	
	private Map<UUID, NuclearSignalOptions> signalDetection = new HashMap<UUID, NuclearSignalOptions>(0);
		
	private boolean normaliseContrast = false; 

	private double angleWindowProportion = 0.05;
	
	private double scale = 1; // hold the length of a pixel in metres

	private NucleusType nucleusType = null;


	/**
	 * Should a reanalysis be performed?
	 */
	private boolean performReanalysis = false;

	/**
	 * Should images for a reanalysis be aligned
	 * beyond the offsets provided?
	 */
	private boolean realignMode = false;

	private boolean refoldNucleus = false;

	private File folder = null;
	private File mappingFile = null;

	private String refoldMode = null;

	private int xoffset = 0;
	private int yoffset = 0;
	
	private boolean keepFailedCollections = false;// allow failed collection to be retained for manual analysis
	
	private int channel = 0;

	public AnalysisOptions(){
		
		this.addCannyOptions("nucleus");
		this.addCannyOptions("tail");
	}
	
	
	
	/**
	 * Duplicate the data in the template options
	 * @param template
	 */
	public AnalysisOptions(IAnalysisOptions template){
		nucleusThreshold = template.getNucleusThreshold();
		minNucleusSize   = template.getMinNucleusSize();
		maxNucleusSize   = template.getMaxNucleusSize();
		minNucleusCirc   = template.getMinNucleusCirc();
		maxNucleusCirc   = template.getMaxNucleusCirc();
		
		edgeDetection    = new HashMap<String, ICannyOptions>(0);
		for(String s : template.getCannyOptionTypes()){
			edgeDetection.put(s, template.getCannyOptions(s));
		}

        signalDetection = new HashMap<UUID, NuclearSignalOptions>(0);
        
        for(UUID s : template.getNuclearSignalGroups()){
			signalDetection.put(s, template.getNuclearSignalOptions(s));
		}
		
		normaliseContrast      = template.isNormaliseContrast(); 
		angleWindowProportion  = template.getAngleWindowProportion();
		scale                  = template.getScale(); 
		nucleusType            = template.getNucleusType();
		performReanalysis      = template.isReanalysis() ;
		realignMode            = template.realignImages();
		refoldNucleus          = template.refoldNucleus();
		folder                 = template.getFolder();
		mappingFile            = template.getMappingFile();
		refoldMode             = template.getRefoldMode();
		xoffset                = template.getXOffset();
		yoffset                = template.getYOffset();
		keepFailedCollections  = template.isKeepFailedCollections();
		channel                = template.getChannel();
	}


	/*
    -----------------------
    Getters
    -----------------------
	 */

	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#getFolder()
	 */
	@Override
	public File getFolder(){
		return this.folder;
	}

	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#getMappingFile()
	 */
	@Override
	public File getMappingFile(){
		return this.mappingFile;
	}

	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#getNucleusThreshold()
	 */
	@Override
	public int getNucleusThreshold(){
		return this.nucleusThreshold;
	}

	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#getMinNucleusSize()
	 */
	@Override
	public double getMinNucleusSize(){
		return this.minNucleusSize;
	}

	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#getMaxNucleusSize()
	 */
	@Override
	public double getMaxNucleusSize(){
		return this.maxNucleusSize;
	}

	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#getMinNucleusCirc()
	 */
	@Override
	public double getMinNucleusCirc(){
		return this.minNucleusCirc;
	}

	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#getMaxNucleusCirc()
	 */
	@Override
	public double getMaxNucleusCirc(){
		return this.maxNucleusCirc;
	}

	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#getAngleWindowProportion()
	 */
	@Override
	public double getAngleWindowProportion(){
		return this.angleWindowProportion;
	}

	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#getNucleusType()
	 */
	@Override
	public NucleusType getNucleusType(){
		return this.nucleusType;
	}
	
	

	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#getRefoldMode()
	 */
	@Override
	public String getRefoldMode(){
		return this.refoldMode;
	}

	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#isReanalysis()
	 */
	@Override
	public boolean isReanalysis(){
		return this.performReanalysis;
	}

	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#realignImages()
	 */
	@Override
	public boolean realignImages(){
		return this.realignMode;
	}

	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#refoldNucleus()
	 */
	@Override
	public boolean refoldNucleus(){
		return this.refoldNucleus;
	}

	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#getXOffset()
	 */
	@Override
	public int getXOffset(){
		return  this.xoffset;
	}

	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#getYOffset()
	 */
	@Override
	public int getYOffset(){
		return  this.yoffset;
	}

	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#getScale()
	 */
	@Override
	public double getScale() {
		return scale;
	}
	

	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#getChannel()
	 */
	@Override
	public int getChannel() {
		return channel;
	}



	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#setChannel(int)
	 */
	@Override
	public void setChannel(int channel) {
		this.channel = channel;
	}



	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#setScale(double)
	 */
	@Override
	public void setScale(double scale) {
		this.scale = scale;
	}


	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#setNucleusThreshold(int)
	 */
	@Override
	public void setNucleusThreshold(int nucleusThreshold) {
		this.nucleusThreshold = nucleusThreshold;
	}

	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#setMinNucleusSize(double)
	 */
	@Override
	public void setMinNucleusSize(double minNucleusSize) {
		this.minNucleusSize = minNucleusSize;
	}


	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#setMaxNucleusSize(double)
	 */
	@Override
	public void setMaxNucleusSize(double maxNucleusSize) {
		this.maxNucleusSize = maxNucleusSize;
	}


	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#setMinNucleusCirc(double)
	 */
	@Override
	public void setMinNucleusCirc(double minNucleusCirc) {
		this.minNucleusCirc = minNucleusCirc;
	}


	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#setMaxNucleusCirc(double)
	 */
	@Override
	public void setMaxNucleusCirc(double maxNucleusCirc) {
		this.maxNucleusCirc = maxNucleusCirc;
	}


	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#setAngleWindowProportion(double)
	 */
	@Override
	public void setAngleWindowProportion(double proportion) {
		this.angleWindowProportion = proportion;
	}


	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#setNucleusType(components.nuclear.NucleusType)
	 */
	@Override
	public void setNucleusType(NucleusType nucleusType) {
		this.nucleusType = nucleusType;
	}


	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#setPerformReanalysis(boolean)
	 */
	@Override
	public void setPerformReanalysis(boolean performReanalysis) {
		this.performReanalysis = performReanalysis;
	}


	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#setRealignMode(boolean)
	 */
	@Override
	public void setRealignMode(boolean realignMode) {
		this.realignMode = realignMode;
	}


	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#setRefoldNucleus(boolean)
	 */
	@Override
	public void setRefoldNucleus(boolean refoldNucleus) {
		this.refoldNucleus = refoldNucleus;
	}


	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#setFolder(java.io.File)
	 */
	@Override
	public void setFolder(File folder) {
		this.folder = folder;
	}


	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#setMappingFile(java.io.File)
	 */
	@Override
	public void setMappingFile(File mappingFile) {
		this.mappingFile = mappingFile;
	}


	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#setRefoldMode(java.lang.String)
	 */
	@Override
	public void setRefoldMode(String refoldMode) {
		this.refoldMode = refoldMode;
	}


	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#setXoffset(int)
	 */
	@Override
	public void setXoffset(int xoffset) {
		this.xoffset = xoffset;
	}


	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#setYoffset(int)
	 */
	@Override
	public void setYoffset(int yoffset) {
		this.yoffset = yoffset;
	}

	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#isNormaliseContrast()
	 */
	@Override
	public boolean isNormaliseContrast() {
		return normaliseContrast;
	}


	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#setNormaliseContrast(boolean)
	 */
	@Override
	public void setNormaliseContrast(boolean normaliseContrast) {
		this.normaliseContrast = normaliseContrast;
	}
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#getCannyOptions(java.lang.String)
	 */
	@Override
	public ICannyOptions getCannyOptions(String type){
		return edgeDetection.get(type); 
	}
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#addCannyOptions(java.lang.String)
	 */
	@Override
	public void addCannyOptions(String type){
		edgeDetection.put(type, new CannyOptions()); 
	}
	
	@Override
	public void addCannyOptions(String type, ICannyOptions options){
		edgeDetection.put(type, options); 
	}
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#getCannyOptionTypes()
	 */
	@Override
	public Set<String> getCannyOptionTypes(){
		return edgeDetection.keySet();
	}
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#hasCannyOptions(java.lang.String)
	 */
	@Override
	public boolean hasCannyOptions(String type){
		if(this.edgeDetection.containsKey(type)){
			return true;
		} else {
			return false;
		}
	}
	
    /* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#getNuclearSignalGroups()
	 */
    @Override
	public Set<UUID> getNuclearSignalGroups(){
		return signalDetection.keySet();
	}
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#getNuclearSignalOptions(java.util.UUID)
	 */
    @Override
	public NuclearSignalOptions getNuclearSignalOptions(UUID signalGroup){
        if(this.signalDetection.containsKey(signalGroup)){
            return this.signalDetection.get(signalGroup);
        } else {
            this.addNuclearSignalOptions(signalGroup);
            return this.signalDetection.get(signalGroup);
        }

	}
	
    /* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#addNuclearSignalOptions(java.util.UUID)
	 */
    @Override
	public void addNuclearSignalOptions(UUID id){
        signalDetection.put(id, new NuclearSignalOptions());
    }
    
    /* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#addNuclearSignalOptions(java.util.UUID, analysis.signals.NuclearSignalOptions)
	 */
    @Override
	public void addNuclearSignalOptions(UUID id, NuclearSignalOptions options){
        signalDetection.put(id, options);
    }


	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#hasSignalDetectionOptions(java.util.UUID)
	 */
    @Override
	public boolean hasSignalDetectionOptions(UUID signalGroup){
        if(this.signalDetection.containsKey(signalGroup)){

			return true;
		} else {
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#isKeepFailedCollections()
	 */
	@Override
	public boolean isKeepFailedCollections() {
		return keepFailedCollections;
	}

	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#setKeepFailedCollections(boolean)
	 */
	@Override
	public void setKeepFailedCollections(boolean keepFailedCollections) {
		this.keepFailedCollections = keepFailedCollections;
	}

	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#isValid(components.nuclei.Nucleus)
	 */
	@Override
	public boolean isValid(Nucleus c){
		boolean result = true;
		
		if(c.getStatistic(NucleusStatistic.AREA) < getMinNucleusSize()){
			
			result = false;
		}
		
		if(c.getStatistic(NucleusStatistic.AREA) > getMaxNucleusSize()){
			
			result = false;
		}
		
		if(c.getStatistic(NucleusStatistic.CIRCULARITY) < getMinNucleusCirc()){
			
			result = false;
		}
		
		if(c.getStatistic(NucleusStatistic.CIRCULARITY) > getMaxNucleusCirc()){
			
			result = false;
		}
		
		return result;
	}


	
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(angleWindowProportion);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		
		result = prime * result + channel;
		result = prime * result
				+ ((edgeDetection == null) ? 0 : edgeDetection.hashCode());
		result = prime * result + ((folder == null) ? 0 : folder.hashCode());
		
		temp = Double.doubleToLongBits(maxNucleusCirc);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(maxNucleusSize);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(minNucleusCirc);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(minNucleusSize);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + (normaliseContrast ? 1231 : 1237);
		result = prime * result + nucleusThreshold;
		result = prime * result
				+ ((nucleusType == null) ? 0 : nucleusType.hashCode());
		result = prime * result + (performReanalysis ? 1231 : 1237);
		result = prime * result + (realignMode ? 1231 : 1237);
		temp = Double.doubleToLongBits(scale);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result
				+ ((signalDetection == null) ? 0 : signalDetection.hashCode());
		return result;
	}



	/* (non-Javadoc)
	 * @see analysis.IAnalysisOptions#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
//		finest("Testing equality");
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AnalysisOptions other = (AnalysisOptions) obj;
		if (Double.doubleToLongBits(angleWindowProportion) != Double
				.doubleToLongBits(other.angleWindowProportion))
			return false;
		if (channel != other.channel)
			return false;
//		finest("Testing edge detection parameters");
		if (edgeDetection == null) {
			if (other.edgeDetection != null)
				return false;
		} else if (!edgeDetection.equals(other.edgeDetection))
			return false;
		
//		finest("Testing doubles");
		if (Double.doubleToLongBits(maxNucleusCirc) != Double
				.doubleToLongBits(other.maxNucleusCirc))
			return false;
		if (Double.doubleToLongBits(maxNucleusSize) != Double
				.doubleToLongBits(other.maxNucleusSize))
			return false;
		if (Double.doubleToLongBits(minNucleusCirc) != Double
				.doubleToLongBits(other.minNucleusCirc))
			return false;
		if (Double.doubleToLongBits(minNucleusSize) != Double
				.doubleToLongBits(other.minNucleusSize))
			return false;
		
//		finest("Testing contrast");
		if (normaliseContrast != other.normaliseContrast)
			return false;
		if (nucleusThreshold != other.nucleusThreshold)
			return false;
		if (nucleusType != other.nucleusType)
			return false;
		if (performReanalysis != other.performReanalysis)
			return false;
		if (realignMode != other.realignMode)
			return false;
		if (Double.doubleToLongBits(scale) != Double
				.doubleToLongBits(other.scale))
			return false;
//		finest("Testing signal detection");
		if (signalDetection == null) {
			if (other.signalDetection != null)
				return false;
		} else if (!signalDetection.equals(other.signalDetection))
			return false;
		return true;
	}





	public class CannyOptions implements ICannyOptions {

		private static final long serialVersionUID = 1L;
		
		// values for Canny edge deteection
		private boolean useCanny; 
		private boolean cannyAutoThreshold;
		
		private boolean flattenChromocentres; 	// should the white threshold be lowered to hide internal structures?
		private int flattenThreshold; // if the white threhold is lower, this is the value
		private boolean useKuwahara;	// perform a Kuwahara filtering to enhance edge detection?
		private int kuwaharaKernel;		// the radius of the Kuwahara kernel - must be an odd number
		
		private float lowThreshold;		// the canny low threshold
		private float highThreshold;	// the canny high threshold
		private float kernelRadius;		// the kernel radius
		private int   kernelWidth;		// the kernel width
		private int   closingObjectRadius; // the circle radius for morphological closing
		
		public CannyOptions(){
			
		}
		
		/* (non-Javadoc)
		 * @see analysis.ICannyOptions#isUseCanny()
		 */
		@Override
		public boolean isUseCanny() {
			return useCanny;
		}


		/* (non-Javadoc)
		 * @see analysis.ICannyOptions#setUseCanny(boolean)
		 */
		@Override
		public void setUseCanny(boolean useCanny) {
			this.useCanny = useCanny;
		}
		
		/* (non-Javadoc)
		 * @see analysis.ICannyOptions#isUseFlattenImage()
		 */
		@Override
		public boolean isUseFlattenImage() {
			return flattenChromocentres;
		}
		
		/* (non-Javadoc)
		 * @see analysis.ICannyOptions#setFlattenImage(boolean)
		 */
		@Override
		public void setFlattenImage(boolean flattenImage) {
			this.flattenChromocentres = flattenImage;
		}
		
		/* (non-Javadoc)
		 * @see analysis.ICannyOptions#getFlattenThreshold()
		 */
		@Override
		public int getFlattenThreshold() {
			return flattenThreshold;
		}


		/* (non-Javadoc)
		 * @see analysis.ICannyOptions#setFlattenThreshold(int)
		 */
		@Override
		public void setFlattenThreshold(int flattenThreshold) {
			this.flattenThreshold = flattenThreshold;
		}
		
		
		/* (non-Javadoc)
		 * @see analysis.ICannyOptions#isUseKuwahara()
		 */
		@Override
		public boolean isUseKuwahara() {
			return useKuwahara;
		}
		
		/* (non-Javadoc)
		 * @see analysis.ICannyOptions#setUseKuwahara(boolean)
		 */
		@Override
		public void setUseKuwahara(boolean b){
			this.useKuwahara = b;
		}
		
		/* (non-Javadoc)
		 * @see analysis.ICannyOptions#getKuwaharaKernel()
		 */
		@Override
		public int getKuwaharaKernel(){
			return kuwaharaKernel;
		}
		
		/* (non-Javadoc)
		 * @see analysis.ICannyOptions#setKuwaharaKernel(int)
		 */
		@Override
		public void setKuwaharaKernel(int radius){
			kuwaharaKernel = radius;
		}

		/* (non-Javadoc)
		 * @see analysis.ICannyOptions#getClosingObjectRadius()
		 */
		@Override
		public int getClosingObjectRadius() {
			return closingObjectRadius;
		}


		/* (non-Javadoc)
		 * @see analysis.ICannyOptions#setClosingObjectRadius(int)
		 */
		@Override
		public void setClosingObjectRadius(int closingObjectRadius) {
			this.closingObjectRadius = closingObjectRadius;
		}


		/* (non-Javadoc)
		 * @see analysis.ICannyOptions#isCannyAutoThreshold()
		 */
		@Override
		public boolean isCannyAutoThreshold() {
			return cannyAutoThreshold;
		}


		/* (non-Javadoc)
		 * @see analysis.ICannyOptions#setCannyAutoThreshold(boolean)
		 */
		@Override
		public void setCannyAutoThreshold(boolean cannyAutoThreshold) {
			this.cannyAutoThreshold = cannyAutoThreshold;
		}
		
		/* (non-Javadoc)
		 * @see analysis.ICannyOptions#getLowThreshold()
		 */
		@Override
		public float getLowThreshold() {
			return lowThreshold;
		}


		/* (non-Javadoc)
		 * @see analysis.ICannyOptions#setLowThreshold(float)
		 */
		@Override
		public void setLowThreshold(float lowThreshold) {
			this.lowThreshold = lowThreshold;
		}


		/* (non-Javadoc)
		 * @see analysis.ICannyOptions#getHighThreshold()
		 */
		@Override
		public float getHighThreshold() {
			return highThreshold;
		}


		/* (non-Javadoc)
		 * @see analysis.ICannyOptions#setHighThreshold(float)
		 */
		@Override
		public void setHighThreshold(float highThreshold) {
			this.highThreshold = highThreshold;
		}


		/* (non-Javadoc)
		 * @see analysis.ICannyOptions#getKernelRadius()
		 */
		@Override
		public float getKernelRadius() {
			return kernelRadius;
		}


		/* (non-Javadoc)
		 * @see analysis.ICannyOptions#setKernelRadius(float)
		 */
		@Override
		public void setKernelRadius(float kernelRadius) {
			this.kernelRadius = kernelRadius;
		}


		/* (non-Javadoc)
		 * @see analysis.ICannyOptions#getKernelWidth()
		 */
		@Override
		public int getKernelWidth() {
			return kernelWidth;
		}


		/* (non-Javadoc)
		 * @see analysis.ICannyOptions#setKernelWidth(int)
		 */
		@Override
		public void setKernelWidth(int kernelWidth) {
			this.kernelWidth = kernelWidth;
		}
		
		
		
		/* (non-Javadoc)
		 * @see analysis.ICannyOptions#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (cannyAutoThreshold ? 1231 : 1237);
			result = prime * result + closingObjectRadius;
			result = prime * result + (flattenChromocentres ? 1231 : 1237);
			result = prime * result + flattenThreshold;
			result = prime * result + Float.floatToIntBits(highThreshold);
			result = prime * result + Float.floatToIntBits(kernelRadius);
			result = prime * result + kernelWidth;
			result = prime * result + kuwaharaKernel;
			result = prime * result + Float.floatToIntBits(lowThreshold);
			result = prime * result + (useCanny ? 1231 : 1237);
			result = prime * result + (useKuwahara ? 1231 : 1237);
			return result;
		}

		/* (non-Javadoc)
		 * @see analysis.ICannyOptions#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CannyOptions other = (CannyOptions) obj;
//			if (!getOuterType().equals(other.getOuterType()))
//				return false;
			if (cannyAutoThreshold != other.cannyAutoThreshold)
				return false;
			if (closingObjectRadius != other.closingObjectRadius)
				return false;
			if (flattenChromocentres != other.flattenChromocentres)
				return false;
			if (flattenThreshold != other.flattenThreshold)
				return false;
			if (Float.floatToIntBits(highThreshold) != Float
					.floatToIntBits(other.highThreshold))
				return false;
			if (Float.floatToIntBits(kernelRadius) != Float
					.floatToIntBits(other.kernelRadius))
				return false;
			if (kernelWidth != other.kernelWidth)
				return false;
			if (kuwaharaKernel != other.kuwaharaKernel)
				return false;
			if (Float.floatToIntBits(lowThreshold) != Float
					.floatToIntBits(other.lowThreshold))
				return false;
			if (useCanny != other.useCanny)
				return false;
			if (useKuwahara != other.useKuwahara)
				return false;
			return true;
		}
		
		private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		    
			/*
			 * The chromocentre flattening parameter and Kuwahara
			 * kernel parameter are transient. When these are stored,
			 * check if they were filled, and override if needed.
			 */
			in.defaultReadObject();
		    
		}
	}
}
