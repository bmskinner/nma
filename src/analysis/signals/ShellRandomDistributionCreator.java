/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package analysis.signals;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import logging.Loggable;
import analysis.signals.ShellDetector.Shell;
import components.CellularComponent;
import components.generic.XYPoint;


public class ShellRandomDistributionCreator implements Loggable {
	
	public static final UUID RANDOM_SIGNAL_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
	
	/**
	 * Store the shell as a key, and the number of signals measured as a value
	 */
	private Map<Integer, Integer> map = new HashMap<Integer, Integer>();
	
	public static final int DEFAULT_ITERATIONS = 10000;

	
	public ShellRandomDistributionCreator(CellularComponent template, int shellCount, int iterations){
		
		if(shellCount<=1){
			throw new IllegalArgumentException("Shell count must be > 1");
		}
		
		// Make a list of random points
		List<XYPoint> list = new ArrayList<XYPoint>();
		for(int i=0; i<iterations; i++){
			list.add(createRandomPoint(template));
		}
		
		
		// Find the shell for these points in the template
		ShellDetector detector;
		try {
			detector = new ShellDetector(template, shellCount);
			
			// initialise the map
			for(int i=-1; i<shellCount; i++){
				map.put(i, 0);
			}
			
			
			for(XYPoint p : list){
				int shell = detector.findShell(p);
				
				
				int count = map.get(shell);
				map.put(shell, ++count);
			}
			
		} catch (ShellAnalysisException e) {
			error("Simulation failed", e);
		}
		
		int neg1 = -1;
		
		if(map.get(neg1) > 0){
			fine("Unable to map "+map.get(neg1)+" points");
		}

	}
	
	/**
	 * Get the number of signals measured in the given shell
	 * @param shell
	 * @return
	 */
	public int getCount(int shell){
		if(!map.containsKey(shell)){
			return 0;
		}
		return map.get(shell);
	}
	
	/**
	 * Get the total number of hits excluding unmapped points
	 * @return
	 */
	private int getTotalCount(){
		int result = 0;
		for(int i : map.keySet()){
			if (i==-1){
				continue;
			}
			result += map.get(i);
		}
		return result;
	}
	
	/**
	 * Get the proportion of total signal in the given shell
	 * @param shell
	 * @return
	 */
	public double getProportion(int shell){
		if(!map.containsKey(shell)){
			return 0;
		}
		int total = getTotalCount();
		
		int count = map.get(shell);
		
		double prop = (double) count / (double) total;

		return prop;
	}
	
	public double[] getProportions(){
		
		int shells = map.size()-1;
		
		double[] result = new double[shells];
		
		for(int i=0; i<shells; i++){
			result[i] = getProportion(i);
		}
		return result;
	}
	
	public int[] getCounts(){
		
		int shells = map.size()-1;
		
		int[] result = new int[shells];
		
		for(int i=0; i<shells; i++){
			result[i] = getCount(i);
		}
		return result;
	}
	
	/**
	 * Create a random point that lies within the template
	 * @param template
	 * @return
	 */
	private XYPoint createRandomPoint(CellularComponent template){
		
		Rectangle r = template.getBounds();
		
		
		// Make a random position in the rectangle
		// nextDouble is exclusive of the top value,
		// so add 1 to make it inclusive
		double rx = ThreadLocalRandom.current().nextDouble(r.x, r.width + 1);
		double ry = ThreadLocalRandom.current().nextDouble(r.y, r.height + 1);
		
		XYPoint p = new XYPoint(rx, ry);
		
		if(template.containsPoint(p)){
			return p;
		} else {
			return createRandomPoint(template);
		}
		
	}
}
