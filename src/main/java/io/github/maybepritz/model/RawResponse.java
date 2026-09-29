package io.github.maybepritz.model;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class RawResponse {
    private final int statusCode;
//    private final String statusText;
    private final Map<String, String> headers;
    private final byte[] body;

    public RawResponse(int statusCode, Map<String, String> headers, byte[] body) {
        this.statusCode = statusCode;
//        this.statusText = statusText != null ? statusText : "";

        Map<String, String> lowerHeaders = new HashMap<>();

        if(headers != null) {
            for(Map.Entry<String, String> entry : headers.entrySet()) {
                if(entry.getKey() != null){
                    lowerHeaders.put(entry.getKey().toLowerCase(), entry.getValue());
                }
            }
        }

        this.headers = lowerHeaders;
        this.body = body != null ? body.clone() : new byte[0];
    }

    public int getStatusCode() { return this.statusCode; }
//    public String getStatusText() { return this.statusText; }
    public Map<String, String> getHeaders() { return this.headers; }
    public byte[] getBody() { return this.body; }

    public int getBodySize() {return body.length; }
    public String getHeader(String name) { return headers.get(name); }

    public boolean isHtml() {
        String contentType = getHeader("content-type");
        return contentType != null && contentType.equalsIgnoreCase("text/html");
    }

    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }
}
