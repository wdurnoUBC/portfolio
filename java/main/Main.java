package main;

import java.io.IOException;

import guiForLIMS.LIMSGUI1;
import hallam.microbiology.ubc.util.JDBCUtilities;

public class Main {

	public static LIMSGUI1 limsgui ;
	
	public static void main(String[] args) {
		try {
			JDBCUtilities.testConfig();
		} catch (IOException ex) {
			System.out.println("Cannot find properties");
			
			// How do I do this without a parent frame?
//			JOptionPane.showMessageDialog(frame, "Cannot find properties", "Error", JOptionPane.ERROR_MESSAGE);
			
			ex.printStackTrace();
			System.exit(1);
		}

		limsgui = new LIMSGUI1 () ;
	}

}
