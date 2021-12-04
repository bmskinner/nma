/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nuclear_morphology.analysis.detection.pipelines;

import java.io.File;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * An implementation of the Finder for cells
 * 
 * @author ben
 * @since 1.13.5
 *
 */
public abstract class CellFinder extends AbstractFinder<Collection<ICell>> {
	
	private static final Logger LOGGER = Logger.getLogger(CellFinder.class.getName());

    /**
     * Construct the finder using an options
     * 
     * @param op the options for cell detection
     */
    protected CellFinder(@NonNull final IAnalysisOptions op) {
        super(op);

    }

    @Override
    public Collection<ICell> findInFolder(@NonNull final File folder) throws ImageImportException {

    	final Queue<ICell> list = new ConcurrentLinkedQueue<>();
    	File[] arr = folder.listFiles();
    	if (arr == null)
    		return list;
    	
    	// single threaded for use in testing only
    	for(File f : arr) {
    		if(Thread.interrupted())
    			continue;
    		if(f.isDirectory())
    			continue;
    		if (!ImageImporter.fileIsImportable(f))
    			continue;
    		try {
    			list.addAll(findInImage(f));
    		} catch (ImageImportException e) {
    			LOGGER.log(Loggable.STACK, "Error searching image", e);
    		}
    		LOGGER.fine("Found images in "+f.getName());
    	}

    	// Submitted to the FJP::commonPool, which is thread limited by the ThreadManger
//    	Stream.of(arr).parallel().forEach(f -> {
//
//    		if(Thread.interrupted())
//    			return;
//    		if(f.isDirectory())
//    			return;
//    		if (!ImageImporter.fileIsImportable(f))
//    			return;
//    		try {
//    			list.addAll(findInImage(f));
//    		} catch (ImageImportException e) {
//    			LOGGER.log(Loggable.STACK, "Error searching image", e);
//    		}
//    		LOGGER.fine("Found images in "+f.getName());
//    	});
    	return list;
    }
    
    public static boolean isValid(HashOptions o, CellularComponent c) {
    	if (c == null)
            return false;
        if (c.getStatistic(Measurement.AREA) < o.getInt(HashOptions.MIN_SIZE_PIXELS))
            return false;
        if (c.getStatistic(Measurement.AREA) > o.getInt(HashOptions.MAX_SIZE_PIXELS))
            return false;
        if (c.getStatistic(Measurement.CIRCULARITY) < o.getDouble(HashOptions.MIN_CIRC))
            return false;
        if (c.getStatistic(Measurement.CIRCULARITY) > o.getDouble(HashOptions.MAX_CIRC))
            return false;
        return true;
    }

}
