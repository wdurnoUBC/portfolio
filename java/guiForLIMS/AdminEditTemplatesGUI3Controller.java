package guiForLIMS;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import sqlInterface.SQL2;
import sqlInterface.SQLUtil;

public class AdminEditTemplatesGUI3Controller 
{
	
	AdminEditTemplatesGUI3 frame ;
	LIMSGUI1 limsgui ;
	private boolean ignoreComboBox; // Flag to make comboBox listener only called on user action
	private boolean ignoreLists;
	
	public AdminEditTemplatesGUI3Controller ( LIMSGUI1 parent, AdminEditTemplatesGUI3 aetg3 )
	{
		limsgui = parent ;
		frame = aetg3 ;
		ignoreComboBox = false;
		ignoreLists = false;
		
		// Button: Create
		frame.btnCreate.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
					Set<String> templates = SQL2.getTemplates( limsgui.currentTable ) ;
					String newTemplate = (String) frame.comboBox.getSelectedItem() ;
					
					if (newTemplate == null || newTemplate.trim().length() == 0) {
						JOptionPane.showMessageDialog(frame, "Please enter a template name", "Warning", JOptionPane.WARNING_MESSAGE);
					} else if ( SQL2.isReserved(newTemplate)) {
						JOptionPane.showMessageDialog(frame, "Template name is reserved", "Error", JOptionPane.ERROR_MESSAGE);
					} else if ( templates.contains(newTemplate) ) {
						JOptionPane.showMessageDialog(frame, "Template already exists", "Error", JOptionPane.ERROR_MESSAGE);
					} else {
						Vector<String> values = new Vector<String>() ;
						values.add( limsgui.currentTable ) ;
						values.add( newTemplate ) ;
						values.add( null ) ;
						SQL2.insert( "META_LIMS_TABLE_1" , values ) ;
						ignoreComboBox = true;
						frame.comboBox.setSelectedItem(newTemplate);
						updateComboBox();
						updateLists();
						frame.lblMessage.setText( newTemplate + " has been created") ;
					}
					limsgui.fg4c.tableController.refreshResults();
			} } ) ;
		
		// Button: Delete
		frame.btnDelete.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				Set<String> templates = SQL2.getTemplates( limsgui.currentTable ) ;
				String template = (String) frame.comboBox.getSelectedItem() ;
				if (SQL2.isReserved(template)) {
					JOptionPane.showMessageDialog(frame, template + " cannot be removed", "Warning", JOptionPane.WARNING_MESSAGE);
					return;
				}
				if ( templates.contains(template) )
				{
					// Delete template from META_LIMS_TABLE_1
					Vector<String> cols = new Vector<String>();
					Vector<String> vals = new Vector<String>();
					cols.add("tables");
					cols.add("template");
					vals.add(limsgui.currentTable);
					vals.add(template);
					
					SQL2.deleteWhere( "META_LIMS_TABLE_1" , cols, vals) ;
					frame.comboBox.setSelectedItem(null);
					updateComboBox();
					updateLists();
					frame.lblMessage.setText( template + " has been removed" ) ;
					
					// Change all data items using that template to use DefaultTemplate
					SQL2.setWhere( limsgui.currentTable, "template", "DefaultTemplate", "template", template);
				}
				else
				{
					JOptionPane.showMessageDialog(frame, template + " does not exist", "Error", JOptionPane.ERROR_MESSAGE);
				}
				limsgui.fg4c.tableController.refreshResults();
			}});
		
		// Combo box listener
		frame.comboBox.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				
				if (!ignoreComboBox) {
					updateLists();
				} else {
					ignoreComboBox = false;
				}
				frame.lblMessage.setText("");
			}
		}) ;
		
		// ListOut listener
		frame.listOut.addListSelectionListener( new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting() && !ignoreLists) {
					String template = (String) frame.comboBox.getSelectedItem() ;
					if( SQL2.getTemplates(limsgui.currentTable).contains( template ) ) 
					{
						String s = (String) frame.listOut.getSelectedValue() ;
						Vector<String> values = new Vector<String>() ;
						values.add( limsgui.currentTable ) ;
						values.add( template ) ;
						values.add( s ) ;
						SQL2.insert( "META_LIMS_TABLE_1" , values ) ;
						updateLists();
					}
				}
				
			}} )  ;
		
		// ListIn listener
		frame.listIn.addListSelectionListener( new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting() && !ignoreLists) {
					String template = (String) frame.comboBox.getSelectedItem() ;
					if( SQL2.getTemplates(limsgui.currentTable).contains( template ) ) 
					{
						String s = (String) frame.listIn.getSelectedValue() ;
						Vector<String> cols = new Vector<String>();
						Vector<String> vals = new Vector<String>();
						cols.add("tables");
						cols.add("template");
						cols.add("columns");
						vals.add(limsgui.currentTable);
						vals.add(template);
						vals.add(s);
						
						SQL2.deleteWhere( "META_LIMS_TABLE_1" , cols, vals) ;
						updateLists() ;
					}
				}
			}} )  ;
		
		update();
	}
	
	public void update() {
		updateCurrentTable();
		updateComboBox();
		updateLists();
		frame.lblMessage.setText("");
	}
	
	private void updateCurrentTable() {
		if ( limsgui.currentTable == null ) {
			frame.lblCurrentTable.setText( "PLEASE INITIALIZE" ) ;
		} else {
			frame.lblCurrentTable.setText( "Edit Templates in " + limsgui.currentTable ) ;
		}
	}
	
	private void updateComboBox() {
		String currentTemplate = (String)frame.comboBox.getSelectedItem();
		frame.comboBox.removeAllItems() ;
		
		Set<String> templates = SQL2.getTemplates( limsgui.currentTable ) ;
		if ( templates != null ) {
			
			for ( String s : templates ) {
				frame.comboBox.addItem( s ) ;
			}
		}
		ignoreComboBox = true;
		frame.comboBox.setSelectedItem(currentTemplate);
	}
	
	private void updateLists() {
		String currentTemplate = (String)frame.comboBox.getSelectedItem();
		ignoreLists = true;
		frame.listIn.setListData(new String[0]);
		frame.listOut.setListData(new String[0]);
		Set<String> templates = SQL2.getTemplates( limsgui.currentTable ) ;
		
		if (currentTemplate != null && SQLUtil.setContainsSQLString (templates , currentTemplate )) {
			Set<String> templateSet = SQL2.getTemplateColumns( limsgui.currentTable , currentTemplate) ;
			Vector<String> colVec = SQL2.getColumns( limsgui.currentTable ) ;

			if ( templateSet != null && colVec != null ) {
				List<String> diff = new ArrayList<String>() ;
				List<String> templateList = new ArrayList<String>();
				for ( String s : colVec ) {
					if( templateSet.contains( s ) ) {
						templateList.add ( s ) ;
					} else {
						diff.add ( s ) ;
					}
				}
				frame.listIn.setListData( templateList.toArray() ) ;
				frame.listOut.setListData( diff.toArray() ) ;
			}
		}
		ignoreLists = false;
		// This will make the window slower, but will update the main table's view automatically:
		//limsgui.fg4c.tableController.refreshResults();
	}
}