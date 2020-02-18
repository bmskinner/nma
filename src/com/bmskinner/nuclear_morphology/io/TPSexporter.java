package com.bmskinner.nuclear_morphology.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.MultipleDatasetAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.mesh.DefaultMesh;
import com.bmskinner.nuclear_morphology.analysis.mesh.Mesh;
import com.bmskinner.nuclear_morphology.analysis.mesh.MeshCreationException;
import com.bmskinner.nuclear_morphology.analysis.mesh.MeshVertex;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.Taggable;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.io.Io.Exporter;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.IJ;

/**
 * Export nuclear border landmarks in TPS format suitable
 * for geometric morphometric analysis by packages such
 * as geomorph
 * @author Ben Skinner
 * @since 1.18.0
 *
 */
public class TPSexporter extends MultipleDatasetAnalysisMethod implements Exporter {
	
	private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	
	private final File exportFile;
	private final StringBuilder sb = new StringBuilder();
	private static final String DEFAULT_MULTI_FILE_NAME = "Multiple_dataset_export.tps";
	
	public TPSexporter(@NonNull File file, @NonNull List<IAnalysisDataset> list) {
		super(list);

		if (file.isDirectory())
			file = new File(file, DEFAULT_MULTI_FILE_NAME);

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
        return new DefaultAnalysisResult(datasets);
	}
	
	/**
	 * Export each dataset in turn 
	 */
	private void export() {
		for(IAnalysisDataset d : datasets) {
			
			if(!d.getCollection().hasConsensus()) {
				LOGGER.fine("No consensus in "+d.getName()+", skipping");
				continue;
			}
			try {
				Mesh<Taggable> consensus = new DefaultMesh<>(d.getCollection().getConsensus());
				for(Nucleus n : d.getCollection().getNuclei()) {
					appendPerimeter(n.getVerticallyRotatedNucleus(), consensus);
				}
			} catch (MeshCreationException e) {
				LOGGER.log(Level.SEVERE, "Unable to create mesh for nucleus", e);
			}
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
		// Use this 
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
