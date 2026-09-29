package io.github.maybepritz.net;

import java.net.URI;
import java.util.*;

public class LinkExtractor {
    public static List<String> extractLinks(String host, String path, String body) {
        Set<String> links = new LinkedHashSet<>();

        if (body == null || body.isEmpty()) {
            return new ArrayList<>();
        }

        int len = body.length();
        int cur = 0;

        while (cur < len) {
            int tagStart = body.indexOf('<', cur);
            if (tagStart == -1) break;

            int tagEnd = body.indexOf('>', tagStart);
            if (tagEnd == -1) break;

            int p = tagStart + 1;
            while (p < tagEnd && Character.isWhitespace(body.charAt(p))) {
                p++;
            }

            if (p < tagEnd && ((body.charAt(p) | 32) == 'a')) {
                char nextChar = p + 1 < tagEnd ? body.charAt(p + 1) : '>';
                if (Character.isWhitespace(nextChar) || nextChar == '>' || nextChar == '/') {
                    String tagContent = body.substring(p, tagEnd);
                    String href = Objects.requireNonNull(parseHrefFromTag(tagContent)).trim();
                    if (isValidLink(href)) {
                        String normalizedPath = normalizeUrl(host, path, href);
                        if (normalizedPath != null)
                            links.add(normalizedPath);
                    }

                }
            }

            cur = tagEnd + 1;
        }

        return new ArrayList<>(links);
    }

    private static String parseHrefFromTag(String tag) {
        String lowerTag = tag.toLowerCase();
        int hrefIdx = lowerTag.indexOf("href");
        if (hrefIdx == -1) return null;

        int p = hrefIdx + 4;
        int len = tag.length();

        while (p < len && Character.isWhitespace(tag.charAt(p))) {
            p++;
        }

        if (p >= len || tag.charAt(p) != '=') {
            return null;
        }

        do {
            p++;
        } while (p < len && Character.isWhitespace(tag.charAt(p)));

        if (p >= len) return null;

        char quote = tag.charAt(p);
        if (quote == '"' || quote == '\'') {
            int start = p + 1;
            int end = tag.indexOf(quote, start);
            if (end != -1) {
                return tag.substring(start, end);
            }
        } else {
            int end = p;
            while (end < len && !Character.isWhitespace(tag.charAt(end)) && tag.charAt(end) != '/') {
                end++;
            }
            if (end > p) {
                return tag.substring(p, end);
            }
        }

        return null;
    }

    private static boolean isValidLink(String link) {
        String lower = link.toLowerCase().trim();
        return !lower.isEmpty()
                && !lower.startsWith("#")
                && !lower.startsWith("javascript:")
                && !lower.startsWith("mailto:")
                && !lower.startsWith("tel:");
    }

    private static String normalizeUrl(String targetHost, String currentPath, String rawHref) {
        try {
            // Если currentPath пустой, берем корень
            if (currentPath == null || currentPath.isEmpty()) {
                currentPath = "/";
            }

            URI baseUri = new URI("http://" + targetHost + currentPath);
            URI resolved = baseUri.resolve(rawHref);

            // Проверяем хост: разрешаем ТОЛЬКО ссылки на целевой хост
            if (resolved.getHost() != null && !resolved.getHost().equalsIgnoreCase(targetHost)) {
                return null; // Чужой хост/поддомен (например, home.web.cern.ch)
            }

            String path = resolved.getPath();
            if (path == null || path.isEmpty()) {
                path = "/";
            }

            if (resolved.getRawQuery() != null && !resolved.getRawQuery().isEmpty()) {
                path += "?" + resolved.getRawQuery();
            }

            return path;
        } catch (Exception ignored) {
            return null; // Невалидный формат URL
        }
    }
}
