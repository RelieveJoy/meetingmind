package com.meetingmind.controller;

import com.meetingmind.dto.ApiResponse;
import com.meetingmind.dto.DocumentResult;
import com.meetingmind.dto.TemplateInfo;
import com.meetingmind.entity.DocumentData;
import com.meetingmind.service.DocumentGeneratorService;
import com.meetingmind.service.TemplateEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 文档生成控制器
 * 
 * 提供文档生成、增量更新、下载等 REST API 接口
 */
@Slf4j
@RestController
@RequestMapping("/api/doc")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentGeneratorService documentGeneratorService;
    private final TemplateEngine templateEngine;

    /**
     * 生成完整文档
     * 
     * @param sessionId 会话ID
     * @param format 输出格式 (markdown 或 word)
     * @param template 模板名称
     * @return 文档生成结果
     */
    @PostMapping("/generate")
    public ApiResponse<DocumentResult> generateDocument(
            @RequestParam String sessionId,
            @RequestParam(defaultValue = "markdown") String format,
            @RequestParam(defaultValue = "full_meeting") String template) {

        log.info("Generate document request: sessionId={}, format={}, template={}", sessionId, format, template);

        try {
            DocumentResult result = documentGeneratorService.generate(sessionId, format, template);
            return ApiResponse.success(result);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid argument: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("Document generation failed", e);
            return ApiResponse.error(500, "文档生成失败: " + e.getMessage());
        }
    }

    /**
     * 增量追加分段到文档
     * 
     * @param sessionId 会话ID
     * @param segment 分段数据
     * @param template 模板名称
     * @return 文档更新结果
     */
    @PostMapping("/append/segment")
    public ApiResponse<DocumentResult> appendSegment(
            @RequestParam String sessionId,
            @RequestBody DocumentData.SegmentData segment,
            @RequestParam(defaultValue = "full_meeting") String template) {

        log.info("Append segment request: sessionId={}, segmentIndex={}", sessionId, segment.getIndex());

        try {
            DocumentResult result = documentGeneratorService.appendSegment(sessionId, segment, template);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("Failed to append segment", e);
            return ApiResponse.error(500, "追加分段失败: " + e.getMessage());
        }
    }

    /**
     * 增量追加摘要到文档
     * 
     * @param sessionId 会话ID
     * @param payload 摘要数据 (包含 summary, keyPoints, actionItems)
     * @return 文档更新结果
     */
    @PostMapping("/append/summary")
    public ApiResponse<DocumentResult> appendSummary(
            @RequestParam String sessionId,
            @RequestBody Map<String, Object> payload) {

        log.info("Append summary request: sessionId={}", sessionId);

        try {
            String summary = (String) payload.get("summary");
            @SuppressWarnings("unchecked")
            List<String> keyPoints = (List<String>) payload.get("keyPoints");
            @SuppressWarnings("unchecked")
            List<String> actionItems = (List<String>) payload.get("actionItems");

            DocumentResult result = documentGeneratorService.appendSummary(sessionId, summary, keyPoints, actionItems);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("Failed to append summary", e);
            return ApiResponse.error(500, "追加摘要失败: " + e.getMessage());
        }
    }

    /**
     * 从缓存生成 Word 文档
     * 
     * @param sessionId 会话ID
     * @return Word 文档生成结果
     */
    @PostMapping("/generate/word")
    public ApiResponse<DocumentResult> generateWordFromCache(@RequestParam String sessionId) {
        log.info("Generate Word from cache request: sessionId={}", sessionId);

        try {
            DocumentResult result = documentGeneratorService.generateWordFromCache(sessionId);
            return ApiResponse.success(result);
        } catch (IllegalArgumentException e) {
            log.warn("No cached document: {}", e.getMessage());
            return ApiResponse.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to generate Word from cache", e);
            return ApiResponse.error(500, "生成Word文档失败: " + e.getMessage());
        }
    }

    /**
     * 下载文档
     * 
     * @param sessionId 会话ID
     * @param format 文档格式
     * @return 文档资源
     */
    @GetMapping("/download/{sessionId}")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "markdown") String format) {

        log.info("Download document request: sessionId={}, format={}", sessionId, format);

        try {
            Resource resource = documentGeneratorService.downloadDocument(sessionId, format);
            String contentType = "markdown".equalsIgnoreCase(format) || "md".equalsIgnoreCase(format)
                    ? "text/markdown; charset=utf-8"
                    : "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

            String fileName = sessionId + "_" + System.currentTimeMillis() + "." +
                    ("markdown".equalsIgnoreCase(format) || "md".equalsIgnoreCase(format) ? "md" : "docx");

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch (IllegalArgumentException e) {
            log.warn("Document not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Download failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取可用模板列表
     * 
     * @return 模板列表
     */
    @GetMapping("/templates")
    public ApiResponse<List<TemplateInfo>> getTemplates() {
        List<TemplateInfo> templates = templateEngine.getAvailableTemplates();
        return ApiResponse.success(templates);
    }

    /**
     * 获取缓存的 Markdown 内容
     * 
     * @param sessionId 会话ID
     * @return Markdown 内容
     */
    @GetMapping("/cache/{sessionId}")
    public ApiResponse<String> getCachedMarkdown(@PathVariable String sessionId) {
        String markdown = documentGeneratorService.getCachedMarkdown(sessionId);
        if (markdown == null) {
            return ApiResponse.error(404, "No cached document found for session: " + sessionId);
        }
        return ApiResponse.success(markdown);
    }

    /**
     * 清除会话缓存
     * 
     * @param sessionId 会话ID
     * @return 操作结果
     */
    @DeleteMapping("/cache/{sessionId}")
    public ApiResponse<String> clearCache(@PathVariable String sessionId) {
        documentGeneratorService.clearCache(sessionId);
        return ApiResponse.success("Cache cleared for session: " + sessionId);
    }
}

