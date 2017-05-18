package com.github.pigeon.api.model;

/**
 * @author liuhaoyong on 2016/12/17.
 */
public class CommonEvent implements java.io.Serializable {

    private static final long serialVersionUID = -8358950857852340633L;

    private String traceId;

    private String eventKey;

    public String getEventKey() {
        return eventKey;
    }

    public void setEventKey(String eventKey) {
        this.eventKey = eventKey;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
