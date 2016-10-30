package charting.options;

import gui.components.ColourSelecter.ColourSwatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import components.ICell;
import components.generic.MeasurementScale;
import stats.PlottableStatistic;
import charting.options.DefaultTableOptions.TableType;
import analysis.AnalysisDataset;
import analysis.IAnalysisDataset;

/**
 * Builder for a TableOptions object. This simplifies the creation
 * of the options when not all parameters need to be set.
 * @author bms41
 *
 */
public class TableOptionsBuilder {

	private ColourSwatch swatch        = ColourSwatch.REGULAR_SWATCH;
	private List<IAnalysisDataset> list = new ArrayList<IAnalysisDataset>();
	private TableType type             = null;
	private List<PlottableStatistic> stats    = new ArrayList<PlottableStatistic>();;
	private UUID segID                 = null; // the id of the segment (not consistent between datasets)
	private int segPosition            = 0;    // the position of the segment in the profile (consistent between datasets)
	private MeasurementScale scale     = MeasurementScale.PIXELS;
	private JTable target              = null;
	private Map<Integer, TableCellRenderer> renderer = new HashMap<Integer, TableCellRenderer>(1);

	private ICell cell                  = null;

	public TableOptionsBuilder(){}

	public TableOptionsBuilder setDatasets(List<IAnalysisDataset> list){
		this.list = list;
		return this;
	}


	public TableOptionsBuilder setType(TableType type){
		this.type = type;
		return this;
	}

	public TableOptionsBuilder addStatistic(PlottableStatistic s){
		this.stats.add(s);
		return this;
	}

	public TableOptionsBuilder setSegID(UUID segID) {
		this.segID = segID;
		return this;
	}

	public TableOptionsBuilder setSegPosition(int segPosition) {
		this.segPosition = segPosition;
		return this;
	}

	public TableOptionsBuilder setScale(MeasurementScale s){
		this.scale = s;
		return this;
	}

	public TableOptionsBuilder setCell(ICell cell){
		this.cell = cell;
		return this;
	}
	
	public TableOptionsBuilder setSwatch(ColourSwatch swatch){
		this.swatch = swatch;
		return this;
	}
	
	public TableOptionsBuilder setTarget(JTable target){
		this.target = target;
		return this;
	}
	
	public TableOptionsBuilder setRenderer(int column, TableCellRenderer r) {
		renderer.put(column, r);
		return this;
	}


	public DefaultTableOptions build(){
		DefaultTableOptions options =  new DefaultTableOptions(list);
		options.setType(type);
		options.setStats(stats);
		options.setSegID(segID);
		options.setSegPosition(segPosition);
		options.setScale(scale);
		options.setCell(cell);
		options.setSwatch(swatch);
		options.setTarget(target);
		
		for(int i : renderer.keySet()){
			options.setRenderer(i, renderer.get(i));
		}

		return options;

	}
}
