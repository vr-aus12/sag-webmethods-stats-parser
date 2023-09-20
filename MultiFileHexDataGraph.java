package test;

import org.jfree.chart.*;
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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MultiFileHexDataGraph extends ApplicationFrame {

    private final String directoryPath; // Directory containing the files
    private final Map<String, XYSeries> seriesMap = new HashMap<>();
    Map<Integer, String> metricNameMap = new HashMap<>();


    public MultiFileHexDataGraph(String title, String directoryPath,String fileStartsWith) {
        super(title);
        this.directoryPath = directoryPath;
        metricNameMap.put(3, "TotalMem");
        metricNameMap.put(4, "FreeMem");
        metricNameMap.put(5, "CurT");
        metricNameMap.put(6, "MaxT");
        metricNameMap.put(7, "SSNs");
        metricNameMap.put(8, "SSNx");
        metricNameMap.put(9, "SSNAvg");
        metricNameMap.put(10, "REQs");
        metricNameMap.put(11, "REQx");
        metricNameMap.put(12, "REQAvg");
        metricNameMap.put(13, "StartReq");
        metricNameMap.put(14, "EndReq");
        createChartsAndTabs(fileStartsWith != null?fileStartsWith:"stats");
    }

    private void createChartsAndTabs(String fileStartsWith) {
        JTabbedPane tabbedPane = new JTabbedPane(); // Create the tabbed pane here

        File directory = new File(directoryPath);
        FilenameFilter filter = (dir, name) -> name.startsWith(fileStartsWith) && (name.endsWith(".log") || name.endsWith(".zip"));
        File[] files = directory.listFiles(filter);

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    processFile(file);
                }
            }
        }

        for (int i=3;i<=14;i++) {

            String metricName = metricNameMap.get(i);
            XYSeries series = seriesMap.get(metricName);
            createChartAndTab(tabbedPane, metricName, series);
        }

        setContentPane(tabbedPane); // Set the tabbedPane as the content pane
    }

    private void processFile(File file) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.ENGLISH);
            dateFormat.setTimeZone(TimeZone.getTimeZone("AEST"));

            if (file.getName().endsWith(".log")) {
                processLogFile(file, dateFormat);
            } else if (file.getName().endsWith(".zip")) {
                processZipFile(file, dateFormat);
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    private void processLogFile(File file, SimpleDateFormat dateFormat) throws IOException, ParseException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            // Skip the first line
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim(); // Remove leading and trailing whitespace
                if (line.isEmpty() || line.contains("---( Start )---") || line.contains("TotalMem")) {
                    continue; // Skip empty lines or lines with undesired content
                }
                String[] parts = line.split(" ");
                Date datetime = dateFormat.parse(parts[0] + " " + parts[1] + " " + parts[2]);

                for (int dataIndex = 3; dataIndex <= 14; dataIndex++) {
                    String metricName = metricNameMap.get(dataIndex);
                    long value = Long.parseLong(parts[dataIndex], 16);
                    addToSeries(metricName, datetime, value);
                }
            }
        }
    }

    private void processZipFile(File file, SimpleDateFormat dateFormat) throws IOException, ParseException {
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(file))) {
            ZipEntry zipEntry;

            if ( (zipEntry = zipInputStream.getNextEntry()) != null){
                if (!zipEntry.isDirectory() && zipEntry.getName().endsWith(".log")) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(zipInputStream));

                    String line;

                    while ((line = reader.readLine()) != null) {
                        line = line.trim(); // Remove leading and trailing whitespace
                        if (line.isEmpty() || line.contains("---( Start )---") || line.contains("TotalMem")) {
                            continue; // Skip empty lines or lines with undesired content
                        }
                        String[] parts = line.split(" ");
                        Date datetime = dateFormat.parse(parts[0] + " " + parts[1] + " " + parts[2]);

                        for (int dataIndex = 3; dataIndex <= 14; dataIndex++) {
                            String metricName = metricNameMap.get(dataIndex);
                            long value = Long.parseLong(parts[dataIndex], 16);
                            addToSeries(metricName, datetime, value);
                        }
                    }

                    reader.close();
                }
            }
        }
    }

    private void addToSeries(String metricName, Date datetime, long value) {
        XYSeries series = seriesMap.computeIfAbsent(metricName, k -> new XYSeries(metricName));
        series.add(datetime.getTime(), value);
    }

    private void createChartAndTab(JTabbedPane tabbedPane, String metricName, XYSeries series) {
        XYSeriesCollection dataset = new XYSeriesCollection(series);

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



    public static void main(String[] args) {
        MultiFileHexDataGraph chart = new MultiFileHexDataGraph("Performance Metrics", "H:\\stats\\","stats"); // Update the directory path
        chart.pack();
        UIUtils.centerFrameOnScreen(chart);
        chart.setVisible(true);
    }
}
