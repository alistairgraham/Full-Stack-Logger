package nz.ac.vuw.swen301.assignment3.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

public class Resthome4LogsAppender extends AppenderSkeleton {
    static final String TEST_SCHEME = "http";
    private static final String TEST_HOST = "localhost";
    private static final int TEST_PORT = 8080;
    private static final String TEST_PATH = "/resthome4logs";
    private static final String SERVICE_PATH = TEST_PATH + "/logs";
    private boolean closed = false;
    private ObjectMapper objectMapper = new ObjectMapper();
    private ArrayList<LogEvent> logList = new ArrayList<>();
    public static final int BATCH_SIZE = 10;


    @Override
    protected void append(LoggingEvent log) {
        if (closed || log==null){return;}

        LogEvent logEvent = new LogEvent();
        logEvent.setup(UUID.randomUUID().toString(), log.getRenderedMessage(), Instant.ofEpochMilli(log.getTimeStamp()).toString(), log.getThreadName(), log.getLoggerName(), log.getLevel().toString(), "yo");
        logList.add(logEvent);

        if (logList.size() >= BATCH_SIZE) {
            postLogsBatch();
        }
    }

    private void postLogsBatch() {
        try {
            String logJson = objectMapper.writeValueAsString(logList);
            URI uri = new URIBuilder().setScheme(TEST_SCHEME).setHost(TEST_HOST).setPort(TEST_PORT).setPath(SERVICE_PATH).build();
            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpPost request = new HttpPost(uri);

            StringEntity entity = new StringEntity(logJson);
            entity.setContentType(ContentType.APPLICATION_JSON.toString());
            request.setEntity(entity);
            httpClient.execute(request);
            logList.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void close() {
        if (logList.size()>0) {
            postLogsBatch();
        }
        closed = true;
    }


    @Override
    public boolean requiresLayout() {
        return false;
    }

}
