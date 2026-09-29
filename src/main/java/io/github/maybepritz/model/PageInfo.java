package io.github.maybepritz.model;

import java.util.Collections;
import java.util.List;

public class PageInfo {
    private final String url;
    private final int size;
    private final int statusCode;
    private final List<String> links;

    public PageInfo(String url, int size, int statusCode, List<String> links) {
        this.url = url;
        this.size = size;
        this.statusCode = statusCode;
        this.links = links != null ? Collections.unmodifiableList(links) : Collections.emptyList();
    }

    public String getUrl() { return this.url; }
    public int getSize() { return this.size; }
    public int getStatusCode() { return this.statusCode; }
    public List<String> getLinks() { return this.links; }
    public int getLinksCount() { return this.links.size(); }
}
