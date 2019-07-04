package nz.ac.vuw.swen301.assignment3.server;

import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class StatService extends HttpServlet {
    private static DateTimeFormatter formatter = new DateTimeFormatterBuilder().appendInstant(3).toFormatter();
    private ArrayList<String> loggerList = new ArrayList<>();
    private ArrayList<String> levelList = new ArrayList<>();
    private ArrayList<String> threadList = new ArrayList<>();
    private ArrayList<String> dayList = new ArrayList<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)  {
        Integer[][] data = getLogs();
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet("Statistics");
        if (data!=null) {
            populateSheet(sheet, data);
        }

        resp.setContentType("application/vnd.ms-excel");
        resp.setStatus(HttpStatus.SC_OK);
        try {
            ByteArrayOutputStream outByteStream = new ByteArrayOutputStream();
            workbook.write(outByteStream);
            OutputStream outStream = resp.getOutputStream();
            outStream.write(outByteStream.toByteArray());
            outStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }


    private void populateSheet(HSSFSheet sheet, Integer[][] data) {
        int rowNum = 0;
        int colNum = 1;
        int maxColNum = data[0].length;

        Row row = sheet.createRow(rowNum++);
        Cell cell = null;
        for (int day=0; day<dayList.size(); day++) {
            cell = row.createCell(colNum++);
            cell.setCellValue(dayList.get(day));
        }

        for (int i=0; i<loggerList.size(); i++) {
            colNum = 0;
            row = sheet.createRow(rowNum++);
            cell = row.createCell(colNum++);
            cell.setCellValue(loggerList.get(i));
            for (int j=0; j<maxColNum; j++) {
                cell = row.createCell(colNum++);
                cell.setCellValue(data[i][j]);
            }
        }
        for (int i=0; i<levelList.size(); i++) {
            colNum = 0;
            row = sheet.createRow(rowNum++);
            cell = row.createCell(colNum++);
            cell.setCellValue(levelList.get(i));
            for (int j=0; j<maxColNum; j++) {
                cell = row.createCell(colNum++);
                cell.setCellValue(data[i+loggerList.size()][j]);
            }
        }
        for (int i=0; i<threadList.size(); i++) {
            colNum = 0;
            row = sheet.createRow(rowNum++);
            cell = row.createCell(colNum++);
            cell.setCellValue(threadList.get(i));
            for (int j=0; j<maxColNum; j++) {
                cell = row.createCell(colNum++);
                cell.setCellValue(data[i+loggerList.size()+levelList.size()][j]);
            }
        }
    }


    private Integer[][] getLogs() {
        Collection<LogEvent> realDatabase = Database.getValues();
        // Clone it
        Collection<LogEvent> logList = new ArrayList<>();
        for (LogEvent log : realDatabase) {
            LogEvent newLog = new LogEvent();
            newLog.setup(log.getId(), log.getMessage(), log.getTimestamp(), log.getThread(), log.getLogger(), log.getLevel(), log.getErrorDetails());
            logList.add(newLog);
        }

        if (logList == null || logList.isEmpty()) {
            return null;
        }
        Set<String> loggers = new HashSet<>();
        Set<String> levels = new HashSet<>();
        Set<String> threads = new HashSet<>();
        Set<String> days = new HashSet<>();

        getRows(logList, loggers,levels, threads);
        getCols(logList, days);

        // Convert sets to lists
        Map<String, Integer> loggerMap = convertToCountMap(loggers);
        Map<String, Integer> levelMap = convertToCountMap(levels);
        Map<String, Integer> threadMap = convertToCountMap(threads);
        Map<String, Integer> dayMap = convertToCountMap(days);

        // Sum occurrences
        for (LogEvent log : logList) {
            tallyUp(log.getLogger(), loggerMap);
            tallyUp(log.getLevel(), levelMap);
            tallyUp(log.getThread(), threadMap);
            log.setTimestamp(formatter.format(Instant.parse(log.getTimestamp()).truncatedTo(ChronoUnit.DAYS)));
            tallyUp(log.getTimestamp(), dayMap);
        }

        // Convert to lists and sort them
        loggerList = new ArrayList<>(loggerMap.keySet());
        loggerList.sort(Comparator.naturalOrder());
        levelList = new ArrayList<>(levelMap.keySet());
        levelList.sort(Comparator.naturalOrder());
        threadList = new ArrayList<>(threadMap.keySet());
        threadList.sort(Comparator.naturalOrder());
        dayList = new ArrayList<>(dayMap.keySet());
        dayList.sort((a,b) -> {
            Instant aTime = Instant.parse(a);
            Instant bTime = Instant.parse(b);
            return bTime.compareTo(aTime);
        });

        // Initialise data
        Integer[][] data = new Integer[loggerList.size()+levelList.size()+threadList.size()][dayList.size()];
        for(int i=0; i<data.length; i++) {
            for (int j=0; j<data[0].length; j++) {
                data[i][j] = 0;
            }
        }

        // Build the data
        for (int col=0; col<dayList.size(); col++) {

            for (LogEvent log : logList) {
                int row = 0;
                if (!log.getTimestamp().equals(dayList.get(col))) continue;

                //Logger rows
                for (int i = 0; i < loggerList.size(); i++) {
                    if (log.getLogger().equals(loggerList.get(i))) {
                        data[row][col]++;
                    }
                    row++;
                }
                //Level rows
                for (int i = 0; i < levelList.size(); i++) {
                    if (log.getLevel().equals(levelList.get(i))) {
                        data[row][col]++;
                    }
                    row++;
                }
                //Thread rows
                for (int i = 0; i < threadList.size(); i++) {
                    if (log.getThread().equals(threadList.get(i))) {
                        data[row][col]++;
                    }
                    row++;
                }
            }
        }
        return data;
    }


    private void tallyUp(Object logItem, Map<String, Integer> map) {
        for (String item : map.keySet()) {
            if (item.equals(logItem)) {
                map.put(item, map.get(item)+1);
            }
        }
    }


    private int getRows(Collection<LogEvent> logList, Set<String> loggers, Set<String> levels, Set<String> threads) {
        int rowCount = 0;

        for (LogEvent log : logList) {

            if (loggers.add(log.getLogger())) {
                rowCount++;
            }
            if (levels.add(log.getLevel())) {
                rowCount++;
            }
            if (threads.add(log.getThread())) {
                rowCount++;
            }
        }
        return rowCount;
    }


    private int getCols(Collection<LogEvent> logList, Set<String> days) {
        int colCount = 0;

        for (LogEvent log : logList) {
            if (days.add(formatter.format(Instant.parse(log.getTimestamp()).truncatedTo(ChronoUnit.DAYS)))) {
                colCount++;
            }
        }
        return colCount;
    }


    private Map<String, Integer> convertToCountMap(Set<String> itemSet) {
        Map<String, Integer> itemMap = new HashMap<>();
        for (String item : itemSet) {
            itemMap.put(item, 0);
        }
        return itemMap;
    }

}












