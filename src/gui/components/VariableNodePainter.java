package gui.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ij.IJ;
import jebl.evolution.graphs.Node;
import jebl.evolution.trees.RootedTree;
import jebl.gui.trees.treeviewer.painters.BasicLabelPainter;

public class VariableNodePainter extends BasicLabelPainter {

//	List<Node> highlights = null; // the nodes to highlight
	Map<Node, Color> clusterMemberships = null; // nodes to highlight
	
//	public VariableNodePainter(String title, RootedTree tree, PainterIntent intent, List<Node> highlights) {
//		super(title, tree, intent);
////		this.highlights = highlights;
//		
//	}
	
	public VariableNodePainter(String title, RootedTree tree, PainterIntent intent, Map<Node, Color> highlights) {
		super(title, tree, intent);
		this.clusterMemberships = highlights;
		
	}
	
	@Override
	public void paint(Graphics2D g2, Node item, Justification justification, Rectangle2D bounds) {
		final Font oldFont = g2.getFont();
		final Font newFont = new Font(oldFont.getFontName(), Font.BOLD, oldFont.getSize());
	

        g2.setPaint(Color.LIGHT_GRAY);
        g2.setFont(oldFont);

        
        if(clusterMemberships.containsKey(item)){
        	g2.setFont(newFont);
        	g2.setPaint(clusterMemberships.get(item));
        } else {
        	g2.setFont(newFont);
        	g2.setPaint(Color.LIGHT_GRAY);
        }


        final String label = getLabel(item);
        if (label != null) {

        	Rectangle2D rect = g2.getFontMetrics().getStringBounds(label, g2);

        	float xOffset = 0;

        	float yOffset = (float) g2.getFontMetrics().getAscent();


        	float y = yOffset + (float) bounds.getY();
        	switch (justification) {
        	case CENTER:
        		break;
        	case FLUSH:
        	case LEFT:
        		xOffset = (float) bounds.getX();
        		break;
        	case RIGHT:
        		xOffset = (float) (bounds.getX() + bounds.getWidth() - rect.getWidth());
        		break;
        	default:
        		throw new IllegalArgumentException("Unrecognized alignment enum option");
        	}

        	g2.drawString(label, xOffset, y);
        }

        g2.setFont(oldFont);
		
	}

}
