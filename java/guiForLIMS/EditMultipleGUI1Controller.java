package guiForLIMS;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;
import java.util.Vector;

import javax.swing.JOptionPane;

import sqlInterface.SQL2;

public class EditMultipleGUI1Controller {

	LIMSGUI1 limsgui ;
	EditMultipleGUI1 frame ;
	
	public EditMultipleGUI1Controller(LIMSGUI1 parent) {
		
		limsgui = parent;
		frame = new EditMultipleGUI1();
		
		
		ActionListener listener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String column = (String)frame.comboBox.getSelectedItem();
				String value = frame.txtValue.getText();

				if (column.equals("template") && !isValidTemplate(value)) {
					limsgui.eting1c.show();
					return;
				}

				// Get selected ids from tableController
				FrontGUI3TableController tc = limsgui.fg4c.tableController;
				int[] selectedRows = tc.table.getSelectedRows();

				if (selectedRows.length < 1) {
					frame.lblMessage.setText("Please select items to edit");
					return;
				}

				if (tc.model.findColumn(column) != -1) {
					int colIdx = tc.table.getColumn(column).getModelIndex();
					// For each item, set value
					for (int i : selectedRows) {
						tc.model.setValueAt(value, tc.table.convertRowIndexToModel(i), colIdx);
					}
				} else {
					// TODO: Be able to edit columns that aren't displayed
					JOptionPane.showMessageDialog(frame, "Column not in table", "Warning", JOptionPane.WARNING_MESSAGE);
				}

				frame.txtValue.setText("");
				frame.lblMessage.setText("");
			}};


			frame.btnSetForAll.addActionListener(listener);
			frame.txtValue.addActionListener(listener);
	}
	
	private void populateComboBox() {
		frame.comboBox.removeAllItems() ;
		Vector<String> columns = SQL2.getColumns(limsgui.currentTable);
		if ( columns != null ) {
			columns.remove("id");
			for( String s : columns )
				frame.comboBox.addItem( s ) ;
		}
		else
			System.err.println( "ERROR 1 EditMultipleGUI1Controller" ) ;
		
	}
	
	private boolean isValidTemplate(String value) {
		Set<String> templates = SQL2.getTemplates(limsgui.currentTable);
		if (!limsgui.fg4c.frame.btnAdmin.isEnabled()) {
			templates.remove("index");
		}
		return templates.contains(value);
	}
	
	public void show()
	{
		populateComboBox();
		frame.lblMessage.setText("");
		frame.setVisible(true) ;
	}
	
	public void hide()
	{
		frame.setVisible(false) ;
	}
	
}
