package guiForLIMS;

import java.awt.Color;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class AdminEditUsersGUI2 extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public JComboBox comboBox ;
	public JButton btnAdd ;
	public JButton btnRemove ;
	public JCheckBox chckbxAdminStatus ;
	public JTextField txtPassword;
	private JLabel lblEditUsers;
	public JLabel lblErrorMessage;
	private JLabel lblName;
	private JLabel lblPassword;
	public JCheckBox chckbxAllowDeletion;
	
	/**
	 * Create the frame.
	 */
	public AdminEditUsersGUI2() {
		setLayout(null);
		
		comboBox = new JComboBox();
		comboBox.setEditable(true);
		comboBox.setBounds(111, 49, 173, 27);
		add(comboBox);
		
		txtPassword = new JTextField();
		txtPassword.setBounds(111, 88, 173, 28);
		add(txtPassword);
		txtPassword.setColumns(10);
		
		btnAdd = new JButton("Add/Update");
		btnAdd.setBounds(38, 163, 116, 29);
		add(btnAdd);
		
		btnRemove = new JButton("Remove");
		btnRemove.setBounds(166, 163, 93, 29);
		add(btnRemove);
		
		chckbxAdminStatus = new JCheckBox("Admin Status");
		chckbxAdminStatus.setBounds(38, 128, 116, 23);
		add(chckbxAdminStatus);
		
		lblEditUsers = new JLabel("Edit Users");
		lblEditUsers.setFont(new Font("Lucida Grande", Font.BOLD, 13));
		lblEditUsers.setBounds(6, 6, 116, 16);
		add(lblEditUsers);
		
		lblErrorMessage = new JLabel("");
		lblErrorMessage.setForeground(Color.RED);
		lblErrorMessage.setBounds(38, 204, 406, 16);
		add(lblErrorMessage);
		
		lblName = new JLabel("Name:");
		lblName.setBounds(38, 55, 49, 16);
		add(lblName);
		
		lblPassword = new JLabel("Password:");
		lblPassword.setBounds(38, 94, 75, 16);
		add(lblPassword);
		
		chckbxAllowDeletion = new JCheckBox("Allow users to delete multiple items at once");
		chckbxAllowDeletion.setSelected(true);
		chckbxAllowDeletion.setBounds(6, 287, 316, 23);
		add(chckbxAllowDeletion);
	}
}
