package guiForLIMS;

import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class AdminPopLIMSGUI3 extends JPanel {

	private static final long serialVersionUID = 1L;
	public JButton btnPopulate;
	public JProgressBar progressBar;
	private JLabel lblNewLabel;
	
	public AdminPopLIMSGUI3() {
		setLayout(null);
		setPreferredSize(new Dimension(584, 124));
		
		progressBar = new JProgressBar();
		progressBar.setVisible(false);
		progressBar.setSize(348, 20);
		progressBar.setLocation(111, 92);
		add(progressBar);
		
		btnPopulate = new JButton("Populate");
		btnPopulate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
			}
		});
		btnPopulate.setBounds(417, 51, 98, 29);
		add(btnPopulate);
		
		JLabel lblPopulateMlims = new JLabel("Populate mLIMS");
		lblPopulateMlims.setFont(new Font("Lucida Grande", Font.BOLD, 13));
		lblPopulateMlims.setBounds(6, 6, 120, 16);
		add(lblPopulateMlims);
		
		JLabel lblUpdateLimsFrom = new JLabel("Populate LIMS from the actual location of the data (either starting");
		lblUpdateLimsFrom.setBounds(6, 32, 433, 16);
		add(lblUpdateLimsFrom);
		
		lblNewLabel = new JLabel("from scratch, or updating if you have already populated before):");
		lblNewLabel.setBounds(6, 56, 413, 16);
		add(lblNewLabel);
	}
}
