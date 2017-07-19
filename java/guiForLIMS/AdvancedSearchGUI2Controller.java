package guiForLIMS;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import sqlInterface.SQL2;

public class AdvancedSearchGUI2Controller {
	
	AdvancedSearchGUI2 frame;
	LIMSGUI1 limsgui;
	
	public AdvancedSearchGUI2Controller(LIMSGUI1 parent) {
		
		limsgui = parent;
		frame = new AdvancedSearchGUI2();
		
		frame.btnSearch.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				search();
			}} );
		
	}

	// Perform advanced search
	private void search() {
		
		String term1 = frame.textField_1.getText().trim();
		String term2 = frame.textField_2.getText().trim();
		String term3 = frame.textField_3.getText().trim();
		
		String col1 = (String)frame.comboBox_1.getSelectedItem();
		String col2 = (String)frame.comboBox_2.getSelectedItem();
		String col3 = (String)frame.comboBox_3.getSelectedItem();
		
		Boolean exact1 = frame.chckbxExact_1.isSelected();
		Boolean exact2 = frame.chckbxExact_2.isSelected();
		Boolean exact3 = frame.chckbxExact_3.isSelected();
		
		String log1 = (String)frame.comboBox_log1.getSelectedItem();
		String log2 = (String)frame.comboBox_log2.getSelectedItem();
		
		String statement = "SELECT * FROM `" + limsgui.currentTable + "` WHERE ";
		Vector<String> terms = new Vector<String>();

		if (term1.length() > 0) {
			statement += formStatement(terms, term1, col1, exact1);

			if (term2.length() > 0) {
				statement += " " + log1 + " (" + formStatement(terms, term2, col2, exact2);

				if (term3.length() > 0) {
					statement += " " + log2 + " " + formStatement(terms, term3, col3, exact3) + ")";
				} else {
					statement += ")";
				}
				
			} else if (term3.length() > 0) {
				statement += " " + log2 + " " + formStatement(terms, term3, col3, exact3);
			}

		} else if (term2.length() > 0) {
			statement += formStatement(terms, term2, col2, exact2);

			if (term3.length() > 0) {
				statement += " " + log2 + " " + formStatement(terms, term3, col3, exact3);
			}

		} else if (term3.length() > 0) {
			statement += formStatement(terms, term3, col3, exact3);

		} else {
			frame.lblMessage.setVisible(true);
			return;
		}
		frame.lblMessage.setVisible(false);
		limsgui.fg4c.tableController.display(SQL2.selectStatement(limsgui.currentTable, statement, terms, true));
	}
	
	// Form a "where" statement for a mySQL "SELECT * FROM table WHERE column=?" or "column LIKE ?"
	private String formStatement(Vector<String> terms, String term, String column, Boolean exact) {
		String stmt = new String();
		String stmtEnd = new String();
		
		// Substring vs wholestring
		if (exact) {
			stmtEnd = "=?";
		} else {
			stmtEnd = " LIKE ?";
		}
		
		// One column vs any column
		if (!column.equals("(Any column)")) {
			stmt = "`" + column + "`" + stmtEnd;
			terms.add(formatTerm(term, exact));
		} else {
			Vector<String> allCols = SQL2.getColumns(limsgui.currentTable);
			stmt += "(`" + allCols.get(0) + "`" + stmtEnd + "";
			terms.add(formatTerm(term, exact));
			
			for (int i=1; i<allCols.size(); i++) {
				stmt += " OR `" + allCols.get(i) + "`" + stmtEnd + "";
				terms.add(formatTerm(term, exact));
			}
			stmt += ")";
		}
		return stmt;
	}
	
	// Adds % symbols for inexact search terms
	private String formatTerm(String term, boolean exact) {
		if (exact) {
			return term;
		} else {
			return "%" + term + "%";
		}
	}
	
	// Populate all the column choice comboboxes
	private void populateColumnChoices() {
		Vector<String> columns = SQL2.getColumns(limsgui.currentTable);
		
		frame.comboBox_1.addItem("(Any column)");
		frame.comboBox_2.addItem("(Any column)");
		frame.comboBox_3.addItem("(Any column)");
		
		for (String s : columns) {
			frame.comboBox_1.addItem(s);
			frame.comboBox_2.addItem(s);
			frame.comboBox_3.addItem(s);
		}
	}
	
	public void show()
	{
		frame.lblMessage.setVisible(false);
		populateColumnChoices();
		frame.setVisible(true) ;
	}
	
	public void hide()
	{
		frame.setVisible(false) ;
	}
}