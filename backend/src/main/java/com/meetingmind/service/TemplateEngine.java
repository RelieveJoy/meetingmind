package com.meetingmind.service;

import com.meetingmind.entity.DocumentData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模板引擎
 * 
 * 处理模板解析和渲染，支持内置模板和自定义模板
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateEngine {

    /**
     * 渲染模板
     * 
     * @param templateName 模板名称
     * @param data 文档数据
     * @return 渲染后的内容
     */
    public String render(String templateName, DocumentData data) {
        Map<String, Object> context = buildContext(data);
        
        switch (templateName) {
            case "full_meeting":
                return renderFullMeetingTemplate(context);
            case "simple_summary":
                return renderSimpleSummaryTemplate(context);
            case "review":
                return renderReviewTemplate(context);
            default:
                log.warn("Unknown template: {}, using full_meeting as default", templateName);
                return renderFullMeetingTemplate(context);
        }
    }

    /**
     * 获取可用模板列表
     * 
     * @return 模板信息列表
     */
    public List<com.meetingmind.dto.TemplateInfo> getAvailableTemplates() {
        List<com.meetingmind.dto.TemplateInfo> templates = new ArrayList<>();
        
        templates.add(com.meetingmind.dto.TemplateInfo.builder()
                .name("full_meeting")
                .description("完整会议记录")
                .build());
        
        templates.add(com.meetingmind.dto.TemplateInfo.builder()
                .name("simple_summary")
                .description("简洁总结")
                .build());
        
        templates.add(com.meetingmind.dto.TemplateInfo.builder()
                .name("review")
                .description("对比校对")
                .build());
        
        return templates;
    }

    /**
     * 构建模板上下文
     * 
     * @param data 文档数据
     * @return 上下文Map
     */
    private Map<String, Object> buildContext(DocumentData data) {
        Map<String, Object> context = new HashMap<>();
        context.put("title", data.getTitle());
        context.put("generatedTime", data.getGeneratedTime());
        context.put("duration", data.getDuration());
        context.put("totalWords", data.getTotalWords());
        context.put("sessionId", data.getSessionId());
        context.put("startTime", data.getStartTime());
        context.put("endTime", data.getEndTime());
        context.put("summary", data.getSummary());
        context.put("keyPoints", data.getKeyPoints());
        context.put("actionItems", data.getActionItems());
        context.put("segments", data.getSegments());
        return context;
    }

    /**
     * 渲染完整会议记录模板
     * 
     * @param context 上下文
     * @return 渲染后的内容
     */
    private String renderFullMeetingTemplate(Map<String, Object> context) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("# ").append(context.get("title")).append("\n\n");
        sb.append("## 基本信息\n\n");
        sb.append("- 生成时间: ").append(context.get("generatedTime")).append("\n");
        sb.append("- 会话时长: ").append(context.get("duration")).append("\n");
        sb.append("- 总字数: ").append(context.get("totalWords")).append(" 字\n");
        sb.append("- 会话ID: ").append(context.get("sessionId")).append("\n");
        sb.append("- 开始时间: ").append(context.get("startTime")).append("\n");
        sb.append("- 结束时间: ").append(context.get("endTime")).append("\n\n");
        
        sb.append("## 实时总结\n\n");
        sb.append(context.get("summary")).append("\n\n");
        
        sb.append("## 关键要点\n\n");
        List<String> keyPoints = (List<String>) context.get("keyPoints");
        if (keyPoints != null && !keyPoints.isEmpty()) {
            for (int i = 0; i < keyPoints.size(); i++) {
                sb.append(i + 1).append(". ").append(keyPoints.get(i)).append("\n");
            }
        } else {
            sb.append("暂无关键要点\n");
        }
        sb.append("\n");
        
        sb.append("## 行动项\n\n");
        List<String> actionItems = (List<String>) context.get("actionItems");
        if (actionItems != null && !actionItems.isEmpty()) {
            for (String item : actionItems) {
                sb.append("- [ ] ").append(item).append("\n");
            }
        } else {
            sb.append("暂无行动项\n");
        }
        sb.append("\n");
        
        sb.append("## 完整转写\n\n");
        List<DocumentData.SegmentData> segments = (List<DocumentData.SegmentData>) context.get("segments");
        if (segments != null && !segments.isEmpty()) {
            for (DocumentData.SegmentData segment : segments) {
                sb.append("### 分段 ").append(segment.getIndex());
                sb.append(" [").append(segment.getStartTime());
                sb.append(" - ").append(segment.getEndTime()).append("]\n\n");
                
                if (segment.getOriginalText() != null && !segment.getOriginalText().isBlank()) {
                    sb.append("**原始文本:**\n").append(segment.getOriginalText()).append("\n\n");
                }
                
                if (segment.getOptimizedText() != null && !segment.getOptimizedText().isBlank()) {
                    sb.append("**优化后:**\n").append(segment.getOptimizedText()).append("\n\n");
                }
                
                sb.append("---\n\n");
            }
        } else {
            sb.append("暂无转写内容\n");
        }
        
        return sb.toString();
    }

    /**
     * 渲染简洁总结模板
     * 
     * @param context 上下文
     * @return 渲染后的内容
     */
    private String renderSimpleSummaryTemplate(Map<String, Object> context) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("# ").append(context.get("title")).append("\n\n");
        sb.append("## 会议总结\n\n");
        sb.append(context.get("summary")).append("\n\n");
        
        sb.append("## 关键要点\n\n");
        List<String> keyPoints = (List<String>) context.get("keyPoints");
        if (keyPoints != null && !keyPoints.isEmpty()) {
            for (int i = 0; i < keyPoints.size(); i++) {
                sb.append(i + 1).append(". ").append(keyPoints.get(i)).append("\n");
            }
        } else {
            sb.append("暂无关键要点\n");
        }
        sb.append("\n");
        
        sb.append("## 行动项\n\n");
        List<String> actionItems = (List<String>) context.get("actionItems");
        if (actionItems != null && !actionItems.isEmpty()) {
            for (String item : actionItems) {
                sb.append("- [ ] ").append(item).append("\n");
            }
        } else {
            sb.append("暂无行动项\n");
        }
        
        return sb.toString();
    }

    /**
     * 渲染对比校对模板
     * 
     * @param context 上下文
     * @return 渲染后的内容
     */
    private String renderReviewTemplate(Map<String, Object> context) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("# ").append(context.get("title")).append("\n\n");
        sb.append("## 对比校对\n\n");
        
        List<DocumentData.SegmentData> segments = (List<DocumentData.SegmentData>) context.get("segments");
        if (segments != null && !segments.isEmpty()) {
            for (DocumentData.SegmentData segment : segments) {
                sb.append("### 分段 ").append(segment.getIndex());
                sb.append(" [").append(segment.getStartTime());
                sb.append(" - ").append(segment.getEndTime()).append("]\n\n");
                
                if (segment.getOriginalText() != null && !segment.getOriginalText().isBlank()) {
                    sb.append("**原始文本:**\n").append(segment.getOriginalText()).append("\n\n");
                }
                
                if (segment.getOptimizedText() != null && !segment.getOptimizedText().isBlank()) {
                    sb.append("**优化后:**\n").append(segment.getOptimizedText()).append("\n\n");
                }
                
                sb.append("---\n\n");
            }
        } else {
            sb.append("暂无对比内容\n");
        }
        
        return sb.toString();
    }
}

