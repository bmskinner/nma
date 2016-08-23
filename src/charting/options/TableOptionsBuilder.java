package charting.options;

import gui.components.ColourSelecter.ColourSwatch;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import components.Cell;
import components.generic.MeasurementScale;
import stats.PlottableStatistic;
import charting.options.TableOptions.TableType;
import analysis.AnalysisDataset;

/**
 * Builder for a TableOptions object. This simplifies the creation
 * of the options when not all parameters need to be set.
 * @author bms41
 *
 */
public class TableOptionsBuilder {

	private ColourSwatch swatch        = ColourSwatch.REGULAR_SWATCH;
	private List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
	private TableType type             = null;
	private List<PlottableStatistic> stats    = new ArrayList<PlottableStatistic>();;
	private UUID segID                 = null; // the id of the segment (not consistent between datasets)
	private int segPosition            = 0;    // the position of the segment in the profile (consistent between datasets)
	private MeasurementScale scale     = MeasurementScale.PIXELS;

	private Cell cell                  = null;

	public TableOptionsBuilder(){}

	public TableOptionsBuilder setDatasets(List<AnalysisDataset> list){
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

	public TableOptionsBuilder setCell(Cell cell){
		this.cell = cell;
		return this;
	}
	
	public TableOptionsBuilder setSwatch(ColourSwatch swatch){
		this.swatch = swatch;
		return this;
	}

	public TableOptions build(){
		TableOptions options =  new TableOptions(list);
		options.setType(type);
		options.setStats(stats);
		options.setSegID(segID);
		options.setSegPosition(segPosition);
		options.setScale(scale);
		options.setCell(cell);
		options.setSwatch(swatch);
		return options;

	}
}
