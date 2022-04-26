package com.bmskinner.nuclear_morphology.visualisation.image;

import java.awt.image.BufferedImage;
import java.util.logging.Logger;

import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
import com.bmskinner.nuclear_morphology.components.cells.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.cells.Nucleus;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.mesh.DefaultMesh;
import com.bmskinner.nuclear_morphology.components.mesh.DefaultMeshImage;
import com.bmskinner.nuclear_morphology.components.mesh.Mesh;
import com.bmskinner.nuclear_morphology.components.mesh.MeshCreationException;
import com.bmskinner.nuclear_morphology.components.mesh.MeshImage;
import com.bmskinner.nuclear_morphology.components.mesh.MeshImageCreationException;
import com.bmskinner.nuclear_morphology.components.mesh.UncomparableMeshImageException;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.process.ImageProcessor;

public class WarpedCellPainter implements ImagePainter {

	private static final Logger LOGGER = Logger.getLogger(WarpedCellPainter.class.getName());

	private IAnalysisDataset dataset;
	private ICell cell;

	/**
	 * Create with cell to paint and ratio for final image
	 * @param cell
	 * @param ratio
	 */
	public WarpedCellPainter(IAnalysisDataset dataset, ICell cell) {
		this.dataset = dataset;
		this.cell = cell;
	}
	
	@Override
	public BufferedImage paintDecorated(int w, int h) {
		return paintRaw(w, h);
	}

	@Override
	public BufferedImage paintRaw(int w, int h) {
		ImageProcessor ip = ImageImporter.importFullImageTo24bit(cell.getPrimaryNucleus());
		try {
			Mesh consensusMesh = new DefaultMesh(dataset.getCollection().getConsensus());
			for(Nucleus n : cell.getNuclei()) {
				Mesh m = new DefaultMesh(n, consensusMesh);
				MeshImage im = new DefaultMeshImage(m, ip.duplicate());
				ip = im.drawImage(consensusMesh);
				ip.flipVertical();
			}
		} catch (MeshCreationException | IllegalArgumentException | MeshImageCreationException | UncomparableMeshImageException | MissingLandmarkException | ComponentCreationException e) {
			LOGGER.log(Loggable.STACK, "Error making mesh or loading image", e);
		}
		return ImageAnnotator.resizeKeepingAspect(ip, w, h).getBufferedImage();
	}

	@Override
	public BufferedImage paintMagnified(BufferedImage smallInput, BufferedImage largeInput, int cx, int cy,
			int smallRadius, int bigRadius) {
		// TODO Auto-generated method stub
		return smallInput;
	}



}
