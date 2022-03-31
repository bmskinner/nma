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
package com.bmskinner.nuclear_morphology.components.signals;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.DoubleStream;
import java.util.stream.LongStream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.components.cells.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.io.XmlSerializable;
import com.bmskinner.nuclear_morphology.stats.Stats;

/**
 * This shell result is designed to allow raw data to be
 * exported from shell analyses. Averages and normalisations are computed when data are requested,
 * rather than needing to be pre-computed and stored.
 * @author bms41
 * @since 1.13.8
 *
 */
public class DefaultShellResult implements IShellResult {
    
	private final int nShells;
	private final ShrinkType type;
	private final Map<CountType, ShellCount> map = new EnumMap<>(CountType.class);    
    
    /**
     * Create with the given number of shells
     * @param nShells
     */
    public DefaultShellResult(int nShells, ShrinkType type){
        if (nShells < 1) 
            throw new IllegalArgumentException("Shell count must be greater than 1");
        
        this.nShells = nShells;
        this.type = type;
        
        for(CountType t : CountType.values()) {
        	map.put(t, new ShellCount());
        }
    }
    
    /**
     * Construct from an XML element. Use for 
     * unmarshalling. The element should conform
     * to the specification in {@link XmlSerializable}.
     * @param e the XML element containing the data.
     */
    public DefaultShellResult(Element e) throws ComponentCreationException {
    	
    	if(e.getChild("nShells")==null)
    		throw new ComponentCreationException("No nShells child in element: "+e.toString());
    	
    	if(e.getChild("ShrinkType")==null)
    		throw new ComponentCreationException("No ShrinkType child in element: "+e.toString());
    	
    	if(e.getChildren("ShellCount").isEmpty())
    		throw new ComponentCreationException("No ShellCount children in element: "+e.toString());

    	nShells = Integer.parseInt(e.getChildText("nShells"));
    	type = ShrinkType.valueOf(e.getChildText("ShrinkType"));
    	
    	for(Element el : e.getChildren("ShellCount")) {
    		CountType c = CountType.valueOf(el.getAttributeValue("CountType"));
    		ShellCount s = new ShellCount(el);
    		map.put(c, s);
    	}
    }
    
    @Override
	public Element toXmlElement() {
		Element e = new Element("ShellResult");
		
		e.addContent(new Element("nShells").setText(String.valueOf(nShells)));
		e.addContent(new Element("ShrinkType").setText(type.toString()));
		
		for(Entry<CountType, ShellCount> entry : map.entrySet()) {
			e.addContent(entry.getValue().toXmlElement()
					.setAttribute("CountType", entry.getKey().toString()));
		}
		
		return e;
	}



	/**
     * Create with the given number of shells
     * @param nShells
     */
    private DefaultShellResult(DefaultShellResult r){
    	this(r.getNumberOfShells(), r.getType());
        
        for(CountType t : CountType.values()) {
        	map.put(t, r.map.get(t).duplicate());
        }
    }
    
    @Override
    public IShellResult duplicate() {
    	return new DefaultShellResult(this);
    }
    
    /**
     * Add shell data for the given nucleus in the given cell.
     * @param cell the cell
     * @param component the nucleus
     * @param shellData the pixel intensity counts per shell
     */
    public void addShellData(@NonNull CountType countType, @NonNull ICell c, @NonNull Nucleus n, long @NonNull [] shellData){
        addShellData(countType, c, n, null, shellData);
    }
    
    /**
     * Add shell data for a signal in the given nucleus in the given cell.
     * @param cell the cell
     * @param nucleus the nucleus
     * @param signal the signal
     * @param shellData the pixel intensity counts per shell
     */
    public void addShellData(@NonNull CountType countType, @NonNull ICell cell, @NonNull Nucleus nucleus, @Nullable INuclearSignal signal, long @NonNull [] shellData){
        if (shellData.length != nShells) 
            throw new IllegalArgumentException("Shell count must be "+nShells);
        
        // Sanitise inputs
        for(int i=0; i<nShells; i++){
            if(shellData[i]<0){
                shellData[i]=0;
            }
        }
        
        ShellKey k = signal==null 
        		? new ShellKey(cell.getId(), nucleus.getID()) 
        		: new ShellKey(cell.getId(), nucleus.getID(), signal.getID());

        map.get(countType).putValues(k, shellData);
    }
    
    @Override
	public long[] getPixelValues(@NonNull CountType countType, @NonNull ICell cell, @NonNull Nucleus nucleus, @Nullable INuclearSignal signal) {
    	ShellKey k = signal==null 
        		? new ShellKey(cell.getId(), nucleus.getID()) 
        		: new ShellKey(cell.getId(), nucleus.getID(), signal.getID());
    	return map.get(countType).getPixelIntensities(k);
    }  
    
    @Override
	public double[] getProportions(@NonNull CountType countType, @NonNull ICell cell, @NonNull Nucleus nucleus, @Nullable INuclearSignal signal) {
    	ShellKey k = signal==null 
        		? new ShellKey(cell.getId(), nucleus.getID()) 
        		: new ShellKey(cell.getId(), nucleus.getID(), signal.getID());
        		
        long[] intensities = map.get(countType).getPixelIntensities(k);
        if(intensities==null)
        	return makeEmptyArray();
        long total = LongStream.of(intensities).sum();
        if(total==0)
        	return makeEmptyArray();

        return LongStream.of(intensities).mapToDouble(l-> (double)l/(double)total).toArray();
    } 
    
    private double[] makeEmptyArray() {
    	double[] result = new double[nShells];
    	Arrays.fill(result, 0);
    	return result;
    }
    
    @Override
    public double[] getProportions(@NonNull Aggregation agg, @NonNull Normalisation norm) {
    	double[] result = new double[nShells];
    	for(int i=0; i<nShells; i++){
            result[i] = getAverageProportion(norm, agg, i);
        }
        return result;
    }
    
    @Override
    public double[] getStdErrs(@NonNull Aggregation agg, @NonNull Normalisation norm) {
    	double[] result = new double[nShells];
    	for(int i=0; i<nShells; i++){
            result[i] = getStdErr(norm, agg, i);
        }
        return result;
    }

    @Override
    public int getNumberOfShells() {
        return nShells;
    }
    
    @Override
    public ShrinkType getType() {
        return type;
    }
    
    @Override
   	public int getNumberOfSignals(@NonNull Aggregation agg) {
    	return map.get(CountType.SIGNAL).size(agg);
    }
    
    /**
     * Get the observed values as a long array.
     * @return the observed shell values
     */
    @Override
	public long[] getAggregateCounts(@NonNull Aggregation agg, @NonNull Normalisation norm) {
        long[] observed = new long[nShells];
        int nCells = map.get(CountType.SIGNAL).size(agg);
        double[] means = getProportions(agg, norm);
        for (int i = 0; i < nShells; i++) {
            double meanSignalProportion = means[i]; 
            observed[i] = (long) (meanSignalProportion * nCells); 
        }
        return observed;
    }

	@Override
	public double getOverallShell(@NonNull Aggregation agg, @NonNull Normalisation norm) {
		double[] props = getProportions(agg, norm);
		
		double overall = 0;
		for(int i=0; i<nShells; i++) {
			overall += props[i]*i;
		}
		return overall;
	}
	    
    /**
     * Get the proportions of signal in the given shell by type
     * @param agg the type of signal to fetch
     * @param norm the normalisation to apply
     * @param shell the shell to fetch
     * @return the proportion of signal in the shell in the range 0-1 for each object
     */
    private double[] getProportions(Aggregation agg, Normalisation norm, int shell){
        if(shell<0||shell>=nShells)
            throw new IllegalArgumentException("Shell is out of bounds");
                        
        switch(norm) {
	    	case NONE: return map.get(CountType.SIGNAL).keys(agg).stream()
					.mapToDouble(key->{
						double sig = map.get(CountType.SIGNAL).getPixelIntensity(key, shell);
						double tot = map.get(CountType.SIGNAL).sum(key);
						return sig/tot;
					}).toArray();
	    	case DAPI: {
	    		return map.get(CountType.SIGNAL).keys(agg).stream()
	    		.mapToDouble(k-> {
	    			
	    			long[] sigs = map.get(CountType.SIGNAL).getPixelIntensities(k);
	    			long[] cnts = map.get(CountType.COUNTERSTAIN).getPixelIntensities(k.componentKey());
	    			
	    			double sig = sigs[shell];
	    			double cnt = cnts[shell];
	    			double nor = cnt==0d?0d:sig/cnt;
	    			        			
	    			double totalNor = 0;
	    			for(int i=0; i<nShells; i++) {	
	    				totalNor += cnts[i]==0d?0d:(double)sigs[i]/(double)cnts[i];
	    			}
	    			return nor / totalNor;	
	    		}).toArray();
	    	}
	    	default: return new double[0];
        }	
    }
        
    /**
     * Get the mean proportion of signal in the given shell by type
     * @param type the type of signal to fetch
     * @param norm the normalisation to apply
     * @param shell the shell to fetch
     * @return the mean signal proportion (the mean of the values returned by {@link getProportions()}
     */
    private double getAverageProportion(Normalisation norm, Aggregation agg, int shell){
        return DoubleStream.of(getProportions(agg, norm, shell)).average().orElse(-1);
    }
    
    private double getStdErr(Normalisation norm, Aggregation agg, int shell){
        return Stats.stderr(getProportions(agg, norm, shell));
    }
    
    @Override
    public String toString(){
    	StringBuilder sb = new StringBuilder();
    	sb.append("Shell result hash: "+hashCode()+"\n");
    	sb.append("Shrink type: "+type+"\n")
    	.append("nShells: "+nShells+"\n");
    	for(CountType t : CountType.values()){
    		sb.append(t +"\n"+map.get(t).toString());
    	}
    	return sb.toString();
    }

    
    
    @Override
	public int hashCode() {
		return Objects.hash(map, nShells, type);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DefaultShellResult other = (DefaultShellResult) obj;
		
		if(nShells != other.nShells)
			return false;
		if(type != other.type)
			return false;
		
		if(map.size()!=other.map.size())
			return false;
		
		for(Entry<CountType, ShellCount> e : map.entrySet()) {
			if(!other.map.containsKey(e.getKey()))
				return false;
			if(!other.map.get(e.getKey()).equals(e.getValue()))
				return false;
		}
		
		return true;
	}

    
    
    
}
