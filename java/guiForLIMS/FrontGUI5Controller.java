package guiForLIMS;

import java.awt.event.ActionEvent; 
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Vector;
//import au.com.bytecode.opencsv;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import sqlInterface.SQL2;

public class FrontGUI5Controller 
{
	public LIMSGUI1 limsgui ;
	public FrontGUI3TableController tableController ;
	public FrontGUI4LinkerController linkerController ;
	public FrontGUI5 frame ;
	
	public FrontGUI5Controller ( LIMSGUI1 parent )
	{
		limsgui = parent ;
		frame = new FrontGUI5() ;
		tableController = new FrontGUI3TableController(limsgui, frame.table ) ;
		linkerController = new FrontGUI4LinkerController( limsgui, frame.linkerPanel );
		
		// Button: View
		frame.btnView.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				limsgui.vg1c.show();
			}
	    });
		
		// Button: Back
		frame.btnBack.addActionListener( new ActionListener()  {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				// Pop the last query off the stack
				Vector<Vector<String>> result = SQL2.executeLastQuery(limsgui.currentTable);
				
				if (result != null) {
					tableController.display(result) ;
				}
			}
		});
		
		// Button: Advanced search
		frame.btnAdvancedSearch.addActionListener( new ActionListener()  {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				limsgui.asg2c.show();
			}
		});
		
		/*// REFACTOR: This feature has been moved to a right-click menu
		// Button: Edit multiple items
		frame.btnEditMultiple.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (tableController.table.getSelectedRows().length < 1) {
					JOptionPane.showMessageDialog(frame, "Please select items to edit", "Warning", JOptionPane.WARNING_MESSAGE);
				} else {
					limsgui.emg1c.show();
				}
			}
	    });
		*/
		
		/* // REFACTOR: This feature has been moved to a right-click menu
		// Button: Open containing folder
		frame.btnOpenFolder.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				if (limsgui.pathPrefix == null) {
					JOptionPane.showMessageDialog(frame, "Location of data has not been set", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				int[] indices = frame.table.getSelectedRows() ;
				for ( int i = 0 ; i < indices.length ; i++ )
				{
					String dir = getFrom( "download" , indices[i] ) ;
					int idx = dir.lastIndexOf(File.separator);
					if (idx >=0 ) {
						dir = dir.substring(0, idx);
						download(limsgui.pathPrefix + dir);
					} else {
						JOptionPane.showMessageDialog(frame, "Cannot find file", "Error", JOptionPane.ERROR_MESSAGE);
					}
				}
			}
	    });
		*/
		
		// Button: Export table to file
		frame.btnExport.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				// Open file chooser to specify file to save to
				JFileChooser fc = new JFileChooser();
				// Edit FileFilter here to accept other file formats
				FileNameExtensionFilter fileExtFilter = new FileNameExtensionFilter("TSV file", "txt", "tsv");
				
				fc.setDialogTitle("Choose File to Export Table to");
				fc.addChoosableFileFilter(fileExtFilter);
				int returnVal = fc.showDialog(frame, "Choose");

				File file = null;
				
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					file = fc.getSelectedFile();
					// Use tableController to loop over values and produce file
					tableController.exportToFile(file);
				}
			}
		});
		
		// Button: New Admin
		frame.btnAdmin.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				limsgui.ag3c.show();
			}
	    });
		
		// Button: Add item
		frame.btnAddItem.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				limsgui.aig1c.show() ;
			}
	    });
		
		// Button: Refresh
		frame.btnRefresh.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				tableController.refreshResults();
			}
	    });
		
		// Listener for search button and search text box
		ActionListener searchListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String column = (String)frame.comboBox.getSelectedItem();
				String term = frame.txtSearchAll.getText().trim();
				
				if (column.equals("(Any column)")) {
					column = null;
				}
				if (frame.chckbxExact.isSelected()) {
					searchExact( term, column) ;
				} else {
					search( term, column ) ;
				}
			}};
		
		// Button: Fast Search
		frame.btnFastSearch.addActionListener( searchListener);
		frame.txtSearchAll.addActionListener( searchListener );
		
		/* // REFACTOR: This feature has been moved to a right-click menu
		// Button: download
		frame.btnDownload.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {

				if (limsgui.pathPrefix == null) {
					JOptionPane.showMessageDialog(frame, "Location of data has not been set", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				int[] indices = frame.table.getSelectedRows() ;
				for ( int i = 0 ; i < indices.length ; i++ )
				{
					String dir = getFrom( "download" , indices[i] ) ;
					download(limsgui.pathPrefix + dir);
				}
				
			}} );
		*/
		
		/*// REFACTOR: 'This button has been moved to a right-click menu
		//Button: Remove Item
		frame.btnRemoveItem.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int numItems = frame.table.getSelectedRows().length;

				if (numItems > 0) {
					Object[] options = {"Yes", "Cancel"};
					String message;
					if (numItems == 1) {
						message = "Are you sure you want to delete this item?";
					} else if (limsgui.ag3c.frame.aeug2.chckbxAllowDeletion.isSelected() || frame.btnAdmin.isEnabled()) {
						message = "Are you sure you want to delete these " + numItems + " items?";
					} else {
						JOptionPane.showMessageDialog(frame,
								"Deletion of multiple items is disabled for users,\n"
								+ "please delete one at a time or log in as an admin", "Warning", JOptionPane.WARNING_MESSAGE);
						return;
					}

					int n = JOptionPane.showOptionDialog(frame,
							message,
							"Are you sure?",
							JOptionPane.YES_NO_OPTION,
							JOptionPane.QUESTION_MESSAGE,
							null,
							options,
							options[1]);

					if (n == JOptionPane.YES_OPTION) {
						removeItems();
					}

				} else {
					JOptionPane.showMessageDialog(frame, "Please select an item to delete", "Warning", JOptionPane.WARNING_MESSAGE);
				}
			}}) ;
		*/
		
		//Button: Search index
		frame.btnSearchindex.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				searchExact( "index", "template" ) ;
			}});
		
		//Button: Log Out
		frame.btnLogOut.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				limsgui.lg1c.clearFields();
				limsgui.lg1c.hideErrorMsg();
				limsgui.lg1c.show();
				hide();
			}});
		
		//Button: Show/Hide Linker
		frame.btnShowhideLinker.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				frame.linkerPanel.setVisible(!frame.linkerPanel.isVisible());
			}});
		
		// Add items to columnChoiceBox		
		Vector<String> comboBoxItems = SQL2.getColumns(limsgui.currentTable);
		frame.comboBox.insertItemAt("(Any column)", 0);
		frame.comboBox.setSelectedIndex(0);
		for (String s : comboBoxItems) {
			frame.comboBoxModel.addElement(s);
		}
		
		//create custom close operation
		frame.addWindowListener(new WindowAdapter() {
			
		      public void windowClosing(WindowEvent e) {
		    	  
		  		if (!limsgui.lg1c.frame.isVisible()) {
					// Program is being closed... close database connection
					SQL2.closeConnection();
				}
		      }
		});
		
		/* // REFACTOR: This feature has been moved to a right-click menu
		//Button: Update Here
		frame.btnUpdateHere.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				int[] indices = tableController.table.getSelectedRows() ;
				
				if (indices.length == 0) {
					System.out.println("No item selected");
					return;
				}
				
				int modelIdx = tableController.table.convertRowIndexToModel(indices[0]);
				String id = tableController.id.get(modelIdx);
				
				System.out.println("table index = " + indices[0]);
				System.out.println("model index = " + modelIdx);
				
				int numColumns = SQL2.getColumns(limsgui.currentTable).size();
				Vector<Vector<String>> result = SQL2.searchColumnExact(limsgui.currentTable, id, "id", numColumns, false);
				
				if (result.size() == 0) {
					System.out.println("Can't find item");
					return;
				}
				
				int downloadIdx = SQL2.getColumnIndex(limsgui.currentTable, "download");
				int isDirectoryIdx = SQL2.getColumnIndex(limsgui.currentTable, "isDirectory");
				
				if (downloadIdx == -1 || isDirectoryIdx == -1) {
					System.out.println("Error error!");
					return;
				}
				
				if (!result.get(0).get(isDirectoryIdx).equals("true")) {
					System.out.println("Not a directory");
					return;
				}
				
				String download = result.get(0).get(downloadIdx);
				Vector<String> cols = new Vector<String>();
				Vector<String> vals = new Vector<String>();
				cols.add("setting");
				cols.add("setKey");
				vals.add("pathPrefix");
				vals.add(limsgui.currentTable);
				Vector<Vector<String>> metaResult = SQL2.select("META_LIMS_TABLE_3", cols, vals, false);
				String pathPrefix;
				if (metaResult.size() > 0 ) {
					pathPrefix = metaResult.get(0).get(3);
				} else {
					System.out.println("Must set data location");
					return;
				}
				
				download = pathPrefix + download;
				File f;
				
				try {
					f = new File(download);
				} catch (Exception e) {
					System.err.println("Couldn't create file object! " + e.getLocalizedMessage());
					return;
				}
				
				try {
					limsgui.ag3c.aplg3c.crawl(f);
				} catch (Exception e) {
					System.err.println("Crawl didn't work " + e.getLocalizedMessage());
					return;
				}

				limsgui.ag3c.aplg3c.updateHere(f, id, numColumns);
				
			}});
		*/
		
	}
	
	/* // REFACTOR: This feature has been moved to a right-click menu
	private void download(String dir) {
		if ( dir != null ) {
			
			File f = new File(dir);
			if (!f.exists()) {
				JOptionPane.showMessageDialog(frame, "Cannot find file", "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			ProcessBuilder pb = new ProcessBuilder("open" , dir ) ;
			pb.command( "open" , dir ) ;
			try { pb.start() ; } catch (IOException e) 
			{
				System.err.println( "Failed to open" ) ;
				e.printStackTrace() ;
			}
		} else {
			JOptionPane.showMessageDialog(frame, "Cannot find file", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	*/
	
	// Searches the given column for the exact match
	public void searchExact( String term, String column ) {
		
		if (limsgui.currentTable != null) {
			Vector<Vector<String>> out;
			
			if (column == null) {
				out = SQL2.searchAllColumnsExact(limsgui.currentTable, term, true);
			} else {
				out = SQL2.searchColumnExact(limsgui.currentTable, term, column, true);
			}
			
			tableController.display(out);
		} else {
			System.err.println( "ERROR : FrontGUI4Controller type 1" ) ;
		}
		
	}
	
	public void search( String term, String column )
	{
		if ( limsgui.currentTable != null ) {
			Vector<Vector<String>> out;
			if (column == null) {
				out = SQL2.searchAllColumns(limsgui.currentTable, term, true);
			} else {
				out = SQL2.searchColumn(limsgui.currentTable, term, column, true);
			}
			tableController.display( out) ;
		}
		else
		{
			System.err.println( "ERROR : FrontGUI4Controller type 1" ) ;
		}
	}
	
	/* // REFACTOR: This feature has been moved to a right-click menu
	// Return the value in the column 'colName' for the given row in the main table
	private String getFrom ( String colName , int row )
	{	
		String id = limsgui.fg4c.tableController.id.get(row);
		Vector<Vector<String>> result = SQL2.searchColumnExact(limsgui.currentTable, id, "id", false);
		
		Vector<String> columns = SQL2.getColumns(limsgui.currentTable);
		int colIdx = columns.indexOf(colName);
		
		return result.get(0).get(colIdx);
	}
	*/
	
	/*// REFACTOR: This functionality has been moved to a right-click menu
	// Remove all selected items
	private void removeItems() {
		FrontGUI3TableController c = limsgui.fg4c.tableController;
		
		int[] indices = c.table.getSelectedRows() ;
		Vector<Vector<String>> result = c.lastResult ;
		Vector<Vector<String>> toDelete = new Vector<Vector<String>>();
		
		for (int i=0; i<indices.length; i++) {
			System.out.println("table index = " + indices[0]);
			System.out.println("model index = " + c.table.convertRowIndexToModel(indices[0]));

			int modelIdx = c.table.convertRowIndexToModel(indices[0]);
			String id = c.id.get( modelIdx ) ;
			System.out.println("id = " + id);
			
			if (id.equals(limsgui.rootID)) {
				JOptionPane.showMessageDialog(frame, "Cannot delete root item", "Warning", JOptionPane.WARNING_MESSAGE);
				return;
			}
			
			SQL2.deleteWhere( limsgui.currentTable , "id", id) ;
			// Delete links to and from deleted item
			SQL2.deleteWhere("META_LIMS_TABLE_4", "linkfrom", id);

			Vector<String> cols = new Vector<String>();
			Vector<String> vals = new Vector<String>();
			cols.add("linkto");
			cols.add("tocolumn");
			vals.add(id);
			vals.add("id");
			SQL2.deleteWhere("META_LIMS_TABLE_4", cols, vals);
			// Remove deleting item from table result
			toDelete.add(result.get(modelIdx));
		}
		// now update the display
		result.removeAll(toDelete);
		c.display( result ) ;
	}
	*/
	
	public void show()
	{
		frame.setVisible(true) ;
	}
	
	public void hide()
	{
		frame.setVisible(false) ;
		if (!limsgui.lg1c.frame.isVisible()) {
			// Program is completely closed; close database connection
			SQL2.closeConnection();
		}
	}

	public void showAdmin(boolean b)
	{	
		frame.btnAdmin.setEnabled(b);
	}
	
}
