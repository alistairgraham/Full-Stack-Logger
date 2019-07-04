package nz.ac.vuw.swen301.assignment3.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.entity.ContentType;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;


public class LogService extends HttpServlet {
    private static final int MIN_LOGS = 1;
    private static final int MAX_LOGS = 50;
    private ObjectMapper objectMapper = new ObjectMapper();
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


    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String limit = req.getParameter("limit");
        String level = req.getParameter("level");
        // Validating request
        if (limit==null || level==null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        int maxLogs = 0;
        Level maxLevel;
        try {
            maxLogs = Integer.parseInt(limit);
            maxLevel = Level.valueOf(level);
        }
        catch (IllegalArgumentException | NullPointerException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        if (maxLogs<MIN_LOGS || maxLogs>MAX_LOGS) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // Process the logs
        ArrayList<LogEvent> currentLogs = new ArrayList<>();
        ArrayList<LogEvent> filteredLogs = new ArrayList<>();
        int maxLevelIndex = maxLevel.ordinal();
        currentLogs.addAll(Database.getValues());
        //Remove logs outside of level
        for (int i=0; i<currentLogs.size(); i++) {
            LogEvent log = currentLogs.get(i);
            int logIndex = Level.valueOf(log.getLevel()).ordinal();
            if (logIndex >= maxLevelIndex) {
                filteredLogs.add(log);
            }
        }
        filteredLogs.sort((a,b) -> {
            Instant aTime = Instant.parse(a.getTimestamp());
            Instant bTime = Instant.parse(b.getTimestamp());
            int result = bTime.compareTo(aTime);
            if (result==0) {
                result = b.getId().compareTo(a.getId());
            }
            return result;
        });

        // Create JSON from logs
        String logListJSON = objectMapper.writeValueAsString(filteredLogs.subList(0, (maxLogs<filteredLogs.size())?maxLogs:filteredLogs.size()));
        resp.setContentType(ContentType.APPLICATION_JSON.getMimeType());
        PrintWriter out = resp.getWriter();
        out.print(logListJSON);
        out.close();
    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (req==null || resp==null) {
            return;
        }

        BufferedReader br = req.getReader();
        String logsJSON = br.lines().collect(Collectors.joining());

        if (logsJSON == null || logsJSON.isEmpty() || req.getContentType()==null || !req.getContentType().startsWith(ContentType.APPLICATION_JSON.getMimeType())) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400
            return;
        }

        // Try create LogEvents from the data
        LogEvent[] logArray;
        try {
            logArray = createLogArray(logsJSON);
        } catch (IOException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400
            return;
        }
        if (logArray == null || logArray.length == 0) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400
            return;
        }
        // Check data types
        for (int i=0; i<logArray.length; i++) {
            LogEvent log = logArray[i];
            try {
                UUID.fromString(log.getId());
                String time = log.getTimestamp();
                if (!time.matches("\\d{4}-\\d{2}-\\d{2}[T]\\d{2}:\\d{2}:\\d{2}.\\d{3}Z")) {
                    throw new DateTimeException("Not ISO8601 date");
                }
                Instant.parse(time);
            } catch (IllegalArgumentException | DateTimeException e) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400
                return;
            }
        }
        // Check for duplicates in request
        for (int i=0; i<logArray.length; i++) {
            for (int j=0; j<logArray.length; j++) {
                if (i!=j && logArray[i]!=null && (logArray[i] == logArray[j] || logArray[i].equals(logArray[j]))) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400
                    return;
                }
            }
        }
        // Check for duplicates in cache
        for (int i=0; i<logArray.length; i++) {
            if (Database.containsKey(logArray[i].getId())) {
                resp.setStatus(HttpServletResponse.SC_CONFLICT); // 409
                return;
            }
        }
        // Successfully add logs to cache
        for (int i=0; i<logArray.length; i++) {
            Database.add(logArray[i].getId(), logArray[i]);
        }
        resp.setStatus(HttpServletResponse.SC_CREATED); // 201
    }


    private LogEvent[] createLogArray(String logs) throws IOException {
        LogEvent[] logArray;
        logArray = objectMapper.readValue(logs, LogEvent[].class);
        return logArray;
    }

}
