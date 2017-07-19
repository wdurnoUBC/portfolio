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

public class AdminEditIndexGUI2 extends JPanel {
	private static final long serialVersionUID = 1L;
	
	public JList list;
	public JButton btnAdd;
	public JButton btnRemove;
	public JTextField txtIndexItem;
	private JPanel panel_1;
	public JLabel lblMessage;
	public JLabel lblTitle;
	
	public AdminEditIndexGUI2() {
		setLayout(new BorderLayout(0, 0));
		
		JScrollPane scrollPane = new JScrollPane();
		add(scrollPane, BorderLayout.CENTER);
		
		list = new JList();
		scrollPane.setViewportView(list);
		
		JPanel panel = new JPanel();
		panel.setPreferredSize(new Dimension(100, 80));
		add(panel, BorderLayout.NORTH);
		panel.setLayout(null);
		
		txtIndexItem = new JTextField();
		txtIndexItem.setBounds(6, 34, 186, 28);
		txtIndexItem.setText("Index item");
		panel.add(txtIndexItem);
		txtIndexItem.setColumns(10);
		
		btnAdd = new JButton("Add");
		btnAdd.setBounds(204, 35, 75, 29);
		panel.add(btnAdd);
		
		btnRemove = new JButton("Remove");
		btnRemove.setBounds(278, 35, 93, 29);
		panel.add(btnRemove);
		
		lblTitle = new JLabel("Add or Remove Index Items");
		lblTitle.setFont(new Font("Lucida Grande", Font.BOLD, 13));
		lblTitle.setBounds(6, 6, 438, 16);
		panel.add(lblTitle);
		
		panel_1 = new JPanel();
		panel_1.setPreferredSize(new Dimension(100, 25));
		add(panel_1, BorderLayout.SOUTH);
		
		lblMessage = new JLabel("");
		panel_1.add(lblMessage);
	}

}
