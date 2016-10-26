package components.active;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import analysis.IAnalysisDataset;
import analysis.profiles.ProfileException;
import analysis.profiles.ProfileManager;
import analysis.profiles.RuleSetCollection;
import analysis.signals.SignalManager;
import components.ICell;
import components.ICellCollection;
import components.generic.IProfileCollection;
import components.generic.MeasurementScale;
import components.generic.ProfileType;
import components.generic.Tag;
import components.nuclear.ISignalGroup;
import components.nuclear.NucleusType;
import components.nuclei.ConsensusNucleus;
import components.nuclei.Nucleus;
import stats.PlottableStatistic;

/**
 * This class provides access to child dataset ICell lists
 * and statistics
 * @author ben
 *
 */
public class VirtualCellCollection implements ICellCollection {
	
	private static final long serialVersionUID = 1L;
	
	private IAnalysisDataset parent;
	
	private Set<UUID> cellIDs = new HashSet<UUID>(0);

	private final UUID 	uuid;			// the collection id

	private String 	    name;			// the name of the collection

	//this holds the mapping of tail indexes etc in the median profile arrays
	protected Map<ProfileType, IProfileCollection> profileCollections = new HashMap<ProfileType, IProfileCollection>();

	private ConsensusNucleus consensusNucleus; 	// the refolded consensus nucleus
	
	private transient boolean isRefolding = false;
	
	public VirtualCellCollection(IAnalysisDataset parent, String name){
		this(parent, name, java.util.UUID.randomUUID() );
	}
	
	public VirtualCellCollection(IAnalysisDataset parent, String name, UUID id){
		this.parent = parent;
		this.name = name;
		this.uuid = id;
	}


	@Override
	public void setName(String s) {
		this.name = s;
		
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public UUID getID() {
		return uuid;
	}

	@Override
	public Set<ICell> getCells() {
		
		Set<ICell> result = new HashSet<ICell>(cellIDs.size());
		
		for(ICell cell : parent.getCollection().getCells()){
			if(cellIDs.contains(cell.getId())){
				result.add(cell);
			}
		}
		return result;
	}

	@Override
	public Set<ICell> getCells(File f) {
		Set<ICell> result = new HashSet<ICell>(cellIDs.size());
		
		for(ICell cell : parent.getCollection().getCells()){
			if(cellIDs.contains(cell.getId())){
				if(cell.getNucleus().getSourceFile().equals(f)){
					result.add(cell);
				}
			}
		}
		return result;
	}

	@Override
	public Set<UUID> getCellIDs() {
		return cellIDs;
	}

	@Override
	public Set<Nucleus> getNuclei() {
		Set<Nucleus> result = new HashSet<Nucleus>(cellIDs.size());
		for(UUID id : cellIDs){
			Nucleus n = parent.getCollection().getCell(id).getNucleus();
				result.add(n);
		}

		return result;
	}

	@Override
	public Set<Nucleus> getNuclei(File imageFile) {
		Set<Nucleus> result = new HashSet<Nucleus>(cellIDs.size());
		for(UUID id : cellIDs){
			Nucleus n = parent.getCollection().getCell(id).getNucleus();
			
			if(n.getSourceFile().equals(imageFile)){
				result.add(n);
			}
		}

		return result;
	}

	@Override
	public void addCell(ICell c) {
		cellIDs.add(c.getId());
		
	}

	@Override
	public void replaceCell(ICell c) {}

	@Override
	public ICell getCell(UUID id) {
		if(cellIDs.contains(id)){
			return parent.getCollection().getCell(id);
		}
		return null;
	}

	@Override
	public NucleusType getNucleusType() {
		return parent.getCollection().getNucleusType();
	}

	@Override
	public void removeCell(ICell c) {
		cellIDs.remove(c.getId());
		
	}

	@Override
	public int size() {
		return cellIDs.size();
	}

	@Override
	public boolean hasConsensusNucleus() {
		return this.consensusNucleus!=null;
	}

	@Override
	public void setConsensusNucleus(ConsensusNucleus n) {
		consensusNucleus = n;
	}

	@Override
	public Nucleus getConsensusNucleus() {
		return consensusNucleus;
	}

	@Override
	public void setRefolding(boolean b) {
		isRefolding = b;
	}

	@Override
	public boolean isRefolding() {
		return isRefolding;
	}

	@Override
	public boolean hasCells() {
		return !cellIDs.isEmpty();
	}

	@Override
	public boolean contains(ICell cell) {
		return cellIDs.contains(cell.getId());
	}

	@Override
	public boolean containsExact(ICell cell) {
		return parent.getCollection().containsExact(cell);
	}

	@Override
	public boolean hasLockedCells() {
		return parent.getCollection().hasLockedCells();
	}

	@Override
	public void setCellsLocked(boolean b) {}

	@Override
	public IProfileCollection getProfileCollection(ProfileType type) {
		return profileCollections.get(type);
	}

	@Override
	public void setProfileCollection(ProfileType type, IProfileCollection p) {
		profileCollections.put(type, p);
		
	}

	@Override
	public void removeProfileCollection(ProfileType type) {
		profileCollections.remove(type);
		
	}

	@Override
	public File getFolder() {
		return parent.getCollection().getFolder();
	}

	@Override
	public String getOutputFolderName() {
		return parent.getCollection().getOutputFolderName();
	}

	@Override
	public File getOutputFolder() {
		return parent.getCollection().getOutputFolder();
	}

	@Override
	public Set<File> getImageFiles() {
		
		Set<File> result = new HashSet<File>(cellIDs.size());
		
		for(ICell c : getCells()){
			result.add(c.getNucleus().getSourceFile());
		}
		return result;
	}

	@Override
	public Set<UUID> getSignalGroupIDs() {
		return parent.getCollection().getSignalGroupIDs();
	}

	@Override
	public void removeSignalGroup(UUID id) {}

	@Override
	public ISignalGroup getSignalGroup(UUID signalGroup) {
		return parent.getCollection().getSignalGroup(signalGroup);
	}

	@Override
	public boolean hasSignalGroup(UUID signalGroup) {
		return parent.getCollection().hasSignalGroup(signalGroup);
	}

	@Override
	public Collection<ISignalGroup> getSignalGroups() {
		return parent.getCollection().getSignalGroups();
	}

	@Override
	public void addSignalGroup(UUID newID, ISignalGroup newGroup) {
		parent.getCollection().addSignalGroup(newID, newGroup);
	}

	@Override
	public SignalManager getSignalManager() {
		return parent.getCollection().getSignalManager();
	}

	@Override
	public RuleSetCollection getRuleSetCollection() {
		return parent.getCollection().getRuleSetCollection();
	}

	@Override
	public void updateVerticalNuclei() {
		parent.getCollection().updateVerticalNuclei();
	}

	@Override
	public boolean updateSourceFolder(File expectedImageDirectory) {
		return parent.getCollection().updateSourceFolder(expectedImageDirectory);
	}

	@Override
	public Nucleus getNucleusMostSimilarToMedian(Tag referencePoint) throws ProfileException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ProfileManager getProfileManager() {
		return parent.getCollection().getProfileManager();
	}

	@Override
	public ICellCollection and(ICellCollection collection) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ICellCollection not(ICellCollection collection) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ICellCollection xor(ICellCollection collection) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ICellCollection filterCollection(PlottableStatistic stat, MeasurementScale scale, double lower,
			double upper) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int countShared(IAnalysisDataset d2) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int countShared(ICellCollection d2) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMedianArrayLength() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMaxProfileLength() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getMedianPathLength() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getMedianStatistic(PlottableStatistic stat, MeasurementScale scale) throws Exception {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double[] getMedianStatistics(PlottableStatistic stat, MeasurementScale scale) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double[] getMedianStatistics(PlottableStatistic stat, MeasurementScale scale, UUID id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getNormalisedDifferenceToMedian(Tag pointType, ICell c) {
		// TODO Auto-generated method stub
		return 0;
	}

}
