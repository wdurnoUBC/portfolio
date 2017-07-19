package guiForLIMS;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class AdminEditColumnsGUI2 extends JPanel {

	private static final long serialVersionUID = 1L;
	
	public JTextField txtNewColumn;
	public JButton btnAddColumn ;
	public JButton btnRemoveColumn ;
	public JTable table ;
	private JScrollPane scrollPane;
	private JPanel panel;
	private JPanel panel_1;
	public JLabel lblTable;

	/**
	 * Create the frame.
	 */
	public AdminEditColumnsGUI2() {
		setLayout(new BorderLayout(0, 0));
		
		scrollPane = new JScrollPane();
		add(scrollPane);
		
		table = new JTable();
		scrollPane.setViewportView(table);
		table.setTableHeader(null);
		
		panel = new JPanel();
		panel.setPreferredSize(new Dimension(100, 80));
		add(panel, BorderLayout.SOUTH);
		panel.setLayout(null);
		
		txtNewColumn = new JTextField();
		txtNewColumn.setBounds(7, 52, 303, 22);
		panel.add(txtNewColumn);
		txtNewColumn.setColumns(10);
		
		btnAddColumn = new JButton("Add Column");
		btnAddColumn.setBounds(322, 50, 122, 29);
		panel.add(btnAddColumn);
		
		btnRemoveColumn = new JButton("Remove Column");
		btnRemoveColumn.setBounds(7, 6, 146, 29);
		panel.add(btnRemoveColumn);
		
		panel_1 = new JPanel();
		panel_1.setPreferredSize(new Dimension(100, 60));
		add(panel_1, BorderLayout.NORTH);
		panel_1.setLayout(null);
		//setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		lblTable = new JLabel("Edit Columns");
		lblTable.setFont(new Font("Lucida Grande", Font.BOLD, 13));
		lblTable.setBounds(6, 5, 438, 16);
		panel_1.add(lblTable);
		lblTable.setHorizontalAlignment(SwingConstants.LEFT);
		
		JLabel lblSelectAColumn = new JLabel("Columns currently in table: (Edit names by double-clicking on them)");
		lblSelectAColumn.setBounds(6, 38, 438, 16);
		panel_1.add(lblSelectAColumn);
	}
}
