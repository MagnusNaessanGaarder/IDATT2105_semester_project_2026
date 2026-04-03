package com.example.InternalControl.service.checklist.mapper;

import com.example.InternalControl.dto.user.UserDto;
import com.example.InternalControl.dto.checklist.response.ChecklistTemplateItemResponse;
import com.example.InternalControl.dto.checklist.response.ChecklistTemplateResponse;
import com.example.InternalControl.model.auth.AppUser;
import com.example.InternalControl.model.checklist.ChecklistTemplate;
import com.example.InternalControl.model.checklist.ChecklistTemplateItem;
import com.example.InternalControl.repository.user.AppUserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Mapper for converting ChecklistTemplate entities to DTOs.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChecklistTemplateMapper {

    private final AppUserRepository userRepository;
    private final ObjectMapper objectMapper;

    public ChecklistTemplateResponse toResponse(ChecklistTemplate template) {
        if (template == null) {
            return null;
        }

        return ChecklistTemplateResponse.builder()
                .templateId(template.getTemplateId())
                .orgNumber(template.getOrgNumber())
                .moduleType(template.getModuleType())
                .title(template.getTitle())
                .description(template.getDescription())
                .frequency(template.getFrequency())
                .isActive(template.getIsActive())
                .createdByUserId(template.getCreatedByUserId())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .items(template.getItems() != null
                        ? template.getItems().stream()
                                .map(this::toItemResponse)
                                .collect(Collectors.toList())
                        : null)
                .build();
    }

    public ChecklistTemplateItemResponse toItemResponse(ChecklistTemplateItem item) {
        if (item == null) {
            return null;
        }

        return ChecklistTemplateItemResponse.builder()
                .itemId(item.getItemId())
                .templateId(item.getTemplate() != null ? item.getTemplate().getTemplateId() : null)
                .sortOrder(item.getSortOrder())
                .label(item.getLabel())
                .description(item.getDescription())
                .itemType(item.getItemType())
                .isRequired(item.getIsRequired())
                .expectedText(item.getExpectedText())
                .expectedNumericMin(item.getExpectedNumericMin())
                .expectedNumericMax(item.getExpectedNumericMax())
                .choiceOptionsJson(item.getChoiceOptionsJson())
                .build();
    }

    public List<ChecklistTemplateResponse> toResponseList(List<ChecklistTemplate> templates) {
        if (templates == null) {
            return Collections.emptyList();
        }
        return templates.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private UserDto toUserDto(AppUser user) {
        if (user == null) {
            return null;
        }
        return UserDto.builder()
                .userId(user.getUserId())
                .displayName(user.getDisplayName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private List<String> parseChoiceOptions(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse choice options JSON: {}", json);
            return Collections.emptyList();
        }
    }
}
