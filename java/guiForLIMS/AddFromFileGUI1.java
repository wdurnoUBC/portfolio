package guiForLIMS;

import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JCheckBox;

public class AddFromFileGUI1 extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	public JTextField txtSelectedFile;
	public JComboBox comboBox;
	public JButton btnChooseFile;
	public JCheckBox chckbxAddToFolder;
	public JButton btnImport;
	public JButton btnCancel;

	/**
	 * Create the frame.
	 */
	public AddFromFileGUI1() {
		setResizable(false);
		setTitle("Add From File");
		//setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 473, 238);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));
		
		JPanel templatePanel = new JPanel();
		contentPane.add(templatePanel);
		templatePanel.setLayout(null);
		
		comboBox = new JComboBox();
		comboBox.setBounds(6, 35, 213, 27);
		templatePanel.add(comboBox);
		
		JLabel lblSelectATemplate = new JLabel("Select a template for your data:");
		lblSelectATemplate.setBounds(6, 7, 213, 16);
		templatePanel.add(lblSelectATemplate);
		
		JLabel lblSelectTheFile = new JLabel("Select the CSV or TSV file to import data from:");
		lblSelectTheFile.setBounds(6, 76, 228, 16);
		templatePanel.add(lblSelectTheFile);
		
		txtSelectedFile = new JTextField();
		txtSelectedFile.setBounds(6, 104, 336, 28);
		templatePanel.add(txtSelectedFile);
		txtSelectedFile.setColumns(10);
		
		btnChooseFile = new JButton("Choose File");
		btnChooseFile.setBounds(339, 105, 117, 29);
		templatePanel.add(btnChooseFile);
		
		chckbxAddToFolder = new JCheckBox("Add to Folder");
		chckbxAddToFolder.setBounds(0, 143, 128, 23);
		templatePanel.add(chckbxAddToFolder);
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setPreferredSize(new Dimension(getWidth(), 40));
		buttonPanel.setMaximumSize(new Dimension(getWidth(), 40));
		buttonPanel.setMinimumSize(new Dimension(getWidth(), 40));
		contentPane.add(buttonPanel);
		buttonPanel.setLayout(null);
		
		btnImport = new JButton("Import");
		btnImport.setBounds(339, 6, 117, 29);
		buttonPanel.add(btnImport);
		
		btnCancel = new JButton("Cancel");
		btnCancel.setBounds(217, 6, 117, 29);
		buttonPanel.add(btnCancel);
	}
}
