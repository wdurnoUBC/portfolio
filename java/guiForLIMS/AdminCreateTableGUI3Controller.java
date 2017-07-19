package guiForLIMS;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import sqlInterface.SQL2;
import sqlInterface.SQLUtil;

public class AdminCreateTableGUI3Controller 
{
	
	AdminCreateTableGUI3 frame ;
	LIMSGUI1 limsgui ;
	
	public AdminCreateTableGUI3Controller ( LIMSGUI1 parent, AdminCreateTableGUI3 actg3 )
	{
		limsgui = parent ;
		frame = actg3 ;
		
		// Button: Make
		frame.btnCreate.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String name = frame.txtTableName.getText().trim() ;
				if (name.length() == 0) {
					frame.txtTableName.setText("PLEASE ENTER A TABLE NAME");
					
				} else if (!SQLUtil.isValidName(name)) {
					JOptionPane.showMessageDialog(frame, "Invalid table name: Must contain only alphanumeric characters and spaces", "Error", JOptionPane.ERROR_MESSAGE);
					
				} 
				else if ( SQL2.colSet( "META_LIMS_TABLE_1", 1).contains( name ) ) {
					name += " IS TAKEN" ;
					frame.txtTableName.setText(name) ;
					
				} 
				else {
					createNewTable(name);
				}
			}
		});
		
		// Button: Delete
		frame.btnDelete.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (frame.list.getSelectedValue() != null) {
					
					String name = (String)frame.list.getSelectedValue();
					
					// "Are you sure?" dialog box
					Object[] options = {"Delete Table", "Cancel"};
					int n = JOptionPane.showOptionDialog(frame,
							"Are you sure you want to delete " + name + "?\n" + "This will delete all of the items and links in it.",
							"Delete Table",
							JOptionPane.YES_NO_OPTION,
							JOptionPane.QUESTION_MESSAGE,
							null,
							options,
							options[1]);

					if (n == JOptionPane.YES_OPTION) {
						deleteTable(name);
					}
				} else { 
					JOptionPane.showMessageDialog(frame, "Please select a table to delete", "Error", JOptionPane.ERROR_MESSAGE);
				}
			}});
		
		// Button: Duplicate
		frame.btnDuplicate.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (frame.list.getSelectedValue() != null) {
					String oldTable = (String)frame.list.getSelectedValue();

					JFrame frame = new JFrame("InputDialog for Duplicating Table");
					String newTable = JOptionPane.showInputDialog(frame, 
							"Enter a name for the new table:", 
							"Duplicate " + oldTable, 
							JOptionPane.PLAIN_MESSAGE);
					// if the user presses Cancel, this will be null
					
					if (newTable == null) {
						return;
					} else if (newTable.trim().length() == 0 || !SQLUtil.isValidName(newTable)) {
						JOptionPane.showMessageDialog(frame, "Invalid table name: Must contain only alphanumeric characters and spaces.\n" + "Table not duplicated.", "Error", JOptionPane.ERROR_MESSAGE);
					} else {
						// Duplicate table
						SQL2.copyTable(oldTable, newTable); // Create newTable and copy all contents from oldTable into newTable
						updateList();
						limsgui.ag3c.astg2c.updateComboBox();
					}
				} else {
					JOptionPane.showMessageDialog(frame, "Please select a table to duplicate", "Error", JOptionPane.ERROR_MESSAGE);
				}
			}});
					
	}
	
	// Add new table to META_LIMS_TABLE_1
	public void createNewTable(String name) {
		Vector<String> temp = new Vector<String>() ;
		temp.add( name ) ;
		temp.add( null ) ;
		temp.add( "id" ) ;
		SQL2.insert( "META_LIMS_TABLE_1" , temp ) ;
		temp.clear();
		temp.add( name ) ;
		temp.add( null ) ;
		temp.add( "template" ) ;
		SQL2.insert( "META_LIMS_TABLE_1" , temp ) ;
		temp.clear();
		temp.add( name ) ;
		temp.add( null ) ;
		temp.add( "isDirectory" ) ;
		SQL2.insert( "META_LIMS_TABLE_1" , temp ) ;
		temp.clear();
		temp.add( name ) ;
		temp.add( null ) ;
		temp.add( "name" ) ;
		SQL2.insert( "META_LIMS_TABLE_1" , temp ) ;
		temp.clear();
		temp.add( name ) ;
		temp.add( null ) ;
		temp.add( "download" ) ;
		SQL2.insert( "META_LIMS_TABLE_1" , temp ) ;
		SQL2.createTable( name ) ;
		SQL2.addColumn( name , "template" ) ;
		SQL2.addColumn( name , "isDirectory" ) ;
		SQL2.addColumn( name , "name" ) ;
		SQL2.addColumn( name , "download" ) ;
		addDefaultTemplate(name);
		addIndexTemplate(name);
		updateList() ;
		name += " HAS BEEN ADDED" ;
		limsgui.ag3c.astg2c.updateComboBox();
		frame.txtTableName.setText(name) ;
	}
	
	// Delete table
	private void deleteTable(String toDelete) {
		// Delete from META_LIMS_TABLE_1
		SQL2.deleteWhere("META_LIMS_TABLE_1", "tables", toDelete);
		
		// Delete references from META_LIMS_TABLE_3
		SQL2.deleteWhere("META_LIMS_TABLE_3", "setKey", toDelete);

		// Delete links from META_LIMS_TABLE_4
		SQL2.deleteWhere("META_LIMS_TABLE_4", "tableName", toDelete);

		// Delete table itself
		SQL2.removeTable(toDelete);

		updateList();
		frame.txtTableName.setText(toDelete + " HAS BEEN DELETED");

		if (toDelete.equals(limsgui.currentTable)) {
			String newTable = (String)frame.list.getModel().getElementAt(0);
			SQL2.setWhere("META_LIMS_TABLE_3", "setKey", newTable, "setting", "currentTable");
			limsgui.currentTable = newTable;
			if (newTable != null) {
				JOptionPane.showMessageDialog(frame, "You deleted the current table, so mLIMS has switched to " + limsgui.currentTable, "Table switched", JOptionPane.PLAIN_MESSAGE);
			}
		}
		limsgui.ag3c.astg2c.updateComboBox();
	}
	
	// Add DefaultTemplate to the new table
	private void addDefaultTemplate(String tableName) {
		Vector<String> temp = new Vector<String>() ;
		temp.add( tableName ) ;
		temp.add( "DefaultTemplate" ) ;
		temp.add( null ) ;
		SQL2.insert( "META_LIMS_TABLE_1" , temp ) ;	
		temp.clear();
		temp.add( tableName ) ;
		temp.add( "DefaultTemplate" ) ;
		temp.add( "isDirectory" ) ;
		SQL2.insert( "META_LIMS_TABLE_1" , temp ) ;
		temp.clear();
		temp.add( tableName ) ;
		temp.add( "DefaultTemplate" ) ;
		temp.add( "name" ) ;
		SQL2.insert( "META_LIMS_TABLE_1" , temp ) ;
		temp.clear();
		temp.add( tableName ) ;
		temp.add( "DefaultTemplate" ) ;
		temp.add( "download" ) ;
		SQL2.insert( "META_LIMS_TABLE_1" , temp ) ;
	}
	
	// Add IndexTemplate to the new table
	private void addIndexTemplate(String tableName) {
		Vector<String> temp = new Vector<String>() ;
		temp.add( tableName ) ;
		temp.add( "index" ) ;
		temp.add( null ) ;
		SQL2.insert( "META_LIMS_TABLE_1" , temp ) ;	
		temp.clear();
		temp.add( tableName ) ;
		temp.add( "index" ) ;
		temp.add( "name" ) ;
		SQL2.insert( "META_LIMS_TABLE_1" , temp ) ;
	}
	
	public void updateList()
	{
		Set<String> tables = SQL2.getTables();
		frame.list.setListData(tables.toArray());
	}
	
}