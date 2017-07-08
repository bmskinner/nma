/*******************************************************************************
 *      Copyright (C) 2016 Ben Skinner
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

package com.bmskinner.nuclear_morphology.gui.main;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Display the memory in use. Based on the monitor in 
 * SeqMonk: https://www.bioinformatics.babraham.ac.uk/projects/seqmonk/
 * @author bms41
 * @since 1.13.7
 *
 */
public class MemoryIndicator extends JPanel
    implements Runnable, Loggable {
    
    private static final long serialVersionUID = 1L;
    private static final Color DARK_GREEN = new Color(0, 180, 0);
    private static final Color DARK_ORANGE = new Color(255, 130, 0);
    private static final Color DARK_RED = new Color(180, 0, 0);
    
    private static final String LOW_MEMORY_TTL = "Low memory";
    private static final String LOW_MEMORY_MSG = "Memory is running low!";
    
    private boolean hasWarned = false;
    private boolean mustWarn = false;
    
    public MemoryIndicator() {
      Thread t = new Thread(this);
      t.start();
    }
    
    @Override
    public void run() {
      do  {
        try {
          Thread.sleep(1000L);
        } catch (InterruptedException e) {
            
        }
        
        if (mustWarn && !hasWarned) {
          showMemoryWarning();
        }
        repaint();
      } while(true);
    }
    
    @Override
    public Dimension getPreferredSize(){
      return new Dimension(100, 20);
    }
    
    private synchronized void showMemoryWarning(){
      if (this.hasWarned) {
        return;
      }
      this.hasWarned = true;
      
      JOptionPane.showMessageDialog(null, LOW_MEMORY_MSG, LOW_MEMORY_TTL, JOptionPane.WARNING_MESSAGE);
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
      
//      log("Memory: "+usedPercentage);
      if ((usedPercentage > 90) && (!this.hasWarned)) {
        this.mustWarn = true;
      }
    }
    
    
            

    
}
