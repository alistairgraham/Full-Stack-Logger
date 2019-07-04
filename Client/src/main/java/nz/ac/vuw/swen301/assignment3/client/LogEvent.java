package nz.ac.vuw.swen301.assignment3.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

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

    public String getErrorDetails() {
        return errorDetails;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getThread() {
        return thread;
    }

    public String getLogger() {
        return logger;
    }

    public String getMessage() {
        return message;
    }

    public String getLevel() {
        return level;
    }


    @Override
    public String toString() {
        return "LogEvent{" +
                "id='" + id + '\'' +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                ", thread='" + thread + '\'' +
                ", logger='" + logger + '\'' +
                ", level='" + level + '\'' +
                '}';
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogEvent otherLog = (LogEvent) o;
        return id.equals(otherLog.id);
    }


    @Override
    public int hashCode() {
        return Objects.hash(id, message, timestamp, thread, logger, level);
    }


}