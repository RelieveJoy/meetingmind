package com.feishu.miji.service;

import com.feishu.miji.entity.SummaryRecord;

import java.util.List;

/**
 * 摘要历史管理服务接口
 * 
 * 负责管理会话的摘要历史，支持查询、导出等功能
 */
public interface SummaryHistoryService {
    
    /**
     * 获取会话的所有总结历史
     * @param sessionId 会话ID
     * @return 摘要记录列表
     */
    List<SummaryRecord> getHistory(String sessionId);
    
    /**
     * 获取最新总结
     * @param sessionId 会话ID
     * @return 最新摘要记录
     */
    SummaryRecord getLatest(String sessionId);
    
    /**
     * 获取指定版本的总结
     * @param sessionId 会话ID
     * @param version 版本号
     * @return 摘要记录
     */
    SummaryRecord getByVersion(String sessionId, int version);
    
    /**
     * 导出总结历史
     * @param sessionId 会话ID
     * @param format 导出格式
     * @return 导出内容
     */
    String exportHistory(String sessionId, ExportFormat format);
    
    /**
     * 清理会话历史
     * @param sessionId 会话ID
     */
    void clearHistory(String sessionId);
    
    /**
     * 保存摘要记录
     * @param record 摘要记录
     */
    void saveRecord(SummaryRecord record);
    
    /**
     * 导出格式枚举
     */
    enum ExportFormat {
        JSON,  // 完整数据结构
        MARKDOWN,  // 可读性好的格式
        TEXT  // 纯文本格式
    }
}