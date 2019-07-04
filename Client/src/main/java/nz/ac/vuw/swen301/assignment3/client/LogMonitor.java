package nz.ac.vuw.swen301.assignment3.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LogMonitor {
    private static final String TEST_SCHEME = "http";
    private static final String TEST_HOST = "localhost";
    private static final int TEST_PORT = 8080;
    private static final String TEST_PATH = "/resthome4logs";
    private static final String LOGS_PATH = TEST_PATH + "/logs";
    private static final String STATS_PATH = TEST_PATH + "/stats";
    private enum Level {
        ALL,
        TRACE,
        DEBUG,
        INFO,
        WARN,
        ERROR,
        FATAL,
        OFF
    }
    private static final Integer MIN_LEVEL = 0;
    private static final Integer MAX_LEVEL = 50;
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static Level selectedLevel;
    private static Integer selectedLimit;

    private static JFrame frame;
    private static JPanel page;
    private static JPanel optionPanel;
    private static JPanel tablePanel;
    private static JScrollPane table;

    public static void main(String[] args) {
        Logger.getLogger("org.apache.http").setLevel(org.apache.log4j.Level.OFF);
        createGUI();
    }


    private static void createGUI() {
        frame = new JFrame();
        frame.setTitle("Ali G's Log Monitor");
        frame.setSize(WIDTH, HEIGHT);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation((int)((screenSize.getWidth()/2)-WIDTH/2), (int)((screenSize.getHeight()/2)-HEIGHT/2));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        page = new JPanel();
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));

        tablePanel = new JPanel();
        tablePanel.setPreferredSize(new Dimension(WIDTH, HEIGHT*6/7));
        table = createTable(new Object[0][0]);
        table.setPreferredSize(new Dimension(WIDTH*6/7, HEIGHT*6/7));
        tablePanel.add(table);

        optionPanel = createOptionPanel();
        page.add(optionPanel);
        page.add(tablePanel);
        frame.add(page);
        frame.setVisible(true);
        frame.revalidate();
        frame.repaint();
    }


    private static JPanel createOptionPanel() {
        JPanel optionPanel = new JPanel();
        GridLayout layout = new GridLayout(1,7);
        layout.setHgap(5);
        optionPanel.setLayout(layout);

        JLabel levelLabel = new JLabel("Min Level:", JLabel.RIGHT);
        levelLabel.setSize(40,40);
        optionPanel.add(levelLabel);

        JComboBox levelCombo = new JComboBox(Level.values());
        levelCombo.setSelectedIndex(0);
        selectedLevel = Level.valueOf(String.valueOf(levelCombo.getSelectedItem()));
        levelCombo.setBounds(50, 50,90,20);
        levelCombo.addActionListener(e -> {
            selectedLevel = Level.valueOf(String.valueOf(levelCombo.getSelectedItem()));
        });
        optionPanel.add(levelCombo);

        JLabel limitLabel = new JLabel("Limit:", JLabel.RIGHT);
        limitLabel.setSize(40,40);
        optionPanel.add(limitLabel);

        JComboBox limitCombo = new JComboBox((IntStream.range(MIN_LEVEL, MAX_LEVEL+1).boxed().collect(Collectors.toList()).toArray()));
        limitCombo.setSelectedIndex(11);
        limitCombo.setBounds(50, 50,90,20);
        limitCombo.addActionListener(e -> {
            selectedLimit = Integer.parseInt(limitCombo.getSelectedItem().toString());
        });
        optionPanel.add(limitCombo);

        //Space filler
        JPanel blank = new JPanel();
        blank.setSize(100,1);
        optionPanel.add(blank);

        JButton fetchButton = new JButton("Fetch");
        fetchButton.addActionListener(e -> {
            fetchData();
            frame.getContentPane().removeAll();
            page.removeAll();
            tablePanel.removeAll();
            tablePanel.setPreferredSize(new Dimension(WIDTH, HEIGHT*6/7));
            table.setPreferredSize(new Dimension(WIDTH*6/7, HEIGHT*6/7));
            tablePanel.add(table);
            page.add(optionPanel);
            page.add(tablePanel);
            frame.add(page);
            frame.setVisible(true);
        });
        optionPanel.add(fetchButton);

        JButton downloadButton = new JButton("Download");
        downloadButton.addActionListener(e -> {
            downloadExcel();
        });
        optionPanel.add(downloadButton);

        selectedLevel = Level.valueOf(String.valueOf(levelCombo.getSelectedItem()));
        selectedLimit = Integer.parseInt(limitCombo.getSelectedItem().toString());

        return optionPanel;
    }


    private static JScrollPane createTable(Object[][] data) {
        String[] colNames = {"Time", "Level", "Logger", "Thread", "Message"};

        JTable table = new JTable(data, colNames);
        JScrollPane scrollPane = new JScrollPane(table);
        table.setFillsViewportHeight(true);

        return scrollPane;
    }

    private static void fetchData() {
        try {
            URIBuilder builder = new URIBuilder();
            builder.setScheme(TEST_SCHEME).setHost(TEST_HOST).setPort(TEST_PORT).setPath(LOGS_PATH);
            builder.addParameter("limit", selectedLimit.toString());
            builder.addParameter("level", selectedLevel.toString());
            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet(builder.build());

            HttpResponse response = httpClient.execute(request);
            String logsJSON = EntityUtils.toString(response.getEntity());
            LogEvent[] responseLogList = new ObjectMapper().readValue(logsJSON, LogEvent[].class);
            Object[][] data = convertLogsToTableData(responseLogList);
            table = createTable(data);
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
            return;
        }
    }


    private static void downloadExcel() {
        try {
            URIBuilder builder = new URIBuilder();
            builder.setScheme(TEST_SCHEME).setHost(TEST_HOST).setPort(TEST_PORT).setPath(STATS_PATH);
            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet(builder.build());

            HttpResponse response = httpClient.execute(request);
            HSSFWorkbook workbook = new HSSFWorkbook(response.getEntity().getContent());

            JFileChooser chooser=new JFileChooser();
            chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
            chooser.setFileSelectionMode(JFileChooser.APPROVE_OPTION);
            chooser.showSaveDialog(null);
            String path=chooser.getSelectedFile().getAbsolutePath();
            FileOutputStream fileOut = new FileOutputStream(path+".xls");

            workbook.write(fileOut);
            fileOut.flush();
            fileOut.close();
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
            return;
        }
    }


    private static Object[][] convertLogsToTableData(LogEvent[] logList) {
        if (logList==null) return new Object[0][0];
        Object[][] data = new Object[logList.length][5];
        for(int i=0; i<logList.length; i++) {
            LogEvent log = logList[i];
            data[i][0] = log.getTimestamp();
            data[i][1] = log.getLevel();
            data[i][2] = log.getLogger();
            data[i][3] = log.getThread();
            data[i][4] = log.getMessage();
        }
        return data;
    }












}