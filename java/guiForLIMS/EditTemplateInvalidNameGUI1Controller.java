package guiForLIMS;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;

import sqlInterface.SQL2;

public class EditTemplateInvalidNameGUI1Controller {
	
	public LIMSGUI1 limsgui ;
	private EditTemplateInvalidNameGUI1 frame ;

	public EditTemplateInvalidNameGUI1Controller ( LIMSGUI1 parent ) {
		
		limsgui = parent;
		frame = new EditTemplateInvalidNameGUI1();
		
		frame.list.setEnabled(false); // Prevents selection and editing but causes entries to be greyed out... maybe a better solution
		
		frame.btnOkay.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				hide() ;
			}
	    });
		
	}
	
	public void updateList() {
		Set<String> templates = SQL2.getTemplates(limsgui.currentTable);
		if (!limsgui.fg4c.frame.btnAdmin.isEnabled()) {
			templates.remove("index");
		}
		frame.list.setListData(templates.toArray());
	}
	
	public void show()
	{
		updateList();
		frame.setVisible(true) ;
	}
	
	public void hide()
	{
		frame.setVisible(false) ;
	}
}
