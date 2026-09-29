package io.github.maybepritz.service;

import io.github.maybepritz.model.PageInfo;

import java.util.List;

public interface ScanListener {
    void onLog(String message);
    void onPageFound(PageInfo page);
    void onFinished(List<PageInfo> results);
}