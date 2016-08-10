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
package gui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.virion.jam.controlpanels.ControlPalette;

import jebl.evolution.graphs.Node;
import jebl.evolution.trees.RootedTree;
import jebl.evolution.trees.Tree;
import jebl.gui.trees.treeviewer.TreeViewer;
//import jebl.gui.trees.treeviewer_dev.DefaultTreeViewer;

@SuppressWarnings("serial")
public class DraggableTreeViewer extends TreeViewer {
	
	List<Line2D.Double> lines = new ArrayList<Line2D.Double>();
	
	public DraggableTreeViewer(){
		super();

	}
	
	public DraggableTreeViewer(ControlPalette controlPalette, int CONTROL_PALETTE_ALIGNMENT) {
		super(controlPalette, CONTROL_PALETTE_ALIGNMENT);
	}
	

	public void addLine(Line2D.Double line){
		clearLines();
		this.lines.add(line);
	}
	
//	@Override
//	public void setTree(Tree tree){
//		super.setTree(tree);
//		sampleTreePane.setTree((RootedTree) tree, null);
//	}
	
//	public Set<Node> getNodesAtPoint(Graphics2D g2, Rectangle r){
//		return sampleTreePane.getNodesAtPoint(g2, r);
//	}
	
		
	public void clearLines(){
		this.lines = new ArrayList<Line2D.Double>();
	}
	
	@Override
	public void paint(Graphics g){
				
		super.paint(g);
		
		Graphics2D g2 = (Graphics2D) g;
		g2.setPaint(Color.BLACK);
		g2.setStroke(new BasicStroke(2f));
		for(Line2D.Double line : lines){
			g2.draw(line);
		}
	}

}
