package com.feishu.miji.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentData {

    private String title;
    private LocalDateTime generatedTime;
    private String duration;
    private int totalWords;
    private String sessionId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int segmentCount;
    private String summary;
    private List<String> keyPoints;
    private List<String> actionItems;
    private List<SegmentData> segments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SegmentData {
        private int index;
        private String startTime;
        private String endTime;
        private String originalText;
        private String optimizedText;
    }
}