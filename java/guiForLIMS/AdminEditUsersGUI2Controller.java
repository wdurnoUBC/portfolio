package guiForLIMS;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.JComboBox;

import sqlInterface.SQL2;

public class AdminEditUsersGUI2Controller 
{
	
	LIMSGUI1 limsgui ;
	private AdminEditUsersGUI2 frame ;
	private boolean ignoreComboBox;
	
	Map<String,Boolean> adminMap ; // used for storing who is an admin and who is not
	
	public AdminEditUsersGUI2Controller ( LIMSGUI1 parent, AdminEditUsersGUI2 aeug2 )
	{
		
		limsgui = parent ;
		this.frame = aeug2;
		ignoreComboBox = false;
		
		// Restore "Deletion allowed" setting
		boolean b = false;
		Vector<Vector<String>> result = SQL2.searchColumnExact("META_LIMS_TABLE_3", "deleteMultiple", "setting", false);
		if (result.get(0).get(2).equals("yes")) {
			b = true;
		}
		frame.chckbxAllowDeletion.setSelected(b);
		
		adminMap = new HashMap<String,Boolean>() ;
		
		// Button: add
		frame.btnAdd.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String name = (String)frame.comboBox.getSelectedItem() ;
				String pass = frame.txtPassword.getText().trim();
				
				if (pass.length() == 0 || name == null || name.trim().length() == 0) {
					frame.lblErrorMessage.setText("Please enter both a name and password");
					return;
				}
				
				frame.lblErrorMessage.setText("");
				
				String admin;
				if (frame.chckbxAdminStatus.isSelected()) {
					admin = "yes";
				} else {
					admin = "no";
				}
				
				Set<String> users = SQL2.colSet( "META_LIMS_TABLE_2" , 1 ) ;
				if ( ! users.contains(name) )
				{
					Vector<String> values = new Vector<String>() ;
					values.add(name) ;
					values.add(pass) ;
					values.add(admin) ;
					SQL2.insert( "META_LIMS_TABLE_2" , values ) ;
				}
				else
				{
					Vector<String> cols = new Vector<String>();
					Vector<String> vals = new Vector<String>();
					cols.add("pass");
					cols.add("admin");
					vals.add(pass);
					vals.add(admin);
					SQL2.setWhere("META_LIMS_TABLE_2", "user", name, cols, vals);
				}
				frame.txtPassword.setText("");
				update() ;
			}} ) ;
		
		// Button: remove
		frame.btnRemove.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {  // This should probably have more restrictions
				String name = (String)frame.comboBox.getSelectedItem() ;
				if (name == null || name.trim().length() == 0) {
					frame.lblErrorMessage.setText("Please select a user to remove");
					return;
				}
				if ( ! SQL2.colSet( "META_LIMS_TABLE_2" , 1 ).contains(name) ) {
					frame.lblErrorMessage.setText( name + " does not exist" );
				} else {
					SQL2.deleteWhere("META_LIMS_TABLE_2", "user", name) ;
					frame.lblErrorMessage.setText("");
				}
				frame.txtPassword.setText("");
				update() ;
			}} ) ;
		
		// Combo box listener
		frame.comboBox.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!ignoreComboBox) {
					// get string
					JComboBox cb = (JComboBox) e.getSource() ;
					String selectedUser = (String) cb.getSelectedItem() ;
					Boolean b = adminMap.get(selectedUser) ;
					if ( b != null )
						frame.chckbxAdminStatus.setSelected( b ) ; // This listener is firing at strange times, when b may be null.
					else
						frame.chckbxAdminStatus.setSelected( false ) ;
					update() ;
					cb.setSelectedItem(selectedUser);
				}
			}} ) ;
		
		frame.chckbxAllowDeletion.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// Set preference in META_LIMS_TABLE_3
				String allowDeletion;
				if (frame.chckbxAllowDeletion.isSelected()) {
					allowDeletion = "yes";
				} else {
					allowDeletion = "no";
				}
				SQL2.setWhere("META_LIMS_TABLE_3", "setKey", allowDeletion, "setting", "deleteMultiple");
			}});
		
		update() ;
	}
	
	public void update()
	{
		ignoreComboBox = true;
		Vector<Vector<String>> table = SQL2.selectAll( "META_LIMS_TABLE_2", false) ;
		
		adminMap.clear();
		frame.comboBox.removeAllItems();
		for( Vector<String> vs : table ) {
			String name = vs.get(1) ;
			if ( SQL2.stringCompare( vs.get(3) , "yes" ) )
				adminMap.put(name, true) ;
			else
				adminMap.put(name, false) ;
			frame.comboBox.addItem( name ) ;
		}
		frame.comboBox.setSelectedIndex(-1);
		ignoreComboBox = false;
	}
	
}
