package com.example.InternalControl.dto.user;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for User data.
 * Used to avoid exposing internal entity relationships.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Data
@Builder
public class UserDto {
  private Long userId;
  private String displayName;
  private String email;
  private String phone;
  private Boolean isActive;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
