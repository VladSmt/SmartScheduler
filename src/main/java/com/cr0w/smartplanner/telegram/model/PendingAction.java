package com.cr0w.smartplanner.telegram.model;

import com.cr0w.smartplanner.dto.CreateEventDTO;
import com.cr0w.smartplanner.telegram.enums.ActionType;

public record PendingAction(ActionType type, Long targetId, CreateEventDTO dto, String description) {
}
