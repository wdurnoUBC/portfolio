package guiForLIMS;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import javax.swing.DefaultListSelectionModel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import sqlInterface.SQL2;

public class FrontGUI4LinkerController {

	public LIMSGUI1 limsgui ;
	private FrontGUI4Linker frame;
	private JTable table;
	private DefaultTableModel model;
	private DefaultListSelectionModel listSelectionModel;
	private ArrayList<String> linkFromIDs;

	public FrontGUI4LinkerController(LIMSGUI1 parent, final FrontGUI4Linker frame) {

		limsgui = parent ;
		this.frame = frame;
		table = frame.table;
		model = new DefaultTableModel() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		listSelectionModel = new DefaultListSelectionModel();
		linkFromIDs = new ArrayList<String>();
		
		table.setModel(model);
		table.setSelectionModel(listSelectionModel);

		model.setColumnCount(2);
		model.setRowCount(0);
		table.getColumnModel().getColumn(0).setPreferredWidth(18);
		table.removeColumn(table.getColumnModel().getColumn(1));   // Hide the second column (which shows whether the entry is a heading or not)
		table.setDefaultRenderer(Object.class, new MyTableCellRenderer());

		populateComboBox();

		ActionListener createListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {

				if (linkFromIDs.size() < 1) {
					frame.lblError.setText("Please select an item to link from");
					return;
				} if (frame.linkToField.getText().equals("")) {
					frame.lblError.setText("Please enter a value to link to");
					return;
				}
				frame.lblError.setText("");

				String linkTo = frame.linkToField.getText();
				String toColumn = (String) frame.columnChoiceBox.getSelectedItem();
				
				Vector<String> cols = new Vector<String>();
				cols.add("tableName");
				cols.add("linkfrom");
				cols.add("linkto");
				cols.add("tocolumn");
				cols.add("linktype");

				for (String fromID : linkFromIDs) {
					Vector<String> rowValues = new Vector<String>();
					rowValues.add(limsgui.currentTable);
					rowValues.add("" + fromID);
					rowValues.add(linkTo);
					rowValues.add(toColumn);

					// TODO: Give option of linking from user vs item
					rowValues.add("item");


					Vector<Vector<String>> result = SQL2.select("META_LIMS_TABLE_4", cols, rowValues, false);
					if (result.size() > 0) {
						if (linkFromIDs.size() == 1) {
							frame.lblError.setText("Link already exists");
						}
					} else {
						SQL2.insert("META_LIMS_TABLE_4", rowValues);
						frame.linkToField.setText("");
						displayAlreadyLinked();
					}

				}
			}};


			// Add listener to Create Link button
			frame.btnCreateLink.addActionListener(createListener);
			frame.linkToField.addActionListener(createListener);

			frame.btnRemoveLink.addActionListener( new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					ArrayList<String> linkedTo = getLinkedToValue();
					if (linkedTo != null) {
						Vector<String> cols = new Vector<String>();
						Vector<String> vals = new Vector<String>();
						cols.add("tableName");
						cols.add("linkfrom");
						cols.add("linkto");
						cols.add("tocolumn");
						vals.add(limsgui.currentTable);
						vals.add(linkFromIDs.get(0).toString());
						vals.add(linkedTo.get(0));
						vals.add(linkedTo.get(1));

						SQL2.deleteWhere("META_LIMS_TABLE_4", cols, vals);
						displayAlreadyLinked();
					} else {
						JOptionPane.showMessageDialog(frame, "Please select a link to remove", "Warning", JOptionPane.WARNING_MESSAGE);
					}
				}});

			// Display all the linked ids in the main table
			frame.btnDisplayAllLinked.addActionListener( new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					displayLinkedIDs();
				}});

			// Search "already linked to" value when double clicked
			table.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() == 2) {
						ArrayList<String> linkedTo = getLinkedToValue();
						if (linkedTo != null) {
							Vector<Vector<String>> out = SQL2.searchColumnExact(limsgui.currentTable, linkedTo.get(0), linkedTo.get(1), true );
							limsgui.fg4c.tableController.display( out ) ;
							displayAlreadyLinked();
						}
					}
				}
			});
	}

	// Returns the column value and name (the "linkto" and "tocolumn") from the row currently selected in the linker table
	private ArrayList<String> getLinkedToValue() {
		ArrayList<String> result = new ArrayList<String>();

		int[] indices = table.getSelectedRows() ;
		if (indices.length <= 0)
			return null;
		String colValue = "";
		String colName = "";

		int row = indices[0];
		if (model.getValueAt(row,  1).equals("heading"))
			return null;
		colValue = (String) model.getValueAt(row, 0);

		// Find the name of the column linked to
		for (int i=row-1;i>=0;i--) {
			if (model.getValueAt(i, 1).equals("heading")) {
				colName = (String) model.getValueAt(i, 0);
				colName = colName.substring(0, colName.length()-1); // Trim off colon
				break;
			}
		}
		if (colName.equals("id")) {
			int idx = colValue.indexOf("(");
			colValue = colValue.substring(0, idx-1);
		}
		result.add(colValue);
		result.add(colName);
		return result;
	}

	// Add listener to main table to update "Link from" based on selected row
	public void addSelectedRowListener() {
		
		limsgui.fg4c.tableController.table.getSelectionModel().addListSelectionListener(
				new ListSelectionListener() {
					public void valueChanged(ListSelectionEvent event) {

						FrontGUI3TableController c = limsgui.fg4c.tableController;

						int[] indices = c.table.getSelectedRows() ;
						linkFromIDs.clear();
						
						if ( indices.length > 0 && !event.getValueIsAdjusting()) {
							
							for (int idx : indices) {
								linkFromIDs.add(c.id.get( c.table.convertRowIndexToModel(idx)));
							}
							
							if (indices.length == 1) {
								// Get id of the selected item
								frame.lblLinkFrom.setText("Link from selected item (ID " + linkFromIDs.get(0) + ")");
							} else {
								// Multiple items selected
								frame.lblLinkFrom.setText("Link from selected item (multiple)");
							}
							displayAlreadyLinked();
						} else {
							frame.lblLinkFrom.setText("Link from selected item");
							// Clear table
							for (int i=0; i<model.getRowCount(); i++) {
								model.removeRow(i);
							}
						}
						frame.lblError.setText("");
					}
				}
				);

	}

	// Add "Already linked to" items
	private void displayAlreadyLinked() {

		model.setRowCount(0);
		if (linkFromIDs.size() > 1) {
			return;
		}
		
		Vector<String> cols = new Vector<String>();
		Vector<String> vals = new Vector<String>();
		cols.add("tableName");
		cols.add("linkfrom");
		vals.add(limsgui.currentTable);
		vals.add(linkFromIDs.get(0) + "");

		Vector<Vector<String>> result = SQL2.select("META_LIMS_TABLE_4", cols, vals, false);
		int nameIdx = SQL2.getColumnIndex(limsgui.currentTable, "name");

		if (result.size() > 0) {
			Collections.sort(result, new VectorComparator());
			String currentColumn = result.get(0).get(4);
			String[] rowData = {currentColumn + ":", "heading"};
			model.addRow(rowData);
			int tableSize = SQL2.tableSize(limsgui.currentTable);

			// Add each row to the list under appropriate headings
			for (int i=0; i<result.size(); i++) {
				if (!currentColumn.equals(result.get(i).get(4))) {
					currentColumn = result.get(i).get(4);
					rowData = new String[]{currentColumn + ":", "heading"};
					model.addRow(rowData);
				}
				String toID = result.get(i).get(3);

				if (currentColumn.equals("id")) {
					// if the link is to an id number, also display the item's name
					Vector<String> columns = new Vector<String>();
					Vector<String> values = new Vector<String>();
					columns.add("id");
					values.add(toID);

					Vector<Vector<String>> linkToResult = SQL2.select(limsgui.currentTable, columns, values, tableSize, false);
					String name;
					if (linkToResult.size() > 0) {
						name = linkToResult.get(0).get(nameIdx);
					} else {
						name = "DOES NOT EXIST";
					}
					rowData = new String[]{toID + " (name = " + name + ")", "entry"};
				} else {
					// if the link is not to an id, just display the "linked to" value
					rowData = new String[]{toID, "entry"};
				}
				model.addRow(rowData);
			}
		}
	}

	public void displayLinkedIDs() {
		
		if (linkFromIDs.size() > 1) {
			// Can't display links from more than one item
			return;
		}
			
		// Get list of all linked to ids
		Vector<String> cols = new Vector<String>();
		Vector<String> vals = new Vector<String>();
		cols.add("tableName");
		cols.add("linkfrom");
		cols.add("tocolumn");
		vals.add(limsgui.currentTable);
		vals.add(linkFromIDs.get(0));
		vals.add("id");
		Vector<Vector<String>> idLinks = SQL2.select("META_LIMS_TABLE_4", cols, vals, false);
		if (idLinks == null || idLinks.size() == 0) {
			return;
		}		
		// Find the data item associated with each "linked to" id	
		Vector<String> idColVec = new Vector<String>();
		Vector<String> idValVec = new Vector<String>();
		
		for (Vector<String> vs : idLinks) {
			idValVec.add(vs.get(3));
			idColVec.add("id");
		}
		int tableSize = SQL2.tableSize(limsgui.currentTable);
		
		// Display in main table
		limsgui.fg4c.tableController.display(SQL2.selectOR(limsgui.currentTable, idColVec, idValVec, tableSize, true));
	}

	public void populateComboBox() {
		// Add items to columnChoiceBox		
		frame.columnChoiceBox.removeAllItems();
		Vector<String> comboBoxItems = SQL2.getColumns(limsgui.currentTable);
		if (comboBoxItems != null) {
			for (String s : comboBoxItems) {
				frame.comboBoxModel.addElement(s);
			}
		}
	}

	// Comparator to sort results from META_LIMS_TABLE_4 by "tocolumn"
	public class VectorComparator implements Comparator<List<String>> {
		public VectorComparator() {}

		@Override
		public int compare(List<String> thisRow, List<String> otherRow) {
			return thisRow.get(4).compareTo(otherRow.get(4));
		}
	}

	// Custom table renderer to give heading rows a grey background
	static class MyTableCellRenderer extends DefaultTableCellRenderer {

		private static final long serialVersionUID = 1L;

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			TableModel model = table.getModel();
			Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			if (isSelected) {
				c.setBackground(table.getSelectionBackground());
				c.setForeground(table.getSelectionForeground());
			} else if ((model.getValueAt(row, 1)).equals("heading")) {
				c.setBackground(Color.LIGHT_GRAY);
				c.setForeground(Color.BLACK);
			} else {
				c.setBackground(Color.WHITE);
				c.setForeground(Color.BLACK);
			}
			return c;
		}
	}
}