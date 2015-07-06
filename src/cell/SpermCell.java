package cell;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import components.Flagellum;
import components.Mitochondrion;
import no.components.NuclearSignal;
import no.components.NucleusBorderPoint;
import no.components.NucleusBorderSegment;
import no.components.Profile;
import no.components.SignalCollection;
import no.components.XYPoint;
import no.nuclei.Nucleus;

public class SpermCell implements Cell, Serializable {
	
	private static final long serialVersionUID = 1L;

	private UUID uuid;
	
	private Nucleus nucleus;
	private Flagellum tail;
	private List<Mitochondrion> mitochondria; // unknown staining patterns so far
	
	public SpermCell(){
		this.uuid = java.util.UUID.randomUUID();
	}

	public UUID getCellId() {
		return uuid;
	}

	public Nucleus getNucleus() {
		return nucleus;
	}

	public void setNucleus(Nucleus nucleus) {
		this.nucleus = nucleus;
	}

	public Flagellum getTail() {
		return tail;
	}

	public void setTail(Flagellum tail) {
		this.tail = tail;
	}

	public List<Mitochondrion> getMitochondria() {
		return mitochondria;
	}

	public void setMitochondria(List<Mitochondrion> mitochondria) {
		this.mitochondria = mitochondria;
	}
	
	public void addMitochondrion(Mitochondrion mitochondrion) {
		this.mitochondria.add(mitochondrion);
	}

	@Override
	public void findPointsAroundBorder() {
		this.nucleus.findPointsAroundBorder();
	}

	@Override
	public void intitialiseNucleus(int angleProfileWindowSize) {
		this.nucleus.intitialiseNucleus(angleProfileWindowSize);
		
	}

	@Override
	public UUID getID() {
		return this.nucleus.getID();
	}

	@Override
	public String getPath() {
		return this.nucleus.getPath();
	}

	@Override
	public double[] getPosition() {
		return this.nucleus.getPosition();
	}

	@Override
	public void setPosition(double[] d) {
		this.nucleus.setPosition(d);
		
	}

	@Override
	public File getSourceFile() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public File getNucleusFolder() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getImageName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getAnnotatedImagePath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getOriginalImagePath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getEnlargedImagePath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getImageNameWithoutExtension() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public File getOutputFolder() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDirectory() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPathWithoutExtension() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getNucleusNumber() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getPathAndNumber() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public XYPoint getCentreOfMass() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NucleusBorderPoint getPoint(int i) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getArea() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getFeret() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getNarrowestDiameter() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getPathLength() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getPerimeter() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Profile getAngleProfile() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getAngleProfileWindowSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Profile getDistanceProfile() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getLength() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public NucleusBorderPoint getBorderPoint(int i) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getFailureCode() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean hasSignal(int channel) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasRedSignal() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasGreenSignal() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<NucleusBorderPoint> getBorderList() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void calculateFractionalSignalDistancesFromCoM() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void calculateSignalDistancesFromCoM() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setOutputFolder(String f) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setCentreOfMass(XYPoint d) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateFailureCode(int i) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setBorderList(List<NucleusBorderPoint> list) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public double getMaxX() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getMinX() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getMaxY() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getMinY() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setPathLength(double d) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void calculatePathLength() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setArea(double d) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setFeret(double d) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setPerimeter(double d) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addRedSignal(NuclearSignal n) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addGreenSignal(NuclearSignal n) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getPositionBetween(NucleusBorderPoint pointA,
			NucleusBorderPoint pointB) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public NucleusBorderPoint findOppositeBorder(NucleusBorderPoint p) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NucleusBorderPoint findOrthogonalBorderPoint(NucleusBorderPoint a) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NucleusBorderPoint findPointClosestToLocalMaximum(
			NucleusBorderPoint[] list) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NucleusBorderPoint findPointClosestToLocalMinimum(
			NucleusBorderPoint[] list) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NucleusBorderPoint getNarrowestDiameterPoint() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void flipXAroundPoint(XYPoint p) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public double getMedianDistanceBetweenPoints() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double findRotationAngle() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void calculateSignalAnglesFromPoint(NucleusBorderPoint p) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void exportSignalDistanceMatrix() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void exportAngleProfile() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void exportSegments() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Map<String, Integer> getSegmentMap() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Profile getSingleDistanceProfile() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void exportProfilePlotImage() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dumpInfo(int type) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Profile getAngleProfile(String pointType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getAngle(int index) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getIndex(NucleusBorderPoint p) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getDistance(int index) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void updatePoint(int i, double x, double y) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public NucleusBorderPoint getBorderTag(String s) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getBorderIndex(String s) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Set<String> getTags() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<NucleusBorderSegment> getSegments() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NucleusBorderSegment getSegmentTag(String s) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addBorderTag(String name, int i) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addSegmentTag(String name, int i) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void clearSegments() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void calculateAngleProfile(int angleProfileWindowSize) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setSegments(List<NucleusBorderSegment> newList) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public SignalCollection getSignalCollection() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getSignalCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getSignalCount(int channel) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<NuclearSignal> getSignals(int channel) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<List<NuclearSignal>> getSignals() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Integer> getSignalChannels() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Integer> getBorderTags() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addSegment(NucleusBorderSegment n) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reverse() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getOutputFolderName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateSourceFolder(File newFolder) {
		// TODO Auto-generated method stub
		
	}
	
	
}
