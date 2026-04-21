package com.feishu.miji.service;

import com.feishu.miji.entity.DocumentData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Markdown 导出器
 * 
 * 生成和保存 Markdown 格式文档
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarkdownExporter {

    private final TemplateEngine templateEngine;

    /**
     * 导出为 Markdown 内容
     * 
     * @param data 文档数据
     * @param template 模板名称
     * @return Markdown 内容
     */
    public String export(DocumentData data, String template) {
        return templateEngine.render(template, data);
    }

    /**
     * 保存 Markdown 文件
     * 
     * @param data 文档数据
     * @param template 模板名称
     * @param targetPath 目标路径
     * @return 保存的文件路径
     * @throws IOException 文件操作异常
     */
    public Path saveToFile(DocumentData data, String template, Path targetPath) throws IOException {
        String content = export(data, template);
        Files.createDirectories(targetPath.getParent());
        Files.writeString(targetPath, content);
        log.info("Markdown file saved to: {}", targetPath);
        return targetPath;
    }
}
