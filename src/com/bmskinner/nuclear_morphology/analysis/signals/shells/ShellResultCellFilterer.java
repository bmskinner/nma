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
package com.bmskinner.nuclear_morphology.analysis.signals.shells;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.LongStream;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.nucleus.CellCollectionFilterer;
import com.bmskinner.nuclear_morphology.analysis.nucleus.CellCollectionFilterer.CollectionFilteringException;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.signals.IShellResult;
import com.bmskinner.nuclear_morphology.components.signals.IShellResult.CountType;
import com.bmskinner.nuclear_morphology.components.signals.ISignalGroup;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Allow cell collections to be filtered based on
 * the shell result in each cell.
 * @author ben
 * @since 1.14.0
 *
 */
public class ShellResultCellFilterer {
	
	private UUID signalGroupId;
	private double proportion;
	private int shell;
	private ShellResultFilterOperation operation;
	
	private static final Logger LOGGER = Logger.getLogger(ShellResultCellFilterer.class.getName());
	
	public ShellResultCellFilterer(@NonNull UUID signalGroupId) {
		this.signalGroupId = signalGroupId;
	}
	
	/**
	 * The possible ways in which a shell result can be filtered
	 * @author ben
	 * @since 1.14.0
	 *
	 */
	public enum ShellResultFilterOperation {
		SPECIFIC_SHELL_IS_MORE_THAN,
		SPECIFIC_SHELL_IS_LESS_THAN, 
		SHELL_PLUS_SHELLS_INTERIOR_TO_MORE_THAN, 
		SHELL_PLUS_SHELLS_PERIPHERAL_TO_MORE_THAN
	}
		
	public ShellResultCellFilterer setFilter(@NonNull ShellResultFilterOperation op, int shell, double proportion) {
		operation = op;
		this.shell = shell;
		this.proportion = proportion;
		return this;
	}
	
	public ICellCollection filter(@NonNull ICellCollection c) throws CollectionFilteringException {
		if(operation==null)
			throw new IllegalArgumentException("Operation must be set before filtering");
		
		Optional<ISignalGroup> group = c.getSignalGroup(signalGroupId);
		if(!group.isPresent())
			throw new IllegalArgumentException("No such signal group present: "+signalGroupId);
		
		Optional<IShellResult> r = group.get().getShellResult();
		if(!r.isPresent())
			throw new IllegalArgumentException("No shell result in signal group");
		
		final IShellResult s = r.get();
		Predicate<ICell> pred = (cell)->{
			boolean cellPasses = true;
			for(Nucleus n : cell.getNuclei()) {
				try {
					long[] pixels = s.getPixelValues(CountType.SIGNAL, cell, n, null);
					if(pixels==null) {
						cellPasses &= false;
						continue;
					}
					long total = LongStream.of(pixels).sum();
					double[] props = LongStream.of(pixels).mapToDouble(l->(double)l/(double)total).toArray();
					switch(operation) {
						case SPECIFIC_SHELL_IS_MORE_THAN: cellPasses &= props[shell]>=proportion; 
						break;
						case SPECIFIC_SHELL_IS_LESS_THAN: cellPasses &= props[shell]<=proportion; 
						break;
						
						case SHELL_PLUS_SHELLS_INTERIOR_TO_MORE_THAN: {
							double sum = 0;
							for(int i=shell;i<s.getNumberOfShells(); i++) {
								sum+=props[i];
							}
							cellPasses &= sum>=proportion;
							 break;
						}
						case SHELL_PLUS_SHELLS_PERIPHERAL_TO_MORE_THAN: {
							double sum = 0;
							for(int i=0; i<=shell; i++) {
								sum+=props[i];
							}
							cellPasses &= sum>=proportion;
							 break;
						}
					}
				} catch(Exception e) {
					cellPasses &= false;
					LOGGER.log(Loggable.STACK, "Unable to filter cells", e);
				}
			}
			return cellPasses;
		};
		return CellCollectionFilterer.filter(c, pred);
	}
}
