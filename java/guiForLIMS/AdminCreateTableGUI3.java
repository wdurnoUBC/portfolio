package guiForLIMS;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

public class AdminCreateTableGUI3 extends JPanel {

	public JButton btnCreate;
	public JScrollPane scrollPane;
	public JList list;
	public JButton btnDelete;
	public JTextField txtTableName;
	public JButton btnDuplicate;

	private static final long serialVersionUID = 1L;
	private JPanel createTopPanel;
	
	public AdminCreateTableGUI3() {

		setLayout(new BorderLayout(0, 0));

		JPanel createBottomPanel = new JPanel();
		add(createBottomPanel, BorderLayout.SOUTH);
		createBottomPanel.setPreferredSize(new Dimension(100, 70));
		createBottomPanel.setLayout(null);

		JLabel lblTableName = new JLabel("Table name:");
		lblTableName.setBounds(6, 6, 76, 16);
		createBottomPanel.add(lblTableName);

		txtTableName = new JTextField();
		txtTableName.setBounds(83, 2, 221, 28);
		createBottomPanel.add(txtTableName);
		txtTableName.setColumns(10);

		btnCreate = new JButton("Create");
		btnCreate.setBounds(6, 34, 90, 29);
		createBottomPanel.add(btnCreate);

		btnDelete = new JButton("Delete");
		btnDelete.setBounds(93, 34, 90, 29);
		createBottomPanel.add(btnDelete);
		
		btnDuplicate = new JButton("Duplicate");
		btnDuplicate.setBounds(180, 34, 104, 29);
		createBottomPanel.add(btnDuplicate);

		scrollPane = new JScrollPane();
		add(scrollPane, BorderLayout.CENTER);

		list = new JList();
		scrollPane.setViewportView(list);

		createTopPanel = new JPanel();
		createTopPanel.setPreferredSize(new Dimension(100, 50));
		add(createTopPanel, BorderLayout.NORTH);
		createTopPanel.setLayout(null);

		JLabel lblCreateAndDelete = new JLabel("Create and Delete Tables");
		lblCreateAndDelete.setBounds(6, 5, 170, 16);
		createTopPanel.add(lblCreateAndDelete);
		lblCreateAndDelete.setFont(new Font("Lucida Grande", Font.BOLD, 13));

		JLabel lblExistingTables = new JLabel("Existing tables:");
		lblExistingTables.setBounds(6, 28, 97, 16);
		createTopPanel.add(lblExistingTables);
		
	}
}
