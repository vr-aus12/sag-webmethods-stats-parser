package test;

import org.jfree.chart.*;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.ApplicationFrame;
import org.jfree.chart.ui.UIUtils;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;

import java.awt.BorderLayout;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MultiFileHexDataGraph extends ApplicationFrame {

    private final String directoryPath; // Directory containing the files

    public MultiFileHexDataGraph(String title, String directoryPath) {
        super(title);
        this.directoryPath = directoryPath;
        createChartsForFiles();

    }

    private void createChartsForFiles() {
        File directory = new File(directoryPath);
        File[] files = directory.listFiles();
        System.out.println("files "+files);

        if (files != null) {
            JTabbedPane tabbedPane = new JTabbedPane(); // Create the tabbed pane here

            for (File file : files) {
            	   System.out.println("file "+file);
                if (file.isFile() && (file.getName().startsWith("stats") && (file.getName().endsWith(".log") || file.getName().endsWith(".zip")))) {
                    createChartsFromFile(tabbedPane, file); // Pass the tabbedPane to the method
                }
            }

            setContentPane(tabbedPane); // Set the tabbedPane as the content pane
        }
    }

    private void createChartsFromFile(JTabbedPane tabbedPane, File file) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.ENGLISH);
            dateFormat.setTimeZone(TimeZone.getTimeZone("AEST"));

            if (file.getName().endsWith(".log")) {
                // Read data from a .log file
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    // Skip the first line
                    String line;

                    while ((line = reader.readLine()) != null) {
                        line = line.trim(); // Remove leading and trailing whitespace
                        if (line.isEmpty()) {
                            continue; // Skip empty lines
                        }
                        if (line.contains("---( Start )---"))
                        	continue;
                        
                        if (line.contains("TotalMem"))
                        	continue;
                        String[] parts = line.split(" ");
                        Date datetime = dateFormat.parse(parts[0] + " " + parts[1] + " " + parts[2]);

                        for (int dataIndex = 3; dataIndex <= 14; dataIndex++) {
                            String metricName = getMetricName(dataIndex);
                            long value = Long.parseLong(parts[dataIndex], 16);
                            createAndAddChart(tabbedPane, metricName, datetime, value); // Pass the tabbedPane
                        }
                    }
                }
            } else if (file.getName().endsWith(".zip")) {
                // Extract and read data from a .zip file
                try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(file))) {
                    ZipEntry zipEntry;

                  if ( (zipEntry = zipInputStream.getNextEntry()) != null){
                        if (!zipEntry.isDirectory() && zipEntry.getName().endsWith(".log")) {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(zipInputStream));

                            String line;

                            while ((line = reader.readLine()) != null) {
                                line = line.trim(); // Remove leading and trailing whitespace
                                
                                if (line.isEmpty()) {
                                    continue; // Skip empty lines
                                }
                                if (line.contains("---( Start )---"))
                                	continue;
                                
                                if (line.contains("TotalMem"))
                                	continue;
                                
                                //System.out.println("line "+line);
                                String[] parts = line.split(" ");
                                Date datetime = dateFormat.parse(parts[0] + " " + parts[1] + " " + parts[2]);

                                for (int dataIndex = 3; dataIndex <= 14; dataIndex++) {
                                    String metricName = getMetricName(dataIndex);
                                    long value = Long.parseLong(parts[dataIndex], 16);
                                    createAndAddChart(tabbedPane, metricName, datetime, value); // Pass the tabbedPane
                                }
                            }

                            reader.close();
                        }
                    }
                }
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    private void createAndAddChart(JTabbedPane tabbedPane, String metricName, Date datetime, long value) {
        // Check if a chart with the same metric name already exists
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            if (tabbedPane.getTitleAt(i).equals(metricName)) {
                JPanel chartPanel = (JPanel) tabbedPane.getComponentAt(i);
                ChartPanel existingChartPanel = (ChartPanel) chartPanel.getComponent(0);
                JFreeChart existingChart = existingChartPanel.getChart();
                
                XYPlot existingPlot = existingChart.getXYPlot();
                XYSeriesCollection existingDataset = (XYSeriesCollection) existingPlot.getDataset();
                XYSeries existingSeries = existingDataset.getSeries(0);

                existingSeries.add(datetime.getTime(), value);
               

                // Update the chart and exit the method
                existingChartPanel.repaint();
                return;
            }
        }

        // If no chart with the same metric name exists, create a new chart and add it to the tabbed pane
        XYSeriesCollection dataset = new XYSeriesCollection();
        XYSeries series = new XYSeries(metricName);
        series.add(datetime.getTime(), value);
        dataset.addSeries(series);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                metricName,
                "Timestamp",
                "Value",
                dataset,
                true,
                true,
                false
        );

        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        plot.setRenderer(renderer);

        ChartPanel newChartPanel = new ChartPanel(chart); // Create a new ChartPanel
        newChartPanel.setPreferredSize(new java.awt.Dimension(800, 600));

        JPanel chartPanel = new JPanel(new BorderLayout());
        chartPanel.add(newChartPanel, BorderLayout.CENTER); // Add the new ChartPanel to the JPanel

        tabbedPane.addTab(metricName, chartPanel); // Add the chart to the tabbedPane
    }


    private String getMetricName(int dataIndex) {
        switch (dataIndex) {
            case 3:
                return "TotalMem";
            case 4:
                return "FreeMem";
            case 5:
                return "CurT";
            case 6:
                return "MaxT";
            case 7:
                return "SSNs";
            case 8:
                return "SSNx";
            case 9:
                return "SSNAvg";
            case 10:
                return "REQs";
            case 11:
                return "REQx";
            case 12:
                return "REQAvg";
            case 13:
                return "StartReq";
            case 14:
                return "EndReq";
            default:
                return "Unknown";
        }
    }
    public static void main(String[] args) {
        MultiFileHexDataGraph chart = new MultiFileHexDataGraph("Performance Metrics", "H:\\stats\\"); // Update the directory path
        chart.pack();
        UIUtils.centerFrameOnScreen(chart);
        chart.setVisible(true);
    }
}
