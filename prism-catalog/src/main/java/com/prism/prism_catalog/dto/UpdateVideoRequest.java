package com.prism.prism_catalog.dto;

import java.util.List;

import com.prism.prism_catalog.model.enums.VideoVisibility;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateVideoRequest {

    @Size(min = 1, max = 200, message = "Title must be between 1-200 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    private VideoVisibility visibility;

    @Size(max = 50, message = "Category must not exceed 50 characters")
    private String category;

    private List<@Size(max = 30, message = "Tag must not exceed 30 characters") String> tags;
}
