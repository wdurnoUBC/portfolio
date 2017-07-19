package guiForLIMS;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Set;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import sqlInterface.SQL2;

public class AddItemGUI1Controller 
{

	LIMSGUI1 limsgui ;
	AddItemGUI1 frame ;
	DefaultTableModel model ;
	Vector<JTextField> textVec ;
	AddToFolderController atfg2c;

	public AddItemGUI1Controller ( LIMSGUI1 parent )
	{
		limsgui = parent ;
		frame = new AddItemGUI1 () ;
		model = new DefaultTableModel () ;
		frame.table.setModel( model ) ;
		textVec = new Vector<JTextField>() ;
		atfg2c = new AddToFolderController(limsgui, frame);
		
		populateComboBox();
		if (frame.comboBox.getSelectedItem() == null) {
			frame.lblNotSelected.setVisible(false);
		}

		// Combo box listener
		frame.comboBox.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.lblNotSelected.setVisible(false);
				populateTable() ;
			}
		}) ;

		frame.btnCancel.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				frame.setVisible(false) ;
			}} );

		frame.btnAdd.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				addItem();
			}});

		// Show/hide AddToFolderPanel based on whether checkbox is checked
		frame.chckbxAddToFolder.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				
			}});	
		
		frame.btnAddFromFile.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				limsgui.affg1c.show();
				hide();
			}
		});
	}

	public void addItem() {
		// Get the list of all columns in the table, ignoring 'id'. Get the set of columns edited in the template. Populate the templates columns, and set the rest null. Add to the correct table.
		String template = (String)frame.comboBox.getSelectedItem();
		if ( SQL2.isTemplate( limsgui.currentTable , template ) )
		{
			Vector<String> allCols = SQL2.getColumns( limsgui.currentTable) ;
			Vector<String> tempCols = new Vector<String>() ;

			// The following code removes 'id' from the insertion list, because its value is assigned by the software--not the user.
			// Notice that it can only remove a single instance. Being less robust, this could cause a bug in the future.
			allCols.remove("id");

			if ( model.getRowCount() > 0 && model.getColumnCount() > 0 )
			{
				// populate tempCols
				for( int i = 0 ; i < model.getColumnCount() ; i ++ )
				{
					tempCols.add( model.getColumnName(i) ) ; // this is populated in reverse
				}
				if ( tempCols.size() > 1 ) // reverse selection
				{
					Vector<String> temp = new Vector<String>() ;
					for ( int i = tempCols.size()-1 ; i >= 0 ; i-- )
						temp.add( tempCols.get(i) ) ;
					tempCols = temp ;
				}

				// Columns in tempCols get their user assigned values, while columns in allCols but not in tempCols get a NULL value.
				int i ;
				String path = null;
				Vector<String> values = new Vector<String>() ;
				values.add ( template ) ; // every entry is tagged with the template type
				for ( i = 1 ; i < allCols.size() ; i ++ ) // skip the first, it is the template column
				{
					int c = contains( tempCols , allCols.get(i) ) ;
					if ( c >= 0 ) {
						// If no value is entered, add it as null
						String val = (String) model.getValueAt( 0 , c );
						if (val.equals("")) {
							val = null;
						}
						values.add(val) ;
						
						// If there is a value in the "download" column, use that for the folder path
						if (allCols.get(i).equals("download")) {
							path = val;
							if (path != null) {
								int slashIdx = path.lastIndexOf(File.separator);
								if (slashIdx == -1) {
									// Value entered doesn't contain '/'
									JOptionPane.showMessageDialog(frame, "Value entered in 'download' column is not a valid filepath", "Error", JOptionPane.ERROR_MESSAGE);
								} else {
									path = path.substring(0, slashIdx);
								}
							}
						}
					}
					else
					{
						values.add( null ) ;
					}
				}
				boolean success = false;
				// insert row into table
				if (frame.chckbxAddToFolder.isSelected()) {
					success = atfg2c.addSingleItemToFolder(values, path);
				} else {
					SQL2.insert(limsgui.currentTable, values);
					success = true;
				}
				if (success) {
					populateTable() ;
					frame.lblNotSelected.setVisible(false) ;
					limsgui.fg4c.search( template, "template" ) ;
				}
			}
		}
	}

	private int contains ( Vector<String> vs , String sqlString )
	{
		for ( int i = 0 ; i < vs.size() ; i ++ )
		{
			if ( SQL2.stringCompare( vs.get(i) , sqlString ) )
				return vs.size() - i - 1 ; // reverse indexing ... I'm not entirely sure why this had to happen .. but it does.
		}

		return -1 ;
	}

	public void populateTable()
	{	
		model.setColumnCount( 0 ) ;
		model.setRowCount( 0 ) ;
		String template = (String)frame.comboBox.getSelectedItem();
		if ( template != null )
		{
			Vector<String> tempCols = SQL2.getTemplateColumnVec( limsgui.currentTable , template ) ;
			Vector<String> allCols = SQL2.getColumns(limsgui.currentTable);
			if ( tempCols == null )
			{
				System.out.println( "AddItemGUI1Controller not updating table : tempCols == null" ) ;
			}
			else
			{   // Sort tempCols
				Vector<String> newTempCols = new Vector<String>();
				for ( String col : allCols) {
					if (tempCols.contains(col)) {
						newTempCols.add(col);
					}
				}
				tempCols = newTempCols;

				int mod = 0 ;
				for ( String col : tempCols )
				{
					if ( SQL2.isEditable(col))	// User cannot edit id or template columns
					{
						model.addColumn( col ) ;
					}
					else
					{
						mod += 1 ;
					}
				}
				textVec.clear() ;
				if ( model.getColumnCount() > 0 ) // ask if there are any editable columns available
				{
					model.setRowCount( 1 ) ;
					for ( int i = 0 ; i < tempCols.size() - mod ; i ++ )
					{
						model.setValueAt( "" , 0 , i ) ;
					}
				}
			}
		}
		else
		{
			frame.lblNotSelected.setVisible(true) ;
		}
	}

	private void populateComboBox() {
		frame.comboBox.removeAllItems() ;
		Set<String> templates = SQL2.getTemplates( limsgui.currentTable ) ;
		if ( templates != null ) {
			// Only allow admins to add index items
			if (!limsgui.fg4c.frame.btnAdmin.isEnabled()) {
				templates.remove("index");
			}
			for( String s : templates )
				frame.comboBox.addItem( s ) ;
		}
		else
			System.err.println( "ERROR 1 AddItemGUI1Controller" ) ;
	}

	public void show()
	{
		populateComboBox();
		populateTable() ;
		frame.setVisible(true) ;
	}

	public void hide()
	{
		frame.setVisible(false) ;
	}

}
