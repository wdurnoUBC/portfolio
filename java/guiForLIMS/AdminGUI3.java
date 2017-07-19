package guiForLIMS;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JTextField;
import java.awt.Font;

public class AdminGUI3 extends JFrame {

	private static final long serialVersionUID = 1L;
	
	public JTabbedPane tabbedPane;
	public AdminEditUsersGUI2 aeug2;
	public AdminEditColumnsGUI2 aecg2;
	public AdminEditTemplatesGUI3 aetg3;
	public JPanel tablesPanel;
	public AdminSwitchTableGUI2 astg2;
	public AdminCreateTableGUI3 actg3;
	public JPanel advancedPanel;
	public AdminInitializeLIMSGUI2 ailg2;
	public AdminPopLIMSGUI3 aplg3;
	private JPanel panel;
	private JLabel lblChangeDatabaseLocation;
	public JButton btnReconfigureDatabase;
	private JLabel lblNewLabel;
	public JTextField txtDataLocation;
	public JButton btnUpdateDataLocation;

	public AdminGUI3() {
		setTitle("Admin");
		setMinimumSize(new Dimension(539, 417));
		setBounds(100, 100, 539, 417);
		
		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		getContentPane().add(tabbedPane, BorderLayout.CENTER);
		
		aeug2 = new AdminEditUsersGUI2();
		tabbedPane.addTab("Users", null, aeug2, null);
		
		tablesPanel = new JPanel();
		tablesPanel.setLayout(new BoxLayout(tablesPanel, BoxLayout.Y_AXIS));
		astg2 = new AdminSwitchTableGUI2();
		astg2.setBorder(null);
		actg3 = new AdminCreateTableGUI3();
		tablesPanel.add(astg2);
		JSeparator separator = new JSeparator();
		separator.setForeground(Color.BLACK);
		tablesPanel.add(separator);
		tablesPanel.add(actg3);
		tabbedPane.addTab("Tables", null, tablesPanel, null);
		
		aecg2 = new AdminEditColumnsGUI2();
		tabbedPane.addTab("Columns", null, aecg2, null);		
		
		aetg3 = new AdminEditTemplatesGUI3();
		tabbedPane.addTab("Templates", null, aetg3, null);
		
		advancedPanel = new JPanel();
		advancedPanel.setLayout(new BoxLayout(advancedPanel, BoxLayout.Y_AXIS));
		ailg2 = new AdminInitializeLIMSGUI2();
		aplg3 = new AdminPopLIMSGUI3();
		aplg3.btnPopulate.setLocation(414, 51);
		separator.setForeground(Color.BLACK);
		advancedPanel.add(ailg2);
		JSeparator separator2 = new JSeparator();
		advancedPanel.add(separator2);
		separator2.setForeground(Color.BLACK);
		advancedPanel.add(aplg3);
		JSeparator separator_1 = new JSeparator();
		advancedPanel.add(separator_1);
		separator_1.setForeground(Color.BLACK);
		tabbedPane.addTab("Advanced", null, advancedPanel, null);
		aplg3.setLocation(75, 20);
		
		panel = new JPanel();
		advancedPanel.add(panel);
		panel.setLayout(null);
		panel.setPreferredSize(new Dimension(500, 140));
		
		lblChangeDatabaseLocation = new JLabel("Change database location, username, or password:");
		lblChangeDatabaseLocation.setBounds(6, 46, 320, 16);
		panel.add(lblChangeDatabaseLocation);
		
		btnReconfigureDatabase = new JButton("Reconfigure database");
		btnReconfigureDatabase.setBounds(333, 41, 179, 29);
		panel.add(btnReconfigureDatabase);
		
		lblNewLabel = new JLabel("Change location of data:");
		lblNewLabel.setBounds(6, 92, 165, 16);
		panel.add(lblNewLabel);
		
		txtDataLocation = new JTextField();
		txtDataLocation.setBounds(171, 86, 241, 28);
		panel.add(txtDataLocation);
		txtDataLocation.setColumns(10);
		
		btnUpdateDataLocation = new JButton("Update");
		btnUpdateDataLocation.setBounds(424, 87, 88, 29);
		panel.add(btnUpdateDataLocation);
		
		JLabel lblChangeConfiguration = new JLabel("Change Configuration");
		lblChangeConfiguration.setFont(new Font("Lucida Grande", Font.BOLD, 13));
		lblChangeConfiguration.setBounds(6, 18, 165, 16);
		panel.add(lblChangeConfiguration);
		
	}
}
