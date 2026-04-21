package com.feishu.miji.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResult {

    private String documentId;
    private String sessionId;
    private String fileName;
    private String filePath;
    private String format;
    private String template;
    private long fileSize;
    private String downloadUrl;
}