package com.ikoyki.webtools.kanban.backend.dto.response;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardResponse {
    private Long id;
    private String name;
    private List<ColumnResponse> columns;
}
