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

import jebl.evolution.graphs.Node;
import jebl.evolution.trees.RootedTree;
import jebl.evolution.trees.Tree;
import jebl.gui.trees.treeviewer.TreeViewer;
//import jebl.gui.trees.treeviewer_dev.DefaultTreeViewer;

@SuppressWarnings("serial")
public class DraggableTreeViewer extends TreeViewer {
	
	List<Line2D.Double> lines = new ArrayList<Line2D.Double>();
//	SelectableTreePane sampleTreePane;
	
	public DraggableTreeViewer(){
		super();
//		sampleTreePane = new SelectableTreePane();
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
