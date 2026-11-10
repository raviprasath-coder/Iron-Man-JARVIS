package com.jarvis.jarvis_backend;

public class JarvisResponse {
    private String reply;
    private String actionType;
    private boolean success;
    private Object data;

    public JarvisResponse() {}

    public JarvisResponse(String reply, String actionType, boolean success, Object data) {
        this.reply = reply;
        this.actionType = actionType;
        this.success = success;
        this.data = data;
    }

    public static JarvisResponse success(String reply, String actionType, Object data) {
        return new JarvisResponse(reply, actionType, true, data);
    }

    public static JarvisResponse error(String reply) {
        return new JarvisResponse(reply, "ERROR", false, null);
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
