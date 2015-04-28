package no.gui;

import ij.io.OpenDialog;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.text.DefaultCaret;

import java.awt.FlowLayout;

import javax.swing.JLabel;
import javax.swing.JButton;

import no.analysis.AnalysisCreator;
import no.collections.INuclearCollection;
import no.imports.PopulationImporter;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JTextArea;

import java.awt.SystemColor;
import java.io.File;
import java.text.DecimalFormat;

import javax.swing.JScrollPane;
import javax.swing.BoxLayout;
import javax.swing.SwingConstants;

import java.awt.Font;

import javax.swing.JTable;
import java.awt.Component;

public class MainWindow extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTextArea textArea = new JTextArea();;
	private JLabel lblStatusLine = new JLabel("No analysis open");
	private final JPanel panelAggregates = new JPanel();
	private JTable table;


	/**
	 * Create the frame.
	 */
	public MainWindow() {
		setTitle("Nuclear Morphology Analysis");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 547, 326);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		
		JScrollPane scrollPane = new JScrollPane();
		contentPane.add(scrollPane, BorderLayout.WEST);
		textArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
		DefaultCaret caret = (DefaultCaret)textArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		
		scrollPane.setViewportView(textArea);
		textArea.setLineWrap(true);
		textArea.setBackground(SystemColor.menu);
		textArea.setEditable(false);
		textArea.setRows(9);
		textArea.setColumns(30);
		
		JLabel lblAnalysisLog = new JLabel("Analysis Log");
		lblAnalysisLog.setHorizontalAlignment(SwingConstants.CENTER);
		scrollPane.setColumnHeaderView(lblAnalysisLog);
		
		JPanel panelHeader = new JPanel();
		contentPane.add(panelHeader, BorderLayout.NORTH);
//		
		JButton btnNewAnalysis = new JButton("New analysis");
		btnNewAnalysis.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				newAnalysis();
			}
		});
		panelHeader.add(btnNewAnalysis);
		
		JButton btnLoadSavedNuclei = new JButton("Load saved nuclei");
		btnLoadSavedNuclei.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				loadNuclei();
			}
		});
		
		panelHeader.add(btnLoadSavedNuclei);
		
		JPanel panelFooter = new JPanel();
		contentPane.add(panelFooter, BorderLayout.SOUTH);
		
//		JLabel lblStatusLine = new JLabel("No analysis open");
		panelFooter.add(lblStatusLine);
		
		JPanel panelStats = new JPanel();
		contentPane.add(panelStats, BorderLayout.CENTER);
		panelStats.setLayout(new BoxLayout(panelStats, BoxLayout.Y_AXIS));
		
		JLabel lblPopulationStatistics = new JLabel("Statistics");
		lblPopulationStatistics.setAlignmentX(Component.CENTER_ALIGNMENT);
		lblPopulationStatistics.setHorizontalAlignment(SwingConstants.CENTER);
		panelStats.add(lblPopulationStatistics);
		
		table = new JTable();
		panelStats.add(table);
		contentPane.add(panelAggregates, BorderLayout.EAST);
		
		JLabel lblAggregates = new JLabel("Aggregates");
		panelAggregates.add(lblAggregates);
	}
	
	/**
	 * Standard log - append a newline
	 * @param s the string to log
	 */
	public void log(String s){
		textArea.append(s+"\n");
	}
	
	/**
	 * Continuous log - do not append a newline
	 * @param s the string to log
	 */
	public void logc(String s){
		textArea.append(s);
	}
	
	public void newAnalysis(){
		
		Thread thr = new Thread() {
			public void run() {
				AnalysisCreator analysisCreator = new AnalysisCreator(MainWindow.this);
				analysisCreator.run();
			}
		};
		thr.start();		
	}
	
	public void loadNuclei(){
		OpenDialog fileDialog = new OpenDialog("Select a save file...");
		String fileName = fileDialog.getPath();
		if(fileName==null) return;
		INuclearCollection collection = PopulationImporter.readPopulation(new File(fileName), this);
		log("Opened collection: "+collection.getType());
		
		DecimalFormat df = new DecimalFormat("#.00"); 
		lblStatusLine.setText("Analysing: "+collection.getFolder()+" : "+collection.getType());

		String[] columnNames = {"Field", "Value"};

		Object[][] data = {
				{"Nuclei", collection.getNucleusCount()},
				{"Median area", df.format(collection.getMedianNuclearArea())},
				{"Median perim", df.format(collection.getMedianNuclearPerimeter())},
				{"Median feret", df.format(collection.getMedianFeretLength())},
				{"Signal channels", collection.getSignalChannels().size()},
				{"Ran", collection.getOutputFolderName()},
				{"Type", collection.getClass().getSimpleName()}
			};

				TableModel model = new DefaultTableModel(data, columnNames);
				table.setModel(model);
	}

	
}
