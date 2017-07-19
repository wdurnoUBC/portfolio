package guiForLIMS;

import hallam.microbiology.ubc.util.JDBCUtilities;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import sqlInterface.SQL2;

public class AdminGUI3Controller {
	
	public AdminGUI3 frame ;
	LIMSGUI1 limsgui ;
	public AdminEditUsersGUI2Controller aeug2c ;
	public AdminEditColumnsGUI2Controller aecg2c ;
	public AdminEditTemplatesGUI3Controller aetg3c ;
	public AdminSwitchTableGUI2Controller astg2c;
	public AdminCreateTableGUI3Controller actg3c;
	public AdminInitializeLIMSGUI2Controller ailg2c;
	public AdminPopLIMSGUI3Controller aplg3c;

	public AdminGUI3Controller(LIMSGUI1 parent) {
		
		frame = new AdminGUI3();
		limsgui = parent;
		aeug2c = new AdminEditUsersGUI2Controller(limsgui, frame.aeug2);
		aecg2c = new AdminEditColumnsGUI2Controller(limsgui, frame.aecg2);
		aetg3c = new AdminEditTemplatesGUI3Controller(limsgui, frame.aetg3);
		astg2c = new AdminSwitchTableGUI2Controller(limsgui, frame.astg2);
		actg3c = new AdminCreateTableGUI3Controller(limsgui, frame.actg3);
		ailg2c = new AdminInitializeLIMSGUI2Controller(limsgui, frame.ailg2);
		aplg3c = new AdminPopLIMSGUI3Controller(limsgui, frame.aplg3);
		
		// Update appropriate tab when tab selection changes
		frame.tabbedPane.addChangeListener( new ChangeListener() {
	        public void stateChanged(ChangeEvent e) {
	            switch(frame.tabbedPane.getSelectedIndex()) {
	            case 0:
	            	aeug2c.update();
	            	break;
	            case 1:
	            	astg2c.update();
	            	actg3c.updateList();
	            	break;
	            case 2:
	            	aecg2c.update();
	            	break;
	            case 3:
	            	aetg3c.update();
	            	aetg3c.frame.comboBox.setSelectedItem(null);
	            	break;
	            }
	        }
	    });
		
		frame.txtDataLocation.setText(limsgui.pathPrefix);
		
		// Update the pathPrefix setting
		frame.btnUpdateDataLocation.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				// First request admin credentials
				if (confirmAdmin()) {

					String path = frame.txtDataLocation.getText();
					File f = new File(path);
					if (!f.exists()) {
						JOptionPane.showMessageDialog(frame, "Cannot find that file path - please check that you are\n"
								+ "connected if it is not a local path.", "Error", JOptionPane.ERROR_MESSAGE);
						frame.txtDataLocation.setText(limsgui.pathPrefix);
					} else {
						Vector<String> cols = new Vector<String>();
						Vector<String> vals = new Vector<String>();
						cols.add("setting");
						cols.add("setKey");
						vals.add("pathPrefix");
						vals.add(limsgui.currentTable);
						Vector<Vector<String>> results = SQL2.select("META_LIMS_TABLE_3", cols, vals, false);

						if (results == null || results.size() == 0) {
							vals.clear();
							vals.add("pathPrefix");
							vals.add(limsgui.currentTable);
							vals.add(path);
							SQL2.insert("META_LIMS_TABLE_3", vals);
						} else {
							SQL2.setWhere("META_LIMS_TABLE_3", "setValue", path, "id", results.get(0).get(0));
						}
						limsgui.pathPrefix = path;
						JOptionPane.showMessageDialog(frame, "Data location successfully set", "Success", JOptionPane.INFORMATION_MESSAGE);
					}
				}
			}	
		});
		
		// Change the database location, username, or password
		frame.btnReconfigureDatabase.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (confirmAdmin()) {
					JDBCUtilities.changeDBInfo(false);
				}
			}
		});
	}

	// Returns false if the user cancelled or entered incorrect info, true if they succeeded
	public boolean confirmAdmin() {
		JPanel credPanel = new JPanel();
		JTextField username = new JTextField(20);
		JTextField password = new JTextField(20);
		credPanel.setPreferredSize(new Dimension(340, 50));
		
		credPanel.add(new JLabel("Username:"));
		credPanel.add(username);
		credPanel.add(new JLabel("Password:"));
		credPanel.add(password);

		int result = JOptionPane.showConfirmDialog(null, credPanel, 
				"Please Enter Admin Credentials", JOptionPane.OK_CANCEL_OPTION);
		if (result == JOptionPane.OK_OPTION) {
			// Check if correct
			Vector<String> columnList = new Vector<String>();
			Vector<String> userInfoList = new Vector<String>();
			columnList.add("user");
			columnList.add("pass");
			userInfoList.add(username.getText());
			userInfoList.add(password.getText());

			Vector<Vector<String>> userResult = SQL2.select("META_LIMS_TABLE_2", columnList, userInfoList, false);
			if (userResult == null || userResult.size() == 0 || userResult.get(0).get(3).equals("no")) {
				// Incorrect credentials
				JOptionPane.showMessageDialog(frame, "Incorrect admin credentials", "Error", JOptionPane.ERROR_MESSAGE);
				return false;
			} else {
				return true; // Correct credentials
			}
		}
		return false; // User cancelled
	}
	
	
	public void show()
	{	
		frame.setTitle("Admin - " + limsgui.currentTable);
		frame.setVisible(true) ;
	}
	
	public void hide()
	{
		frame.setVisible(false) ;
	}
}
