package guiForLIMS;

import java.awt.BorderLayout; 
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.BoxLayout;

public class FrontGUI5 extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	public JTable table;
	private JPanel panel;
	public FrontGUI4Linker linkerPanel;
	public JButton btnView;
	public JTextField txtSearchAll;
	public JButton btnAddItem;
//	public JButton btnDownload; // REFACTOR: This feature has been moved to a right-click menu
	public JButton btnRefresh;
	public JButton btnFastSearch;
//	public JButton btnRemoveItem; // REFACTOR: This feature has been moved to a right-click menu
	public JButton btnSearchindex;
	public JButton btnLogOut;
	public JButton btnShowhideLinker;
	public JComboBox comboBox;
	public DefaultComboBoxModel comboBoxModel;
	public JButton btnAdmin;
	public JCheckBox chckbxEnableEditing;
//	public JButton btnEditMultiple; // REFACTOR: This feature has been moved to a right-click menu
	JCheckBox chckbxExact;
	private Component verticalStrut;
//	public JButton btnOpenFolder; // REFACTOR: This feature has been moved to a right-click menu
	public JButton btnAdvancedSearch;
	private Component verticalStrut_1;
	public JButton btnExport;
	public JButton btnBack;
//	public JButton btnUpdateHere; // REFACTOR: This feature has been moved to a right-click menu
	
	/**
	 * Create the frame.
	 */
	public FrontGUI5() {
		setTitle("mLIMS");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 807, 668);
		setMinimumSize(new Dimension(500, 636));
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		
		JScrollPane scrollPane = new JScrollPane();
		
		table = new JTable();
		scrollPane.setViewportView(table);
		
		panel = new JPanel();
		contentPane.setLayout(new BorderLayout(0, 0));
		
		contentPane.add(panel, BorderLayout.WEST);
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setPreferredSize(new Dimension(200, 700));
		
		btnView = new JButton("View");
		btnView.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(btnView);
		
		txtSearchAll = new JTextField();
		panel.add(txtSearchAll);
		txtSearchAll.setColumns(11);
		txtSearchAll.setMaximumSize(new Dimension(200, 30));
		
		comboBoxModel = new DefaultComboBoxModel();
		comboBox = new JComboBox(comboBoxModel);
		comboBox.setMaximumSize(new Dimension(200, 30));
		panel.add(comboBox);
		
		chckbxExact = new JCheckBox("Exact Match");
		chckbxExact.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(chckbxExact);
		
		btnFastSearch = new JButton("Search");
		btnFastSearch.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(btnFastSearch);
		
		verticalStrut = Box.createVerticalStrut(20);
		panel.add(verticalStrut);
		
		btnBack = new JButton("Back");
		btnBack.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(btnBack);
		
		btnSearchindex = new JButton("Return to Index");
		btnSearchindex.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(btnSearchindex);
		
		btnAdvancedSearch = new JButton("Advanced Search");
		btnAdvancedSearch.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(btnAdvancedSearch);
		
		btnAddItem = new JButton("Add Item(s)");
		btnAddItem.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(btnAddItem);
		
		/* // REFACTOR: This feature has been moved to a right-click menu
		btnRemoveItem = new JButton("Remove Item");
		btnRemoveItem.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(btnRemoveItem);
		*/
		
		/* // REFACTOR: This feature has been moved to a right-click menu
		btnEditMultiple = new JButton("Edit Multiple");
		btnEditMultiple.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(btnEditMultiple);
		*/
		
		btnRefresh = new JButton("Refresh");
		btnRefresh.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(btnRefresh);
		
		/* // REFACTOR: This feature has been moved to a right-click menu
		btnDownload = new JButton("Download");
		btnDownload.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(btnDownload);
		*/
		
		btnExport = new JButton("Export Table");
		btnExport.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(btnExport);
		
		/* // REFACTOR: This feature has been moved to a right-click menu
		btnOpenFolder = new JButton("Open Parent Folder");
		btnOpenFolder.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(btnOpenFolder);
		*/
		
		btnShowhideLinker = new JButton("Show/Hide Linker");
		btnShowhideLinker.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(btnShowhideLinker);
		
		btnAdmin = new JButton("Admin");
		btnAdmin.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(btnAdmin);
		
		btnLogOut = new JButton("Log Out");
		btnLogOut.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(btnLogOut);
		
		/* // REFACTOR: This feature has been moved to a right-click menu
		btnUpdateHere = new JButton("Update Here");
		btnUpdateHere.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
			}
		});
		btnUpdateHere.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(btnUpdateHere);
		*/
		
		verticalStrut_1 = Box.createVerticalStrut(20);
		panel.add(verticalStrut_1);
		
		chckbxEnableEditing = new JCheckBox("Enable Editing");
		chckbxEnableEditing.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(chckbxEnableEditing);
		
		contentPane.add(scrollPane, BorderLayout.CENTER);
		
		linkerPanel = new FrontGUI4Linker();
		contentPane.add(linkerPanel, BorderLayout.EAST);
		
	}

}