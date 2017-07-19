package guiForLIMS;

import java.awt.Color;
import java.awt.EventQueue;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

public class LoginGUI1 extends JFrame {


	private static final long serialVersionUID = 1L;
	public JPanel contentPane;
	public JTextField usernameField;
	public JPasswordField passwordField;
	public JButton btnLogin;
	public JLabel lblUsername;
	public JLabel lblPassword;
	public JLabel lblIncorrectInfo;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					LoginGUI1 frame = new LoginGUI1();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public LoginGUI1() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 353, 157);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		usernameField = new JTextField();
		usernameField.setBounds(107, 16, 229, 28);
		contentPane.add(usernameField);
		usernameField.setColumns(10);
		
		passwordField = new JPasswordField();
		passwordField.setBounds(107, 56, 229, 28);
		contentPane.add(passwordField);
		
		lblUsername = new JLabel("Username:");
		lblUsername.setBounds(20, 22, 75, 16);
		contentPane.add(lblUsername);
		
		lblPassword = new JLabel("Password:");
		lblPassword.setBounds(20, 62, 75, 16);
		contentPane.add(lblPassword);
		
		btnLogin = new JButton("Log In");
		btnLogin.setBounds(231, 96, 105, 29);
		contentPane.add(btnLogin);
		
		lblIncorrectInfo = new JLabel("Incorrect username or password");
		lblIncorrectInfo.setForeground(Color.RED);
		lblIncorrectInfo.setBounds(20, 101, 213, 16);
		contentPane.add(lblIncorrectInfo);
	}
}
