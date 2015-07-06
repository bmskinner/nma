package no.components;

import java.io.File;
import java.io.Serializable;

public class AnalysisOptions implements Serializable {

	private static final long serialVersionUID = 1L;
	private  int    nucleusThreshold;
	private  int    signalThreshold;
	private  double minNucleusSize;
	private  double maxNucleusSize;
	private  double minNucleusCirc;
	private  double maxNucleusCirc;
	
	// values for Canny edge deteection
	private boolean useCanny; 
	private boolean cannyAutoThreshold;
	private float lowThreshold;
	private float highThreshold;
	private float kernelRadius;
	private int   kernelWidth;
	private int   closingObjectRadius; // for morphological closing
	
	private boolean normaliseContrast; 

	private int angleProfileWindowSize;

	private Class<?> nucleusClass;

	/**
	 * The class of NucleusCollection to use in the analysis
	 */
//	private Class<?> collectionClass;

	/**
	 * Should a reanalysis be performed?
	 */
	private boolean performReanalysis;

	/**
	 * Should images for a reanalysis be aligned
	 * beyond the offsets provided?
	 */
	private boolean realignMode;

	private boolean refoldNucleus;

	private File folder;
	private File mappingFile;

	private String refoldMode;

	private int xoffset;
	private int yoffset;

	private  double minSignalSize;
	private  double maxSignalFraction;



	public AnalysisOptions(){

	}


	/*
    -----------------------
    Getters
    -----------------------
	 */

	public File getFolder(){
		return this.folder;
	}

	public File getMappingFile(){
		return this.mappingFile;
	}

	public int getNucleusThreshold(){
		return this.nucleusThreshold;
	}

	public int getSignalThreshold(){
		return this.signalThreshold;
	}

	public double getMinNucleusSize(){
		return this.minNucleusSize;
	}

	public double getMaxNucleusSize(){
		return this.maxNucleusSize;
	}

	public double getMinNucleusCirc(){
		return this.minNucleusCirc;
	}

	public double getMaxNucleusCirc(){
		return this.maxNucleusCirc;
	}

	public double getMinSignalSize(){
		return this.minSignalSize;
	}

	public double getMaxSignalFraction(){
		return this.maxSignalFraction;
	}

	public int getAngleProfileWindowSize(){
		return this.angleProfileWindowSize;
	}

	public Class<?> getNucleusClass(){
		return this.nucleusClass;
	}

//	public Class<?> getCollectionClass(){
//		return this.collectionClass;
//	}

	public String getRefoldMode(){
		return this.refoldMode;
	}

	public boolean isReanalysis(){
		return this.performReanalysis;
	}

	public boolean realignImages(){
		return this.realignMode;
	}

	public boolean refoldNucleus(){
		return this.refoldNucleus;
	}

	public int getXOffset(){
		return  this.xoffset;
	}

	public int getYOffset(){
		return  this.yoffset;
	}

	public void setNucleusThreshold(int nucleusThreshold) {
		this.nucleusThreshold = nucleusThreshold;
	}


	public void setSignalThreshold(int signalThreshold) {
		this.signalThreshold = signalThreshold;
	}


	public void setMinNucleusSize(double minNucleusSize) {
		this.minNucleusSize = minNucleusSize;
	}


	public void setMaxNucleusSize(double maxNucleusSize) {
		this.maxNucleusSize = maxNucleusSize;
	}


	public void setMinNucleusCirc(double minNucleusCirc) {
		this.minNucleusCirc = minNucleusCirc;
	}


	public void setMaxNucleusCirc(double maxNucleusCirc) {
		this.maxNucleusCirc = maxNucleusCirc;
	}


	public void setAngleProfileWindowSize(int angleProfileWindowSize) {
		this.angleProfileWindowSize = angleProfileWindowSize;
	}


	public void setNucleusClass(Class<?> nucleusClass) {
		this.nucleusClass = nucleusClass;
	}


//	public void setCollectionClass(Class<?> collectionClass) {
//		this.collectionClass = collectionClass;
//	}


	public void setPerformReanalysis(boolean performReanalysis) {
		this.performReanalysis = performReanalysis;
	}


	public void setRealignMode(boolean realignMode) {
		this.realignMode = realignMode;
	}


	public void setRefoldNucleus(boolean refoldNucleus) {
		this.refoldNucleus = refoldNucleus;
	}


	public void setFolder(File folder) {
		this.folder = folder;
	}


	public void setMappingFile(File mappingFile) {
		this.mappingFile = mappingFile;
	}


	public void setRefoldMode(String refoldMode) {
		this.refoldMode = refoldMode;
	}


	public void setXoffset(int xoffset) {
		this.xoffset = xoffset;
	}


	public void setYoffset(int yoffset) {
		this.yoffset = yoffset;
	}


	public void setMinSignalSize(double minSignalSize) {
		this.minSignalSize = minSignalSize;
	}


	public void setMaxSignalFraction(double maxSignalFraction) {
		this.maxSignalFraction = maxSignalFraction;
	}


	public float getLowThreshold() {
		return lowThreshold;
	}


	public void setLowThreshold(float lowThreshold) {
		this.lowThreshold = lowThreshold;
	}


	public float getHighThreshold() {
		return highThreshold;
	}


	public void setHighThreshold(float highThreshold) {
		this.highThreshold = highThreshold;
	}


	public float getKernelRadius() {
		return kernelRadius;
	}


	public void setKernelRadius(float kernelRadius) {
		this.kernelRadius = kernelRadius;
	}


	public int getKernelWidth() {
		return kernelWidth;
	}


	public void setKernelWidth(int kernelWidth) {
		this.kernelWidth = kernelWidth;
	}


	public boolean isNormaliseContrast() {
		return normaliseContrast;
	}


	public void setNormaliseContrast(boolean normaliseContrast) {
		this.normaliseContrast = normaliseContrast;
	}


	public boolean isUseCanny() {
		return useCanny;
	}


	public void setUseCanny(boolean useCanny) {
		this.useCanny = useCanny;
	}


	public int getClosingObjectRadius() {
		return closingObjectRadius;
	}


	public void setClosingObjectRadius(int closingObjectRadius) {
		this.closingObjectRadius = closingObjectRadius;
	}


	public boolean isCannyAutoThreshold() {
		return cannyAutoThreshold;
	}


	public void setCannyAutoThreshold(boolean cannyAutoThreshold) {
		this.cannyAutoThreshold = cannyAutoThreshold;
	}
}
