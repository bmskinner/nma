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
package com.bmskinner.nma.gui.main;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.DecimalFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.bmskinner.nma.components.Version;
import com.bmskinner.nma.core.GlobalOptions;

/**
 * Display the memory in use. Based on the monitor in 
 * SeqMonk: https://www.bioinformatics.babraham.ac.uk/projects/seqmonk/
 * @author Ben Skinner
 * @since 1.13.7
 *
 */
public class MemoryIndicator extends JPanel
    implements Runnable {
	
	private static final Logger LOGGER = Logger.getLogger(MemoryIndicator.class.getName());
    
    private static final long serialVersionUID = 1L;
    private static final Color DARK_GREEN = new Color(0, 180, 0);
    private static final Color DARK_ORANGE = new Color(255, 130, 0);
    private static final Color DARK_RED = new Color(180, 0, 0);
    
    private static final String LOW_MEMORY_TTL = "Low memory";
    private static final String LOW_MEMORY_MSG = "Memory is running low! There is %s available to NMA.";
    
    private static final long SLEEP_TIME = 500L;
    
    protected static final String DEFAULT_DECIMAL_FORMAT = "#0.00";
	protected static final DecimalFormat DF = new DecimalFormat(DEFAULT_DECIMAL_FORMAT);
	
    private static final int PREFERRED_WIDTH = 100;
    private static final int PREFERRED_HEIGHT = 20;
    
    /** The lowest fraction of total system memory that the JVM can access before a notification is made */
    private static final double MEMORY_LOW_WARN_RATIO = 0.5;
    
    private boolean hasWarned = false;
    private boolean mustWarn = false;
    
    public MemoryIndicator() {
    	Thread t = new Thread(this);
    	t.setName("Memory use tracking thread");
    	t.start();

    	// If we are warning on low available JVM memory
    	if(GlobalOptions.getInstance().getBoolean(GlobalOptions.WARN_LOW_JVM_MEMORY_FRACTION)) {
    		try {
    			// Get the total system memory
    			OperatingSystemMXBean osmxb = ManagementFactory.getOperatingSystemMXBean();
    			Method method = osmxb.getClass().getMethod("getTotalPhysicalMemorySize");
    			method.setAccessible(true);

    			Long totalMemory = (Long) method.invoke(osmxb);
    			long jvmMaxMem =  Runtime.getRuntime().maxMemory();
    			double availableMemoryToJVM = (double)jvmMaxMem /  (double)totalMemory;
    			long egMemory =  Double.valueOf(totalMemory*0.8d).longValue()/(1024*1024);
    			if(availableMemoryToJVM<MEMORY_LOW_WARN_RATIO) {
    				LOGGER.info("NMA has only %s memory available of the system %s (%s%%). To increase maximum memory, you may wish to run the NMA standalone jar file from command line via 'java -Xmx%sm -jar Nuclear_Morphology_Analysis_%s.jar'. Disable this message via View>Preferences"
    						.formatted(formatMemory(jvmMaxMem), 
    								formatMemory(totalMemory),
    								DF.format(availableMemoryToJVM*100),
    								egMemory,
    								Version.currentVersion()
    								));
    			}

    		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | InvocationTargetException e) {
    			LOGGER.log(Level.FINE, "Unable to get total system memory: %s".formatted(e.getMessage()));
    		}
    	}
    }
    
    @Override
    public void run() {
      do  {
        try {
          Thread.sleep(SLEEP_TIME);
        } catch (InterruptedException e) {
        	LOGGER.log(Level.SEVERE, "Error in memory monitoring thread: %s".formatted(e.getMessage()));
        }
        
        if (mustWarn && !hasWarned)
          showMemoryWarning();
        repaint();
      } while(true);
    }
    
    @Override
    public Dimension getPreferredSize(){
      return new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT);
    }

    private synchronized void showMemoryWarning(){
    	if (this.hasWarned)
    		return;
    	hasWarned = true;

    	

    	JOptionPane.showMessageDialog(null, LOW_MEMORY_MSG.formatted(formatMemory(Runtime.getRuntime().maxMemory())), LOW_MEMORY_TTL, JOptionPane.WARNING_MESSAGE);
    }
    
    @Override
    public void paintComponent(Graphics g){
      super.paintComponent(g);
      paintMemory(g);
    }
      
    
    private void paintMemory(Graphics g){
      int xStart = 0;
      int xWidth = getWidth();
      
      long max = Runtime.getRuntime().maxMemory();
      long allocated = Runtime.getRuntime().totalMemory();
      long used = allocated - Runtime.getRuntime().freeMemory();
      
      this.setToolTipText("Using %s of %s".formatted(formatMemory(used), formatMemory(max)));

      g.setColor(DARK_GREEN);
      g.fillRect(xStart, 0, xWidth, getHeight());
      
      int allocatedWidth = (int)(xWidth * ((double)allocated / (double)max));
      g.setColor(DARK_ORANGE);
      g.fillRect(xStart, 0, allocatedWidth, getHeight());
      
      int usedWidth = (int)(xWidth * ((double)used / (double)max));
      g.setColor(DARK_RED);
      g.fillRect(xStart, 0, usedWidth, getHeight());
      
      int usedPercentage = (int)(100.0D * ( (double)used / (double)max));
      g.setColor(Color.WHITE);
      g.drawString(usedPercentage + "%", xStart + xWidth / 2 - 10, getHeight() - 5);

      if ((usedPercentage > 90) && (!this.hasWarned)) {
        this.mustWarn = true;
      }
    }
    
    private static String formatMemory(long value) {
    	double mb = 1024 * 1024;
    	double gb = mb * 1024;
    	double m = value/mb;
    	double g = value/gb;
    	return g < 1 ? "%s MiB".formatted(DF.format(m)) : "%s GiB".formatted(DF.format(g));
    }
}
