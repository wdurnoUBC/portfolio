package guiForLIMS;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class EditTemplateInvalidNameGUI1 extends JFrame {
	
	public JList list;
	public JButton btnOkay;
	
	public EditTemplateInvalidNameGUI1() {
		setTitle("Invalid Template Name");
		setBounds(100, 100, 338, 243);
		setMinimumSize(new Dimension(338, 200));
		
		JPanel panel = new JPanel();
		panel.setPreferredSize(new Dimension(100, 60));
		getContentPane().add(panel, BorderLayout.NORTH);
		panel.setLayout(null);
		
		JLabel lblYouHaveEntered = new JLabel("You have entered an invalid template name.  Please");
		lblYouHaveEntered.setBounds(6, 6, 326, 22);
		panel.add(lblYouHaveEntered);
		
		JLabel lblNewLabel = new JLabel("choose from the following existing templates:");
		lblNewLabel.setBounds(6, 30, 301, 16);
		panel.add(lblNewLabel);
		
		JScrollPane scrollPane = new JScrollPane();
		getContentPane().add(scrollPane, BorderLayout.CENTER);
		
		list = new JList();
		scrollPane.setViewportView(list);
		
		JPanel panel_1 = new JPanel();
		getContentPane().add(panel_1, BorderLayout.SOUTH);
		
		btnOkay = new JButton("Okay");
		panel_1.add(btnOkay);
	}

	private static final long serialVersionUID = 1L;
}
