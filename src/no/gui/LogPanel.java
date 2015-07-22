package no.gui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.SystemColor;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.DefaultCaret;

public class LogPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	
	private JTextArea textArea = new JTextArea();
	
	private JPanel 					logPanel;				// messages and errors
	private JPanel 					progressPanel;			// progress bars for analyses

	public LogPanel() {
		
		this.setLayout(new BorderLayout());
		this.logPanel = createLogPanel();
		this.add(logPanel, BorderLayout.CENTER);
	}
	
	/**
	 * Create the log panel for updates
	 * @return a scrollable panel
	 */
	private JPanel createLogPanel(){
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		JScrollPane scrollPane = new JScrollPane();
		textArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
		DefaultCaret caret = (DefaultCaret)textArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		
		scrollPane.setViewportView(textArea);
		textArea.setBackground(SystemColor.menu);
		textArea.setEditable(false);
		textArea.setRows(9);
		textArea.setColumns(40);
		
		
		panel.add(scrollPane, BorderLayout.CENTER);

		progressPanel = new JPanel();
		progressPanel.setLayout(new BoxLayout(progressPanel, BoxLayout.Y_AXIS));
		panel.add(progressPanel, BorderLayout.NORTH);
		
		return panel;
	}
	
	/**
	 * Standard log - append a newline
	 * @param s the string to log
	 */
	public void log(String s){
		logc(s+"\n");
	}
	
	/**
	 * Continuous log - do not append a newline
	 * @param s the string to log
	 */
	public void logc(String s){
		textArea.append(s);
	}
	
	public void addProgressBar(JProgressBar progressBar){
		progressPanel.add(progressBar);
	}
	
	public void removeProgressBar(JProgressBar progressBar){
		progressPanel.remove(progressBar);
	}

}
