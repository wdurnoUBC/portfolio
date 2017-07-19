package guiForLIMS;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;

public class AdminEditTemplatesGUI3 extends JPanel {
	
	private static final long serialVersionUID = 1L;
	public JComboBox comboBox;
	public DefaultComboBoxModel comboBoxModel;
	public JLabel lblTemplate;
	public JButton btnCreate;
	public JButton btnDelete;
	public JLabel lblMessage;
	public JList listIn;
	public JList listOut;
	private JPanel panel_2;
	private JLabel lblIn;
	private JLabel lblOut;
	public JLabel lblCurrentTable;

	public AdminEditTemplatesGUI3() {
		setLayout(new BorderLayout(0, 0));
		
		JPanel panel = new JPanel();
		add(panel, BorderLayout.NORTH);
		panel.setPreferredSize(new Dimension(200, 125));
		panel.setLayout(null);
		
		lblCurrentTable = new JLabel("Edit Templates");
		lblCurrentTable.setFont(new Font("Lucida Grande", Font.BOLD, 13));
		lblCurrentTable.setBounds(6, 6, 438, 16);
		panel.add(lblCurrentTable);
		
		comboBoxModel = new DefaultComboBoxModel();
		
		lblTemplate = new JLabel("Template:");
		lblTemplate.setBounds(6, 34, 62, 16);
		panel.add(lblTemplate);
		comboBox = new JComboBox(comboBoxModel);
		comboBox.setBounds(80, 28, 152, 27);
		comboBox.setEditable(true);
		panel.add(comboBox);
		
		btnCreate = new JButton("Create Template");
		btnCreate.setBounds(6, 73, 146, 29);
		panel.add(btnCreate);
		
		btnDelete = new JButton("Delete Template");
		btnDelete.setBounds(152, 73, 146, 29);
		panel.add(btnDelete);
		
		lblMessage = new JLabel("");
		lblMessage.setForeground(Color.BLACK);
		lblMessage.setBounds(238, 30, 289, 20);
		panel.add(lblMessage);
		
		JScrollPane scrollPane = new JScrollPane();
		add(scrollPane);
		
		JPanel panel_1 = new JPanel();
		scrollPane.setViewportView(panel_1);
		panel_1.setLayout(new GridLayout(1, 0, 0, 0));
		
		listIn = new JList();
		listIn.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		panel_1.add(listIn);
		
		listOut = new JList();
		listOut.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		panel_1.add(listOut);
		
		panel_2 = new JPanel();
		scrollPane.setColumnHeaderView(panel_2);
		panel_2.setLayout(new GridLayout(0, 2, 0, 0));
		
		lblIn = new JLabel("IN");
		lblIn.setHorizontalAlignment(SwingConstants.CENTER);
		panel_2.add(lblIn);
		
		lblOut = new JLabel("OUT");
		lblOut.setHorizontalAlignment(SwingConstants.CENTER);
		panel_2.add(lblOut);
	}
}
