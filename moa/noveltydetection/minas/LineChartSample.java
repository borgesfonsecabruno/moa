package moa.noveltydetection.minas;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;

public class LineChartSample extends ApplicationFrame {
	protected String path1 = "C:\\Users\\bf_04\\Documents\\";
	protected String path2;
	protected String title;
	protected String x;
	protected String y;
	protected double[] N1;
	protected double[] N2;

	public LineChartSample(String algo, String title, String chartTitle, String x, String y) throws IOException {
		super(title);

		this.title = title;
		this.x = x;
		this.y = y;
		this.path2 = algo;

		//try {
			//this.N1 = ManipuladorArquivo.leitor(this.path1 + "Elaine - " + this.path2 + ".txt");
			//this.N2 = ManipuladorArquivo.leitor(this.path1 + "Bruno - " + this.path2 + ".txt");
			System.out.println(Arrays.toString(N1));
			System.out.println(Arrays.toString(N2));

		//} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		//}

		JFreeChart xylineChart = ChartFactory.createXYLineChart(chartTitle, x, y, createDataset(),
				PlotOrientation.VERTICAL, true, true, false);

		ChartPanel chartPanel = new ChartPanel(xylineChart);
		chartPanel.setPreferredSize(new java.awt.Dimension(560, 367));
		final XYPlot plot = xylineChart.getXYPlot();

		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
		renderer.setSeriesPaint(0, Color.RED);
		renderer.setSeriesPaint(1, Color.GREEN);
		renderer.setSeriesStroke(0, new BasicStroke(4.0f));
		renderer.setSeriesStroke(1, new BasicStroke(3.0f));
		plot.setRenderer(renderer);
		setContentPane(chartPanel);

		pack();
		RefineryUtilities.centerFrameOnScreen(this);
		setVisible(true);

	}

	private XYDataset createDataset() {
		final XYSeries firefox = new XYSeries("Elaine");
		for (int i = 0; i < N1.length; i++)
			firefox.add(i, N1[i]);

		final XYSeries chrome = new XYSeries("Bruno");
		for (int i = 0; i < N2.length; i++)
			chrome.add(i, N2[i]);

		final XYSeriesCollection dataset = new XYSeriesCollection();
		dataset.addSeries(firefox);
		dataset.addSeries(chrome);
		return dataset;
	}

}