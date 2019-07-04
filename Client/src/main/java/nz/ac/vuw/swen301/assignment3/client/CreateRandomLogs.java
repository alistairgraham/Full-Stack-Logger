package nz.ac.vuw.swen301.assignment3.client;

import com.fasterxml.jackson.databind.introspect.TypeResolutionContext;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.Configurator;
import org.apache.log4j.spi.LoggingEvent;

import javax.security.auth.login.Configuration;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static nz.ac.vuw.swen301.assignment3.client.Resthome4LogsAppender.BATCH_SIZE;

public class CreateRandomLogs {
    private static int id = 0;
    private static Logger logger;
    private enum MyLevel {
        TRACE,
        DEBUG,
        INFO,
        WARN,
        ERROR,
        FATAL
    }

    private static void createRandomLogs() {
        long start = System.currentTimeMillis();
        for(int count = 0; true; count++) {
            Random r = new Random();
            logger = Logger.getLogger("MyLogger"+(int)((Math.random()*2)+1));
            int levelNum = ThreadLocalRandom.current().nextInt(0, MyLevel.values().length);
            String message = "M" + id++;
            Level level = Level.toLevel(MyLevel.values()[levelNum].toString());
            Instant timeStamp = Instant.now().plus((r.nextInt(3)), ChronoUnit.DAYS);

            LoggingEvent log = new LoggingEvent(null, logger, timeStamp.toEpochMilli(), level, message, null);
            logger.callAppenders(log);

            // Wait for one second
            while((System.currentTimeMillis()-start-(count*1000)<1000)) {}
        }
    }


    public static void main (String[] args) {
        Logger.getLogger("org.apache.http").setLevel(Level.OFF);

        Resthome4LogsAppender appender = new Resthome4LogsAppender();
        Logger.getLogger("MyLogger1").addAppender(appender);
        Logger.getLogger("MyLogger2").addAppender(appender);
        createRandomLogs();
    }
}
