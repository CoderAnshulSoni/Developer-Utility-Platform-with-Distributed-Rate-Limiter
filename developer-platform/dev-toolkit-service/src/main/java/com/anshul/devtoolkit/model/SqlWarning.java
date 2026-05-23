package com.anshul.devtoolkit.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlWarning {
    private String type;
    private String severity; // HIGH, MEDIUM, LOW
    private String message;
    private String suggestion;
}
