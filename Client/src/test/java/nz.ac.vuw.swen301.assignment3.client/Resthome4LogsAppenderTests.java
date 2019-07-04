package nz.ac.vuw.swen301.assignment3.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.*;
import org.junit.runners.MethodSorters;

import java.net.URI;
import java.util.ArrayList;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class Resthome4LogsAppenderTests {

    /** ////////////// Please note that my appender only posts logs every 10th time (BATCH_SIZE) //////////////// **/
    /** ///////// These tests build on each other which is not ideal but avoids restarting the server /////////// **/

    private static final String TEST_SCHEME = "http";
    private static final String TEST_HOST = "localhost";
    private static final int TEST_PORT = 8080;
    private static final String TEST_PATH = "/resthome4logs";
    private static final String SERVICE_PATH = TEST_PATH + "/logs";
    private static final String MAX_LIMIT = "50";
    private static int LOG_COUNT = 0;
    public static final int BATCH_SIZE = 10;
    private ObjectMapper objectMapper;
    private Logger logger;
    private static ArrayList<String> messageList = new ArrayList<>();
    private static ArrayList<String> levelList = new ArrayList<>();
    private enum MyLevel {
        TRACE,
        DEBUG,
        INFO,
        WARN,
        ERROR,
        FATAL
    }


    @BeforeClass
    public static void initialise_Class() {
        Logger.getLogger("org.apache.http").setLevel(Level.OFF);
    }


    @Before
    public void initialise_Test() throws InterruptedException {
        objectMapper = new ObjectMapper();
        logger = Logger.getLogger("myLogger1");
        logger.removeAllAppenders();
        logger.setLevel(Level.ALL);
        Resthome4LogsAppender appender = new Resthome4LogsAppender();
        logger.addAppender(appender);
    }


    @Test
    public void testAInvalidPost() throws Exception {
        Assume.assumeTrue(isServerReady());
        try {
            logger.log(null, null);
        } catch (NullPointerException e) {
            return;
        }
        assertFalse("Should have created null pointer exception", true);

        // post 9 logs
        for (int i=0; i<BATCH_SIZE-1; i++) {
            String message = "M"+ LOG_COUNT;
            Level level = Level.toLevel(MyLevel.values()[(i%MyLevel.values().length)].toString());
            messageList.add(message);
            levelList.add(level.toString());
            logger.log(level, message);
            LOG_COUNT++;
            Thread.sleep(1);
        }
        // try posting 1 null one (batch size is 10)
        LoggingEvent log = null;
        ((Resthome4LogsAppender)logger.getAllAppenders().nextElement()).append(log);
        // get logs
        HttpResponse response = getLogsFromServer(MAX_LIMIT, Level.ALL);
        assertEquals(ContentType.APPLICATION_JSON.getMimeType(), response.getEntity().getContentType().getValue());
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        String logsJSON = EntityUtils.toString(response.getEntity());
        LogEvent[] responseLogList = objectMapper.readValue(logsJSON, LogEvent[].class);

        assertEquals(0, responseLogList.length);
    }


    @Test
    public void testBValid_Post() throws Exception {
        Assume.assumeTrue(isServerReady());
        logger.setLevel(Level.ALL);
        int numLogs = BATCH_SIZE;

        // post logs
        for (int i=0; i<numLogs; i++) {
            String message = "M"+ LOG_COUNT;
            Level level = Level.toLevel(MyLevel.values()[(i%MyLevel.values().length)].toString());
            messageList.add(message);
            levelList.add(level.toString());
            logger.log(level, message);
            LOG_COUNT++;
            Thread.sleep(1);
        }

        // get logs
        HttpResponse response = getLogsFromServer(MAX_LIMIT, Level.ALL);
        assertEquals(ContentType.APPLICATION_JSON.getMimeType(), response.getEntity().getContentType().getValue());
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        String logsJSON = EntityUtils.toString(response.getEntity());
        LogEvent[] responseLogList = objectMapper.readValue(logsJSON, LogEvent[].class);

        assertEquals(numLogs, responseLogList.length);
        assertTrue(logsMatch(responseLogList, messageList, levelList));
    }


    @Test
    public void testCValid_Post_More() throws Exception {
        Assume.assumeTrue(isServerReady());
        logger.setLevel(Level.ALL);

        // post logs
        for (int i=0; i<BATCH_SIZE; i++) {
            String message = "M"+ LOG_COUNT;
            Level level = Level.toLevel(MyLevel.values()[(i%MyLevel.values().length)].toString());
            messageList.add(message);
            levelList.add(level.toString());
            logger.log(level, message);
            LOG_COUNT++;
            Thread.sleep(1);
        }
        // get logs
        HttpResponse response = getLogsFromServer(MAX_LIMIT, Level.ALL);
        // test response
        assertEquals(ContentType.APPLICATION_JSON.getMimeType(), response.getEntity().getContentType().getValue());
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        String logsJSON = EntityUtils.toString(response.getEntity());
        LogEvent[] responseLogList = objectMapper.readValue(logsJSON, LogEvent[].class);

        assertEquals(BATCH_SIZE*2, responseLogList.length);
        assertTrue(logsMatch(responseLogList, messageList, levelList));
    }


    @Test
    public void testDMax_Level() throws Exception {
        Assume.assumeTrue(isServerReady());
        logger.setLevel(Level.FATAL);

        // should only actually post Fatal logs
        for (int i=0; i<BATCH_SIZE*MyLevel.values().length; i++) {
            String message = "M"+ LOG_COUNT;
            Level level = Level.toLevel(MyLevel.values()[(i%MyLevel.values().length)].toString());
            if (level.equals(Level.FATAL)) {
                messageList.add(message);
                levelList.add(level.toString());
            }
            logger.log(level, message);
            LOG_COUNT++;
            Thread.sleep(1);
        }
        // get logs
        HttpResponse response = getLogsFromServer(MAX_LIMIT, Level.ALL);
        assertEquals(ContentType.APPLICATION_JSON.getMimeType(), response.getEntity().getContentType().getValue());
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        String logsJSON = EntityUtils.toString(response.getEntity());
        LogEvent[] responseLogList = objectMapper.readValue(logsJSON, LogEvent[].class);

        assertEquals(BATCH_SIZE*3, responseLogList.length);
        for (int i=0; i<responseLogList.length-(BATCH_SIZE*2); i++) {
            if (!responseLogList[i].getLevel().equals(Level.FATAL.toString())) {
                assertFalse("Only FATAL should be logged", true);
            }
        }
    }

    @Test
    public void testEDifferentLogger() throws Exception {
        Assume.assumeTrue(isServerReady());
        logger = Logger.getLogger("MyLogger2");
        logger.removeAllAppenders();
        Resthome4LogsAppender appender = new Resthome4LogsAppender();
        logger.setLevel(Level.ALL);
        logger.addAppender(appender);

        // post logs
        for (int i=0; i<BATCH_SIZE; i++) {
            String message = "M"+ LOG_COUNT;
            Level level = Level.toLevel(MyLevel.values()[(i%MyLevel.values().length)].toString());
            messageList.add(message);
            levelList.add(level.toString());
            logger.log(level, message);
            LOG_COUNT++;
            Thread.sleep(1);
        }

        // get logs
        HttpResponse response = getLogsFromServer(MAX_LIMIT, Level.ALL);
        assertEquals(ContentType.APPLICATION_JSON.getMimeType(), response.getEntity().getContentType().getValue());
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        String logsJSON = EntityUtils.toString(response.getEntity());
        LogEvent[] responseLogList = objectMapper.readValue(logsJSON, LogEvent[].class);

        assertEquals(BATCH_SIZE*4, responseLogList.length);
        assertTrue(logsMatch(responseLogList, messageList, levelList));
    }


    @Test
    public void testF_layout() {
        assertFalse("Layout should not be required", ((Appender)logger.getAllAppenders().nextElement()).requiresLayout());
    }


    @Test
    public void testG_close() throws Exception {
        Assume.assumeTrue(isServerReady());
        Resthome4LogsAppender appender = new Resthome4LogsAppender();
        logger = Logger.getLogger("ClosableLogger");
        logger.addAppender(appender);
        logger.setLevel(Level.ALL);

        // post 1 log
        String message = "MyMsg";
        Level level = Level.ERROR;
        messageList.add(message);
        levelList.add(level.toString());
        logger.log(level, message);

        appender.close();

        // get logs
        HttpResponse response = getLogsFromServer(MAX_LIMIT, Level.ALL);
        assertEquals(ContentType.APPLICATION_JSON.getMimeType(), response.getEntity().getContentType().getValue());
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        String logsJSON = EntityUtils.toString(response.getEntity());
        LogEvent[] responseLogList = objectMapper.readValue(logsJSON, LogEvent[].class);

        assertEquals((BATCH_SIZE*4)+1, responseLogList.length);
        assertTrue(logsMatch(responseLogList, messageList, levelList));

        // post logs while closed
        for (int i=0; i<BATCH_SIZE; i++) {
            message = "M"+ LOG_COUNT;
            level = Level.toLevel(MyLevel.values()[(i%MyLevel.values().length)].toString());
            messageList.add(message);
            levelList.add(level.toString());
            logger.log(level, message);
            LOG_COUNT++;
            Thread.sleep(1);
        }
        // get logs
        response = getLogsFromServer(MAX_LIMIT, Level.ALL);
        logsJSON = EntityUtils.toString(response.getEntity());
        responseLogList = objectMapper.readValue(logsJSON, LogEvent[].class);
        assertEquals((BATCH_SIZE*4)+1, responseLogList.length);

        // post 9 logs
        for (int i=0; i<BATCH_SIZE-1; i++) {
            message = "M"+ LOG_COUNT;
            level = Level.toLevel(MyLevel.values()[(i%MyLevel.values().length)].toString());
            messageList.add(message);
            levelList.add(level.toString());
            logger.log(level, message);
            LOG_COUNT++;
            Thread.sleep(1);
        }
        // try posting 1 null one (batch size is 10)
        LoggingEvent log = null;
        appender.append(log);
        // get logs
        response = getLogsFromServer(MAX_LIMIT, Level.ALL);
        logsJSON = EntityUtils.toString(response.getEntity());
        responseLogList = objectMapper.readValue(logsJSON, LogEvent[].class);
        assertEquals((BATCH_SIZE*4)+1, responseLogList.length);
    }


    private boolean logsMatch(LogEvent[] logList, ArrayList<String> messageList, ArrayList<String> levelList) {
        if (logList==null || messageList==null || levelList==null) {
            return false;
        }
        int numLogs = logList.length;
        if (numLogs!=messageList.size() && numLogs!=levelList.size()) {
            return false;
        }
        for (int i=0; i<numLogs; i++) {
            LogEvent log = logList[numLogs-i-1];
            if (!log.getMessage().equals(messageList.get(i)) || !log.getLevel().equals(levelList.get(i))){
                return false;
            }
        }
        return true;
    }


    private HttpResponse getLogsFromServer(String limit, Level level) throws Exception {
        URIBuilder builder = new URIBuilder();
        builder.setScheme(TEST_SCHEME).setHost(TEST_HOST).setPort(TEST_PORT).setPath(SERVICE_PATH);
        builder.addParameter("limit", limit);
        builder.addParameter("level", level.toString());
        return get(builder.build());
    }


    private HttpResponse get(URI uri) throws Exception {
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet(uri);
        return httpClient.execute(request);
    }


    private boolean isServerReady() throws Exception {
        URIBuilder builder = new URIBuilder();
        builder.setScheme(TEST_SCHEME).setHost(TEST_HOST).setPort(TEST_PORT).setPath(TEST_PATH);
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

}
