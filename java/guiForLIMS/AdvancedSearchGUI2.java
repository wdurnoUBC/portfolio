package guiForLIMS;

import java.awt.Color;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class AdvancedSearchGUI2 extends JFrame {
	private static final long serialVersionUID = 1L;
	
	public JButton btnSearch;
	public JComboBox comboBox_1;
	public JComboBox comboBox_2;
	public JComboBox comboBox_3;
	public JComboBox comboBox_log1;
	public JComboBox comboBox_log2;
	public JTextField textField_1;
	public JTextField textField_2;
	public JTextField textField_3;
	public JCheckBox chckbxExact_1;
	public JCheckBox chckbxExact_2;
	public JCheckBox chckbxExact_3;
	public JLabel lblMessage;
	
	public AdvancedSearchGUI2() {
		setTitle("Advanced Search");
		setBounds(100, 100,706, 209);
		setResizable(false);
		getContentPane().setLayout(null);
		
		comboBox_1 = new JComboBox();
		comboBox_1.setBounds(409, 33, 197, 27);
		getContentPane().add(comboBox_1);
		
		comboBox_2 = new JComboBox();
		comboBox_2.setBounds(409, 72, 197, 27);
		getContentPane().add(comboBox_2);
		
		comboBox_3 = new JComboBox();
		comboBox_3.setBounds(409, 113, 197, 27);
		getContentPane().add(comboBox_3);
		
		textField_1 = new JTextField();
		textField_1.setBounds(109, 31, 257, 28);
		getContentPane().add(textField_1);
		textField_1.setColumns(10);
		
		textField_2 = new JTextField();
		textField_2.setBounds(109, 71, 257, 28);
		getContentPane().add(textField_2);
		textField_2.setColumns(10);
		
		textField_3 = new JTextField();
		textField_3.setBounds(109, 111, 257, 28);
		getContentPane().add(textField_3);
		textField_3.setColumns(10);
		
		comboBox_log1 = new JComboBox();
		comboBox_log1.setBounds(6, 73, 85, 27);
		comboBox_log1.addItem("AND");
		comboBox_log1.addItem("OR");
		getContentPane().add(comboBox_log1);
		
		comboBox_log2 = new JComboBox();
		comboBox_log2.setBounds(6, 112, 85, 27);
		comboBox_log2.addItem("AND");
		comboBox_log2.addItem("OR");
		getContentPane().add(comboBox_log2);
		
		JLabel label = new JLabel("(");
		label.setFont(new Font("Lucida Grande", Font.PLAIN, 24));
		label.setBounds(95, 67, 12, 32);
		getContentPane().add(label);
		
		JLabel label_1 = new JLabel(")");
		label_1.setFont(new Font("Lucida Grande", Font.PLAIN, 24));
		label_1.setBounds(680, 108, 12, 32);
		getContentPane().add(label_1);
		
		JLabel lblIn = new JLabel("in");
		lblIn.setBounds(378, 37, 19, 16);
		getContentPane().add(lblIn);
		
		JLabel label_2 = new JLabel("in");
		label_2.setBounds(378, 76, 19, 16);
		getContentPane().add(label_2);
		
		JLabel label_3 = new JLabel("in");
		label_3.setBounds(378, 115, 19, 16);
		getContentPane().add(label_3);
		
		btnSearch = new JButton("Search");
		btnSearch.setBounds(575, 152, 117, 29);
		getContentPane().add(btnSearch);
		
		JLabel lblSearchFor = new JLabel("Search for...");
		lblSearchFor.setBounds(6, 6, 103, 16);
		getContentPane().add(lblSearchFor);
		
		chckbxExact_1 = new JCheckBox("Exact");
		chckbxExact_1.setBounds(610, 33, 72, 23);
		getContentPane().add(chckbxExact_1);
		
		chckbxExact_2 = new JCheckBox("Exact");
		chckbxExact_2.setBounds(610, 72, 72, 23);
		getContentPane().add(chckbxExact_2);
		
		chckbxExact_3 = new JCheckBox("Exact");
		chckbxExact_3.setBounds(610, 113, 72, 23);
		getContentPane().add(chckbxExact_3);
		
		lblMessage = new JLabel("Please enter a search term");
		lblMessage.setForeground(Color.RED);
		lblMessage.setBounds(16, 157, 189, 16);
		getContentPane().add(lblMessage);
	}
}
