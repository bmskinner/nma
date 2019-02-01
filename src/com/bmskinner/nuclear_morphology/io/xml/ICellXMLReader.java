package com.bmskinner.nuclear_morphology.io.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.components.DefaultCell;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.SegmentedFloatProfile;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.nuclear.DefaultNuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.NucleusFactory;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;

import ij.gui.PolygonRoi;
import ij.gui.Roi;

/**
 * Reader for cells encoded in XML
 * @author bms41
 * @since 1.15.1
 *
 */
public class ICellXMLReader extends XMLReader<ICell> {
	
	private final NucleusFactory fact;
	private final double windowProportion;

	/**
	 * Create a reader
	 * @param e the cell element to read
	 * @param factory the factory to create nuclei
	 * @param windowProportion the window proportion
	 */
	public ICellXMLReader(@NonNull final Element e, @NonNull final NucleusFactory factory, final double windowProportion) {
		super(e);
		this.fact = factory;
		this.windowProportion = windowProportion;
	}
	
	@Override
	public ICell read() throws XMLReadingException {
		UUID id = UUID.fromString(rootElement.getChildText(XMLCreator.ID_KEY));
		ICell cell = new DefaultCell(id);
		
		for(Element n : rootElement.getChildren((XMLCreator.NUCLEUS_KEY))) {
			XMLReader<Nucleus> nuclReader = new NucleusXMLReader(n, fact, windowProportion);
			cell.addNucleus(nuclReader.read());
		}
		return cell;
	}
	
	

}
