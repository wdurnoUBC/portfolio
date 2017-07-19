package guiForLIMS;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import au.com.bytecode.opencsv.CSVReader;

import sqlInterface.SQL2;

public class AddFromFileGUI1Controller {
	
	LIMSGUI1 limsgui;
	AddFromFileGUI1 frame;
	AddToFolderController atfgc;
	
	public AddFromFileGUI1Controller(LIMSGUI1 parent) {
		
		limsgui = parent;
		frame = new AddFromFileGUI1();
		atfgc = new AddToFolderController(limsgui, frame);
		
		// Import items from selected file using given options
		frame.btnImport.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				addAllFromFile();
			}} );
		
		// Open file chooser
		frame.btnChooseFile.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				JFileChooser fc = new JFileChooser();
				// Edit FileFilter here to accept other file formats
				FileNameExtensionFilter fileExtFilter = new FileNameExtensionFilter("CSV/TSV file", "csv", "tsv");
				
				fc.setDialogTitle("Choose File to Import Items From");
				fc.addChoosableFileFilter(fileExtFilter);
				int returnVal = fc.showDialog(frame, "Choose");

				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File file = fc.getSelectedFile();
					frame.txtSelectedFile.setText(file.getPath());
				}
			}} );
		
		// Cancel
		frame.btnCancel.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				hide();
			}} );
		
		frame.chckbxAddToFolder.setSelected(true);
	}
	
	private void populateComboBox() {
		Set<String> tempSet = SQL2.getTemplates(limsgui.currentTable);
		tempSet.remove("index");
		
		for (String template : tempSet) {
			frame.comboBox.addItem(template);
		}
	}

	// Adds multiple items at once from a csv file
	private void addAllFromFile() {
		try {

			File file = new File(frame.txtSelectedFile.getText());
			String template = (String)frame.comboBox.getSelectedItem();
			CSVReader reader = getReader(file);
			if (reader == null) {
				return;
			}

			String [] headers;
			ArrayList<Integer> indices = new ArrayList<Integer>();
			Vector<String> allCols = SQL2.getColumns(limsgui.currentTable);
			allCols.remove("id");

			// Read column headers
			if ((headers = reader.readNext()) != null) {

				// Check that each column exists
				for (String col : headers) {
					if (col.trim().length() == 0) {
						JOptionPane.showMessageDialog(frame, "Missing column header", "Error", JOptionPane.ERROR_MESSAGE);
						return;
					} else if (!allCols.contains(col)) {
						JOptionPane.showMessageDialog(frame, "Column '" + col + "' does not exist", "Error", JOptionPane.ERROR_MESSAGE);
						return;
					} else if (col.equals("template")){
						JOptionPane.showMessageDialog(frame, "Cannot include 'template' column in file,\n" + "as template has already been chosen", "Error", JOptionPane.ERROR_MESSAGE);
						return;
					}
					// Get index of column in database
					indices.add(allCols.indexOf(col));
				}
			}
			String folderPath = null;
			int downloadcol = Arrays.asList(headers).indexOf("download");
			System.out.println("downloadcol = " + downloadcol);
			
			// If file doesn't contain "download" column, prompt user to enter a path
			if (downloadcol == -1) {
				
				String path = atfgc.displayPathDialog();
				if (path == null) {
					// User cancelled; do nothing
					return;
				} else if (path.length() <=0){
					// User entered no value and pressed 'OK'; display error message
					JOptionPane.showMessageDialog(frame, "No folder path was specified. Please try again and either \n" +
							"specify a folder path or uncheck 'Add to Folder'.", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				} else {
				    folderPath = path;
				}
			}
			
			String [] nextLine;
			Vector<Vector<String>> allItems = new Vector<Vector<String>>();
			// Create vector of null values as the "default" values to insert
			Vector<String> nullVec = new Vector<String>();
			for (int j=0; j<allCols.size(); j++) {
				nullVec.add(null);
			}

			int tempIdx = allCols.indexOf("template");
			int nameIdx = allCols.indexOf("name");
			boolean success = false;

			// Read rows of data
			while ((nextLine = reader.readNext()) != null) {
				System.out.println("folderPath = " + folderPath);
				// Copy null vector then add specified values in the right places
				Vector<String> values = new Vector<String>(nullVec);
				values.setElementAt(template, tempIdx);
				for (int i=0; i<nextLine.length; i++) {
					if (nextLine[i].trim().length() > 0) {
						values.setElementAt(nextLine[i], indices.get(i));
					}
				}
				System.out.println("values = " + values);
				
				if (frame.chckbxAddToFolder.isSelected()) {
					
					// If file contained "download" column and item actually has a value in that column, use that value
					if (downloadcol != -1 && downloadcol < nextLine.length && nextLine[downloadcol].trim().length() > 0) {
						
						String fullPath = nextLine[downloadcol];
						System.out.println("fullPath = " + fullPath);
						int slashIdx = fullPath.lastIndexOf(File.separator);
						String itemPath = fullPath.substring(0, slashIdx);
						System.out.println("itemPath = " + itemPath);
						success = atfgc.addSingleItemToFolder(values, itemPath);
					}
					// Otherwise: 1) File contains download but item has no value for it: folderPath is null, prompt user to enter a path for that item
					else if (folderPath == null) {
						String path = atfgc.displayPathDialogOneItem(values.get(nameIdx));
						if (path != null) {
							atfgc.addSingleItemToFolder(values, path);
						}
					}
					// 2) File doesn't contain download: user will have already entered value for folderPath
					else {
						success = atfgc.addSingleItemToFolder(values, folderPath);
					}
				} else {
					// Add to the list of items which can be batch inserted
					allItems.add(values);
				}
			}

			// If not adding to folder, insert all items as a batch
			if (!frame.chckbxAddToFolder.isSelected()) {
				SQL2.insertBatch(limsgui.currentTable, allItems);
				success = true;
			}
			// Close 'Add from file' window after successful import
			if (success) {
				limsgui.fg4c.search( template, "template" ) ;
				hide();
			}

		} catch (IOException e) {
			JOptionPane.showMessageDialog(frame, "Could not import from file", "Error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}
	
	private CSVReader getReader(File f) {
		String name = f.getName();
		int idx = name.lastIndexOf(".");
		
		if (idx > 0) {
			String extension = name.substring(idx+1);
			try {
				if (extension.equals("csv")) {
					return new CSVReader(new FileReader(f));
				} else if (extension.equals("tsv")) {
					return new CSVReader(new FileReader(f), '\t');
				} else {
					JOptionPane.showMessageDialog(frame, "Invalid file type", "Error", JOptionPane.ERROR_MESSAGE);
					return null;
				}
			} catch (FileNotFoundException e) {
				JOptionPane.showMessageDialog(frame, "File not found", "Error", JOptionPane.ERROR_MESSAGE);
				e.printStackTrace();
				return null;
			}
		} else {
			JOptionPane.showMessageDialog(frame, "Missing file extension", "Error", JOptionPane.ERROR_MESSAGE);
			return null;
		}
	}
	
	public void show()
	{
		populateComboBox();
		frame.setVisible(true) ;
	}
	
	public void hide()
	{
		frame.setVisible(false) ;
	}
	
}
