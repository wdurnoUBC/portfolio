package guiForLIMS;

import java.awt.Color;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class EditMultipleGUI1 extends JFrame {

	private static final long serialVersionUID = 1L;
	public JTextField txtValue;
	public JButton btnSetForAll;
	public JComboBox comboBox;
	public JLabel lblMessage;
	
	public EditMultipleGUI1() {
		setResizable(false);
		setBounds(100, 100, 397, 178);
		setTitle("Edit Multiple Items");
		getContentPane().setLayout(null);
		
		comboBox = new JComboBox();
		comboBox.setBounds(79, 13, 234, 27);
		getContentPane().add(comboBox);
		
		txtValue = new JTextField();
		txtValue.setBounds(79, 52, 306, 28);
		getContentPane().add(txtValue);
		txtValue.setColumns(10);
		
		JLabel lblColumn = new JLabel("Column:");
		lblColumn.setBounds(6, 17, 61, 16);
		getContentPane().add(lblColumn);
		
		JLabel lblValue = new JLabel("Value:");
		lblValue.setBounds(19, 58, 48, 16);
		getContentPane().add(lblValue);
		
		btnSetForAll = new JButton("Set for all selected items");
		btnSetForAll.setBounds(99, 121, 199, 29);
		getContentPane().add(btnSetForAll);
		
		lblMessage = new JLabel("");
		lblMessage.setForeground(Color.RED);
		lblMessage.setBounds(6, 93, 379, 16);
		getContentPane().add(lblMessage);
	}
}
