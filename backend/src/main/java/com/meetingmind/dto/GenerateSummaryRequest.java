package com.meetingmind.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 生成总结请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateSummaryRequest {
    
    /**
     * 会话ID
     */
    @NotBlank(message = "会话ID不能为空")
    private String sessionId;
    
    /**
     * 待总结的文本内容
     */
    @NotBlank(message = "文本内容不能为空")
    private String text;
    
    /**
     * 总结类型：brief（简略）、detailed（详细）、bullet_points（要点列表）
     */
    private String summaryType = "brief";
    
    /**
     * 最大长度（字符数）
     */
    private Integer maxLength;
    
    /**
     * 自定义提示词（可选）
     */
    private String customPrompt;
}


