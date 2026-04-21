package com.meetingmind.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateInfo {

    private String name;
    private String displayName;
    private String description;
    private boolean builtIn;
}

