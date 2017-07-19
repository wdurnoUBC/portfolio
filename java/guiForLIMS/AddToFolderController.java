package guiForLIMS;

import java.io.File;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import sqlInterface.SQL2;

public class AddToFolderController {
	
	LIMSGUI1 limsgui;
	JFrame frame;
	
	public AddToFolderController(LIMSGUI1 parent, JFrame frame) {
		
		limsgui = parent;
		this.frame = frame;
	}
	
	// Checks the given folder path for correctness and calls addChildren to add each new folder
	// in the path.  Returns folder id or null if path is not correct.
	public String addPathItems(String folderPath, String origPath) {
		
		int idx = folderPath.lastIndexOf(File.separator);
		
		if (idx >= 0 ) {
			String id = itemExists(folderPath);
			
			if (id != null) {
				
				// Found the lowest folder in the path that exists; start recursively adding folders starting with its child
				String remainder = origPath.substring(folderPath.length());
				int end = remainder.indexOf(File.separator);
				end = remainder.indexOf(File.separator, end + 1);
				String childPath;
				
				if (end < 0) {
					childPath = folderPath + remainder;  // Last folder name in path
				} else {
					childPath = folderPath + remainder.substring(0, end);
				}
				
				return addChildren(childPath, origPath, id);
			} else {
				// Get path of parent and recurse until you find a folder that exists
				String parentPath = folderPath.substring(0, idx);
				return addPathItems(parentPath, origPath);
			}
		} else {
			JOptionPane.showMessageDialog(frame, "Path must begin with /", "Error", JOptionPane.ERROR_MESSAGE);
			return null;
		}
	}
	
	// Recursively add folder items with links between them and returns the id of the lowest parent folder
	private String addChildren(String folderPath, String origPath, String parentID) {
		
		if (!origPath.startsWith(folderPath)) {
			// Error!
			System.out.println("ERROR: folderPath = " + folderPath + ", origPath = " + origPath);
			return null;
		}
		
		// Get the path of the next level down
		String remainder = origPath.substring(folderPath.length());
		int end = remainder.indexOf(File.separator);
		end = remainder.indexOf(File.separator, end + 1);
		String childPath;
		
		if (end < 0) {
			childPath = folderPath + remainder;  // Last folder name in path
		} else {
			childPath = folderPath + remainder.substring(0, end);
		}
		String folderID = addFolderItem(folderPath);
		
		// Link to new folder item from its parent
		Vector<String> vals = new Vector<String>();
		vals.add(limsgui.currentTable); // tableName
		vals.add(parentID); // linkfrom
		vals.add(folderID);             // linkto
		vals.add("id");                 // tocolumn
		vals.add("item"); // linktype
		SQL2.insert("META_LIMS_TABLE_4", vals);
		
		// If folder is the last one in the path, add item(s)
		if (folderPath.equals(origPath)) {
			System.out.println("folderID = " + folderID);
			System.out.println("parentID = " + parentID);
			return folderID;
		}
		// Otherwise, recurse
		else {
			return addChildren(childPath, origPath, folderID);
		}
	}
	
	// Add a single item to a folder given the values (already formatted for insertion) and folderPath;
	// if folderPath is null, bring up text box to enter path instead
	public boolean addSingleItemToFolder(Vector<String> values, String folderPath) {

		// If folderPath is null or empty, bring up dialog box to enter a path
		if (folderPath == null || folderPath.trim().length() == 0) {
			String path = displayPathDialog();
			
			if (path == null) {
				// User cancelled; do nothing
				return false;
			} else if (path.length() <=0){
				// User entered no value and pressed 'OK'; display error message
				JOptionPane.showMessageDialog(frame, "No folder path was specified. Please try again and either \n" +
						"specify a folder path or uncheck 'Add to Folder'.", "Error", JOptionPane.ERROR_MESSAGE);
				return false;
			} else {
			    folderPath = path;
			}
		}
		
		// If parent folder already exists, just add the item and link it; otherwise, call addPathItems to recursively add folders
		String parentID = itemExists(folderPath);
		if (parentID == null) {
			// Folder item does not exist yet
			parentID = addPathItems(folderPath, folderPath);
		}
		
		if (parentID == null) {
			// Tried to add folder items but the path entered was not correct
			return false;
		}
		
		String id = SQL2.insert(limsgui.currentTable, values);

		// Add a link from the folder item
		Vector<String> linkRow = new Vector<String>();
		linkRow.add(limsgui.currentTable);
		linkRow.add(parentID);
		linkRow.add(id);
		linkRow.add("id");
		linkRow.add("item");
		SQL2.insert("META_LIMS_TABLE_4", linkRow);
		return true;
	}

	// Add new folder item with ".." item
	private String addFolderItem(String path) {
		Vector<String> values = new Vector<String>();
		Vector<String> columns = SQL2.getColumns(limsgui.currentTable);
		
		for (int i=1; i<columns.size(); i++) {
			if (columns.get(i).equals("isDirectory")) {
				values.add("true");
			} else if (columns.get(i).equals("name")) {
				values.add(path.substring(path.lastIndexOf(File.separator) + 1, path.length()));
			} else if (columns.get(i).equals("download")) {
				values.add(path);
			} else if (columns.get(i).equals("template")) {
				values.add("DefaultTemplate");
			} else {
				values.add(null);
			}
		}
		
		String folderID = SQL2.insert(limsgui.currentTable, values);
//		int numColumns = SQL2.getColumns(limsgui.currentTable).size();
//		System.out.println(numColumns);
//		Set<Vector<String>> dotDotLinks = limsgui.ag3c.aplg3c.addDotDot(path, numColumns);
//		for (Vector<String> link : dotDotLinks) {
//			SQL2.insert("META_LIMS_TABLE_4", link);
//		}
		return folderID;
	}
	
	// Check if an item with the given path exists; returns id if it does, null otherwise
	public String itemExists(String path) {
		Vector<Vector<String>> results = SQL2.searchColumnExact(limsgui.currentTable, path, "download", false);
		if (results.size() == 0) {
			return null;
		} else if (results.size() == 1) {
			return results.get(0).get(0);
		} else {
			JOptionPane.showMessageDialog(frame, "More than one item matches download path " + path, "Error", JOptionPane.ERROR_MESSAGE);
			System.err.println("More than one item matches download path " + path);
			return null;
		}
	}

	// Display a dialog requesting a folder path from the user; return this path or null if dialog was cancelled
	public String displayPathDialog() {
		Object[] possibilities = null;
		String path = (String)JOptionPane.showInputDialog(
				frame,
				"Please enter the path of the folder containing your item(s):",
				"Enter Folder Path",
				JOptionPane.PLAIN_MESSAGE,
				null,
				possibilities,
				"/Volumes/data/LimsData");
		return path;
	}
	
	// Display a dialog requesting a folder path from the user for a particular item from a file; return this path or null if dialog was cancelled
	public String displayPathDialogOneItem(String name) {
		Object[] possibilities = null;
		String path = (String)JOptionPane.showInputDialog(
				frame,
				"Please enter the path of the folder containing " + name + ":",
				"Enter Folder Path",
				JOptionPane.PLAIN_MESSAGE,
				null,
				possibilities,
				"/Volumes/data/LimsData");
		return path;
	}

}
