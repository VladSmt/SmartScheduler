package com.cr0w.smartplanner.telegram.enums;

import java.util.Optional;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CallbackOperation {
    // Івенти
    EVENT_VIEW("GET"),    // Перегляд деталей
    EVENT_PAGE("PAG"),    // Навігація по списку (пагінація)
    EVENT_DELETE("DEL"),  // Видалення
    EVENT_EDIT("EDT"),    // Редагування

    // Налаштування
    SETTINGS("SET"),
    HELP("HELP"),
    SET_MODE("MODE"),     // Зміна режиму (AI/Manual)
    SET_LANG("LANG"),     // Зміна мови

    // Інше
    IGNORE("noop"),      // Для кнопок-заглушок
    CONFIRM("CONFIRM"),
    CANCEL("CANCEL");
    private final String prefix;

    // Статичний метод для швидкого пошуку за рядком (використовується в роутері)
    public static Optional<CallbackOperation> fromPrefix(String data) {
        if (data == null) return Optional.empty();

        for (CallbackOperation op : values()) {
            if (data.startsWith(op.prefix + ":")) {
                return Optional.of(op);
            }
        }
        return Optional.empty();
    }
}
