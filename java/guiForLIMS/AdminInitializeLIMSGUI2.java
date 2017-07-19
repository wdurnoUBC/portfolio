package guiForLIMS;

import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class AdminInitializeLIMSGUI2 extends JPanel {

	private static final long serialVersionUID = 1L;
	public JTextField txtAdminName;
	public JTextField txtAdminPass;
	public JButton btnInitialize ;
	private JLabel lblInitializeMlims;

	/**
	 * Create the frame.
	 */
	public AdminInitializeLIMSGUI2() {
		setPreferredSize(new Dimension(440, 70));
		setMaximumSize(new Dimension(3000, 70));
		setLayout(null);
		
		lblInitializeMlims = new JLabel("Initialize mLIMS");
		lblInitializeMlims.setBounds(6, 6, 124, 16);
		add(lblInitializeMlims);
		lblInitializeMlims.setFont(new Font("Lucida Grande", Font.BOLD, 13));
		
		txtAdminName = new JTextField();
		txtAdminName.setBounds(6, 28, 159, 28);
		add(txtAdminName);
		txtAdminName.setText("Admin Name");
		txtAdminName.setColumns(10);
		
		txtAdminPass = new JTextField();
		txtAdminPass.setBounds(174, 28, 149, 28);
		add(txtAdminPass);
		txtAdminPass.setText("Admin Pass");
		txtAdminPass.setColumns(10);
		
		btnInitialize = new JButton("Initialize");
		btnInitialize.setBounds(323, 29, 98, 28);
		add(btnInitialize);
	}

}