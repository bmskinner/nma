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
package gui.components;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.table.TableModel;

import ij.io.SaveDialog;
import utility.Constants;

@SuppressWarnings("serial")
public class ExportableTable extends JTable {
	
	private TablePopupMenu popup;
	
	public ExportableTable(){
		super();
		popup = new TablePopupMenu(this);
		this.setComponentPopupMenu(popup);
	}
	
	public ExportableTable(TableModel model){
		super(model);
		popup = new TablePopupMenu(this);
		this.setComponentPopupMenu(popup);
	}
	
	private class TablePopupMenu extends JPopupMenu {
		
		private ExportableTable table;

		private static final long serialVersionUID = 1L;
		JMenuItem exportMenuItem = new JMenuItem( new AbstractAction("Export"){
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent arg0) {
				export();	
			}
		});
		
		public TablePopupMenu(ExportableTable table) {
			
			super("Popup");
			this.table = table;
			this.add(exportMenuItem);

	    }
		
		private void export(){
			
			
			// get a place to save to
			SaveDialog saveDialog = new SaveDialog("Export table to...", "Table", Constants.TAB_FILE_EXTENSION);

			String fileName = saveDialog.getFileName();
			String folderName = saveDialog.getDirectory();
			
			if(fileName!=null && folderName!=null){
				File saveFile = new File(folderName+File.separator+fileName);
				
				// write out the model
				String string = makeExportString();
				PrintWriter out;
				try {

					out = new PrintWriter(saveFile);
					out.println(string);
					out.close();
				} catch (FileNotFoundException e) {
					
				}
				
			} 

		}
		
		private String makeExportString(){
			StringBuilder builder = new StringBuilder();
			TableModel model = table.getModel();
			for(int col=0; col<model.getColumnCount(); col++){
				builder.append(model.getColumnName(col)+"\t");;
			}
			builder.append("\r\n");
			for(int row=0; row<model.getRowCount(); row++){

				for(int col=0; col<model.getColumnCount(); col++){
					Object value = model.getValueAt(row, col);
					builder.append(value+"\t");
				}
				builder.append("\r\n");
			}
			return builder.toString();
		}
		
	}

}
