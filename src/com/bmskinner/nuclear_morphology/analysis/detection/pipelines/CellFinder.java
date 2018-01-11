/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


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
 * 
 * @author ben
 * @since 1.13.5
 *
 */
public abstract class CellFinder extends AbstractFinder<List<ICell>> {

    /**
     * Construct the finder using an options
     * 
     * @param op
     */
    public CellFinder(final IAnalysisOptions op) {
        super(op);

    }

    @Override
    public List<ICell> findInFolder(final File folder) throws ImageImportException, ComponentCreationException {

        if (folder == null) {
            throw new IllegalArgumentException("Folder cannot be null");
        }
        List<ICell> list = new ArrayList<>();
        File[] arr = folder.listFiles();
        if (arr == null) {
            return null;
        }
        
        Stream.of(arr).parallel().forEach(f -> {
            
            if(Thread.interrupted()){
                return;
            }
            
            if (!f.isDirectory()) {

                if (ImageImporter.fileIsImportable(f)) {
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
