package gui.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import ij.IJ;
import jebl.evolution.graphs.Node;
import jebl.evolution.trees.RootedTree;
import jebl.gui.trees.treeviewer.painters.BasicLabelPainter;

public class VariableNodePainter extends BasicLabelPainter {

	List<Node> highlights; // the nodes to highlight
	
	public VariableNodePainter(String title, RootedTree tree, PainterIntent intent, List<Node> highlights) {
		super(title, tree, intent);
		this.highlights = highlights;
		
	}
	
	@Override
	public void paint(Graphics2D g2, Node item, Justification justification, Rectangle2D bounds) {
		final Font oldFont = g2.getFont();

//        if (this.background != null) {
//            g2.setPaint(background);
//            g2.fill(bounds);
//        }
//
//        if (borderPaint != null && borderStroke != null) {
//            g2.setPaint(borderPaint);
//            g2.setStroke(borderStroke);
//            g2.draw(bounds);
//        }
		

        g2.setPaint(Color.BLACK);
        g2.setFont(new Font("sansserif", Font.PLAIN, 8));

        if(highlights.contains(item)){
        	g2.setPaint(Color.YELLOW);
        	g2.fill(bounds);
        	g2.setPaint(Color.BLUE);
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
