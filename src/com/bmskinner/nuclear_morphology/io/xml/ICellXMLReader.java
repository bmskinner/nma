package com.bmskinner.nuclear_morphology.io.xml;

import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.components.cells.DefaultCell;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.NucleusFactory;

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
