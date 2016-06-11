package analysis.signals;

import java.util.List;
import java.util.UUID;

import components.Cell;

import ij.process.ImageProcessor;
import analysis.AnalysisDataset;
import analysis.AnalysisWorker;
import analysis.mesh.NucleusMesh;
import analysis.mesh.NucleusMeshImage;

public class SignalWarper extends AnalysisWorker {
	
	private UUID signalGroup;
	ImageProcessor[] warpedImages;
		
	public SignalWarper(AnalysisDataset dataset, UUID signalGroup){
		super(dataset);
		this.signalGroup = signalGroup;
		
		SignalManager m =  getDataset().getCollection().getSignalManager();
		int count = m.getNumberOfCellsWithNuclearSignals(signalGroup);
		
		warpedImages = new ImageProcessor[ count ];
		this.setProgressTotal(count);
		
		
		fine("Created signal warper for "+dataset.getName()+" signal group "+signalGroup);
	}
	

	@Override
	protected Boolean doInBackground() throws Exception {
		boolean result = false;
		try {
			finer("Running warper");
			
			if( ! getDataset().getCollection().hasConsensusNucleus()){
				warn("No consensus nucleus in dataset");
				return false;
			}
			
			generateImages();
			result = true;
			
		} catch (Exception e){
			result = false;
		}
		return result;
	}
	
	/**
	 * Get the images with warped signals
	 * @return
	 */
	public ImageProcessor[] getResults(){
		return warpedImages;
	} 
	
	private void generateImages(){
		
		NucleusMesh meshConsensus = new NucleusMesh( getDataset().getCollection().getConsensusNucleus());
		
		SignalManager m =  getDataset().getCollection().getSignalManager();
		List<Cell> cells = m.getCellsWithNuclearSignals(signalGroup, true);
		
		int cellNumber = 0;
		for(Cell cell : cells){
			fine("Drawing signals for cell "+cell.getNucleus().getNameAndNumber());
			// Get each nucleus. Make a mesh.
			NucleusMesh cellMesh = new NucleusMesh(cell.getNucleus(), meshConsensus);
			
			// Get the image with the signal
			ImageProcessor ip = cell.getNucleus().getSignalCollection().getImage(signalGroup);
			
			// Create NucleusMeshImage from nucleus.
			NucleusMeshImage im = new NucleusMeshImage(cellMesh,ip);
			
			// Draw NucleusMeshImage onto consensus mesh.
			ImageProcessor warped = im.meshToImage(meshConsensus);
			warpedImages[cellNumber] = warped;
			publish(cellNumber++);
			
			
		}
		
	}

}
