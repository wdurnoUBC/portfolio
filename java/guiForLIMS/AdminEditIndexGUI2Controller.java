package guiForLIMS;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Vector;

import sqlInterface.SQL2;

public class AdminEditIndexGUI2Controller {
	
	LIMSGUI1 limsgui;
	private AdminEditIndexGUI2 frame;

	public AdminEditIndexGUI2Controller(LIMSGUI1 parent, AdminEditIndexGUI2 aeig2) {
		
		limsgui = parent;
		frame = aeig2;
		
		// Button: Add
		frame.btnAdd.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String str = frame.txtIndexItem.getText().trim();
				if (str.length() == 0) {
					frame.lblMessage.setText("Please enter an item to add");
				} else {
					frame.lblMessage.setText("");
					Vector<String> columns = SQL2.getColumns(limsgui.currentTable);
					Vector<String> values = new Vector<String>();
					columns.remove(0); // id
					values.add ( "index" ) ; // index template
					
					for ( int i = 1 ; i < columns.size() ; i ++ ) { // skip the first, it is the template column
						if ( columns.get(i).equals("name")) {
							values.add(str) ;
						} else {
							values.add( null ) ;
						}
					}
					SQL2.insert(limsgui.currentTable, values);
					updateList();
					frame.txtIndexItem.setText("");
				}
			}
		});
		
		// Button: Remove
		frame.btnRemove.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Object[] objToDelete = frame.list.getSelectedValues();
				String[] toDelete = Arrays.copyOf(objToDelete, objToDelete.length, String[].class);
				
				if (toDelete == null || toDelete.length == 0) {
					frame.lblMessage.setText("Please select an item to remove");
					return;
				} else {
					frame.lblMessage.setText("");
					for (String s : toDelete) {
						Vector<String> cols = new Vector<String>();
						Vector<String> vals = new Vector<String>();
						cols.add("template");
						cols.add("name");
						vals.add("index");
						vals.add(s);
						
						SQL2.deleteWhere(limsgui.currentTable, cols, vals);
					}
					updateList();
				}
			}
		});
	}
	
	public void updateList() {
		Vector<Vector<String>> results = SQL2.searchColumnExact(limsgui.currentTable, "index", "template", false);
		if (results == null) {
			return;
		}
		Vector<String> columns = SQL2.getColumns(limsgui.currentTable);
		Vector<String> itemNames = new Vector<String>();

		for (int i = 0; i<results.size(); i++) {
			String name = null;
			for (int j = 0; j<columns.size(); j++) {
				if (columns.get(j).equals("name")) {
					name = results.get(i).get(j);
					break;
				}
			}
			itemNames.add(name);
		}
		frame.list.setListData(itemNames.toArray());
		frame.lblTitle.setText("Add or Remove Index Items for " + limsgui.currentTable);
	}
}
