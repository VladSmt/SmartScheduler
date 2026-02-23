package com.cr0w.smartplanner.telegram.model;

import com.cr0w.smartplanner.telegram.enums.CallbackOperation;

public record EventPayload(Long id, CallbackOperation action) {
}