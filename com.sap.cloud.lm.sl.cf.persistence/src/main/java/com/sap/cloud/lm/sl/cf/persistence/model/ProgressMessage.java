package com.sap.cloud.lm.sl.cf.persistence.model;

import java.util.Date;

public class ProgressMessage implements Comparable<ProgressMessage> {
    private static final int MAX_TEXT_LENGTH = 4000;

    private long id;
    private String processId;
    private String taskId;
    private String taskExecutionId;
    private ProgressMessageType type;
    private String text;
    private Date timestamp;

    public ProgressMessage() {
    }

    public ProgressMessage(String processId, String taskId, String taskExecutionId, ProgressMessageType type, String text, Date timestamp) {
        this.processId = processId;
        this.taskId = taskId;
        this.taskExecutionId = taskExecutionId;
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

    public String getTaskExecutionId() {
        return taskExecutionId;
    }

    public void setTaskExecutionId(String taskExecutionId) {
        this.taskExecutionId = taskExecutionId;
    }

    public String getFullTaskId() {
        return getTaskExecutionId() == null ? getTaskId() : getTaskId() + getTaskExecutionId();
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
        return "ProgressMessage [id=" + id + ", processId=" + processId + ", taskId=" + taskId + ", taskExecutionId=" + taskExecutionId
            + ", type=" + type + ", text=" + text + ", timestamp=" + timestamp + "]";
    }

    @Override
    public int compareTo(ProgressMessage otherProgressMessage) {
        return this.timestamp.compareTo(otherProgressMessage.getTimestamp());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (id ^ (id >>> 32));
        result = prime * result + ((processId == null) ? 0 : processId.hashCode());
        result = prime * result + ((taskExecutionId == null) ? 0 : taskExecutionId.hashCode());
        result = prime * result + ((taskId == null) ? 0 : taskId.hashCode());
        result = prime * result + ((text == null) ? 0 : text.hashCode());
        result = prime * result + ((timestamp == null) ? 0 : timestamp.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ProgressMessage)) {
            return false;
        }
        ProgressMessage other = (ProgressMessage) obj;
        if (id != other.id) {
            return false;
        }
        if (processId == null) {
            if (other.processId != null) {
                return false;
            }
        } else if (!processId.equals(other.processId)) {
            return false;
        }
        if (taskExecutionId == null) {
            if (other.taskExecutionId != null) {
                return false;
            }
        } else if (!taskExecutionId.equals(other.taskExecutionId)) {
            return false;
        }
        if (taskId == null) {
            if (other.taskId != null) {
                return false;
            }
        } else if (!taskId.equals(other.taskId)) {
            return false;
        }
        if (text == null) {
            if (other.text != null) {
                return false;
            }
        } else if (!text.equals(other.text)) {
            return false;
        }
        if (timestamp == null) {
            if (other.timestamp != null) {
                return false;
            }
        } else if (!timestamp.equals(other.timestamp)) {
            return false;
        }
        if (type != other.type) {
            return false;
        }
        return true;
    }


}
