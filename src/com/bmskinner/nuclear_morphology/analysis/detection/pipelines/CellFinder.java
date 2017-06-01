package com.bmskinner.nuclear_morphology.analysis.detection.pipelines;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;

/**
 * An implementation of the Finder for cells
 * @author ben
 * @since 1.13.5
 *
 */
public abstract class CellFinder extends AbstractFinder<List<ICell>> {

	/**
	 * Construct the finder using an options
	 * @param op
	 */
	public CellFinder(IAnalysisOptions op) {
		super(op);

	}
	
	@Override
	public List<ICell> findInFolder(File folder) throws ImageImportException, ComponentCreationException{
		
		if(folder==null){
			throw new IllegalArgumentException("Folder cannot be null");
		}
		List<ICell> list = new ArrayList<>();
		File[] arr = folder.listFiles();
		if(arr==null){
			return null;
		}

		Stream.of(arr).parallel().forEach( f -> {
			if( ! f.isDirectory()){
				
				if(ImageImporter.fileIsImportable(f)){
					try {
						list.addAll(findInImage(f));
					} catch (ImageImportException | ComponentCreationException e) {
						stack("Error searching image", e);
					}
				}
			}
		});

		return list;
	}

}
