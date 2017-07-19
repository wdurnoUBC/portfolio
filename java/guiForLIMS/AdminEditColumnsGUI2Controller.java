package guiForLIMS;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.DefaultCellEditor;
import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

import sqlInterface.SQL2;

public class AdminEditColumnsGUI2Controller 
{
	
	private AdminEditColumnsGUI2 frame ;
	LIMSGUI1 limsgui ;
	private DefaultTableModel model;
	private boolean ignoreTableEvents ;
	private DefaultCellEditor tableEditor;
	private Vector<String> columns;
	
	public AdminEditColumnsGUI2Controller ( LIMSGUI1 parent, AdminEditColumnsGUI2 aecg2 )
	{
		limsgui = parent ;
		this.frame = aecg2;

		model = new DefaultTableModel();
		model.addColumn("columnName");
		model.setRowCount(0);
		frame.table.setModel(model);
		ignoreTableEvents = false;
		columns = new Vector<String>();
		
		update() ;
		
		// Button: Add Column
		frame.btnAddColumn.addActionListener( new ActionListener() { 
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String colName = frame.txtNewColumn.getText();
				if (isValidColumnName(colName, false)) {

					//update the current table
					boolean successful = SQL2.addColumn( limsgui.currentTable , colName ) ;
					if (!successful) {
						JOptionPane.showMessageDialog(frame, "Invalid column name", "Error", JOptionPane.ERROR_MESSAGE);
						return;
					}

					//update META_LIMS_TABLE_1
					Vector<String> temp = new Vector<String>() ;
					temp.add( limsgui.currentTable ) ;
					temp.add( null ) ;
					temp.add( frame.txtNewColumn.getText() ) ;
					SQL2.insert( "META_LIMS_TABLE_1" , temp ) ;
					update() ;

					limsgui.fg4c.tableController.refreshDisplay();
					frame.txtNewColumn.setText("");
				}
			}
		});
		
		// Button: Remove Column
		frame.btnRemoveColumn.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String column = (String)frame.table.getValueAt(frame.table.getSelectedRow(), 0);
				if ( SQL2.isReserved(column) )
				{
					frame.txtNewColumn.setText( column + " CANNOT BE REMOVED" ) ;
				}
				else
				{
					Set<String> colSet = SQL2.getColumnSet( limsgui.currentTable ) ;
					if ( colSet!=null )
					{
						System.out.println("colSet = " + colSet);
						System.out.println("column = " + column);
						if ( colSet.contains(column) )
						{
							Vector<String> cols = new Vector<String>();
							Vector<String> vals = new Vector<String>();
							cols.add("tables");
							cols.add("columns");
							vals.add(limsgui.currentTable);
							vals.add(column);
							SQL2.deleteWhere( "META_LIMS_TABLE_1" , cols, vals) ;
							
							SQL2.removeColumn(limsgui.currentTable, column);
							update() ;
							limsgui.fg4c.tableController.refreshResults();
						}
						else
						{
							frame.txtNewColumn.setText( column + " NOT FOUND" ) ;
						}
					}
					else
					{
						System.err.println( "AdminEditColumnsGUI1Controller error 2" ) ;
					}
				}
			}
	    });
		
		final InputVerifier iv = new ColumnVerifier();
		
		tableEditor = new DefaultCellEditor(new JTextField()) {
			private static final long serialVersionUID = 1L;

			{
		        getComponent().setInputVerifier(iv);
		    }

		    @Override
		    public boolean stopCellEditing() {
		        if (!iv.shouldYieldFocus(getComponent())) return false;
		        return super.stopCellEditing();
		    }

		    @Override
		    public JTextField getComponent() {
		        return (JTextField) super.getComponent();
		    }
		};
		
		frame.table.setCellEditor(tableEditor);
	
		// Update LIMS database when table is edited
		model.addTableModelListener(new TableModelListener() {
			@Override
			public void tableChanged(TableModelEvent e) {
				if (ignoreTableEvents) {
					return;
				}
				System.out.println("Row = " + e.getFirstRow());
				System.out.println("Value = " + model.getValueAt(e.getFirstRow(), 0));
				String newName = (String) model.getValueAt(e.getFirstRow(), 0);
				String oldName = columns.get(e.getFirstRow());
				SQL2.updateColumn(limsgui.currentTable, oldName, newName);
			}});
		
	}
	
	public void update()
	{
		ignoreTableEvents = true;
		frame.lblTable.setText("Edit Columns in " + limsgui.currentTable ) ;
		
		int rows = model.getRowCount(); 
		for(int i = rows - 1; i >=0; i--)
		{
		   model.removeRow(i); 
		}
		
		Vector<String> col = SQL2.getColumns( limsgui.currentTable ) ;
		if ( col != null )
		{
			for (int i=0; i<col.size(); i++) {
				model.addRow(new String[] {col.get(i)});
			}
		}
		columns = col;
		limsgui.fg4c.linkerController.populateComboBox();
		ignoreTableEvents = false;
	}
	
	// InputVerifier to make sure that only valid column names are entered
	private class ColumnVerifier extends InputVerifier {

		public boolean shouldYieldFocus(JComponent input) {
			boolean inputOK = verify(input);

			if (inputOK) {
				return true;
			}

			//Reinstall the input verifier.
			input.setInputVerifier(this);

			//Tell whoever called us that we don't want to yield focus.
			return false;
		}

		public boolean verify(JComponent input) {
			JTextField cell = (JTextField)input;
			String text = (String)cell.getText();
			return isValidColumnName(text, true);
		}

	}
	
	private boolean isValidColumnName(String name, boolean editing) {
		
		Pattern p = Pattern.compile("[^a-z0-9 _()-?%]", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(name);

		Set<String> colSet = SQL2.getColumnSet( limsgui.currentTable ) ;
		if ( colSet!=null ) {
			if (m.find()) {
				JOptionPane.showMessageDialog(frame, "Invalid characters in name", "Warning", JOptionPane.WARNING_MESSAGE);
				return false;
			} else if (SQL2.isReserved(name)) {
				JOptionPane.showMessageDialog(frame, name + " is a reserved name", "Warning", JOptionPane.WARNING_MESSAGE);
				return false;
			} else if (colSet.contains(name) && !editing) {
				JOptionPane.showMessageDialog(frame, "A column by that name already exists", "Warning", JOptionPane.WARNING_MESSAGE);
				return false;
			} else {
				return true;
			}
		} else {
			return false;
		}
	}
	
}
