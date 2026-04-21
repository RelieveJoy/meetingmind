package com.feishu.miji.service;

import com.feishu.miji.config.DocumentProperties;
import com.feishu.miji.dto.DocumentResult;
import com.feishu.miji.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文档生成服务
 * 
 * 支持增量更新 Markdown 文档，避免频繁操作 Word 文档导致的性能问题
 * 采用 Markdown 缓存机制，最终一次性转换为 Word 文档
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentGeneratorService {

    private final DocumentProperties documentProperties;
    private final TemplateEngine templateEngine;
    private final MarkdownExporter markdownExporter;
    private final WordExporter wordExporter;

    // 存储生成的文档结果
    private final Map<String, DocumentResult> documentStore = new ConcurrentHashMap<>();
    
    // Markdown 缓存，用于增量更新
    private final Map<String, StringBuilder> markdownCache = new ConcurrentHashMap<>();
    
    // 文档数据缓存，用于快速构建完整文档
    private final Map<String, DocumentData> documentDataCache = new ConcurrentHashMap<>();
    
    // 会话开始时间缓存
    private final Map<String, LocalDateTime> sessionStartTimeCache = new ConcurrentHashMap<>();

    /**
     * 生成完整文档
     * 
     * @param sessionId 会话ID
     * @param format 输出格式 (markdown 或 word)
     * @param template 模板名称
     * @return 文档生成结果
     */
    public DocumentResult generate(String sessionId, String format, String template) {
        log.info("Generating document: sessionId={}, format={}, template={}", sessionId, format, template);

        // 构建文档数据
        DocumentData data = buildDocumentData(sessionId);
        documentDataCache.put(sessionId, data);

        // 生成 Markdown 内容并缓存
        String markdownContent = markdownExporter.export(data, template);
        markdownCache.put(sessionId, new StringBuilder(markdownContent));

        // 生成文件名和路径
        String fileName = generateFileName(sessionId, format);
        Path targetPath = buildOutputPath(fileName);

        try {
            if ("markdown".equalsIgnoreCase(format) || "md".equalsIgnoreCase(format)) {
                // 直接保存 Markdown 文件
                markdownExporter.saveToFile(data, template, targetPath);
            } else if ("word".equalsIgnoreCase(format) || "docx".equalsIgnoreCase(format)) {
                // 从 Markdown 转换为 Word
                wordExporter.exportFromMarkdown(markdownContent, data.getTitle(), targetPath);
            } else {
                throw new IllegalArgumentException("Unsupported format: " + format);
            }

            // 构建结果对象
            DocumentResult result = DocumentResult.builder()
                    .documentId(UUID.randomUUID().toString())
                    .sessionId(sessionId)
                    .fileName(fileName)
                    .filePath(targetPath.toString())
                    .format(format)
                    .template(template)
                    .fileSize(Files.size(targetPath))
                    .downloadUrl("/api/doc/download/" + sessionId + "?format=" + format)
                    .build();

            documentStore.put(sessionId, result);
            log.info("Document generated successfully: {}", result.getFilePath());
            return result;

        } catch (Exception e) {
            log.error("Failed to generate document: sessionId={}", sessionId, e);
            throw new RuntimeException("Document generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * 增量追加音频分段到文档
     * 
     * @param sessionId 会话ID
     * @param segment 分段数据
     * @param template 模板名称
     * @return 文档更新结果
     */
    public DocumentResult appendSegment(String sessionId, DocumentData.SegmentData segment, String template) {
        log.info("Appending segment to document: sessionId={}, segmentIndex={}", sessionId, segment.getIndex());

        // 获取或创建 Markdown 缓存
        StringBuilder cache = markdownCache.computeIfAbsent(sessionId, k -> new StringBuilder());
        // 获取或创建文档数据缓存
        DocumentData dataCache = documentDataCache.computeIfAbsent(sessionId, k -> createEmptyDocumentData(sessionId));
        // 记录会话开始时间
        sessionStartTimeCache.computeIfAbsent(sessionId, k -> LocalDateTime.now());

        // 追加分段数据
        dataCache.getSegments().add(segment);
        dataCache.setSegmentCount(dataCache.getSegments().size());

        // 生成分段的 Markdown 内容并追加到缓存
        String segmentMarkdown = renderSegmentMarkdown(segment, template);
        cache.append(segmentMarkdown).append("\n");

        // 保存到文件
        String fileName = generateFileName(sessionId, "markdown");
        Path targetPath = buildOutputPath(fileName);

        try {
            Files.createDirectories(targetPath.getParent());
            Files.writeString(targetPath, cache.toString());

            // 构建结果对象
            DocumentResult result = DocumentResult.builder()
                    .documentId(UUID.randomUUID().toString())
                    .sessionId(sessionId)
                    .fileName(fileName)
                    .filePath(targetPath.toString())
                    .format("markdown")
                    .template(template)
                    .fileSize(Files.size(targetPath))
                    .downloadUrl("/api/doc/download/" + sessionId + "?format=markdown")
                    .build();

            documentStore.put(sessionId, result);
            log.info("Segment appended successfully to: {}", result.getFilePath());
            return result;

        } catch (IOException e) {
            log.error("Failed to append segment: sessionId={}", sessionId, e);
            throw new RuntimeException("Failed to append segment: " + e.getMessage(), e);
        }
    }

    /**
     * 增量追加摘要到文档
     * 
     * @param sessionId 会话ID
     * @param summary 摘要内容
     * @param keyPoints 关键要点
     * @param actionItems 行动项
     * @return 文档更新结果
     */
    public DocumentResult appendSummary(String sessionId, String summary, List<String> keyPoints, List<String> actionItems) {
        log.info("Appending summary to document: sessionId={}", sessionId);

        // 获取或创建 Markdown 缓存
        StringBuilder cache = markdownCache.computeIfAbsent(sessionId, k -> new StringBuilder());
        // 获取或创建文档数据缓存
        DocumentData dataCache = documentDataCache.computeIfAbsent(sessionId, k -> createEmptyDocumentData(sessionId));

        // 更新摘要数据
        dataCache.setSummary(summary);
        if (keyPoints != null) {
            dataCache.getKeyPoints().addAll(keyPoints);
        }
        if (actionItems != null) {
            dataCache.getActionItems().addAll(actionItems);
        }

        // 生成摘要的 Markdown 内容并插入到缓存开头
        String summaryMarkdown = renderSummaryMarkdown(summary, keyPoints, actionItems);
        cache.insert(0, summaryMarkdown + "\n\n");

        // 保存到文件
        String fileName = generateFileName(sessionId, "markdown");
        Path targetPath = buildOutputPath(fileName);

        try {
            Files.createDirectories(targetPath.getParent());
            Files.writeString(targetPath, cache.toString());

            // 构建结果对象
            DocumentResult result = DocumentResult.builder()
                    .documentId(UUID.randomUUID().toString())
                    .sessionId(sessionId)
                    .fileName(fileName)
                    .filePath(targetPath.toString())
                    .format("markdown")
                    .template("incremental")
                    .fileSize(Files.size(targetPath))
                    .downloadUrl("/api/doc/download/" + sessionId + "?format=markdown")
                    .build();

            documentStore.put(sessionId, result);
            log.info("Summary appended successfully to: {}", result.getFilePath());
            return result;

        } catch (IOException e) {
            log.error("Failed to append summary: sessionId={}", sessionId, e);
            throw new RuntimeException("Failed to append summary: " + e.getMessage(), e);
        }
    }

    /**
     * 从缓存生成 Word 文档
     * 
     * @param sessionId 会话ID
     * @return Word 文档生成结果
     */
    public DocumentResult generateWordFromCache(String sessionId) {
        log.info("Generating Word document from cache: sessionId={}", sessionId);

        // 获取 Markdown 缓存
        StringBuilder cache = markdownCache.get(sessionId);
        DocumentData dataCache = documentDataCache.get(sessionId);

        if (cache == null) {
            throw new IllegalArgumentException("No cached document found for session: " + sessionId);
        }

        // 生成文件名和路径
        String fileName = generateFileName(sessionId, "word");
        Path targetPath = buildOutputPath(fileName);

        try {
            // 获取文档标题
            String title = dataCache != null && dataCache.getTitle() != null 
                    ? dataCache.getTitle() 
                    : "会议记录 - " + sessionId;
            
            // 从 Markdown 转换为 Word
            wordExporter.exportFromMarkdown(cache.toString(), title, targetPath);

            // 构建结果对象
            DocumentResult result = DocumentResult.builder()
                    .documentId(UUID.randomUUID().toString())
                    .sessionId(sessionId)
                    .fileName(fileName)
                    .filePath(targetPath.toString())
                    .format("word")
                    .template("from_cache")
                    .fileSize(Files.size(targetPath))
                    .downloadUrl("/api/doc/download/" + sessionId + "?format=word")
                    .build();

            documentStore.put(sessionId + "_word", result);
            log.info("Word document generated from cache: {}", result.getFilePath());
            return result;

        } catch (Exception e) {
            log.error("Failed to generate Word from cache: sessionId={}", sessionId, e);
            throw new RuntimeException("Failed to generate Word document: " + e.getMessage(), e);
        }
    }

    /**
     * 下载文档
     * 
     * @param sessionId 会话ID
     * @param format 文档格式
     * @return 文档资源
     */
    public Resource downloadDocument(String sessionId, String format) {
        // 构建缓存键
        String key = "word".equalsIgnoreCase(format) || "docx".equalsIgnoreCase(format) 
                ? sessionId + "_word" 
                : sessionId;
        
        // 获取文档结果
        DocumentResult result = documentStore.get(key);
        if (result == null) {
            result = documentStore.get(sessionId);
        }
        
        if (result == null) {
            throw new IllegalArgumentException("Document not found for session: " + sessionId);
        }

        try {
            // 加载文档文件
            Path filePath = Paths.get(result.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                throw new RuntimeException("Document file not found: " + filePath);
            }

            return resource;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load document: " + e.getMessage(), e);
        }
    }

    /**
     * 清除会话缓存
     * 
     * @param sessionId 会话ID
     */
    public void clearCache(String sessionId) {
        markdownCache.remove(sessionId);
        documentDataCache.remove(sessionId);
        sessionStartTimeCache.remove(sessionId);
        documentStore.remove(sessionId);
        documentStore.remove(sessionId + "_word");
        log.info("Cache cleared for session: {}", sessionId);
    }

    /**
     * 获取缓存的 Markdown 内容
     * 
     * @param sessionId 会话ID
     * @return Markdown 内容
     */
    public String getCachedMarkdown(String sessionId) {
        StringBuilder cache = markdownCache.get(sessionId);
        return cache != null ? cache.toString() : null;
    }

    /**
     * 创建空的文档数据对象
     * 
     * @param sessionId 会话ID
     * @return 文档数据对象
     */
    private DocumentData createEmptyDocumentData(String sessionId) {
        return DocumentData.builder()
                .title("会议记录 - " + sessionId)
                .sessionId(sessionId)
                .generatedTime(LocalDateTime.now())
                .segments(new ArrayList<>())
                .keyPoints(new ArrayList<>())
                .actionItems(new ArrayList<>())
                .segmentCount(0)
                .totalWords(0)
                .build();
    }

    /**
     * 渲染分段的 Markdown 内容
     * 
     * @param segment 分段数据
     * @param template 模板名称
     * @return Markdown 内容
     */
    private String renderSegmentMarkdown(DocumentData.SegmentData segment, String template) {
        StringBuilder sb = new StringBuilder();
        sb.append("### 分段 ").append(segment.getIndex())
          .append(" [").append(segment.getStartTime())
          .append(" - ").append(segment.getEndTime()).append("]\n\n");
        
        if (segment.getOriginalText() != null && !segment.getOriginalText().isBlank()) {
            sb.append("**原始文本:**\n").append(segment.getOriginalText()).append("\n\n");
        }
        
        if (segment.getOptimizedText() != null && !segment.getOptimizedText().isBlank()) {
            sb.append("**优化后:**\n").append(segment.getOptimizedText()).append("\n\n");
        }
        
        sb.append("---\n");
        return sb.toString();
    }

    /**
     * 渲染摘要的 Markdown 内容
     * 
     * @param summary 摘要内容
     * @param keyPoints 关键要点
     * @param actionItems 行动项
     * @return Markdown 内容
     */
    private String renderSummaryMarkdown(String summary, List<String> keyPoints, List<String> actionItems) {
        StringBuilder sb = new StringBuilder();
        
        if (summary != null && !summary.isBlank()) {
            sb.append("## 实时总结\n\n").append(summary).append("\n\n---\n\n");
        }
        
        if (keyPoints != null && !keyPoints.isEmpty()) {
            sb.append("## 关键要点\n\n");
            for (int i = 0; i < keyPoints.size(); i++) {
                sb.append(i + 1).append(". ").append(keyPoints.get(i)).append("\n");
            }
            sb.append("\n---\n\n");
        }
        
        if (actionItems != null && !actionItems.isEmpty()) {
            sb.append("## 行动项\n\n");
            for (String item : actionItems) {
                sb.append("- [ ] ").append(item).append("\n");
            }
            sb.append("\n---\n\n");
        }
        
        return sb.toString();
    }

    /**
     * 构建文档数据
     * 
     * @param sessionId 会话ID
     * @return 文档数据对象
     */
    private DocumentData buildDocumentData(String sessionId) {
        SessionContext context = getSessionContext(sessionId);

        String title = "会议记录 - " + (context != null && context.getSessionId() != null ? context.getSessionId() : sessionId);
        LocalDateTime now = LocalDateTime.now();

        List<DocumentData.SegmentData> segments = buildSegmentsFromContext(context);
        String summary = buildSummary(context);
        List<String> keyPoints = buildKeyPoints(context);
        List<String> actionItems = buildActionItems(context);

        int totalWords = segments.stream()
                .mapToInt(s -> {
                    int original = s.getOriginalText() != null ? s.getOriginalText().length() : 0;
                    int optimized = s.getOptimizedText() != null ? s.getOptimizedText().length() : 0;
                    return Math.max(original, optimized);
                })
                .sum();

        String duration = calculateDuration(context);

        return DocumentData.builder()
                .title(title)
                .generatedTime(now)
                .duration(duration)
                .totalWords(totalWords)
                .sessionId(sessionId)
                .startTime(context != null ? context.getCreatedAt() : null)
                .endTime(now)
                .segmentCount(segments.size())
                .summary(summary)
                .keyPoints(keyPoints)
                .actionItems(actionItems)
                .segments(segments)
                .build();
    }

    /**
     * 获取会话上下文
     * 
     * @param sessionId 会话ID
     * @return 会话上下文
     */
    private SessionContext getSessionContext(String sessionId) {
        try {
            // TODO: 集成 SessionManagementService 获取实际会话上下文
            return null;
        } catch (Exception e) {
            log.warn("Could not retrieve session context for: {}", sessionId);
            return null;
        }
    }

    /**
     * 从会话上下文构建分段数据
     * 
     * @param context 会话上下文
     * @return 分段数据列表
     */
    private List<DocumentData.SegmentData> buildSegmentsFromContext(SessionContext context) {
        List<DocumentData.SegmentData> segments = new ArrayList<>();

        if (context == null || context.getCompletedOptimizations() == null) {
            return segments;
        }

        int index = 0;
        for (OptimizationResult opt : context.getCompletedOptimizations()) {
            DocumentData.SegmentData segment = DocumentData.SegmentData.builder()
                    .index(index++)
                    .startTime(formatSeconds(opt.getOptimizedAt() != null ?
                            java.time.Duration.between(context.getCreatedAt(), opt.getOptimizedAt()).getSeconds() : 0))
                    .endTime(formatSeconds(0))
                    .originalText(opt.getOriginalText())
                    .optimizedText(opt.getOptimizedText())
                    .build();
            segments.add(segment);
        }

        return segments;
    }

    /**
     * 从会话上下文构建摘要
     * 
     * @param context 会话上下文
     * @return 摘要内容
     */
    private String buildSummary(SessionContext context) {
        if (context == null || context.getCurrentSummary() == null) {
            return "暂无摘要";
        }
        Summary summary = context.getCurrentSummary();
        return summary.getContent() != null ? summary.getContent() : "暂无摘要内容";
    }

    /**
     * 从会话上下文构建关键要点
     * 
     * @param context 会话上下文
     * @return 关键要点列表
     */
    private List<String> buildKeyPoints(SessionContext context) {
        if (context == null || context.getCurrentSummary() == null || context.getCurrentSummary().getKeyPoints() == null) {
            return new ArrayList<>();
        }
        return context.getCurrentSummary().getKeyPoints();
    }

    /**
     * 从会话上下文构建行动项
     * 
     * @param context 会话上下文
     * @return 行动项列表
     */
    private List<String> buildActionItems(SessionContext context) {
        if (context == null || context.getCurrentSummary() == null || context.getCurrentSummary().getActionItems() == null) {
            return new ArrayList<>();
        }
        return context.getCurrentSummary().getActionItems();
    }

    /**
     * 计算会话时长
     * 
     * @param context 会话上下文
     * @return 时长字符串
     */
    private String calculateDuration(SessionContext context) {
        if (context == null || context.getTotalTranscribedSeconds() == null) {
            return "0分0秒";
        }
        double totalSeconds = context.getTotalTranscribedSeconds();
        int minutes = (int) (totalSeconds / 60);
        int seconds = (int) (totalSeconds % 60);
        return minutes + "分" + seconds + "秒";
    }

    /**
     * 生成文件名
     * 
     * @param sessionId 会话ID
     * @param format 格式
     * @return 文件名
     */
    private String generateFileName(String sessionId, String format) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String extension = "markdown".equalsIgnoreCase(format) || "md".equalsIgnoreCase(format) ? "md" : "docx";
        return sessionId + "_" + timestamp + "." + extension;
    }

    /**
     * 构建输出路径
     * 
     * @param fileName 文件名
     * @return 输出路径
     */
    private Path buildOutputPath(String fileName) {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Path baseDir = Paths.get(documentProperties.getBaseDir());
        Path outputDir = baseDir.resolve(documentProperties.getDocDir()).resolve(dateStr);
        return outputDir.resolve(fileName);
    }

    /**
     * 格式化秒数为时间字符串
     * 
     * @param seconds 秒数
     * @return 时间字符串 (HH:MM)
     */
    private String formatSeconds(double seconds) {
        int min = (int) (seconds / 60);
        int sec = (int) (seconds % 60);
        return String.format("%02d:%02d", min, sec);
    }
}