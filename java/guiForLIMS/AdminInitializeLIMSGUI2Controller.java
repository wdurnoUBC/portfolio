package guiForLIMS;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;
import java.util.Vector;

import javax.swing.JOptionPane;

import sqlInterface.SQL2;

public class AdminInitializeLIMSGUI2Controller 
{
	public LIMSGUI1 limsgui ;
	private AdminInitializeLIMSGUI2 frame ;
	
	public AdminInitializeLIMSGUI2Controller ( LIMSGUI1 parent, AdminInitializeLIMSGUI2 ailg2 )
	{
		limsgui = parent ;
		frame = ailg2;
		
		// Button: Initialize
		frame.btnInitialize.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String name = frame.txtAdminName.getText() ;
				String pass = frame.txtAdminPass.getText() ;
				Vector<String> cols = new Vector<String>();
				Vector<String> vals = new Vector<String>();
				cols.add("user");
				cols.add("pass");
				cols.add("admin");
				vals.add(name);
				vals.add(pass);
				vals.add("yes");
				Vector<Vector<String>> result = SQL2.select("META_LIMS_TABLE_2", cols, vals, false);
				
				if (result.size() > 0) {
					frame.txtAdminName.setText("");
					frame.txtAdminPass.setText("");

					Object[] options = {"Initialize", "Cancel"};
					int n = JOptionPane.showOptionDialog(frame,
							"Are you sure?",
							"Initialize LIMS",
							JOptionPane.YES_NO_OPTION,
							JOptionPane.QUESTION_MESSAGE,
							null,
							options,
							options[1]);

					if (n == JOptionPane.YES_OPTION) {
						initializeLIMS(name, pass);
					}
				} else {
					JOptionPane.showMessageDialog(frame, "Incorrect username or password", "Warning", JOptionPane.WARNING_MESSAGE);
				}
			}
	    });
		
	}
	
	private void initializeLIMS(String name, String pass) {
		// Clean up old tables
		Set<String> tables = SQL2.getTables() ;
		for ( String s : tables )
			SQL2.removeTable(s);

		// Clean up old database completely
		SQL2.removeTable( "META_LIMS_TABLE_1" ) ; // table, template, column
		SQL2.removeTable( "META_LIMS_TABLE_2" ) ; // user , pass , admin
		SQL2.removeTable( "META_LIMS_TABLE_3" ) ; // defaultTable
		SQL2.removeTable( "META_LIMS_TABLE_4" ) ; // from, to, tocolumn

		//Repopulate
		SQL2.createTable( "META_LIMS_TABLE_1" ) ;
		SQL2.addColumn( "META_LIMS_TABLE_1" , "tables" ) ;
		SQL2.addColumn( "META_LIMS_TABLE_1" , "template" ) ;
		SQL2.addColumn( "META_LIMS_TABLE_1" , "columns" ) ;

		limsgui.ag3c.actg3c.createNewTable("home");

		SQL2.createTable( "META_LIMS_TABLE_2" ) ;
		SQL2.addColumn( "META_LIMS_TABLE_2" , "user" ) ;
		SQL2.addColumn( "META_LIMS_TABLE_2" , "pass" ) ;
		SQL2.addColumn( "META_LIMS_TABLE_2" , "admin" ) ;
		Vector<String> temp1 = new Vector<String> () ;
		temp1.add(name) ;
		temp1.add(pass) ;
		temp1.add( "yes" ) ;
		SQL2.insert( "META_LIMS_TABLE_2" , temp1 ) ;
		SQL2.createTable( "META_LIMS_TABLE_3" ) ;
		SQL2.addColumn( "META_LIMS_TABLE_3" , "setting" ) ;
		SQL2.addColumn("META_LIMS_TABLE_3", "setKey");
		SQL2.addColumn("META_LIMS_TABLE_3", "setValue");
		Vector<String> temp2 = new Vector<String> () ;
		temp2.add( "currentTable" ) ;
		temp2.add("home");
		temp2.add(null);
		SQL2.insert( "META_LIMS_TABLE_3" , temp2 ) ;
		temp2.clear();
		temp2.add("deleteMultiple");
		temp2.add("no");
		temp2.add(null);
		SQL2.insert( "META_LIMS_TABLE_3" , temp2 ) ;

		limsgui.currentTable = "home" ;

		// META_LIMS_TABLE_4
		SQL2.createTable( "META_LIMS_TABLE_4" );
		SQL2.addColumn( "META_LIMS_TABLE_4" , "tableName");
		SQL2.addColumn( "META_LIMS_TABLE_4" , "linkfrom" );
		SQL2.addColumn( "META_LIMS_TABLE_4" , "linkto" );
		SQL2.addColumn( "META_LIMS_TABLE_4" , "tocolumn" );
		SQL2.addColumn("META_LIMS_TABLE_4", "linktype");

		limsgui.fg4c.tableController.refreshResults();
	}
	
}