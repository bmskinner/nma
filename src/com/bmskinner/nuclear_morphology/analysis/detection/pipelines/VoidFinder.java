package com.bmskinner.nuclear_morphology.analysis.detection.pipelines;

import java.io.File;
import java.util.stream.Stream;

import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;

/**
 * An implementation of the Finder for analyses that don't return values For
 * example, FISH remapping and signal assignment
 * 
 * @author ben
 * @since 1.13.5
 *
 */
public abstract class VoidFinder extends AbstractFinder<Void> {

    public VoidFinder(IAnalysisOptions op) {
        super(op);
    }

    @Override
    public Void findInFolder(File folder) throws ImageImportException, ComponentCreationException {

        File[] arr = folder.listFiles();
        if (arr == null) {
            return null;
        }

        Stream.of(arr).parallel().forEach(f -> {
            if (!f.isDirectory()) {

                if (ImageImporter.fileIsImportable(f)) {
                    try {
                        findInImage(f);
                    } catch (ImageImportException | ComponentCreationException e) {
                        stack("Error searching image", e);
                    }
                }
            }
        });

        return null;
    }

}
