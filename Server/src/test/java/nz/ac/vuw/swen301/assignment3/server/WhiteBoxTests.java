package nz.ac.vuw.swen301.assignment3.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.IntStream;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class WhiteBoxTests {

    private ObjectMapper objectMapper;
    private List<Level> logLevels = new ArrayList<>(Arrays.asList(Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR, Level.FATAL));
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


    @Before
    public void init() {
        objectMapper = new ObjectMapper();
    }


    @Test
    public void testPost_ValidRequest1() throws IOException {
        LogService service = new LogService();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType(ContentType.APPLICATION_JSON.getMimeType());
        MockHttpServletResponse response = new MockHttpServletResponse();

        ArrayList<LogEvent> logList = createRandomLogList(1);
        String logListJSON = objectMapper.writeValueAsString(logList);

        request.setContent(logListJSON.getBytes());

        service.doPost(request, response);
        assertEquals(201,response.getStatus());
    }


    @Test
    public void testPost_ValidRequest2() throws IOException {
        LogService service = new LogService();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setContentType(ContentType.APPLICATION_JSON.getMimeType());

        ArrayList<LogEvent> logList = createRandomLogList(2);
        String logListJSON = objectMapper.writeValueAsString(logList);

        request.setContent(logListJSON.getBytes());
        service.doPost(request, response);
        assertEquals(201,response.getStatus());
    }


    @Test
    public void testPost_InvalidRequest_ContentType() throws IOException {
        LogService service = new LogService();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("text");
        MockHttpServletResponse response = new MockHttpServletResponse();

        ArrayList<LogEvent> logList = createRandomLogList(1);
        String logListJSON = objectMapper.writeValueAsString(logList);

        request.setContent(logListJSON.getBytes());
        service.doPost(request, response);
        assertEquals(400,response.getStatus());
    }


    @Test
    public void testPost_InvalidRequest_Duplicate1() throws IOException {
        LogService service = new LogService();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType(ContentType.APPLICATION_JSON.getMimeType());
        MockHttpServletResponse response = new MockHttpServletResponse();

        ArrayList<LogEvent> logList = createRandomLogList(1);
        logList.add(logList.get(0));
        assertEquals(2, logList.size());
        String logListJSON = objectMapper.writeValueAsString(logList);

        request.setContent(logListJSON.getBytes());
        service.doPost(request, response);
        assertEquals(HttpStatus.SC_BAD_REQUEST,response.getStatus());
    }


    @Test
    public void testPost_InvalidRequest_Duplicate2() throws IOException {
        LogService service = new LogService();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType(ContentType.APPLICATION_JSON.getMimeType());
        MockHttpServletResponse response = new MockHttpServletResponse();

        ArrayList<LogEvent> logList = createRandomLogList(1);
        String logListJSON = objectMapper.writeValueAsString(logList);

        request.setContent(logListJSON.getBytes());
        service.doPost(request, response);
        assertEquals(201,response.getStatus());
        request.setContent(logListJSON.getBytes());
        service.doPost(request, response);
        assertEquals(HttpStatus.SC_CONFLICT,response.getStatus());
    }

    @Test
    public void testPost_InvalidRequest_ID() throws IOException {
        LogService service = new LogService();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType(ContentType.APPLICATION_JSON.getMimeType());
        MockHttpServletResponse response = new MockHttpServletResponse();

        ArrayList<LogEvent> logList = new ArrayList<>();
        LogEvent log = new LogEvent();
        log.setup("Invalid ID", "Message", (new Date()).toInstant().toString(), "MyThread", "MyLogger", getRandomLevel().toString(), "MyError");
        logList.add(log);
        String logListJSON = objectMapper.writeValueAsString(logList);

        request.setContent(logListJSON.getBytes());
        service.doPost(request, response);
        assertEquals(HttpStatus.SC_BAD_REQUEST,response.getStatus());
    }


    @Test
    public void testPost_InvalidRequest_Date() throws IOException {
        LogService service = new LogService();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType(ContentType.APPLICATION_JSON.getMimeType());
        MockHttpServletResponse response = new MockHttpServletResponse();

        ArrayList<LogEvent> logList = new ArrayList<>();
        LogEvent log = new LogEvent();
        log.setup(UUID.randomUUID().toString(), "Message", "Invalid Date", "MyThread", "MyLogger", getRandomLevel().toString(), "MyError");
        logList.add(log);
        String logListJSON = objectMapper.writeValueAsString(logList);

        request.setContent(logListJSON.getBytes());
        service.doPost(request, response);
        assertEquals(HttpStatus.SC_BAD_REQUEST,response.getStatus());
    }


    @Test
    public void testGet_Invalid_NoParams() throws IOException {
        LogService service = new LogService();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // no request parameters
        service.doGet(request, response);
        assertEquals(400, response.getStatus());
    }


    @Test
    public void testGet_Invalid_Limit() throws IOException {
        LogService service = new LogService();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        // incorrect limit
        request.addParameter("limit", "Not int");
        request.addParameter("level", Level.ALL.toString());
        service.doGet(request, response);
        assertEquals(400, response.getStatus());
    }


    @Test
    public void testGet_Invalid_Level() throws IOException {
        LogService service = new LogService();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        // incorrect level
        request.addParameter("limit", "1");
        request.addParameter("level", "Not Level");
        service.doGet(request, response);
        assertEquals(400, response.getStatus());
    }


    @Test
    public void testGet_Invalid_Limit_Low() throws IOException {
        LogService service = new LogService();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        // incorrect limit
        request.addParameter("limit", "0");
        request.addParameter("level", Level.ALL.toString());
        service.doGet(request, response);
        assertEquals(400, response.getStatus());
    }


    @Test
    public void testGet_Invalid_Limit_High() throws IOException {
        LogService service = new LogService();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        // incorrect limit
        request.addParameter("limit", "51");
        request.addParameter("level", Level.ALL.toString());
        service.doGet(request, response);
        assertEquals(400,response.getStatus());
    }


    @Test
    public void testGet_Levels() throws ServletException, IOException {
        LogService service = new LogService();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setContentType(ContentType.APPLICATION_JSON.getMimeType());

        // Add one of each level
        ArrayList<LogEvent> logList = new ArrayList<>();
        IntStream.range(0, logLevels.size()).forEach((i) -> {
            LogEvent log = new LogEvent();
            log.setup(UUID.randomUUID().toString(), "Message"+i, (new Date()).toInstant().toString(), "MyThread"+i, "MyLogger"+i, logLevels.get(i).toString(), "Error"+i);
            logList.add(log);
        });
        String logListJSON = objectMapper.writeValueAsString(logList);
        request.setContent(logListJSON.getBytes());
        service.doPost(request, response);

        // Test ALL
        LogEvent[] logArr = getLogsGivenLevel(Level.ALL, service);
        assertEquals(logLevels.size(), logArr.length);
        assertTrue(allLevelsPresent(Level.ALL, logArr));
        // Test Trace
        logArr = getLogsGivenLevel(Level.TRACE, service);
        assertEquals(logLevels.size(), logArr.length);
        assertTrue(allLevelsPresent(Level.TRACE, logArr));
        // Test DEBUG
        logArr = getLogsGivenLevel(Level.DEBUG, service);
        assertEquals(logLevels.size()-1, logArr.length);
        assertTrue(allLevelsPresent(Level.DEBUG, logArr));
        // Test INFO
        logArr = getLogsGivenLevel(Level.INFO, service);
        assertEquals(logLevels.size()-2, logArr.length);
        assertTrue(allLevelsPresent(Level.INFO, logArr));
        // Test WARN
        logArr = getLogsGivenLevel(Level.WARN, service);
        assertEquals(logLevels.size()-3, logArr.length);
        assertTrue(allLevelsPresent(Level.WARN, logArr));
        // Test ERROR
        logArr = getLogsGivenLevel(Level.ERROR, service);
        assertEquals(logLevels.size()-4, logArr.length);
        assertTrue(allLevelsPresent(Level.ERROR, logArr));
        // Test FATAL
        logArr = getLogsGivenLevel(Level.FATAL, service);
        assertEquals(logLevels.size()-5, logArr.length);
        assertTrue(allLevelsPresent(Level.FATAL, logArr));
        // Test OFF
        logArr = getLogsGivenLevel(Level.OFF, service);
        assertEquals(logLevels.size()-6, logArr.length);
        assertTrue(allLevelsPresent(Level.OFF, logArr));

    }


    private LogEvent[] getLogsGivenLevel(Level level, LogService service) throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addParameter("limit", "50");
        request.addParameter("level", level.toString());
        service.doGet(request, response);
        LogEvent[] logArr = objectMapper.readValue(response.getContentAsString(), LogEvent[].class);
        return logArr;
    }


    private boolean allLevelsPresent(Level level, LogEvent[] logArr) {
        boolean allPresent = true;

        for (int i=0; i<logArr.length; i++) {
            boolean match = false;
            for (int j=level.ordinal(); j<Level.values().length; j++) {
                if (Level.values()[j].equals(Level.valueOf(logArr[i].getLevel()))) {
                    match = true;
                }
            }
            if (!match) allPresent = false;
        }
        return allPresent;
    }


    @Test
    public void testGet_ValidRequest() throws ServletException, IOException {
        LogService service = new LogService();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        // Enter some data
        postLogs(service, request);
        // Test get
        request = new MockHttpServletRequest();
        Integer limit = 5;
        Level level = Level.ALL;
        request.addParameter("limit", limit.toString());
        request.addParameter("level", level.toString());
        service.doGet(request, response);
        assertEquals(ContentType.APPLICATION_JSON.getMimeType(), response.getContentType());
        assertEquals(200, response.getStatus());

        LogEvent[] logs = objectMapper.readValue(response.getContentAsString(), LogEvent[].class);
        assertEquals(limit.intValue(), logs.length);
        assertTrue(areLogsOrdered(logs));
    }


    private void postLogs(LogService service, HttpServletRequest req) throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType(ContentType.APPLICATION_JSON.getMimeType());
        MockHttpServletResponse response = new MockHttpServletResponse();
        ArrayList<LogEvent> logList = createRandomLogList(10);
        String logListJSON = objectMapper.writeValueAsString(logList);
        request.setContent(logListJSON.getBytes());
        service.doPost(request, response);
    }


    private boolean areLogsOrdered(LogEvent[] logs) {
        for (int i=0; i<logs.length-1; i++) {
            Instant a = Instant.parse(logs[i].getTimestamp());
            Instant b = Instant.parse(logs[i+1].getTimestamp());
            if (a.compareTo(b) < 0) {
                return false;
            }
        }
        return true;
    }


    @Test
    public void testPostInvalidRequest1() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        // no request parameters
        MockHttpServletResponse response = new MockHttpServletResponse();

        LogService service = new LogService();
        service.doGet(request, response);
        assertEquals(400,response.getStatus());
    }


    private ArrayList<LogEvent> createRandomLogList(int size) {
        ArrayList<LogEvent> logList = new ArrayList<>();
        IntStream.range(0, size).forEach((i) -> {
            LogEvent log = new LogEvent();
            log.setup(UUID.randomUUID().toString(), "Message"+i, (new Date()).toInstant().toString(), "MyThread"+i, "MyLogger"+i, getRandomLevel().toString());
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                return;
            }
            logList.add(log);
        });
        return logList;
    }

    private Level getRandomLevel() {
        int random = (int) Math.round((Math.random()*(Level.values().length-1)));
        return Level.values()[random];
    }



}
