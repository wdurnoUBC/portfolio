package guiForLIMS;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;
import java.util.Vector;

import javax.swing.JComboBox;

import sqlInterface.SQL2;

public class AdminSwitchTableGUI2Controller 
{
	LIMSGUI1 limsgui ;
	AdminSwitchTableGUI2 frame ;
	private boolean ignoreComboBox;
	
	public AdminSwitchTableGUI2Controller ( LIMSGUI1 parent, AdminSwitchTableGUI2 astg2 )
	{
		limsgui = parent ;
		frame = astg2;
		ignoreComboBox = false;
		
		if ( limsgui.currentTable == null )
			frame.lblMessage.setText("No current table selected");
		else
			frame.lblMessage.setText("");
		
		updateComboBox();
			
		// Combo box listener
		frame.comboBox.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!ignoreComboBox) {
				// get string
					JComboBox cb = (JComboBox) e.getSource() ;
					String selectedTable = (String) cb.getSelectedItem() ;

					// send to database
					SQL2.setWhere( "META_LIMS_TABLE_3", "setKey", selectedTable, "setting", "currentTable" ) ;
					System.out.println("Old path prefix = " + limsgui.pathPrefix);
					
					update() ;
					limsgui.updatePathPrefix();
					System.out.println("New path prefix = " + limsgui.pathPrefix);
				}
			}
		}) ;
	}
	
	public void update ()
	{
		Vector<Vector<String>> temp1 = SQL2.searchColumnExact("META_LIMS_TABLE_3", "currentTable", "setting", false);
		if ( temp1 == null )
		{
			frame.lblMessage.setText( "PLEASE INITIALIZE DATABASE" ) ;
		}
		else {
			String toTable = temp1.get(0).get(2) ;
			if (!limsgui.currentTable.equals(toTable)) {
				// Change tables
				limsgui.currentTable = toTable ;
				limsgui.fg4c.searchExact("index", "template"); // Go to the new table's index
				frame.lblMessage.setText("");
				limsgui.ag3c.frame.setTitle("Admin - " + toTable);
			}
		}
	}
	
	public void updateComboBox() {
		ignoreComboBox = true;
		
		frame.comboBox.removeAllItems() ;
		Set<String> tables = SQL2.getTables();
		if( tables != null ) {
			for ( String s : tables ) {
				frame.comboBox.addItem( s ) ;
			}
		}
		frame.comboBox.setSelectedItem(limsgui.currentTable);
		
		ignoreComboBox = false;
	}
}
