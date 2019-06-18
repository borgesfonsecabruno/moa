package moa.noveltydetection.minas;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

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

public final class LineChartEx extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected String title;
	protected String chartTitle;
	protected String x;
	protected String y;
	protected double[] N;
	protected double[] N2;

	public LineChartEx(double[] N, String title, String chartTitle, String x, String y) throws IOException {
		this.N = N;
		this.chartTitle = chartTitle;
		this.title = title;
		this.x = x;
		this.y = y;

	}
	
	public LineChartEx(double[] N, double[] N2, String title, String chartTitle, String x, String y) throws IOException {
		this.N = N;
		this.chartTitle = chartTitle;
		this.title = title;
		this.x = x;
		this.y = y;

	}

	public void begin() throws IOException {
		initUI();
	}
	
    private void initUI() throws IOException {

        XYDataset dataset = createDataset();
        JFreeChart chart = createChart(dataset);
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        chartPanel.setBackground(Color.white);
        add(chartPanel);

        pack();
        setTitle(title);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        File f = new File("C:\\Users\\bf_04\\Pictures\\" + chartTitle + ".png");



        BufferedImage chartImage = chart.createBufferedImage( 600, 400, null); 
        ImageIO.write( chartImage, "png", f ); 

    }

    private XYDataset createDataset() {

        XYSeries series = new XYSeries("");
        
        for(int i=0; i<N.length;i++)
        {
        	series.add(i, N[i]);
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series);

        return dataset;
    }

    private JFreeChart createChart(XYDataset dataset) {

        JFreeChart chart = ChartFactory.createXYLineChart(
                chartTitle, 
                x, 
                y, 
                dataset, 
                PlotOrientation.VERTICAL,
                true, 
                true, 
                false 
        );

        XYPlot plot = chart.getXYPlot();

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesPaint(0, Color.RED);
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));

        plot.setRenderer(renderer);
        plot.setBackgroundPaint(Color.white);

        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.BLACK);

        plot.setDomainGridlinesVisible(true);
        plot.setDomainGridlinePaint(Color.BLACK);

        chart.getLegend().setFrame(BlockBorder.NONE);

        chart.setTitle(new TextTitle(chartTitle,
                        new Font("Serif", java.awt.Font.BOLD, 18)
                )
        );

        return chart;

    }

}