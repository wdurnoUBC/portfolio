package guiForLIMS;

import java.awt.Dimension;
import java.awt.Font;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class AdminSwitchTableGUI2 extends JPanel {
	
	private static final long serialVersionUID = 1L;
	
	public JComboBox comboBox ;
	public JLabel lblMessage;
	private JLabel lblCurrentTable;
	final DefaultComboBoxModel comboBoxModel;
	
	public AdminSwitchTableGUI2() {

		setPreferredSize(new Dimension(460, 80));
		setMaximumSize(new Dimension(3000, 80));
		
		comboBoxModel = new DefaultComboBoxModel();
		setLayout(null);
		
		JLabel lblSwitchTables = new JLabel("Switch Table");
		lblSwitchTables.setBounds(9, 6, 85, 16);
		add(lblSwitchTables);
		lblSwitchTables.setFont(new Font("Lucida Grande", Font.BOLD, 13));
		
		lblCurrentTable = new JLabel("Current table:");
		lblCurrentTable.setBounds(9, 36, 86, 16);
		add(lblCurrentTable);
		comboBox = new JComboBox(comboBoxModel);
		comboBox.setBounds(107, 32, 271, 27);
		add(comboBox);
		
		lblMessage = new JLabel("");
		lblMessage.setBounds(106, 80, 0, 0);
		add(lblMessage);
	}
	
}
