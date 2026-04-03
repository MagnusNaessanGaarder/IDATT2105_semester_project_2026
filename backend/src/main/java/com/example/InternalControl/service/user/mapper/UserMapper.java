package com.example.InternalControl.service.user.mapper;

import com.example.InternalControl.dto.user.UserDto;
import com.example.InternalControl.model.auth.AppUser;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting User entities to DTOs.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Component
public class UserMapper {

  public UserDto toDto(AppUser user) {
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
}
