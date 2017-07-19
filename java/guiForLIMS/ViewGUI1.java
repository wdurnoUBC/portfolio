package guiForLIMS;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class ViewGUI1 extends JFrame {

	public JButton btnIntersection ;
	public JButton btnUnion ;
	public JButton btnAll ;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;

	/**
	 * Create the frame.
	 */
	public ViewGUI1() {
		setTitle("View");
		//setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 193, 144);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		GridBagLayout gbl_contentPane = new GridBagLayout();
		gbl_contentPane.columnWidths = new int[]{0, 0};
		gbl_contentPane.rowHeights = new int[]{0, 0, 0, 0};
		gbl_contentPane.columnWeights = new double[]{0.0, Double.MIN_VALUE};
		gbl_contentPane.rowWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
		contentPane.setLayout(gbl_contentPane);
		
		btnIntersection = new JButton("Intersection");
		GridBagConstraints gbc_btnIntersection = new GridBagConstraints();
		gbc_btnIntersection.insets = new Insets(0, 0, 5, 0);
		gbc_btnIntersection.gridx = 0;
		gbc_btnIntersection.gridy = 0;
		contentPane.add(btnIntersection, gbc_btnIntersection);
		
		btnUnion = new JButton("Union");
		GridBagConstraints gbc_btnUnion = new GridBagConstraints();
		gbc_btnUnion.insets = new Insets(0, 0, 5, 0);
		gbc_btnUnion.gridx = 0;
		gbc_btnUnion.gridy = 1;
		contentPane.add(btnUnion, gbc_btnUnion);
		
		btnAll = new JButton("All");
		GridBagConstraints gbc_btnAll = new GridBagConstraints();
		gbc_btnAll.gridx = 0;
		gbc_btnAll.gridy = 2;
		contentPane.add(btnAll, gbc_btnAll);
	}

}
