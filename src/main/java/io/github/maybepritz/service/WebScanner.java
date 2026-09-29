package io.github.maybepritz.service;

import io.github.maybepritz.model.PageInfo;
import io.github.maybepritz.model.RawResponse;
import io.github.maybepritz.net.LinkExtractor;
import io.github.maybepritz.net.SocketClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class WebScanner {
    private final String host;
    private final int port;
    private final int maxPages;

    private final int TIMEOUT = 5000;

    public WebScanner(String host, int port, int maxPages) {
        this.host = host;
        this.port = port;
        this.maxPages = maxPages;
    }

    public List<PageInfo> scan(ScanListener listener) {
        Queue<String> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        List<PageInfo> scannedPages = new ArrayList<>();

        queue.add("/");

        while(!queue.isEmpty() && visited.size() < maxPages) {
            String path = queue.poll();
            if (visited.contains(path)) { continue; }

            listener.onLog(String.format("TCP Connect -> %s:%d [GET %s]", host, port, path));

            try {
                RawResponse response = SocketClient.get(host, port, path, TIMEOUT);
                String fullUrl = "http://" + host + (port == 80 ? "" : ":" + port) + path;
                List<String> linksOnThisPage = new ArrayList<>();

                if(response.isSuccess() && response.isHtml()) {
                    String html = new String(response.getBody(), StandardCharsets.UTF_8);
                    linksOnThisPage = LinkExtractor.extractLinks(host, path, html);

                    queue.addAll(linksOnThisPage);
                }

                PageInfo page = new PageInfo(fullUrl, response.getBodySize(), response.getStatusCode(), linksOnThisPage);
                scannedPages.add(page);
                listener.onPageFound(page);
                listener.onLog(String.format("   <- Код: %d | Размер: %d B | Найдено ссылок: %d",
                        response.getStatusCode(), response.getBodySize(), linksOnThisPage.size()));
            } catch (Exception ex) {
                String errorText = ex.getMessage();
                if (errorText == null || errorText.isEmpty()) {
                    errorText = ex.getClass().getSimpleName();
                }
                if (ex.getCause() != null && ex.getCause().getMessage() != null) {
                    errorText += " (" + ex.getCause().getMessage() + ")";
                }
                listener.onLog(String.format("   [ОШИБКА] %s: %s", path, errorText));
            }

            try {
                Thread.sleep(40);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            visited.add(path);
        }

        listener.onFinished(scannedPages);
        return scannedPages;
    }
}
