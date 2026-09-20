package com.ikoyki.webtools.kanban.backend.dto.request;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBoardRequest {
    private String name;
}
