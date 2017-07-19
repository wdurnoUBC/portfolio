package guiForLIMS;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet; 
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import sqlInterface.SQL2;
import directoryCrawler.Crawler;

public class AdminPopLIMSGUI3Controller {
	
	LIMSGUI1 limsgui ;
	AdminPopLIMSGUI3 frame ;
	private Crawler myCrawler;
	private File root;
	// A set to keep track of the newly added LIMS items in updateLims
	private Set<File> newItems;

	public AdminPopLIMSGUI3Controller(LIMSGUI1 parent, AdminPopLIMSGUI3 aplg2) {
		
		limsgui = parent ;
		frame = aplg2;
		root = null;
//		root = new File("/Users/frussell/Desktop/Website content/Banner");
		newItems = new HashSet<File>();
		
		frame.btnPopulate.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				if (!limsgui.ag3c.confirmAdmin()) {
					return;
				}
				
				//Confirmation dialog box
				Object[] options = {"Update",
				                    "Cancel"};
				int n = JOptionPane.showOptionDialog(frame,
				    "Are you sure?",
				    "Update LIMS",
				    JOptionPane.YES_NO_OPTION,
				    JOptionPane.QUESTION_MESSAGE,
				    null,
				    options,
				    options[1]);
				
				if (n == JOptionPane.YES_OPTION) {
					
					if (limsgui.pathPrefix == null) {
						JOptionPane.showMessageDialog(frame, "Please specify location of data before populating", "Error", JOptionPane.ERROR_MESSAGE);
						return;
					} else {
						root = new File(limsgui.pathPrefix);
					}
					if (!root.exists()) {
						System.err.println("Root Not Found! Have you mounted your file repository?");
						JOptionPane.showMessageDialog(frame, "Root Not Found! Have you mounted your file repository?", "Error", JOptionPane.ERROR_MESSAGE);
						return;
					}
					
					try {
						frame.progressBar.setVisible(true);
						frame.progressBar.setIndeterminate(true);
						crawl(root);
						
						
						// If there is already a "rootID" entry for the table in META_LIMS_TABLE_3, just update
						if (rootIDEntryExists()) {
							System.out.println("Update!");
							updateLims();
						} 
						// Otherwise, populate from scratch
						else {
							System.out.println("Populate!");
							populateLims();
						}	
					} catch (FileNotFoundException e){
						JOptionPane.showMessageDialog(frame, "File not found exception!", "Error", JOptionPane.ERROR_MESSAGE);
					}
				}
			}} ) ;
	}
	
	
	public void crawl(File start) throws FileNotFoundException {
		myCrawler = new Crawler(start);
		myCrawler.initialCrawl();
	}
	
	// Return the id of the root item if it exists, otherwise return 0
	private boolean rootIDEntryExists() {
		
	    // Determine if root for current table already exists in META_LIMS_TABLE_3
		Vector<String> cols = new Vector<String>();
		cols.add("setting");
		cols.add("setKey");
		Vector<String> vals = new Vector<String>();
		vals.add("rootID");
		vals.add(limsgui.currentTable);
		
		Vector<Vector<String>> rootresult = SQL2.select("META_LIMS_TABLE_3", cols, vals, false);
		
		if (rootresult == null || rootresult.size() == 0) {
			return false;
		}
		return true;
	}
	
	private void addLinks(Set<File> files) {
		String table = limsgui.currentTable;
		int tableSize = SQL2.tableSize(table);
		Vector<Vector<String>> allLinks = new Vector<Vector<String>>();
		
		for (File fromFile : files) {
			// Add links from item to all subfiles using myCrawler.tree
			if (fromFile.isDirectory()) {
				Set<File> subFiles = myCrawler.getSub(fromFile);
				
				// Get id of link from file
				Vector<String> columns = new Vector<String>();
				Vector<String> values = new Vector<String>();
				columns.add("isDirectory");
				columns.add("download");
				values.add("true");
				values.add(myCrawler.getName(fromFile));
				Vector<Vector<String>> results = SQL2.select(table, columns, values, tableSize, false);
				String fromID;		
				if (results != null && results.size() > 0) {
					fromID = results.get(0).get(0);
				} else {
					System.out.println("Could not find data item " + myCrawler.getName(fromFile));
					return;	
				}
				
				columns.clear();
				columns.add("download");
				
				for (File toFile : subFiles) {
					// Get id of toFile
					values.clear();
					results.clear();
					values.add(myCrawler.getName(toFile));
					results = SQL2.select(table, columns, values, tableSize, false);
					String toID;
					if (results != null && results.size() > 0) {
						toID = results.get(0).get(0);
						Vector<String> linkvals = getLinkValues(fromID, toID);
						if (!linkExists(linkvals)) {
							allLinks.add(linkvals);
						}
						
					} else {
						System.out.println("Could not find data item " + myCrawler.getName(fromFile));
						return;	
					}

				}
			}
		}
		// Add all the links in batches
		SQL2.insertBatch("META_LIMS_TABLE_4", allLinks);
	}
	
	// Populate from scratch ===============================================================================================================================================
	
	// Populate the lims from scratch
	private void populateLims() {
		class MyWorker extends SwingWorker<Void, Void> {
			protected Void doInBackground() {
				Set<File> files = myCrawler.getFiles();
				int numColumns = SQL2.getColumns(limsgui.currentTable).size();
				
				addAllItems(files, numColumns);
				System.out.println("All items added");
				addLinks(files);
				System.out.println("Links added");
				
//				Vector<Vector<String>> dotDotVals = new Vector<Vector<String>>();
//				for (File file : files) {
//					if (myCrawler.isDirectory(file) && !file.equals(root)) {
//						String parentPath = myCrawler.getName(file);
//						dotDotVals.addAll(addDotDot(parentPath, numColumns));
//					}
//				}
//				SQL2.insertBatch("META_LIMS_TABLE_4", dotDotVals);
				addRootItem();
				limsgui.fg4c.tableController.refreshResults();
				return null;
			}
			
			protected void done() {
		        frame.progressBar.setVisible(false);
				JOptionPane.showMessageDialog(frame, "Done populating LIMS!", "Done", JOptionPane.INFORMATION_MESSAGE);
			}
		}
		new MyWorker().execute();
	}
	
	// Add many items in batch inserts
	private void addAllItems(Set<File> files, int numColumns) {
		Vector<Vector<String>> valuesVec= new Vector<Vector<String>>();
		
		for (File f : files) {
			Vector<String> values = new Vector<String>();
			values = getItemValues(f, numColumns);
			if (values != null) {
				valuesVec.add(values);
			}
		}
		SQL2.insertBatch(limsgui.currentTable, valuesVec);
	}
	
//	// Create a ".." item and return values for a link from its parent and to its "upper" parent (the parent of the folder it's in) 
//	public Set<Vector<String>> addDotDot(String parentPath, int numColumns) {
//		String upperParentPath = parentPath.substring(0, parentPath.lastIndexOf(File.separator));
//		if (upperParentPath.length() == 0) {
//			// upper parent is the root item
//			upperParentPath = File.separator;
//		}
//		
//		String itemPath = parentPath + "/..";
//		String table = limsgui.currentTable;
//		Set<Vector<String>> linkVals = new HashSet<Vector<String>>();
//		
//		System.out.println("parentPath = " + parentPath);
//		System.out.println("upperParentPath = " + upperParentPath);
//		System.out.println("itemPath = " + itemPath);
//		
//		// Add the item
//		Vector<String> values = new Vector<String>();
//		values.add("DefaultTemplate");
//		values.add("false");          // isDirectory
//		values.add("..");             // name ("..")
//		values.add(itemPath);         // download
//		// Add null values in all remaining columns
//		int i = numColumns - 5;
//		while (i > 0) {
//			values.add(null);
//			i--;
//		}
//		
//		String itemID = SQL2.insert(table, values);
//		
//		// Get id of parent folder
//		Vector<String> columnsList = new Vector<String>();
//		Vector<String> valuesList = new Vector<String>();
//		columnsList.add("isDirectory");
//		columnsList.add("download");
//		valuesList.add("true");
//		valuesList.add(parentPath);
//		Vector<Vector<String>> results = SQL2.select(table, columnsList, valuesList);
//		String parentID;		
//		if (results != null && results.size() > 0) {
//			parentID = results.get(0).get(0);
//		} else {
//			System.err.println("Populate LIMS error 2: Could not find " + parentPath);
//			return linkVals;	
//		}
//		
//		// Get id of upper parent folder
//		columnsList.clear();
//		valuesList.clear();
//		columnsList.add("isDirectory");
//		columnsList.add("download");
//		valuesList.add("true");
//		valuesList.add(upperParentPath);
//		results = SQL2.select(table, columnsList, valuesList);
//		String upperParentID;		
//		if (results != null && results.size() > 0) {
//			upperParentID = results.get(0).get(0);
//		} else {
//			System.err.println("Populate LIMS error 2: Could not find upperParent " + upperParentPath);
//			return linkVals;	
//		}
//		
//		// Link from parent to ".."
//		Vector<String> forwardLink = new Vector<String>();
//		forwardLink.add(table);
//		forwardLink.add(parentID);
//		forwardLink.add(itemID);
//		forwardLink.add("id");
//		
//		linkVals.add(forwardLink);
//		
//		// Link from ".." back to the upper parent folder
//		Vector<String> backLink = new Vector<String>();
//		backLink.add(table);
//		backLink.add(itemID);
//		backLink.add(upperParentID);
//		backLink.add("id");
//		
//		linkVals.add(backLink);
//		
//		return linkVals;
//	}

	// Add the "rootID" entry in META_LIMS_TABLE_3 for the current table
	private void addRootItem() {
		// Find id of root item
		Vector<Vector<String>> result = SQL2.searchColumnExact(limsgui.currentTable, "/", "download", false);
		String ID = result.get(0).get(0);

		// Add it to META_LIMS_TABLE_3
		Vector<String> values = new Vector<String>();
		values.add("rootID");
		values.add(limsgui.currentTable);
		values.add(ID);
		SQL2.insert("META_LIMS_TABLE_3", values);
		limsgui.rootID = ID;
	}
	
	
// Update ====================================================================================================================================================================
	
	// Update the lims, adding any new files and marking missing files
	private void updateLims() {
		class MyWorker extends SwingWorker<Void, Void> {
			protected Void doInBackground() {
				newItems.clear();

				// Get id of 'root' item in current table
				Vector<String> cols = new Vector<String>();
				cols.add("setting");
				cols.add("setKey");
				Vector<String> vals = new Vector<String>();
				vals.add("rootID");
				vals.add(limsgui.currentTable);
				
				Vector<Vector<String>> linkResult = SQL2.select("META_LIMS_TABLE_3", cols, vals, false);
				String rootID;
				if (linkResult == null || linkResult.size() == 0) {
					System.err.println("ERROR: Root LIMS item not found");
					return null;
				} else {
					rootID = linkResult.get(0).get(3);
					System.out.println("rootID = " + rootID);
				}

				int numColumns = SQL2.getColumns(limsgui.currentTable).size();
				updateHere(root, rootID, numColumns);
				addLinks(newItems);
				limsgui.fg4c.searchExact("index", "template");
				return null;
			}
			
			protected void done() {
		        frame.progressBar.setVisible(false);
				JOptionPane.showMessageDialog(frame, "Done updating LIMS!", "Done", JOptionPane.INFORMATION_MESSAGE);
			}
		}
		new MyWorker().execute();
	}
	
	// Given that the item already exists in LIMS, update all of its children recursively
	public void updateHere(File f, String id, int numColumns) {
		Vector<Vector<String>> linkResult = new Vector<Vector<String>>();
		
		// Get all id links from file
		linkResult = getLinksFromID(id);
		
		// Compile a "path to id" map for all children that exist in the LIMS
		Map<String, String> children = new HashMap<String, String>();
		for (Vector<String> item : linkResult) {
			String path = SQL2.searchColumnExact(limsgui.currentTable, item.get(3), "id", false).get(0).get(4);
			children.put(limsgui.pathPrefix + path, item.get(3));
		}
		System.out.println("file = " + f);
		System.out.println("children = " + children);
		
		// Get all subfiles from crawler
		Set<File> subFiles = myCrawler.getSub(f);
		
		System.out.println("subFiles = " + subFiles);
		
		for (File file : subFiles) {
			System.out.println("Subfile " + file + " of " + f);
			String childID = null;
			String crawlerPath = file.getAbsolutePath();
			System.out.println(crawlerPath);
			
			// Find ID of LIMS item if it exists
			if (children.containsKey(crawlerPath)) {
				childID = children.get(crawlerPath);
			}
			
			if (childID == null) {
				// If it doesn't exist, create it
				Vector<String> values = new Vector<String>();
				childID = addItem(file, values, numColumns);
				
				// Add link from parent
				SQL2.insert("META_LIMS_TABLE_4", getLinkValues(id, childID));
				
//				// If it's a directory, add a ".."
//				if (myCrawler.isDirectory(file)) {
//					Set<Vector<String>> dotDotLinks = addDotDot(myCrawler.getName(file), numColumns);
//					for (Vector<String> dotDotLink : dotDotLinks) {
//						SQL2.insert("META_LIMS_TABLE_4", dotDotLink);
//					}
//				}
			}
			
			// Recurse over children
			updateHere(file, childID, numColumns);
		}
	}
	
	private Vector<Vector<String>> getLinksFromID(String id) {
		Vector<String> cols = new Vector<String>();
		Vector<String> vals = new Vector<String>();
		cols.add("tableName");
		cols.add("linkfrom");
		cols.add("tocolumn");
		cols.add("linktype");
		vals.add(limsgui.currentTable);
		vals.add(id);
		vals.add("id");
		vals.add("item");
		return SQL2.select("META_LIMS_TABLE_4", cols, vals, false);
	}

	// Add a single item
	private String addItem(File file, Vector<String> values, int numColumns) {
		System.out.println("Adding " + file.getName());
		
		values = getItemValues(file, numColumns);
		if (values != null) {
			Vector<String> vecVals = new Vector<String>(values);
			String id = SQL2.insert(limsgui.currentTable, vecVals);
			return id;
		} else {
			return null;
		}
	}
	
	private Vector<String> getItemValues(File f, int numColumns) {
		// Get item values using myCrawler.name and myCrawler.isDir
		String fullPath = myCrawler.getName(f);
		String pathOfCrawlStart = myCrawler.getRoot().getAbsolutePath();
		System.out.println("pathOfCrawlStart = " + pathOfCrawlStart);
		System.out.println("pathPrefix = " + limsgui.pathPrefix);
		
		// If crawl didn't start at pathPrefix, adjust path of new item
		if (!pathOfCrawlStart.equals(limsgui.pathPrefix)) {
			String addToStart = pathOfCrawlStart.substring(limsgui.pathPrefix.length());
			fullPath = addToStart + fullPath;
			System.out.println("Path in getItemValues = " + fullPath);
		}

		String filename = fullPath.substring(fullPath.lastIndexOf("/") + 1);
		
		// Root item
		if (filename.length() == 0) {
			filename = "LIMS Data";
		}

		Vector<String> values = new Vector<String>();
		values.add("DefaultTemplate");
		values.add("" + myCrawler.isDirectory(f));    // isDirectory
		values.add(filename);                         // name
		values.add(fullPath);                         // download

		// Add null values in all remaining columns
		int i = numColumns - 5;
		while (i > 0) {
			values.add(null);
			i--;
		}
		return values;
	}
	
	// Return values for an SQL command to create the link
	private Vector<String> getLinkValues(String fromID, String toID) {
		Vector<String> linkValues = new Vector<String>();
		linkValues.add(limsgui.currentTable);
		linkValues.add(fromID);
		linkValues.add(toID);
		linkValues.add("id");
		linkValues.add("item");
		
		return linkValues;
	}
	
	private boolean linkExists(Vector<String> linkVals) {
		Vector<String> columns = new Vector<String>();
		columns.add("tableName");
		columns.add("linkfrom");
		columns.add("linkto");
		columns.add("tocolumn");
		columns.add("linktype");
		
		Vector<Vector<String>> result = SQL2.select("META_LIMS_TABLE_4", columns, linkVals, false);
		
		return (result.size() > 0) ;
	}
}
