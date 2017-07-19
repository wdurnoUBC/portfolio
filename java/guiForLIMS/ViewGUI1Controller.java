package guiForLIMS;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import sqlInterface.SQL2;

public class ViewGUI1Controller 
{
	public LIMSGUI1 limsgui ;
	ViewGUI1 frame ;
	
	public ViewGUI1Controller ( LIMSGUI1 parent ) 
	{
		limsgui = parent ;
		frame = new ViewGUI1() ;
		
		// button : all
		frame.btnAll.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				limsgui.view = "all" ;
				refreshDisplayView();
				hide() ;
			}} ) ;
		
		// button : union
		frame.btnUnion.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				limsgui.view = "union" ;
				refreshDisplayView();
				hide() ;
			}} ) ;
		
		// button : intersection
		frame.btnIntersection.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				limsgui.view = "intersection" ;
				refreshDisplayView();
				hide() ;
			}} ) ;
	}
	
	// Set focus to button of current view
	private void setBtnFocus() {
		String view = limsgui.view;
		if (view != null) {
			if (view.equals("all")) {
				frame.btnAll.requestFocusInWindow();
			} 
			else if (view.equals("union")) {
				frame.btnUnion.requestFocusInWindow();
			} 
			else if (view.equals("intersection")) {
				frame.btnIntersection.requestFocusInWindow();
			}
		}
	}
	
	private void refreshDisplayView() {
		FrontGUI3TableController tc = limsgui.fg4c.tableController;
		if (tc.lastResult != null && tc.lastResult.size() > 0) {
			
			if (SQL2.getColumns(limsgui.currentTable).size() != tc.lastResult.get(0).size()) {
				tc.refreshResults();
			} else {
				tc.refreshDisplay();
			}
		}
	}
	
	public void show()
	{
		frame.setVisible(true) ;
		setBtnFocus();
	}
	
	public void hide()
	{
		frame.setVisible(false) ;
	}
}
