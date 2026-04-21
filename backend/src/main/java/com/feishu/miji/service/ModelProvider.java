package com.feishu.miji.service;

import com.feishu.miji.entity.OptimizationResult;
import com.feishu.miji.entity.TranscriptionResult;
import com.feishu.miji.entity.Summary;

import java.util.List;

/**
 * AI 模型提供者接口
 * 
 * 定义文本优化和摘要生成的抽象接口
 * 支持多种实现：Ollama、云端 API 等
 */
public interface ModelProvider {
    
    /**
     * 优化文本
     * 
     * @param texts 待优化文本列表
     * @param context 上下文信息（可选，用于提供会话背景）
     * @return 优化结果
     */
    OptimizationResult optimize(List<TranscriptionResult> texts, String context);
    
    /**
     * 批量优化文本
     * 
     * @param texts 待优化文本列表
     * @param context 上下文信息
     * @return 优化结果列表
     */
    default List<OptimizationResult> optimizeBatch(List<TranscriptionResult> texts, String context) {
        return List.of(optimize(texts, context));
    }
    
    /**
     * 生成摘要
     * 
     * @param fullText 完整文本
     * @return 摘要结果
     */
    Summary generateSummary(String fullText);
    
    /**
     * 增量更新摘要
     * 
     * @param currentSummary 当前摘要
     * @param newText 新增文本
     * @return 更新后的摘要
     */
    default Summary updateSummary(Summary currentSummary, String newText) {
        String updatedText = currentSummary.getContent() + "\n" + newText;
        return generateSummary(updatedText);
    }
    
    /**
     * 获取模型名称
     * 
     * @return 模型标识名称
     */
    String getModelName();
    
    /**
     * 获取模型提供商名称
     * 
     * @return 提供商名称，如 "ollama"、"openai" 等
     */
    default String getProviderName() {
        return "unknown";
    }
    
    /**
     * 健康检查
     * 
     * @return 是否可用
     */
    boolean isHealthy();
    
    /**
     * 获取服务版本
     * 
     * @return 版本信息
     */
    default String getVersion() {
        return "1.0.0";
    }
}
