package no.gui;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

import java.awt.FlowLayout;

import javax.swing.JLabel;
import javax.swing.JButton;

import no.analysis.AnalysisCreator;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JTextArea;

import java.awt.SystemColor;

import javax.swing.JScrollPane;

public class MainWindow extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTextArea textArea = new JTextArea();;


	/**
	 * Create the frame.
	 */
	public MainWindow() {
		setTitle("Nuclear Morphology Analysis");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		
		JScrollPane scrollPane = new JScrollPane();
		contentPane.add(scrollPane, BorderLayout.CENTER);
		
		scrollPane.setViewportView(textArea);
		textArea.setLineWrap(true);
		textArea.setBackground(SystemColor.menu);
		textArea.setEditable(false);
		textArea.setRows(9);
		textArea.setColumns(40);
		
		JPanel panel_1 = new JPanel();
		contentPane.add(panel_1, BorderLayout.NORTH);
//		
		JButton btnNewAnalysis = new JButton("New analysis");
		btnNewAnalysis.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				newAnalysis();
			}
		});
		panel_1.add(btnNewAnalysis);
		
		JButton btnLoadSavedNuclei = new JButton("Load saved nuclei");
		panel_1.add(btnLoadSavedNuclei);
		
		JPanel panel_2 = new JPanel();
		contentPane.add(panel_2, BorderLayout.SOUTH);
		
		JLabel lblNoAnalysisOpen = new JLabel("No analysis open");
		panel_2.add(lblNoAnalysisOpen);
	}
	
	public void log(String s){
		textArea.append(s+"\n");
	}
	
	public void newAnalysis(){
//		log("New analysis\n");
		
		Thread thr = new Thread() {
			public void run() {
				AnalysisCreator analysisCreator = new AnalysisCreator(MainWindow.this);
				analysisCreator.run();
			}
		};
		thr.start();
//		AnalysisCreator analysisCreator = new AnalysisCreator();
//		analysisCreator.run();
		
	}
	
//	class NewAnalysisTask extends SwingWorker<Void, String> {
//		
//		NewAnalysisTask(){
//			
//		}
//		@Override
//		public Void doInBackground(){
//			AnalysisCreator analysisCreator = new AnalysisCreator();
//			publish("Analysis being created");
//			analysisCreator.run();
//			publish("Analysis run");
//			return null;
//		}
//		
//		@Override
//		protected void process(String string){
//			textArea.append(string);
//		}
//	}

}
