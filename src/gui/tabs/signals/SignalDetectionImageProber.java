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
package gui.tabs.signals;

import java.awt.Dimension;
import java.io.File;

import analysis.AnalysisOptions;
import analysis.IAnalysisDataset;
import analysis.signals.NuclearSignalOptions;
import analysis.signals.SignalProberWorker;
import gui.ImageType;
import gui.dialogs.ImageProber;


@SuppressWarnings("serial")
public class SignalDetectionImageProber extends ImageProber {
	
	private IAnalysisDataset dataset;
	private int channel;
	private NuclearSignalOptions testOptions;


	
	public enum SignalImageType implements ImageType {
		DETECTED_OBJECTS ("Detected objects",  0);
		
		private String name;
		private int position; // the order in which the processed images should be displayed
		
		SignalImageType(String name, int position){
			this.name = name;
			this.position = position;
		}
		public String toString(){
			return this.name;
		}
		
		public ImageType[] getValues(){
			return SignalImageType.values();
		}
		@Override
		public int getPosition() {
			return position;
		}
	}
	
	public SignalDetectionImageProber(AnalysisOptions options, File folder, IAnalysisDataset dataset, int channel, NuclearSignalOptions testOptions) {
		super(options, SignalImageType.DETECTED_OBJECTS, folder);

		if(dataset==null){
			throw new IllegalArgumentException("Dataset cannot be null");
		}
		
		this.dataset = dataset;
		this.channel = channel;
		this.testOptions = testOptions;
		createFileList(folder);
		this.setVisible(true);
	}
	
	@Override
	protected void importAndDisplayImage(File imageFile){

		try{
			finer("Opening image "+imageFile.getAbsolutePath());
			setStatusLoading();
			this.setLoadingLabelText("Probing image "+index+": "+imageFile.getAbsolutePath()+"...");
			
			table.setModel(createEmptyTableModel(rows, cols));
			
			for(int col=0; col<cols; col++){
	        	table.getColumnModel().getColumn(col).setCellRenderer(new IconCellRenderer());
	        }
			
			finer("Created worker");
			SignalProberWorker worker = new SignalProberWorker(imageFile, 
					options, 
					SignalImageType.DETECTED_OBJECTS, 
					table.getModel(),
					dataset,
					channel,
					testOptions);
			
			worker.setSmallIconSize(new Dimension(500, table.getRowHeight()-30));
			
			worker.addPropertyChangeListener(this);
			progressBar.setVisible(true);
			
			finer("Running worker");
			worker.execute();

		} catch(Exception e){
				error("Error in signal probing", e);
			}
		}
	
}
