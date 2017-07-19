package guiForLIMS;

import java.util.Vector;
import sqlInterface.SQL2;

// This class is used to store global variables for the GUI

public class LIMSGUI1 
{
	
	// Controllers
	public LoginGUI1Controller lg1c ;
	public FrontGUI5Controller fg4c ;
	public ViewGUI1Controller vg1c ;
	public AddItemGUI1Controller aig1c ;
	public EditTemplateInvalidNameGUI1Controller eting1c;
	public AdminGUI3Controller ag3c ;
	public EditMultipleGUI1Controller emg1c;
	public AdvancedSearchGUI2Controller asg2c;
	public AddFromFileGUI1Controller affg1c;
	
	// Global variables
	public String currentTable ;
	public String pathPrefix ;
	public String rootID ;
	public String view ;
	
	public LIMSGUI1 ()
	{
		// initialize variables
		Vector<Vector<String>> temp1 = SQL2.searchColumnExact("META_LIMS_TABLE_3", "currentTable", "setting", false);
		if ( temp1 == null || temp1.size() == 0) {
			// TODO: Prompt user to initialize database?
			currentTable = null ;
		} else {
			currentTable = temp1.get(0).get(2) ;

			updatePathPrefix();
				
			Vector<String> vals = new Vector<String>();
			Vector<String> cols = new Vector<String>();
			cols.add("setting");
			cols.add("setKey");
			vals.add("rootID");
			vals.add(currentTable);
			temp1 = null;
			temp1 = SQL2.select("META_LIMS_TABLE_3", cols, vals, false);
			if ( temp1 == null || temp1.size() == 0)
				rootID = null ;
			else
				rootID = temp1.get(0).get(3) ;
		}
		
		view = "union" ;
		
		// initialize controllers
		lg1c = new LoginGUI1Controller ( this ) ;
		fg4c = new FrontGUI5Controller ( this ) ;
		fg4c.linkerController.addSelectedRowListener();
		vg1c = new ViewGUI1Controller ( this ) ;
		aig1c = new AddItemGUI1Controller ( this ) ;
		eting1c = new EditTemplateInvalidNameGUI1Controller ( this );
		ag3c = new AdminGUI3Controller ( this ) ;
		emg1c = new EditMultipleGUI1Controller( this );
		asg2c = new AdvancedSearchGUI2Controller( this );
		affg1c = new AddFromFileGUI1Controller( this );
		
		
		// show login screen
		lg1c.show() ;
		
		// These two lines are temporary (they prevent the login screen from popping up, if you also comment out the previous line):
//		fg4c.show() ;
//		fg4c.searchExact( "index" , "template") ;
	}
	
	
	public void updatePathPrefix() {
		Vector<String> cols = new Vector<String>();
		Vector<String> vals = new Vector<String>();
		cols.add("setting");
		cols.add("setKey");
		vals.add("pathPrefix");
		vals.add(currentTable);
		Vector<Vector<String>> metaResult = SQL2.select("META_LIMS_TABLE_3", cols, vals, false);
		if (metaResult.size() > 0 ) {
			pathPrefix = metaResult.get(0).get(3);
		} else {
			System.out.println("Must set data location");
			return;
		}
	}
	
}
