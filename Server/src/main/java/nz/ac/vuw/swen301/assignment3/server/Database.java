package nz.ac.vuw.swen301.assignment3.server;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Database {
    private static Map<String, LogEvent> logMap = new ConcurrentHashMap<>();

    protected static LogEvent add(String key, LogEvent value) {
        return logMap.put(key, value);
    }

    protected static Collection<LogEvent> getValues() {
        return logMap.values();
    }

    protected static boolean containsKey(String key) {
        return logMap.containsKey(key);
    }
}
