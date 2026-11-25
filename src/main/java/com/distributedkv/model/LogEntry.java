
package com.distributedkv.model;

public class LogEntry {

    private final int index;
    private final long term;
    private final String operation;
    private final String key;
    private final String value;

    public LogEntry(int index, long term, String operation,
                    String key, String value) {
        this.index = index;
        this.term = term;
        this.operation = operation;
        this.key = key;
        this.value = value;
    }

    public int getIndex() {
        return index;
    }

    public long getTerm() {
        return term;
    }

    public String getOperation() {
        return operation;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "LogEntry{" +
                "index=" + index +
                ", term=" + term +
                ", operation='" + operation + '\'' +
                ", key='" + key + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
