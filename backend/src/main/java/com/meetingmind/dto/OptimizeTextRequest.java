package com.meetingmind.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文本优化请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptimizeTextRequest {

    /**
     * 会话ID
     */
    private String sessionId;
    /**
     * 待优化的文本
     */
    @NotBlank(message = "文本不能为空")
    private String text;
    
    /**
     * 分段ID
     */
    private String segmentId;
    
    /**
     * 优化类型：punctuation（标点）、grammar（语法）、all（全部）
     */
    private String optimizeType = "all";
    
    /**
     * 上下文信息（可选，用于提高优化质量）
     */
    private String context;
    
    /**
     * 是否保持口语化风格
     */
    private Boolean keepColloquial = true;
}


