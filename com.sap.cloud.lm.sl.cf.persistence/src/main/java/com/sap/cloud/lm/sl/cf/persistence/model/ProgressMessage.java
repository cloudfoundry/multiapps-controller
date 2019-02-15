package com.sap.cloud.lm.sl.cf.persistence.model;

import java.util.Date;

public class ProgressMessage {
    private static final int MAX_TEXT_LENGTH = 4000;

    private long id;
    private String processId;
    private String taskId;
    private ProgressMessageType type;
    private String text;
    private Date timestamp;

    public ProgressMessage() {
    }

    public ProgressMessage(String processId, String taskId, ProgressMessageType type, String text, Date timestamp) {
        this.processId = processId;
        this.taskId = taskId;
        this.type = type;
        setText(text);
        this.timestamp = timestamp;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public ProgressMessageType getType() {
        return type;
    }

    public void setType(ProgressMessageType type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        if (text.length() > MAX_TEXT_LENGTH) {
            this.text = text.substring(0, MAX_TEXT_LENGTH - 3) + "...";
        } else {
            this.text = text;
        }
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public enum ProgressMessageType {
        ERROR, WARNING, INFO, EXT, TASK_STARTUP,
    }

    @Override
    public String toString() {
        return "ProgressMessage [id=" + id + ", processId=" + processId + ", taskId=" + taskId + ", type=" + type + ", text=" + text
            + ", timestamp=" + timestamp + "]";
    }
}
