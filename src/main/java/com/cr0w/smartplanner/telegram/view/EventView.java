package com.cr0w.smartplanner.telegram.view;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.cr0w.smartplanner.dto.CreateEventDTO;
import com.cr0w.smartplanner.dto.EventDTO;
import com.cr0w.smartplanner.telegram.enums.CallbackOperation;
import com.cr0w.smartplanner.telegram.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventView {

    private static final DateTimeFormatter FULL_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final String CALLBACK_DELIMITER = ":";
    private final MessageService ms;

    /**
     * Форматує детальну інформацію про івент.
     */
    public String formatEventDetail(EventDTO event) {
        return ms.getMessage("view.event.detail",
                escapeHtml(event.getTitle()),
                escapeHtml(StringUtils.defaultIfBlank(event.getDescription(), ms.getMessage("event.no_description"))),
                formatDate(event.getEventDate())
        );
    }

    public String formatConfirmation(CreateEventDTO dto) {
        return String.format(
                "<b>🔍 Перевір, чи я все правильно зрозумів:</b>\n\n" +
                        "📌 <b>Назва:</b> %s\n" +
                        "📅 <b>Дата:</b> %s\n" +
                        "📝 <b>Опис:</b> %s\n\n" +
                        "Зберігаємо?",
                dto.getTitle(),
                dto.getEventDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                dto.getDescription() != null ? dto.getDescription() : "немає"
        );
    }

    /**
     * Створює клавіатуру для керування конкретною подією (Перегляд -> Редагувати/Видалити/Назад).
     * * @param eventId ID події
     *
     * @param currentPage Сторінка списку, на яку треба повернутися (UX detail!)
     */
    public InlineKeyboardMarkup createEventControlKeyboard(Long eventId, int currentPage) {
        // Використовуємо наші хелпери та Enum-и
        var editBtn = button(
                ms.getMessage("common.edit"),
                createCallback(CallbackOperation.EVENT_EDIT, eventId)
        );

        var deleteBtn = button(
                ms.getMessage("common.delete"),
                createCallback(CallbackOperation.EVENT_DELETE, eventId)
        );

        var backBtn = button(
                ms.getMessage("common.back_to_list"),
                createCallback(CallbackOperation.EVENT_PAGE, currentPage)
        );

        return InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(editBtn, deleteBtn))
                .keyboardRow(new InlineKeyboardRow(backBtn))
                .build();
    }

    /**
     * Створює клавіатуру списку івентів з пагінацією.
     */
    public InlineKeyboardMarkup createListKeyboard(Page<EventDTO> page) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        // Генеруємо кнопки івентів
        page.getContent().forEach(event -> {
            rows.add(new InlineKeyboardRow(
                    button("🔍 " + escapeHtml(event.getTitle()),
                            createCallback(CallbackOperation.EVENT_VIEW, event.getId()))
            ));
        });

        // Додаємо навігацію, якщо сторінок більше однієї
        if (page.getTotalPages() > 1) {
            rows.add(createNavigationRow(page));
        }

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    /**
     * Форматує список подій для відображення в повідомленні.
     */
    public String formatEventList(Page<EventDTO> page) {
        // 1. Guard Clause: Якщо сторінка порожня, не треба нічого вигадувати
        if (page == null || page.isEmpty()) {
            return ms.getMessage("view.event.list_empty");
        }

        StringBuilder sb = new StringBuilder();

        // 2. Шапка списку (Сторінка X з Y)
        sb.append(ms.getMessage("view.event.list_header",
                page.getNumber() + 1,
                page.getTotalPages()));
        sb.append("\n\n");

        // 3. Розраховуємо стартовий індекс для поточної сторінки
        int index = (page.getNumber() * page.getSize()) + 1;

        for (EventDTO event : page.getContent()) {
            // Використовуємо короткий формат дати для списку
            String dateStr = event.getEventDate() != null
                    ? event.getEventDate().format(DateTimeFormatter.ofPattern("dd.MM HH:mm"))
                    : "??.??";

            // Формуємо рядок за шаблоном із ресурсів
            // Наприклад: "<b>%d.</b> <code>%s</code> — <b>%s</b>\n"
            sb.append(ms.getMessage("view.event.list_item",
                    index++,
                    dateStr,
                    escapeHtml(event.getTitle())));
        }

        return sb.toString();
    }

    private InlineKeyboardRow createNavigationRow(Page<EventDTO> page) {
        List<InlineKeyboardButton> navButtons = new ArrayList<>();

        if (page.hasPrevious()) {
            navButtons.add(button(ms.getMessage("common.back"),
                    createCallback(CallbackOperation.EVENT_PAGE, page.getNumber() - 1)));
        }

        // Інформаційна кнопка (поточна сторінка)
        navButtons.add(button(
                String.format("%d / %d", page.getNumber() + 1, page.getTotalPages()),
                CallbackOperation.IGNORE.getPrefix())
        );

        if (page.hasNext()) {
            navButtons.add(button(ms.getMessage("common.next"),
                    createCallback(CallbackOperation.EVENT_PAGE, page.getNumber() + 1)));
        }

        return new InlineKeyboardRow(navButtons);
    }

    // --- Private Infrastructure Helpers ---

    private String createCallback(CallbackOperation operation, Object payload) {
        return operation.getPrefix() + CALLBACK_DELIMITER + payload.toString();
    }

    private InlineKeyboardButton button(String text, String callbackData) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(callbackData)
                .build();
    }

    private String formatDate(LocalDateTime date) {
        return date != null ? date.format(FULL_DATE_FORMATTER) : ms.getMessage("common.date_not_set");
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}