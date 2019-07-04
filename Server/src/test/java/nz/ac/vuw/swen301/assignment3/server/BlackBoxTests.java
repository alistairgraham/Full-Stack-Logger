package nz.ac.vuw.swen301.assignment3.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.junit.*;
import java.net.URI;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.IntStream;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class BlackBoxTests {
    private static DateTimeFormatter formatter = new DateTimeFormatterBuilder().appendInstant(3).toFormatter();
    private static final String SCHEME = "http";
    private static final String HOST = "localhost";
    private static final int PORT = 8080;
    private static final String PATH = "/resthome4logs"; // as defined in pom.xml
    private static final String LOGS_PATH = PATH + "/logs"; // as defined in pom.xml and web.xml
    private static final String STATS_PATH = PATH + "/stats"; // as defined in pom.xml and web.xml
    private static List<Level> logLevels = new ArrayList<>(Arrays.asList(Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR, Level.FATAL));
    private ObjectMapper objectMapper;

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


    @BeforeClass
    public static void message() {
        System.out.println("This is gonna take like 5 minutes... I'm sorry.");
    }


    @Before
    public void init() {
        objectMapper = new ObjectMapper();
    }


    @Before
    public void startServer() throws Exception {
        Thread.sleep(4000);
        Runtime.getRuntime().exec("mvn jetty:run");
        Thread.sleep(6000);
    }


    @After
    public void stopServer() throws Exception {
        Thread.sleep(4000);
        Runtime.getRuntime().exec("mvn jetty:stop");
        Thread.sleep(4000);
    }


    private HttpResponse get(URI uri) throws Exception {
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet(uri);
        return httpClient.execute(request);
    }


    private HttpResponse post(String logListJSON) throws Exception {
        URI uri = new URIBuilder().setScheme(SCHEME).setHost(HOST).setPort(PORT).setPath(LOGS_PATH).build();
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost(uri);
        StringEntity entity = new StringEntity(logListJSON);
        entity.setContentType(ContentType.APPLICATION_JSON.getMimeType());
        request.setEntity(entity);
        return httpClient.execute(request);
    }


    private boolean isServerReady() throws Exception {
        URIBuilder builder = new URIBuilder();
        builder.setScheme(SCHEME).setHost(HOST).setPort(PORT).setPath(PATH);
        URI uri = builder.build();
        try {
            HttpResponse response = get(uri);
            boolean success = response.getStatusLine().getStatusCode() == 200;

            if (!success) {
                System.err.println("Check whether server is up and running, request to " + uri + " returns " + response.getStatusLine());
            }
            return success;
        }
        catch (Exception x) {
            System.err.println("Encountered error connecting to " + uri + " -- check whether server is running and application has been deployed");
            return false;
        }
    }


    @Test
    public void testStats() throws Exception {
        Assume.assumeTrue(isServerReady());
        // post a log
        ArrayList<LogEvent> logList = new ArrayList<>();
        LogEvent log = new LogEvent();
        log.setup(UUID.randomUUID().toString(), "message", Instant.now().toString(), "thread", "logger", "ALL", "errorDetails");
        logList.add(log);
        String logListJSON = objectMapper.writeValueAsString(logList);
        post(logListJSON);
        // check response
        URI uri = new URIBuilder().setScheme(SCHEME).setHost(HOST).setPort(PORT).setPath(STATS_PATH).build();
        HttpResponse response = get(uri);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertTrue(response.getEntity().getContentType().getValue().startsWith("application/vnd.ms-excel"));
        HSSFWorkbook workbook = new HSSFWorkbook(response.getEntity().getContent());
        HSSFSheet sheet = workbook.getSheetAt(0);
        // column title
        assertEquals(formatter.format(Instant.parse(log.getTimestamp()).truncatedTo(ChronoUnit.DAYS)), sheet.getRow(0).getCell(1).getStringCellValue());
        // row titles
        assertEquals(log.getLogger(), sheet.getRow(1).getCell(0).getStringCellValue());
        assertEquals(log.getLevel(), sheet.getRow(2).getCell(0).getStringCellValue());
        assertEquals(log.getThread(), sheet.getRow(3).getCell(0).getStringCellValue());
        // intersection values
        assertEquals(1, Integer.parseInt(String.valueOf((int)sheet.getRow(1).getCell(1).getNumericCellValue())));
        assertEquals(1, Integer.parseInt(String.valueOf((int)sheet.getRow(2).getCell(1).getNumericCellValue())));
        assertEquals(1, Integer.parseInt(String.valueOf((int)sheet.getRow(3).getCell(1).getNumericCellValue())));
    }


    @Test
    public void testGet_Invalid_NoParams () throws Exception {
        Assume.assumeTrue(isServerReady());
        URIBuilder builder = new URIBuilder();
        builder.setScheme(SCHEME).setHost(HOST).setPort(PORT).setPath(LOGS_PATH);
        // No parameters
        URI uri = builder.build();
        HttpResponse response = get(uri);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
    }


    @Test
    public void testGet_Invalid_Limit () throws Exception {
        Assume.assumeTrue(isServerReady());
        URIBuilder builder = new URIBuilder();
        builder.setScheme(SCHEME).setHost(HOST).setPort(PORT).setPath(LOGS_PATH);
        String limit = "not a limit";
        String level = Level.ALL.toString();
        builder.addParameter("limit", limit);
        builder.addParameter("level", level);
        URI uri = builder.build();
        HttpResponse response = get(uri);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());

        limit = null;
        builder.addParameter("limit", limit);
        builder.addParameter("level", level);
        uri = builder.build();
        response = get(uri);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
    }


    @Test
    public void testGet_Invalid_Level () throws Exception {
        Assume.assumeTrue(isServerReady());
        URIBuilder builder = new URIBuilder();
        builder.setScheme(SCHEME).setHost(HOST).setPort(PORT).setPath(LOGS_PATH);
        String limit = ((Integer)logLevels.size()).toString();
        String level = "Not level";
        builder.addParameter("limit", limit);
        builder.addParameter("level", level);
        URI uri = builder.build();
        HttpResponse response = get(uri);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());

        level = null;
        builder.addParameter("limit", limit);
        builder.addParameter("level", level);
        uri = builder.build();
        response = get(uri);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
    }


    @Test
    public void testGet_Invalid_Limit_High () throws Exception {
        Assume.assumeTrue(isServerReady());
        URIBuilder builder = new URIBuilder();
        builder.setScheme(SCHEME).setHost(HOST).setPort(PORT).setPath(LOGS_PATH);
        String limit = "51";
        String level = Level.ALL.toString();
        builder.addParameter("limit", limit);
        builder.addParameter("level", level);
        URI uri = builder.build();
        HttpResponse response = get(uri);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
    }


    @Test
    public void testGet_Invalid_Limit_Low () throws Exception {
        Assume.assumeTrue(isServerReady());
        URIBuilder builder = new URIBuilder();
        builder.setScheme(SCHEME).setHost(HOST).setPort(PORT).setPath(LOGS_PATH);
        String limit = "0";
        String level = Level.ALL.toString();
        builder.addParameter("limit", limit);
        builder.addParameter("level", level);
        URI uri = builder.build();
        HttpResponse response = get(uri);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
    }


    // Because my tests take 20 seconds to restart the server, I didn't split this one up.
    @Test
    public void testGet_Levels() throws Exception {
        Assume.assumeTrue(isServerReady());
        ArrayList<LogEvent> postedLogList = new ArrayList<>();
        postLogOfEachLevel(postedLogList);

        URIBuilder builder = new URIBuilder();
        builder.setScheme(SCHEME).setHost(HOST).setPort(PORT).setPath(LOGS_PATH);
        String limit = ((Integer)logLevels.size()).toString();

        // Test ALL
        builder.addParameter("limit", limit);
        builder.addParameter("level", Level.ALL.toString());
        HttpResponse response = get(builder.build());
        String logsJSON = EntityUtils.toString(response.getEntity());
        LogEvent[] responseLogList = objectMapper.readValue(logsJSON, LogEvent[].class);
        assertEquals(logLevels.size(), responseLogList.length);
        assertTrue(allLevelsPresent(Level.ALL, responseLogList));

        // Test TRACE
        builder.clearParameters();
        builder.addParameter("limit", limit);
        builder.addParameter("level", Level.TRACE.toString());
        response = get(builder.build());
        logsJSON = EntityUtils.toString(response.getEntity());
        responseLogList = objectMapper.readValue(logsJSON, LogEvent[].class);
        assertEquals(logLevels.size(), responseLogList.length);
        assertTrue(allLevelsPresent(Level.TRACE, responseLogList));

        // Test DEBUG
        builder.clearParameters();
        builder.addParameter("limit", limit);
        builder.addParameter("level", Level.DEBUG.toString());
        response = get(builder.build());
        logsJSON = EntityUtils.toString(response.getEntity());
        responseLogList = objectMapper.readValue(logsJSON, LogEvent[].class);
        assertEquals(logLevels.size()-1, responseLogList.length);
        assertTrue(allLevelsPresent(Level.DEBUG, responseLogList));

        // Test INFO
        builder.clearParameters();
        builder.addParameter("limit", limit);
        builder.addParameter("level", Level.INFO.toString());
        response = get(builder.build());
        logsJSON = EntityUtils.toString(response.getEntity());
        responseLogList = objectMapper.readValue(logsJSON, LogEvent[].class);
        assertEquals(logLevels.size()-2, responseLogList.length);
        assertTrue(allLevelsPresent(Level.INFO, responseLogList));

        // Test WARN
        builder.clearParameters();
        builder.addParameter("limit", limit);
        builder.addParameter("level", Level.WARN.toString());
        response = get(builder.build());
        logsJSON = EntityUtils.toString(response.getEntity());
        responseLogList = objectMapper.readValue(logsJSON, LogEvent[].class);
        assertEquals(logLevels.size()-3, responseLogList.length);
        assertTrue(allLevelsPresent(Level.WARN, responseLogList));

        // Test ERROR
        builder.clearParameters();
        builder.addParameter("limit", limit);
        builder.addParameter("level", Level.ERROR.toString());
        response = get(builder.build());
        logsJSON = EntityUtils.toString(response.getEntity());
        responseLogList = objectMapper.readValue(logsJSON, LogEvent[].class);
        assertEquals(logLevels.size()-4, responseLogList.length);
        assertTrue(allLevelsPresent(Level.ERROR, responseLogList));

        // Test FATAL
        builder.clearParameters();
        builder.addParameter("limit", limit);
        builder.addParameter("level", Level.FATAL.toString());
        response = get(builder.build());
        logsJSON = EntityUtils.toString(response.getEntity());
        responseLogList = objectMapper.readValue(logsJSON, LogEvent[].class);
        assertEquals(logLevels.size()-5, responseLogList.length);
        assertTrue(allLevelsPresent(Level.FATAL, responseLogList));

        // Test OFF
        builder.clearParameters();
        builder.addParameter("limit", limit);
        builder.addParameter("level", Level.OFF.toString());
        response = get(builder.build());
        logsJSON = EntityUtils.toString(response.getEntity());
        responseLogList = objectMapper.readValue(logsJSON, LogEvent[].class);
        assertEquals(logLevels.size()-6, responseLogList.length);
        assertTrue(allLevelsPresent(Level.OFF, responseLogList));
    }


    @Test
    public void testGet_ValidRequest1() throws Exception {
        Assume.assumeTrue(isServerReady());
        ArrayList<LogEvent> postedLogList = new ArrayList<>();
        postLogOfEachLevel(postedLogList);

        URIBuilder builder = new URIBuilder();
        builder.setScheme(SCHEME).setHost(HOST).setPort(PORT).setPath(LOGS_PATH);
        String limit = ((Integer)logLevels.size()).toString();
        String level = Level.ALL.toString();
        builder.addParameter("limit", limit);
        builder.addParameter("level", level);
        URI uri = builder.build();

        HttpResponse response = get(uri);
        HttpEntity entity = response.getEntity();
        String logsJSON = EntityUtils.toString(entity);
        LogEvent[] responseLogList = objectMapper.readValue(logsJSON, LogEvent[].class);

        assertTrue(entity.getContentType().getValue().startsWith(ContentType.APPLICATION_JSON.getMimeType()));
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode()); //200
        assertEquals(logLevels.size(), responseLogList.length);
        // Check if all logs are present in response
        for (LogEvent log : postedLogList) {
            boolean match = false;
            for (int i=0; i<responseLogList.length; i++) {
                if (log.equals(responseLogList[i])) {
                    match = true;
                }
            }
            assertTrue(match);
        }
    }


    @Test
    public void testGet_ValidRequest2 () throws Exception {
        URIBuilder builder = new URIBuilder();
        builder.setScheme(SCHEME).setHost(HOST).setPort(PORT).setPath(LOGS_PATH);
        String limit = ((Integer)logLevels.size()).toString();
        String level = Level.ALL.toString();
        builder.addParameter("limit", limit);
        builder.addParameter("level", level);
        URI uri = builder.build();
        HttpResponse response = get(uri);

        HttpEntity entity = response.getEntity();
        String logsJSON = EntityUtils.toString(entity);
        LogEvent[] responseLogList = objectMapper.readValue(logsJSON, LogEvent[].class);

        assertEquals(0, responseLogList.length);
    }


    @Test
    public void testGet_ValidRequest3() throws Exception {
        Assume.assumeTrue(isServerReady());
        ArrayList<LogEvent> postedLogList = new ArrayList<>();
        postLogOfEachLevel(postedLogList);

        URIBuilder builder = new URIBuilder();
        builder.setScheme(SCHEME).setHost(HOST).setPort(PORT).setPath(LOGS_PATH);
        String limit = "2";
        String level = Level.ALL.toString();
        builder.addParameter("limit", limit);
        builder.addParameter("level", level);
        URI uri = builder.build();

        HttpResponse response = get(uri);
        HttpEntity entity = response.getEntity();
        String logsJSON = EntityUtils.toString(entity);
        LogEvent[] responseLogList = objectMapper.readValue(logsJSON, LogEvent[].class);

        assertEquals(Integer.parseInt(limit), responseLogList.length);
    }


    @Test
    public void testGet_ValidRequest_Ordered() throws Exception {
        Assume.assumeTrue(isServerReady());
        ArrayList<LogEvent> postedLogList = new ArrayList<>();
        postLogOfEachLevel(postedLogList);

        URIBuilder builder = new URIBuilder();
        builder.setScheme(SCHEME).setHost(HOST).setPort(PORT).setPath(LOGS_PATH);
        String limit = "2";
        String level = Level.ALL.toString();
        builder.addParameter("limit", limit);
        builder.addParameter("level", level);
        URI uri = builder.build();

        HttpResponse response = get(uri);
        HttpEntity entity = response.getEntity();
        String logsJSON = EntityUtils.toString(entity);
        LogEvent[] responseLogList = objectMapper.readValue(logsJSON, LogEvent[].class);

        assertTrue(isOrdered(responseLogList));
    }


    // Two tests in one because of the long time taken for tests to run
    @Test
    public void testPost_ValidRequest_Code() throws Exception {
        Assume.assumeTrue(isServerReady());
        ArrayList<LogEvent> logList = new ArrayList<>();

        LogEvent log = new LogEvent();
        log.setup(UUID.randomUUID().toString(), "myMessage", Instant.now().toString(), "MyThread", "MyLogger", Level.FATAL.toString(), "MyError");
        logList.add(log);
        String logListJSON = objectMapper.writeValueAsString(logList);
        HttpResponse response = post(logListJSON);
        assertEquals(HttpStatus.SC_CREATED,response.getStatusLine().getStatusCode());

        logList.clear();
        postLogOfEachLevel(logList);
        assertEquals(HttpStatus.SC_CREATED,response.getStatusLine().getStatusCode());
    }


    @Test
    public void testPost_ValidRequest_Values() throws Exception {
        Assume.assumeTrue(isServerReady());
        ArrayList<LogEvent> logList = new ArrayList<>();
        postLogOfEachLevel(logList);

        URIBuilder builder = new URIBuilder();
        builder.setScheme(SCHEME).setHost(HOST).setPort(PORT).setPath(LOGS_PATH);
        builder.addParameter("limit", ((Integer)logLevels.size()).toString());
        builder.addParameter("level", Level.ALL.toString());
        URI uri = builder.build();

        HttpResponse response = get(uri);
        HttpEntity entity = response.getEntity();
        String logsJSON = EntityUtils.toString(entity);
        LogEvent[] responseLogList = objectMapper.readValue(logsJSON, LogEvent[].class);
        // See if all logs were posted correctly
        for (LogEvent log : logList) {
            boolean match = false;
            for (int i=0; i<responseLogList.length; i++) {
                if (log.equals(responseLogList[i])) {
                    match = true;
                }
            }
            assertTrue(match);
        }
    }


    // Two tests in one because of the long time taken for tests to run
    @Test
    public void testPost_InvalidRequest_Entity() throws Exception {
        Assume.assumeTrue(isServerReady());
        URI uri = new URIBuilder().setScheme(SCHEME).setHost(HOST).setPort(PORT).setPath(LOGS_PATH).build();

        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost(uri);
        HttpResponse response = httpClient.execute(request);
        assertEquals(HttpStatus.SC_BAD_REQUEST,response.getStatusLine().getStatusCode());

        request = new HttpPost(uri);
        StringEntity entity = new StringEntity("");
        entity.setContentType(ContentType.APPLICATION_JSON.toString());
        request.setEntity(entity);
        response = httpClient.execute(request);
        assertEquals(HttpStatus.SC_BAD_REQUEST,response.getStatusLine().getStatusCode());
    }


    @Test
    public void testPost_InvalidRequest1() throws Exception {
        Assume.assumeTrue(isServerReady());
        URI uri = new URIBuilder().setScheme(SCHEME).setHost(HOST).setPort(PORT).setPath(LOGS_PATH).build();
        ArrayList<LogEvent> logList = new ArrayList<>();

        LogEvent log = new LogEvent();
        log.setup(UUID.randomUUID().toString(), "myMessage", Instant.now().toString(), "MyThread", "MyLogger", Level.FATAL.toString(), "MyError");
        logList.add(log);
        String logListJSON = objectMapper.writeValueAsString(logList);

        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost(uri);
        StringEntity entity = new StringEntity(logListJSON);
        // incorrect content type
        entity.setContentType(ContentType.TEXT_HTML.toString());
        request.setEntity(entity);
        HttpResponse response = httpClient.execute(request);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());

        // Incorrect JSON format
        response = post("incorrect");
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());

        // Empty JSON array
        response = post("[]");
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
    }


    // Two tests in one because of the long time taken for tests to run
    @Test
    public void testPost_InvalidRequest2() throws Exception {
        Assume.assumeTrue(isServerReady());
        ArrayList<LogEvent> logList = new ArrayList<>();

        // duplicate in request
        createLogOfEachLevel(logList);
        logList.add(logList.get(0));
        String logListJSON = objectMapper.writeValueAsString(logList);
        HttpResponse response = post(logListJSON);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());

        // wrong id format
        logList.clear();
        IntStream.range(0, logLevels.size()).forEach((i) -> {
            LogEvent log = new LogEvent();
            log.setup("IncorrectID"+i, "Message"+i, Instant.now().toString(), "MyThread"+i, "MyLogger"+i, logLevels.get(i).toString(), "MyError"+i);
            logList.add(log);
        });
        logListJSON = objectMapper.writeValueAsString(logList);
        response = post(logListJSON);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());

        // wrong date format
        logList.clear();
        IntStream.range(0, logLevels.size()).forEach((i) -> {
            LogEvent log = new LogEvent();
            log.setup(UUID.randomUUID().toString(), "Message"+i, "Incorrect date format", "MyThread"+i, "MyLogger"+i, logLevels.get(i).toString(), "MyError"+i);
            logList.add(log);
        });
        logListJSON = objectMapper.writeValueAsString(logList);
        response = post(logListJSON);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());

        // just post some
        logList.clear();
        response = postLogOfEachLevel(logList);
        assertEquals(HttpStatus.SC_CREATED, response.getStatusLine().getStatusCode());

        // try post another (duplicate logs)
        LogEvent log = logList.get(0);
        logList.clear();
        logList.add(log);
        logListJSON = objectMapper.writeValueAsString(logList);
        response = post(logListJSON);
        assertEquals(HttpStatus.SC_CONFLICT, response.getStatusLine().getStatusCode());

        URIBuilder builder = new URIBuilder();
        builder.setScheme(SCHEME).setHost(HOST).setPort(PORT).setPath(LOGS_PATH);
        builder.addParameter("limit", ((Integer)logLevels.size()).toString());
        builder.addParameter("level", Level.ALL.toString());
        URI uri = builder.build();

        response = get(uri);
        HttpEntity entity = response.getEntity();
        String logsJSON = EntityUtils.toString(entity);
        LogEvent[] responseLogList = objectMapper.readValue(logsJSON, LogEvent[].class);
        assertEquals(logLevels.size(), responseLogList.length);
    }


//    @Test
//    public void testPost_InvalidRequest() throws Exception {
//        Assume.assumeTrue(isServerReady());
//        ArrayList<LogEvent> logList = new ArrayList<>();
//
//        // duplicate in request
//        createLogOfEachLevel(logList);
//        logList.add(logList.get(0));
//        String logListJSON = objectMapper.writeValueAsString(logList);
//        HttpResponse response = post(logListJSON);
//        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
//
//        // wrong id format
//        logList.clear();
//        IntStream.range(0, logLevels.size()).forEach((i) -> {
//            LogEvent log = new LogEvent();
//            log.setup("IncorrectID"+i, "Message"+i, Instant.now().toString(), "MyThread"+i, "MyLogger"+i, logLevels.get(i).toString(), "MyError"+i);
//            logList.add(log);
//        });



    private boolean isOrdered(LogEvent[] logs) {
        for (int i=0; i<logs.length-1; i++) {
            Instant a = Instant.parse(logs[i].getTimestamp());
            Instant b = Instant.parse(logs[i+1].getTimestamp());
            if (a.compareTo(b) > 0) {
                return false;
            }
        }
        return true;
    }


    private HttpResponse postLogOfEachLevel(ArrayList<LogEvent> logList) throws Exception {
        createLogOfEachLevel(logList);
        String logListJSON = objectMapper.writeValueAsString(logList);
        return post(logListJSON);
    }

    private void createLogOfEachLevel(ArrayList<LogEvent> logList) {
        IntStream.range(0, logLevels.size()).forEach((i) -> {
            LogEvent log = new LogEvent();
            log.setup(UUID.randomUUID().toString(), "Message"+i, Instant.now().toString(), "MyThread"+i, "MyLogger"+i, logLevels.get(i).toString(), "MyError"+i);
            logList.add(log);
        });
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


}
