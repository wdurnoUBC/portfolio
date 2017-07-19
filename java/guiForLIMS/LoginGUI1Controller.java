package guiForLIMS;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Vector;

import sqlInterface.SQL2;

public class LoginGUI1Controller {

	public LIMSGUI1 limsgui ;
	public LoginGUI1 frame ;
	
	public LoginGUI1Controller ( LIMSGUI1 parent ) {
		
		limsgui = parent ;
		frame = new LoginGUI1 () ;
//		frame.btnLogin.setFocusable(true);  ** Trying to listen for key press events
		
		// Hide error message
		frame.lblIncorrectInfo.setVisible(false);
		
		ActionListener listener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				attemptLogin();
			}};
		
		// Add listener to Login button
		frame.btnLogin.addActionListener( listener );
		
		// Add listeners to username and password fields
		frame.passwordField.addActionListener( listener );
		frame.usernameField.addActionListener( listener );

		//create custom close operation
		frame.addWindowListener(new WindowAdapter() {
			
		      public void windowClosing(WindowEvent e) {
		    	  
		  		if (!limsgui.fg4c.frame.isVisible()) {
					// Program is being closed... close database connection
					SQL2.closeConnection();
				}
		      }
		});
	}
	
	public void show()
	{
		frame.setVisible(true) ;
	}
	
	public void hide()
	{
		frame.setVisible(false) ;
	}

	public void clearFields() {
		frame.usernameField.setText("");
		frame.passwordField.setText("");
	}
	
	public void hideErrorMsg() {
		frame.lblIncorrectInfo.setVisible(false);
	}
	
	private void attemptLogin() {
		// Check if username and password are valid
		Vector<String> columnList = new Vector<String>();
		Vector<String> userInfoList = new Vector<String>();
		columnList.add("user");
		columnList.add("pass");
		userInfoList.add(frame.usernameField.getText());
		userInfoList.add(new String(frame.passwordField.getPassword()));
		
		Vector<Vector<String>> result = SQL2.select("META_LIMS_TABLE_2", columnList, userInfoList, false);
		
		if (result.isEmpty()) {
			// Display error
			frame.lblIncorrectInfo.setVisible(true);
			
		} else {
			// Check if they are an admin
			String isAdmin = result.get(0).get(3);
			
			if (isAdmin.equals("yes")) {
				limsgui.fg4c.showAdmin(true);
			} else {
				limsgui.fg4c.showAdmin(false);
			}
			// Close login window, open main limsgui
			limsgui.fg4c.show() ;
			limsgui.fg4c.searchExact( "index" , "template") ;
			hide();
		}
			
	}
	
}
