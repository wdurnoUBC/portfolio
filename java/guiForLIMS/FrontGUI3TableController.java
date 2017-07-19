package guiForLIMS;

import java.awt.event.ActionEvent; 
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import org.apache.commons.lang3.*;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListSelectionModel;
import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowSorter.SortKey;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import sqlInterface.SQL2;
import sqlInterface.SQLUtil;

/*
 * This controller's job will be to display search results.
 * Results to display will be submitted to this controller as Vector<Vector<String>> type.
 * Each Vector<String> represents a row in the data base that is to be visualized.
 * Depending on the visualization mode (intersection, union, all), this controller will parse the view.
 * I would like the parser to display which columns are contained in which templates.
 */
public class FrontGUI3TableController 
{
	private LIMSGUI1 limsgui ;
	public JTable table ;
	public DefaultTableModel model ;
	public DefaultListSelectionModel listSelectionModel;
	// IDs of displayed items - in order corresponding to table model, NOT necessarily table display
	public Vector<String> id ;
	// These are used externally for retrieval purposes
	public Vector<Vector<String>> lastResult ;
	public Vector<Boolean> lastVisible ;
	public Vector<String> lastColumns ;
	public Set<String> currentColumns ;
	private boolean ignoreTableEvents ;
	private DefaultCellEditor templateColEditor;
	private final JPopupMenu popup;
	
	public FrontGUI3TableController ( LIMSGUI1 parent , JTable parentTable )
	{
		limsgui = parent ;
		table = parentTable ;
		currentColumns = new HashSet<String>() ;
		model = new DefaultTableModel() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isCellEditable(int row, int column) {
				// Return false if "enable editing" checkbox is not checked
				if (!limsgui.fg4c.frame.chckbxEnableEditing.isSelected()) {
					return false;
				}
				
				// Make 'id' column non-editable
				String colName = model.getColumnName(column);
				
				if (colName.equals("template") && model.getValueAt(row, column).equals("index") && !limsgui.fg4c.frame.btnAdmin.isEnabled()) {
					// Only admins can change an item's template from "index"
					return false;
				} else if (colName.equals("id")) {
					// User cannot change an item's id
					return false;
				} else {
					return true;
				}
			}
		};
		listSelectionModel = new DefaultListSelectionModel();
		table.setModel( model ) ;
		table.setSelectionModel( listSelectionModel );
		table.setAutoCreateRowSorter(true);
		id = null ;
		lastResult = null ;
		lastVisible = null ;
		lastColumns = null ;
		ignoreTableEvents = false ;	
		popup = createPopupMenu();
		
		//MouseListener popupListener = new PopupListener();
		//table.addMouseListener(popupListener);
		
		table.setComponentPopupMenu( popup ) ;
		
		final InputVerifier iv = new TemplateVerifier();
		
		templateColEditor = new DefaultCellEditor(new JTextField()) {
			private static final long serialVersionUID = 1L;

			{
		        getComponent().setInputVerifier(iv);
		    }

		    @Override
		    public boolean stopCellEditing() {
		        if (!iv.shouldYieldFocus(getComponent())) return false;
		        return super.stopCellEditing();
		    }

		    @Override
		    public JTextField getComponent() {
		        return (JTextField) super.getComponent();
		    }
		};
		
		// Update LIMS database when table is edited --- Should it be able to handle multiple cells changing at once?
		model.addTableModelListener(new TableModelListener() {
			@Override
			public void tableChanged(TableModelEvent e) {
				if (ignoreTableEvents) {
					return;
				}
				int column = e.getColumn();
				int row = e.getFirstRow();

				if (column >=0 && row >= 0) {
					String newVal = model.getValueAt(row, column).toString();
					if (newVal.equals("")) {
						newVal = null;
					}
					SQL2.setWhere ( limsgui.currentTable ,  model.getColumnName(column), newVal, "id", id.get(row).toString()) ;
				}
			}});

		table.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					if (!limsgui.fg4c.frame.chckbxEnableEditing.isSelected()) {
						// Follow all linked ids
						limsgui.fg4c.linkerController.displayLinkedIDs();
					}
				}
			}
		});
	}
	
	public void display( Vector<Vector<String>> result ) // takes select rows and populates the table
	{	
		// Save current widths and positions of all columns
		Map<String, Integer> columnWidths = new HashMap<String, Integer>();
		ArrayList<String> prevCols = new ArrayList<String>(model.getColumnCount());
		List<? extends SortKey> sortKeys = table.getRowSorter().getSortKeys();
		
		for (int j=0; j<model.getColumnCount(); j++) {
			columnWidths.put(table.getColumnName(j), table.getColumnModel().getColumn(j).getWidth());
			prevCols.add(j, table.getColumnName(j));
		}
		
		ignoreTableEvents = true;
		model.setColumnCount( 0 ) ;
		model.setRowCount( 0 ) ;
		
		if (limsgui.currentTable == null || result == null) {
			return;
		}
		//first, store id numbers for unique item retrieval
		id = SQLUtil.getCol(0, result) ;
		lastResult = result ;
		
		Vector<String> columns = SQL2.getColumns( limsgui.currentTable ) ;
		lastColumns = columns ;
		Vector<String> visCols = new Vector<String>();
		
		//parse
		if ( limsgui.view == "all" )
		{
			visCols = columns;
		}
		
		if ( limsgui.view == "intersection" )
		{
			Set<String> templates = SQLUtil.toSet( SQLUtil.getCol( 1 , result ) ) ; // assumes col 1 (indexed from 0) is the template column
			Set<Set<String>> templateCols = new HashSet<Set<String>>() ;
			for( String t : templates )
			{
				templateCols.add( SQL2.getTemplateColumns( limsgui.currentTable , t ) ) ; // gather the column sets per template
			}
			for( int i = 0 ; i < columns.size() ; i++ )
			{
				boolean b = true ;
				for( Set<String> colSet : templateCols )
				{
					if( ! SQLUtil.setContainsSQLString( colSet , columns.get(i) ) )
					{
						b = false ;
					}
				}
				if( b ) {
					visCols.add(columns.get(i));
				}
			}
		}
		
		if ( limsgui.view == "union" )
		{
			Set<String> templates = SQLUtil.toSet( SQLUtil.getCol( 1 , result ) ) ; // assumes col 1 (indexed from 0) is the template column
			Set<Set<String>> templateCols = new HashSet<Set<String>>() ;
			for( String t : templates )
			{
				templateCols.add( SQL2.getTemplateColumns( limsgui.currentTable , t ) ) ; // gather the column sets per template
			}
			for( int i = 0 ; i < columns.size() ; i++ )
			{
				boolean b = false ;
				for( Set<String> colSet : templateCols )
				{
					if( SQLUtil.setContainsSQLString( colSet , columns.get(i) ) )
					{
						b = true ;
					}
				}
				if( b ) {
					visCols.add(columns.get(i));
				}
			}
		}
		
		// populate the table
		model.setRowCount( result.size() ) ;
		
		// Add columns that were previously visible (if still visible)
		for (String column : prevCols) {
			if(visCols.contains(column)) {
				model.addColumn(column);
			}
		}
		
		// Add columns that were not previously visible (and are now)
		Vector<String> newCols = new Vector<String>(visCols);
		newCols.removeAll(prevCols);
		for (String column : newCols) {
			model.addColumn(column);
		}
		
		for (int j = 0; j < columns.size(); j++) {
			if (visCols.contains(columns.get(j))) {
				
				for (int k = 0; k < result.size(); k++) {
					model.setValueAt(result.get(k).get(j), k, model.findColumn(columns.get(j)));
				}
			}
			
		}
		
		TableColumnModel colModel = table.getColumnModel() ;
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF) ;
		
		for( int i = 0 ; i < model.getColumnCount() ; i++ )
		{
			TableColumn column = colModel.getColumn(i);
			Integer width = columnWidths.get(column.getHeaderValue());
			
			// If column was in previous display (and had its width saved), restore to same width
			if (width == null) {
				column.setPreferredWidth(100);
			} else {
				column.setPreferredWidth(width);
			}
			// Set special cell editor for template column
			if (column.getIdentifier().equals("template")) {
				column.setCellEditor(templateColEditor);
			}
		}
		// Preserve sorting order
		if (table.getRowCount() > 0) {
			table.getRowSorter().setSortKeys(sortKeys);
		}
		ignoreTableEvents = false;
	}
	
	// Re-display the last results
	public void refreshDisplay() {
		display(lastResult);
	}
	
	// Refresh the data of the currently displayed entries from the database
	public void refreshResults() {
		Vector<String> columns = new Vector<String>();
		int chunk = 500;
		for (int j=0; j<chunk; j++) {
			columns.add("id");
		}
		Vector<Vector<String>> newResult = new Vector<Vector<String>>();
		
		// Retrieve the data in chunks of 100 ids at a time
		if (lastResult != null) {
			int tableSize = SQL2.tableSize(limsgui.currentTable);
			
			for (int i=0; i<id.size(); i+=chunk) {
				int numVals = Math.min(chunk, id.size() - i);
				if (numVals != chunk) {
					columns.setSize(numVals);
				}
				List<String> vals = new ArrayList<String>(id.subList(i, i + numVals));
				Vector<String> valsVec = new Vector<String>(vals);
				newResult.addAll(SQL2.selectOR(limsgui.currentTable, columns, valsVec, tableSize, false));
			}
		}
		display(newResult);
	}
	
	// InputVerifier to make sure that only valid templates are entered in the template column
	private class TemplateVerifier extends InputVerifier {

		public boolean shouldYieldFocus(JComponent input) {
			boolean inputOK = verify(input);

			if (inputOK) {
				return true;
			}
			//Pop up the message dialog.
			limsgui.eting1c.show();

			//Reinstall the input verifier.
			input.setInputVerifier(this);

			//Tell whoever called us that we don't want to yield focus.
			return false;
		}

		public boolean verify(JComponent input) {
			JTextField cell = (JTextField)input;
			String text = (String)cell.getText();
			Set<String> templates = SQL2.getTemplates(limsgui.currentTable);
			// Only allow admins to enter "index"
			if (!limsgui.fg4c.frame.btnAdmin.isEnabled()) {
				templates.remove("index");
			}
			return templates.contains(text);
		}

	}

	// Export current table to a tab separated text file
	public void exportToFile(File file) {
		PrintWriter pw = null;

		try {
			FileWriter fw = new FileWriter(file, true);
			pw = new PrintWriter(fw);
			
			// Print headers
			ArrayList<String> headers = new ArrayList<String>();
			for (int j = 0; j<model.getColumnCount(); j++) {
				int k = table.convertColumnIndexToModel(j);
				headers.add(model.getColumnName(k));
			}
			pw.println(StringUtils.join(headers, "\t"));
			
			for (int i = 0; i<model.getRowCount(); i++) {
				ArrayList<String> row = new ArrayList<String>();
				
				for (int m = 0; m<model.getColumnCount(); m++) {
					int k = table.convertColumnIndexToModel(m);
					row.add((String)model.getValueAt(i, k));
				}
				pw.println(StringUtils.join(row, "\t"));
			}

			JOptionPane.showMessageDialog(limsgui.fg4c.frame, "Done exporting table", "Successful export", JOptionPane.INFORMATION_MESSAGE);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (pw != null) {
				pw.close();
			}
		}
	}
	
	// Create the popup menu for table cells
	public JPopupMenu createPopupMenu() {
		
		final JPopupMenu popup = new JPopupMenu();
		JMenuItem item = new JMenuItem("Edit");
		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				int rl = table.getSelectedRows().length ;
				if( rl == 0 )
				{
					JOptionPane.showMessageDialog(limsgui.fg4c.frame, "Please select all rows to edit", "Warning", JOptionPane.INFORMATION_MESSAGE);
				}
				else if ( rl > 0 )
				{
					limsgui.emg1c.show();
				}
			}});
		popup.add(item);
		
		item = new JMenuItem("Delete");
		popup.add(item);
		item.addActionListener( new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				int numItems = table.getSelectedRows().length;

				if (numItems > 0) {
					Object[] options = {"Yes", "Cancel"};
					String message;
					if (numItems == 1) {
						message = "Are you sure you want to delete this item?";
					} else if (limsgui.ag3c.frame.aeug2.chckbxAllowDeletion.isSelected() || limsgui.fg4c.frame.btnAdmin.isEnabled()) {
						message = "Are you sure you want to delete these " + numItems + " items?";
					} else {
						JOptionPane.showMessageDialog(limsgui.fg4c.frame,
								"Deletion of multiple items is disabled for users,\n"
								+ "please delete one at a time or log in as an admin", "Warning", JOptionPane.WARNING_MESSAGE);
						return;
					}

					int n = JOptionPane.showOptionDialog(limsgui.fg4c.frame,
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
					JOptionPane.showMessageDialog(limsgui.fg4c.frame, "Please select an item to delete", "Warning", JOptionPane.WARNING_MESSAGE);
				}
			}});
		
		item = new JMenuItem("Open");
		popup.add(item);
		item.addActionListener( new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (limsgui.pathPrefix == null) {
					JOptionPane.showMessageDialog(limsgui.fg4c.frame, "Location of data has not been set", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				int[] indices = table.getSelectedRows() ;
				for ( int i = 0 ; i < indices.length ; i++ )
				{
					String dir = getFrom( "download" , indices[i] ) ;
					download(limsgui.pathPrefix + dir);
				}
			}});
		
		item = new JMenuItem("Open Parent Folder");
		popup.add(item);
		item.addActionListener( new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (limsgui.pathPrefix == null) {
					JOptionPane.showMessageDialog(limsgui.fg4c.frame, "Location of data has not been set", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				int[] indices = table.getSelectedRows() ;
				for ( int i = 0 ; i < indices.length ; i++ )
				{
					String dir = getFrom( "download" , indices[i] ) ;
					int idx = dir.lastIndexOf(File.separator);
					if (idx >=0 ) {
						dir = dir.substring(0, idx);
						download(limsgui.pathPrefix + dir);
					} else {
						JOptionPane.showMessageDialog(limsgui.fg4c.frame, "Cannot find file", "Error", JOptionPane.ERROR_MESSAGE);
					}
				}
			}});
		
		item = new JMenuItem("Update here");
		popup.add(item);
		item.addActionListener( new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				int[] indices = table.getSelectedRows() ;
				
				if (indices.length == 0) {
					System.out.println("No item selected");
					return;
				}
				 
				int modelIdx = table.convertRowIndexToModel(indices[0]);
				String ids = id.get(modelIdx);
				
				System.out.println("table index = " + indices[0]);
				System.out.println("model index = " + modelIdx);
				
				int numColumns = SQL2.getColumns(limsgui.currentTable).size();
				Vector<Vector<String>> result = SQL2.searchColumnExact(limsgui.currentTable, ids, "id", numColumns, false);
				
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

				limsgui.ag3c.aplg3c.updateHere(f, ids, numColumns);
			}});
		
		return popup;
	}
	
	/*
	class PopupListener extends MouseAdapter {
	    public void mousePressed(MouseEvent e) {
	        //maybeShowPopup(e);
	    }

	    public void mouseReleased(MouseEvent e) {
	        maybeShowPopup(e);
	    }

	    private void maybeShowPopup(MouseEvent e) {
	        if (e.isPopupTrigger()) {
	        	
	        	System.out.println( "Click!" ) ;
	        	
	        	
	        	int row = table.rowAtPoint(e.getPoint());
	        	int[] selected = table.getSelectedRows();
	        	
	        	// If 0 or 1 rows selected OR multiple are selected but not the one right-clicked on, change row selection
	        	if (selected.length <= 1 || !Arrays.asList(ArrayUtils.toObject(selected)).contains(row)) {
		        	table.setRowSelectionInterval(row, row);
	        	}
	        	
	            popup.show(e.getComponent(),
	                       e.getX(), e.getY());
	            
	        }
	    }
	}*/
	
	// Remove all selected items
	private void removeItems() {
		
		int[] indices = table.getSelectedRows() ;
		int[] modelIndices = new int[indices.length];
		for (int i=0; i<indices.length; i++) {
			modelIndices[i] = table.convertRowIndexToModel(indices[i]);
		}
		
		Vector<Vector<String>> result = lastResult ;
		Vector<Vector<String>> toDelete = new Vector<Vector<String>>();
		
		for (int i=0; i<modelIndices.length; i++) {
			
			String deleteId = id.get( modelIndices[i] ) ;
			System.out.println("id = " + deleteId);
			
			if (deleteId.equals(limsgui.rootID)) {
				JOptionPane.showMessageDialog(limsgui.fg4c.frame, "Cannot delete root item", "Warning", JOptionPane.WARNING_MESSAGE);
				return;
			}
			
			SQL2.deleteWhere( limsgui.currentTable , "id", deleteId) ;
			// Delete links to and from deleted item
			SQL2.deleteWhere("META_LIMS_TABLE_4", "linkfrom", deleteId);

			Vector<String> cols = new Vector<String>();
			Vector<String> vals = new Vector<String>();
			cols.add("linkto");
			cols.add("tocolumn");
			vals.add(deleteId);
			vals.add("id");
			SQL2.deleteWhere("META_LIMS_TABLE_4", cols, vals);
			// Remove deleting item from table result
			toDelete.add(result.get(modelIndices[i]));
		}
		// now update the display
		result.removeAll(toDelete);
		display( result ) ;
	}
	
	private void download(String dir) {
		if ( dir != null ) {
			
			File f = new File(dir);
			if (!f.exists()) {
				JOptionPane.showMessageDialog(limsgui.fg4c.frame, "Cannot find file", "Error", JOptionPane.ERROR_MESSAGE);
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
			JOptionPane.showMessageDialog(limsgui.fg4c.frame, "Cannot find file", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private String getFrom ( String colName , int row )
	{	
		String id = limsgui.fg4c.tableController.id.get(row);
		Vector<Vector<String>> result = SQL2.searchColumnExact(limsgui.currentTable, id, "id", false);
		
		Vector<String> columns = SQL2.getColumns(limsgui.currentTable);
		int colIdx = columns.indexOf(colName);
		
		return result.get(0).get(colIdx);
	}
	
}
