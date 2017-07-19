package guiForLIMS;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

public class FrontGUI4Linker extends JPanel {

	private static final long serialVersionUID = 1L;
	public JLabel lblLinker;
	public JLabel lblLinkFrom;
	public JTextField linkToField;
	public JLabel lblTo;
	public JLabel lblAlreadyLinked;
	public JButton btnCreateLink;
	private JLabel lblInColumn;
	public JComboBox columnChoiceBox;
	final DefaultComboBoxModel comboBoxModel;
	public JScrollPane  scrollPane;
	public JTable table;
	public JButton btnRemoveLink;
	public JPanel topPanel;
	public JLabel lblError;
	private JPanel bottomPanel;
	public JButton btnDisplayAllLinked;
	
	/**
	 * Create the panel.
	 */
	public FrontGUI4Linker() {
		setPreferredSize(new Dimension(250, 550));
		setLayout(new BorderLayout(0, 0));
		comboBoxModel = new DefaultComboBoxModel();
		
		lblLinker = new JLabel("Linker");
		lblLinker.setFont(new Font("Lucida Grande", Font.BOLD, 13));
		lblLinker.setBounds(101, 6, 55, 16);
		lblLinkFrom = new JLabel("Link from selected item");
		lblLinkFrom.setBounds(6, 34, 239, 16);
		linkToField = new JTextField();
		linkToField.setBounds(113, 62, 132, 28);
		linkToField.setColumns(10);
		lblTo = new JLabel("...to:");
		lblTo.setBounds(82, 68, 29, 16);
		lblAlreadyLinked = new JLabel("Already linked to:");
		lblAlreadyLinked.setBounds(6, 184, 111, 16);
		btnCreateLink = new JButton("Create Link");
		btnCreateLink.setBounds(112, 125, 115, 29);
		lblInColumn = new JLabel("...in this column:");
		lblInColumn.setBounds(6, 97, 111, 16);
		columnChoiceBox = new JComboBox(comboBoxModel);
		columnChoiceBox.setBounds(113, 93, 132, 27);
		
		topPanel = new JPanel();
		add(topPanel, BorderLayout.NORTH);
		topPanel.setLayout(null);
		topPanel.add(lblLinker);
		topPanel.add(lblLinkFrom);
		topPanel.add(linkToField);
		topPanel.add(lblTo);
		topPanel.add(lblAlreadyLinked);
		topPanel.add(btnCreateLink);
		topPanel.add(lblInColumn);
		topPanel.add(columnChoiceBox);
		topPanel.setPreferredSize(new Dimension(237, 200));
		
		lblError = new JLabel("");
		lblError.setForeground(Color.RED);
		lblError.setBounds(6, 154, 239, 16);
		topPanel.add(lblError);
		
		scrollPane = new JScrollPane();
		add(scrollPane, BorderLayout.CENTER);
		
		table = new JTable();
		table.setTableHeader(null);
		scrollPane.setViewportView(table);
//		scrollPane.setBorder(new SoftBevelBorder(10));
		
		bottomPanel = new JPanel();
		bottomPanel.setPreferredSize(new Dimension(100, 70));
		add(bottomPanel, BorderLayout.SOUTH);
		
		btnDisplayAllLinked = new JButton("Display all linked ids");
		bottomPanel.add(btnDisplayAllLinked);
		btnRemoveLink = new JButton("Remove Link");
		bottomPanel.add(btnRemoveLink);
	}
}