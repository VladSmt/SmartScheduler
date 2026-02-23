package com.cr0w.smartplanner.telegram.model;

import java.time.LocalDateTime;

import com.cr0w.smartplanner.telegram.enums.ChatState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddEventContext {
    private ChatState state;
    private String title;
    private LocalDateTime date;
    private String description;
}