package com.bmskinner.nuclear_morphology.analysis.signals.shells;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.LongStream;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult.CountType;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Allow cell collections to be filtered based on
 * the shell result in each cell.
 * @author ben
 * @since 1.14.0
 *
 */
public class ShellResultCellFilterer implements Loggable {
	
	private UUID signalGroupId;
	private double proportion;
	private int shell;
	private ShellResultFilterOperation operation;
	
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
	
	public ICellCollection filter(@NonNull ICellCollection c) {
		if(operation==null)
			throw new IllegalArgumentException("Operation must be set before filtering");
		
		Optional<ISignalGroup> group = c.getSignalGroup(signalGroupId);
		if(!group.isPresent())
			throw new IllegalArgumentException("No such signal group present: "+signalGroupId);
		
		Optional<IShellResult> r = group.get().getShellResult();
		if(!r.isPresent())
			throw new IllegalArgumentException("No shell result in signal group");
		
		final IShellResult s = r.get();
//		log(String.format("Making nucleus filter for operation %s with shell %s and proportion %s on signal group %s", operation, shell, proportion, signalGroupId));
		Predicate<ICell> pred = (cell)->{
			boolean cellPasses = true;
			for(Nucleus n : cell.getNuclei()) {
				try {
					long[] pixels = s.getPixelValues(CountType.SIGNAL, cell, n, null);
					if(pixels==null) {
						cellPasses &= false;
//						log(String.format("Cell: %s No pixels", operation));
						continue;
					}
					long total = LongStream.of(pixels).sum();
					double[] props = LongStream.of(pixels).mapToDouble(l->(double)l/(double)total).toArray();
					switch(operation) {
						case SPECIFIC_SHELL_IS_MORE_THAN: cellPasses &= props[shell]>=proportion; 
//						log(String.format("Cell: %s prop %s and needed %s: %s", operation, props[shell], proportion, cellPasses));
						break;
						case SPECIFIC_SHELL_IS_LESS_THAN: cellPasses &= props[shell]<=proportion; 
//						log(String.format("Cell: %s prop %s and needed %s: %s", operation, props[shell], proportion, cellPasses));
						break;
						
						case SHELL_PLUS_SHELLS_INTERIOR_TO_MORE_THAN: {
							double sum = 0;
							for(int i=shell;i<s.getNumberOfShells(); i++) {
								sum+=props[i];
							}
//							log(String.format("Cell: %s sum %s and needed %s: %s", operation, sum, proportion, cellPasses));
							cellPasses &= sum>=proportion;
							 break;
						}
						case SHELL_PLUS_SHELLS_PERIPHERAL_TO_MORE_THAN: {
							double sum = 0;
							for(int i=0; i<=shell; i++) {
								sum+=props[i];
							}
//							log(String.format("Cell: %s sum %s and needed %s: %s", operation, sum, proportion, cellPasses));
							cellPasses &= sum>=proportion;
							 break;
						}
//						default: log(String.format("Cell: %s needed %s. No valid operation provided", operation, proportion));
					}
				} catch(Exception e) {
					cellPasses &= false;
					System.out.println(e.getMessage());
					e.printStackTrace();
				}
			}
			return cellPasses;
		};
		return c.filter(pred);
	}
}
