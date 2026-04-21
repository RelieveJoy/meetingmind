package com.feishu.miji.service.impl;

import com.feishu.miji.config.SummaryProperties;
import com.feishu.miji.entity.SessionSummaryHistory;
import com.feishu.miji.entity.SummaryRecord;
import com.feishu.miji.service.SummaryHistoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 摘要历史管理服务实现类
 * 
 * 实现基于内存 + 持久化的双层存储结构
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SummaryHistoryServiceImpl implements SummaryHistoryService {
    
    private final SummaryProperties summaryProperties;
    private final ObjectMapper objectMapper;
    
    /**
     * 内存存储：会话ID -> 摘要历史
     */
    private final Map<String, SessionSummaryHistory> historyMap = new ConcurrentHashMap<>();
    
    @Override
    public List<SummaryRecord> getHistory(String sessionId) {
        SessionSummaryHistory history = historyMap.get(sessionId);
        return history != null ? history.getSummaries() : Collections.emptyList();
    }
    
    @Override
    public SummaryRecord getLatest(String sessionId) {
        SessionSummaryHistory history = historyMap.get(sessionId);
        if (history == null || history.getSummaries().isEmpty()) {
            return null;
        }
        return history.getSummaries().get(history.getSummaries().size() - 1);
    }
    
    @Override
    public SummaryRecord getByVersion(String sessionId, int version) {
        SessionSummaryHistory history = historyMap.get(sessionId);
        if (history == null) {
            return null;
        }
        return history.getSummaries().stream()
                .filter(record -> record.getVersion() == version)
                .findFirst()
                .orElse(null);
    }
    
    @Override
    public String exportHistory(String sessionId, ExportFormat format) {
        SessionSummaryHistory history = historyMap.get(sessionId);
        if (history == null) {
            return "";
        }
        
        try {
            switch (format) {
                case JSON:
                    return objectMapper.writeValueAsString(history);
                case MARKDOWN:
                    return exportAsMarkdown(history);
                case TEXT:
                    return exportAsText(history);
                default:
                    return "";
            }
        } catch (Exception e) {
            log.error("导出历史失败，会话ID: {}", sessionId, e);
            return "";
        }
    }
    
    @Override
    public void clearHistory(String sessionId) {
        historyMap.remove(sessionId);
        // 清理持久化文件
        if (summaryProperties.getPersistence().isEnabled()) {
            try {
                Path filePath = getHistoryFilePath(sessionId);
                Files.deleteIfExists(filePath);
            } catch (Exception e) {
                log.error("清理持久化文件失败，会话ID: {}", sessionId, e);
            }
        }
        log.info("清理会话历史，会话ID: {}", sessionId);
    }
    
    @Override
    public void saveRecord(SummaryRecord record) {
        SessionSummaryHistory history = historyMap.computeIfAbsent(
                record.getSessionId(), 
                k -> SessionSummaryHistory.builder()
                        .sessionId(k)
                        .summaries(new ArrayList<>())
                        .totalSummaryCount(0)
                        .build()
        );
        
        // 添加记录
        history.getSummaries().add(record);
        history.setLatestSummary(record.getContent());
        history.setTotalSummaryCount(history.getTotalSummaryCount() + 1);
        history.setLastSummaryTime(record.getCreatedAt());
        
        // 持久化
        if (summaryProperties.getPersistence().isEnabled() && summaryProperties.getPersistence().isAutoSave()) {
            persistHistory(history);
        }
        
        log.info("保存摘要记录，会话ID: {}, 版本: {}", record.getSessionId(), record.getVersion());
    }
    
    /**
     * 导出为 Markdown 格式
     */
    private String exportAsMarkdown(SessionSummaryHistory history) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 会话摘要历史\n\n");
        sb.append("## 会话信息\n");
        sb.append("- 会话ID: " + history.getSessionId() + "\n");
        sb.append("- 摘要总数: " + history.getTotalSummaryCount() + "\n");
        sb.append("- 最后总结时间: " + history.getLastSummaryTime() + "\n\n");
        
        sb.append("## 摘要记录\n");
        for (SummaryRecord record : history.getSummaries()) {
            sb.append("### 版本 " + record.getVersion() + "\n");
            sb.append("- 触发原因: " + record.getTriggerReason() + "\n");
            sb.append("- 生成时间: " + record.getCreatedAt() + "\n");
            sb.append("- 生成耗时: " + record.getDurationMs() + "ms\n");
            sb.append("\n**摘要内容:**\n" + record.getContent() + "\n");
            if (!record.getKeyPoints().isEmpty()) {
                sb.append("\n**关键点:**\n");
                for (String point : record.getKeyPoints()) {
                    sb.append("- " + point + "\n");
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }
    
    /**
     * 导出为纯文本格式
     */
    private String exportAsText(SessionSummaryHistory history) {
        StringBuilder sb = new StringBuilder();
        sb.append("会话摘要历史\n");
        sb.append("====================\n");
        sb.append("会话ID: " + history.getSessionId() + "\n");
        sb.append("摘要总数: " + history.getTotalSummaryCount() + "\n");
        sb.append("最后总结时间: " + history.getLastSummaryTime() + "\n\n");
        
        for (SummaryRecord record : history.getSummaries()) {
            sb.append("版本 " + record.getVersion() + "\n");
            sb.append("---------------------\n");
            sb.append("触发原因: " + record.getTriggerReason() + "\n");
            sb.append("生成时间: " + record.getCreatedAt() + "\n");
            sb.append("生成耗时: " + record.getDurationMs() + "ms\n\n");
            sb.append("摘要内容:\n" + record.getContent() + "\n");
            if (!record.getKeyPoints().isEmpty()) {
                sb.append("\n关键点:\n");
                for (String point : record.getKeyPoints()) {
                    sb.append("- " + point + "\n");
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }
    
    /**
     * 持久化历史记录
     */
    private void persistHistory(SessionSummaryHistory history) {
        try {
            Path filePath = getHistoryFilePath(history.getSessionId());
            // 创建目录
            Files.createDirectories(filePath.getParent());
            // 写入文件
            objectMapper.writeValue(filePath.toFile(), history);
            log.debug("持久化摘要历史，会话ID: {}", history.getSessionId());
        } catch (Exception e) {
            log.error("持久化摘要历史失败，会话ID: {}", history.getSessionId(), e);
        }
    }
    
    /**
     * 获取历史文件路径
     */
    private Path getHistoryFilePath(String sessionId) {
        String basePath = summaryProperties.getPersistence().getBasePath();
        return Paths.get(basePath, sessionId + ".json");
    }
    
    /**
     * 计算文本哈希值
     */
    public String calculateTextHash(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("计算文本哈希失败", e);
            return UUID.randomUUID().toString();
        }
    }
}