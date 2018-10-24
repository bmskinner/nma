package com.bmskinner.nuclear_morphology.io.xml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.DefaultAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.DefaultCell;
import com.bmskinner.nuclear_morphology.components.DefaultCellCollection;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.NucleusFactory;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Read serialised XML dataset files
 * @author bms41
 * @since 1.14.0
 *
 */
public class DatasetXMLReader extends XMLReader<IAnalysisDataset> {

	private NucleusFactory fact = null;
	
	/**
	 * Create with a file to be read
	 * @param f
	 */
	public DatasetXMLReader(@NonNull final File f) {
		super(f);
	}
	
	@Override
	public IAnalysisDataset read() {
		
		try {
		
			SAXBuilder saxBuilder = new SAXBuilder();
			Document document = saxBuilder.build(file);
			XMLOutputter xmlOutput = new XMLOutputter();
			xmlOutput.setFormat(Format.getPrettyFormat());
			xmlOutput.output(document, System.out); 
			
			NucleusType type = NucleusType.valueOf(document.getRootElement().getChild(XMLCreator.NUCLEUS_TYPE_KEY).getText());
			fact = new NucleusFactory(type);
			
			
			return readDataset(document.getRootElement().getChild(XMLCreator.ANALYSIS_DATASET_KEY), type );
			
		} catch (JDOMException | IOException | ComponentCreationException e) {
			stack(e);
		}
		
		return null;
	}
	
	private IAnalysisDataset readDataset(Element e, NucleusType type) throws ComponentCreationException {
		String name = e.getChildText(XMLCreator.NAME_KEY);
		ICellCollection c = readCollection(e.getChild(XMLCreator.CELL_COLLECTION_KEY), type, name);
		IAnalysisDataset d = new DefaultAnalysisDataset(c, file);
		return d;
	}
	
	private ICellCollection readCollection(Element e, NucleusType type, String name) throws ComponentCreationException {
		
		UUID id = UUID.fromString(e.getChildText(XMLCreator.ID_KEY));
		String outFolder = e.getChildText(XMLCreator.OUTPUT_FOLDER_KEY);

		ICellCollection collection = new DefaultCellCollection(null,outFolder, name, type, id);
		
		for(Element cell : e.getChildren(XMLCreator.CELL_KEY))
			collection.add(readCell(cell));

		
		return collection;
	}
	
	private ICell readCell(Element e) throws ComponentCreationException {
		UUID id = UUID.fromString(e.getChildText(XMLCreator.ID_KEY));
		ICell cell = new DefaultCell(id);
		
		for(Element n : e.getChildren((XMLCreator.NUCLEUS_KEY)))
			cell.addNucleus(readNucleus(n));
		return cell;
	}
	
	private Nucleus readNucleus(Element e) throws ComponentCreationException {
		
		Element border = e.getChild(XMLCreator.BORDER_POINTS_KEY);
		List<IPoint> points = new ArrayList<>();
		for(Element point : border.getChildren())
			points.add(readPoint(point));
		
		IPoint com = readPoint(e.getChild(XMLCreator.COM_KEY));
		
		File imageFile = new File(e.getChildText(XMLCreator.SOURCE_FILE_KEY));
		int channel = Integer.valueOf((e.getChildText(XMLCreator.SOURCE_CHANNEL_KEY)));
		
		UUID id = UUID.fromString(e.getChildText(XMLCreator.ID_KEY));
		
		Nucleus n = fact.buildInstance(points, imageFile, channel, com, id);
		
		Element stats = e.getChild(XMLCreator.STATS_KEY);
		for(Element stat : stats.getChildren()) {
			PlottableStatistic s = readStat(stat);
			double d = Double.valueOf(stat.getChildText(XMLCreator.VALUE_KEY));
			n.setStatistic(s, d);
		}
		
		return n;
	}

	
}
