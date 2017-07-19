package guiForLIMS;

import java.awt.Dimension;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.JCheckBox;
import javax.swing.BoxLayout;

public class AddItemGUI1 extends JFrame {

	public JTable table;
	public JComboBox comboBox ;
	public JButton btnAdd;
	public JButton btnCancel;
	public JButton btnAddFromFile;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JPanel bottom_panel;
	final DefaultComboBoxModel comboBoxModel;
	public JLabel lblNotSelected;
	private JPanel top_panel;
	public JCheckBox chckbxAddToFolder;
	
	/**
	 * Create the frame.
	 */
	public AddItemGUI1() {
		setTitle("Add Item(s)");
		//setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 601, 160);
		setMinimumSize(new Dimension(600, 160));
		setMaximumSize(new Dimension(1000, 160));
		
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		
		comboBoxModel = new DefaultComboBoxModel();
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
		
		top_panel = new JPanel();
		contentPane.add(top_panel);
		top_panel.setLayout(null);
		top_panel.setPreferredSize(new Dimension(100, 55));
		
		JLabel lblCurrentTemplate = new JLabel("Current Template:");
		lblCurrentTemplate.setBounds(6, 0, 113, 16);
		top_panel.add(lblCurrentTemplate);
		comboBox = new JComboBox(comboBoxModel);
		comboBox.setBounds(0, 23, 200, 27);
		top_panel.add(comboBox);
		
		lblNotSelected = new JLabel("NOT SELECTED");
		lblNotSelected.setBounds(131, -2, 104, 20);
		top_panel.add(lblNotSelected);
		
		btnAddFromFile = new JButton("Add From File...");
		btnAddFromFile.setBounds(453, 22, 131, 29);
		top_panel.add(btnAddFromFile);
		
		JScrollPane scrollPane = new JScrollPane();
		contentPane.add(scrollPane);
		scrollPane.setPreferredSize(new Dimension(100, 40));
		
		table = new JTable();
		scrollPane.setViewportView(table);
		
		bottom_panel = new JPanel();
		contentPane.add(bottom_panel);
		bottom_panel.setPreferredSize(new Dimension(100, 35));
		bottom_panel.setLayout(null);
		
		btnAdd = new JButton("Add");
		btnAdd.setBounds(0, 6, 88, 29);
		bottom_panel.add(btnAdd);
		
		btnCancel = new JButton("Cancel");
		btnCancel.setBounds(88, 6, 86, 29);
		bottom_panel.add(btnCancel);
		
		chckbxAddToFolder = new JCheckBox("Add To Folder");
		chckbxAddToFolder.setBounds(178, 7, 128, 23);
		bottom_panel.add(chckbxAddToFolder);
		chckbxAddToFolder.setSelected(true);
	}
}
