package charting.options;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

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
	
	private List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
	private Logger programLogger       = null;
	private TableType type             = null;
	private PlottableStatistic stat    = null;
	private UUID segID                 = null; // the id of the segment (not consistent between datasets)
	private int segPosition            = 0;    // the position of the segment in the profile (consistent between datasets)
	private MeasurementScale scale     = MeasurementScale.PIXELS;
	
	private Cell cell                  = null;
		
		public TableOptionsBuilder(){}
		
		public TableOptionsBuilder setDatasets(List<AnalysisDataset> list){
			this.list = list;
			return this;
		}
		
		public TableOptionsBuilder setLogger(Logger logger){
			this.programLogger = logger;
			return this;
		}
		
		public TableOptionsBuilder setType(TableType type){
			this.type = type;
			return this;
		}
		
		public TableOptionsBuilder setStat(PlottableStatistic stat) {
			this.stat = stat;
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
		
		public TableOptions build(){
			TableOptions options =  new TableOptions(list, programLogger);
			options.setType(type);
			options.setStat(stat);
			options.setSegID(segID);
			options.setSegPosition(segPosition);
			options.setScale(scale);
			options.setCell(cell);
			return options;
			
		}
}
