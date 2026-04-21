package com.feishu.miji.service;

import com.feishu.miji.entity.DocumentData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Word 文档导出器
 * 
 * 支持从 DocumentData 直接生成 Word 文档，以及从 Markdown 转换为 Word 文档
 * 采用 Apache POI 实现 Word 文档生成
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WordExporter {

    private final TemplateEngine templateEngine;

    private static final Pattern H1_PATTERN = Pattern.compile("^#\\s+(.+)$");
    private static final Pattern H2_PATTERN = Pattern.compile("^##\\s+(.+)$");
    private static final Pattern H3_PATTERN = Pattern.compile("^###\\s+(.+)$");
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern HORIZONTAL_RULE = Pattern.compile("^---$");
    private static final Pattern LIST_ITEM = Pattern.compile("^-\\s+(.+)$");
    private static final Pattern NUMBERED_ITEM = Pattern.compile("^(\\d+)\\.\\s+(.+)$");
    private static final Pattern CHECKBOX_ITEM = Pattern.compile("^-\\s+\\[\\s*\\]\\s+(.+)$");

    /**
     * 从文档数据生成 Word 文档
     * 
     * @param data 文档数据
     * @param targetPath 目标路径
     * @return 生成的文件路径
     * @throws IOException 文件操作异常
     */
    public Path export(DocumentData data, Path targetPath) throws IOException {
        try (XWPFDocument document = new XWPFDocument()) {
            addTitleParagraph(document, data.getTitle());
            addEmptyParagraph(document);

            addHeadingParagraph(document, "基本信息");
            addKeyValueParagraph(document, "生成时间", formatDateTime(data.getGeneratedTime()));
            addKeyValueParagraph(document, "会话时长", data.getDuration());
            addKeyValueParagraph(document, "总字数", String.valueOf(data.getTotalWords()) + " 字");
            addKeyValueParagraph(document, "会话ID", data.getSessionId());
            addKeyValueParagraph(document, "开始时间", formatDateTime(data.getStartTime()));
            addKeyValueParagraph(document, "结束时间", formatDateTime(data.getEndTime()));

            addEmptyParagraph(document);
            addHeadingParagraph(document, "实时总结");
            addContentParagraph(document, data.getSummary() != null ? data.getSummary() : "暂无摘要");

            addEmptyParagraph(document);
            addHeadingParagraph(document, "关键要点");
            List<String> keyPoints = data.getKeyPoints();
            if (keyPoints != null && !keyPoints.isEmpty()) {
                for (int i = 0; i < keyPoints.size(); i++) {
                    addNumberedParagraph(document, i + 1, keyPoints.get(i));
                }
            } else {
                addContentParagraph(document, "暂无关键要点");
            }

            addEmptyParagraph(document);
            addHeadingParagraph(document, "行动项");
            List<String> actionItems = data.getActionItems();
            if (actionItems != null && !actionItems.isEmpty()) {
                for (String item : actionItems) {
                    addCheckboxParagraph(document, item);
                }
            } else {
                addContentParagraph(document, "暂无行动项");
            }

            List<DocumentData.SegmentData> segments = data.getSegments();
            if (segments != null && !segments.isEmpty()) {
                addEmptyParagraph(document);
                addHeadingParagraph(document, "完整转写");
                for (DocumentData.SegmentData segment : segments) {
                    addEmptyParagraph(document);
                    addSegmentParagraph(document, segment);
                }
            }

            Files.createDirectories(targetPath.getParent());
            try (FileOutputStream out = new FileOutputStream(targetPath.toFile())) {
                document.write(out);
            }
            log.info("Word document saved to: {}", targetPath);
        }
        return targetPath;
    }

    /**
     * 从 Markdown 内容生成 Word 文档
     * 
     * @param markdownContent Markdown 内容
     * @param title 文档标题
     * @param targetPath 目标路径
     * @return 生成的文件路径
     * @throws IOException 文件操作异常
     */
    public Path exportFromMarkdown(String markdownContent, String title, Path targetPath) throws IOException {
        try (XWPFDocument document = new XWPFDocument()) {
            String[] lines = markdownContent.split("\n");
            
            for (String line : lines) {
                processMarkdownLine(document, line);
            }

            Files.createDirectories(targetPath.getParent());
            try (FileOutputStream out = new FileOutputStream(targetPath.toFile())) {
                document.write(out);
            }
            log.info("Word document generated from markdown: {}", targetPath);
        }
        return targetPath;
    }

    /**
     * 处理 Markdown 行
     * 
     * @param document Word 文档
     * @param line Markdown 行
     */
    private void processMarkdownLine(XWPFDocument document, String line) {
        Matcher h1Matcher = H1_PATTERN.matcher(line);
        Matcher h2Matcher = H2_PATTERN.matcher(line);
        Matcher h3Matcher = H3_PATTERN.matcher(line);
        Matcher checkboxMatcher = CHECKBOX_ITEM.matcher(line);
        Matcher listMatcher = LIST_ITEM.matcher(line);
        Matcher numberedMatcher = NUMBERED_ITEM.matcher(line);

        if (h1Matcher.matches()) {
            addTitleParagraph(document, h1Matcher.group(1));
        } else if (h2Matcher.matches()) {
            addHeadingParagraph(document, h2Matcher.group(1));
        } else if (h3Matcher.matches()) {
            addSubHeadingParagraph(document, h3Matcher.group(1));
        } else if (HORIZONTAL_RULE.matcher(line).matches()) {
            addHorizontalRule(document);
        } else if (checkboxMatcher.matches()) {
            addCheckboxParagraph(document, checkboxMatcher.group(1));
        } else if (numberedMatcher.matches()) {
            addNumberedParagraph(document, Integer.parseInt(numberedMatcher.group(1)), numberedMatcher.group(2));
        } else if (listMatcher.matches()) {
            addBulletParagraph(document, listMatcher.group(1));
        } else if (!line.isBlank()) {
            addFormattedParagraph(document, line);
        } else {
            addEmptyParagraph(document);
        }
    }

    /**
     * 添加标题段落
     * 
     * @param document Word 文档
     * @param text 标题文本
     */
    private void addTitleParagraph(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontSize(18);
    }

    /**
     * 添加一级标题段落
     * 
     * @param document Word 文档
     * @param text 标题文本
     */
    private void addHeadingParagraph(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontSize(14);
    }

    /**
     * 添加二级标题段落
     * 
     * @param document Word 文档
     * @param text 标题文本
     */
    private void addSubHeadingParagraph(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontSize(12);
    }

    /**
     * 添加内容段落
     * 
     * @param document Word 文档
     * @param text 内容文本
     */
    private void addContentParagraph(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setFontSize(11);
    }

    /**
     * 添加带格式的段落（支持粗体）
     * 
     * @param document Word 文档
     * @param line 文本行
     */
    private void addFormattedParagraph(XWPFDocument document, String line) {
        XWPFParagraph paragraph = document.createParagraph();
        
        int lastEnd = 0;
        Matcher matcher = BOLD_PATTERN.matcher(line);
        
        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                XWPFRun normalRun = paragraph.createRun();
                normalRun.setText(line.substring(lastEnd, matcher.start()));
                normalRun.setFontSize(11);
            }
            
            XWPFRun boldRun = paragraph.createRun();
            boldRun.setText(matcher.group(1));
            boldRun.setBold(true);
            boldRun.setFontSize(11);
            
            lastEnd = matcher.end();
        }
        
        if (lastEnd < line.length()) {
            XWPFRun normalRun = paragraph.createRun();
            normalRun.setText(line.substring(lastEnd));
            normalRun.setFontSize(11);
        }
        
        if (lastEnd == 0 && line.length() > 0) {
            XWPFRun run = paragraph.createRun();
            run.setText(line);
            run.setFontSize(11);
        }
    }

    /**
     * 添加键值对段落
     * 
     * @param document Word 文档
     * @param key 键
     * @param value 值
     */
    private void addKeyValueParagraph(XWPFDocument document, String key, String value) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun keyRun = paragraph.createRun();
        keyRun.setText(key + ": " + (value != null ? value : ""));
        keyRun.setFontSize(11);
    }

    /**
     * 添加编号列表项
     * 
     * @param document Word 文档
     * @param number 编号
     * @param text 文本
     */
    private void addNumberedParagraph(XWPFDocument document, int number, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(number + ". " + text);
        run.setFontSize(11);
    }

    /**
     * 添加项目符号列表项
     * 
     * @param document Word 文档
     * @param text 文本
     */
    private void addBulletParagraph(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText("• " + text);
        run.setFontSize(11);
    }

    /**
     * 添加复选框列表项
     * 
     * @param document Word 文档
     * @param text 文本
     */
    private void addCheckboxParagraph(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText("☐ " + text);
        run.setFontSize(11);
    }

    /**
     * 添加分段内容
     * 
     * @param document Word 文档
     * @param segment 分段数据
     */
    private void addSegmentParagraph(XWPFDocument document, DocumentData.SegmentData segment) {
        XWPFParagraph p1 = document.createParagraph();
        XWPFRun r1 = p1.createRun();
        r1.setText("分段 " + segment.getIndex() + " [" + segment.getStartTime() + " - " + segment.getEndTime() + "]");
        r1.setBold(true);
        r1.setFontSize(12);

        addContentParagraph(document, "原始文本:");
        addContentParagraph(document, segment.getOriginalText() != null ? segment.getOriginalText() : "");

        addEmptyParagraph(document);
        addContentParagraph(document, "优化后:");
        addContentParagraph(document, segment.getOptimizedText() != null ? segment.getOptimizedText() : "");
    }

    /**
     * 添加空段落
     * 
     * @param document Word 文档
     */
    private void addEmptyParagraph(XWPFDocument document) {
        document.createParagraph();
    }

    /**
     * 添加水平线
     * 
     * @param document Word 文档
     */
    private void addHorizontalRule(XWPFDocument document) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText("────────────────────────────────────────");
        run.setFontSize(10);
    }

    /**
     * 格式化日期时间
     * 
     * @param dateTime 日期时间
     * @return 格式化后的字符串
     */
    private String formatDateTime(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}