package com.bmskinner.nuclear_morphology.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.mesh.DefaultMesh;
import com.bmskinner.nuclear_morphology.analysis.mesh.Mesh;
import com.bmskinner.nuclear_morphology.analysis.mesh.MeshCreationException;
import com.bmskinner.nuclear_morphology.analysis.mesh.MeshVertex;
import com.bmskinner.nuclear_morphology.components.Taggable;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.IJ;

/**
 * Export nuclear border landmarks in TPS format suitable
 * for geometric morphometric analysis by packages such
 * as geomorph. Note that this method only works on single datasets
 * because we cannot guarantee consistency of landmark number across 
 * different datasets.
 * @author Ben Skinner
 * @since 1.18.0
 *
 */
public class TPSexporter extends SingleDatasetAnalysisMethod implements Io {
	
	private static final Logger LOGGER = Logger.getLogger(TPSexporter.class.getName());
	
	private final File exportFile;
	private final StringBuilder sb = new StringBuilder();
	private static final int TPS_VERTEX_SPACING = 5;
	
	public TPSexporter(@NonNull File file, @NonNull IAnalysisDataset dataset) {
		super(dataset);

		exportFile = file;

		try {
			Files.deleteIfExists(exportFile.toPath());
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Unable to delete file: "+exportFile);
			LOGGER.log(Loggable.STACK, "Unable to delete existing file", e);
		}
	}
	
	@Override
	public IAnalysisResult call() throws Exception {
		export();
        return new DefaultAnalysisResult(dataset);
	}
	
	/**
	 * Export each dataset in turn. If multiple datasets are selected, 
	 * we want the outputs to be comparable i.e. the same segmentation and
	 * number of vertices in each. 
	 */
	private void export() {
		if(!dataset.getCollection().hasConsensus()) {
			LOGGER.fine("No consensus in "+dataset.getName()+", skipping");
			return;
		}
		
		
		try {
			Mesh<Taggable> consensus = new DefaultMesh<>(dataset.getCollection().getConsensus(), TPS_VERTEX_SPACING);
			for(Nucleus n : dataset.getCollection().getNuclei()) {

				try {
					appendPerimeter(n.getOrientedNucleus(), consensus);
				}	catch (MeshCreationException e) {
					LOGGER.warning("Unable to create mesh for nucleus "+n.getNameAndNumber());
					LOGGER.fine("Mesh creation error: "+e.getMessage());
				}
			}
		} catch (MeshCreationException e) {
			LOGGER.log(Level.SEVERE, "Unable to create mesh for consensus nucleus", e);
		}
		
		fireIndeterminateState();
        IJ.append(sb.toString(), exportFile.getAbsolutePath());
	}
	
	/**
	 * Append the perimeter points of the object to the 
	 * string builder in TPS format
	 * @param t
	 * @throws MeshCreationException 
	 */
	private void appendPerimeter(Taggable t, Mesh<Taggable> target) throws MeshCreationException {
		
		/*
		 * TPS format example:
		 * 	LM=3
			873.305842153513 838.969114092841
			758.096241353446 862.011034252855
			947.039986665555 576.291224268689
			ID=D0_R1_10314.jpg
		 */
		
		// A mesh generates the evenly spaced landmarks by default
		Mesh<Taggable> mesh = new DefaultMesh<>(t, target);
				
		int vertexCount = mesh.getPeripheralVertexCount();
		
		sb.append("LM="+vertexCount+Io.NEWLINE);
		for(MeshVertex v : mesh.getPeripheralVertices()) {
			IPoint p = v.getPosition();
			sb.append(p.getX()+Io.SPACE+p.getY()+Io.NEWLINE);
		}
		sb.append("ID="+t.getSourceFileName()+Io.NEWLINE+Io.NEWLINE);
	}
}
