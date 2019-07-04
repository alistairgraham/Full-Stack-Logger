package nz.ac.vuw.swen301.assignment3.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import java.util.Optional;

public class LogEvent {
    @JsonProperty
    private String id;
    @JsonProperty
    private String message;
    @JsonProperty
    private String timestamp;
    @JsonProperty
    private String thread;
    @JsonProperty
    private String logger;
    @JsonProperty
    private String level;
    @JsonProperty
    private String errorDetails;

    public void setup(String id, String message, String timestamp, String thread, String logger, String level, String errorDetails) {
        this.id = id;
        this.message = message;
        this.timestamp = timestamp;
        this.thread = thread;
        this.logger = logger;
        this.level = level;
        this.errorDetails = errorDetails;
    }

    public void setup(String id, String message, String timestamp, String thread, String logger, String level) {
        this.id = id;
        this.message = message;
        this.timestamp = timestamp;
        this.thread = thread;
        this.logger = logger;
        this.level = level;
        this.errorDetails = "";
    }


    public String getId() { return id; }

    public String getTimestamp() {
        return timestamp;
    }

    public String getMessage() {
        return message;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getLevel() {
        return level;
    }

    public String getLogger() { return logger; }

    public String getThread() { return thread; }

    @Override
    public String toString() {
        return "LogEvent{" +
                "id='" + id + '\'' +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                ", thread='" + thread + '\'' +
                ", logger='" + logger + '\'' +
                ", level='" + level + '\'' +
                ", errorDetails='" + errorDetails + '\'' +
                '}';
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogEvent otherLog = (LogEvent) o;
        return id.equals(otherLog.id) &&
                message.equals(otherLog.message) &&
                timestamp.equals(otherLog.timestamp) &&
                thread.equals(otherLog.thread) &&
                logger.equals(otherLog.logger) &&
                level.equals(otherLog.level) &&
                (errorDetails!=null)?errorDetails.equals(otherLog.errorDetails):otherLog.errorDetails==null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, message, timestamp, thread, logger, level);
    }


}
