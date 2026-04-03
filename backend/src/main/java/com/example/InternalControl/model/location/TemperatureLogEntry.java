package com.example.InternalControl.model.location;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author TriTacLe
 * @since 1.0
 */
@Getter
@Setter
@Entity
@Table(name = "temperature_log_entry")
public class TemperatureLogEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "entry_id")
  private Long entryId;

  @Column(name = "org_number", nullable = false)
  private Integer orgNumber;

  @Column(name = "log_point_id", nullable = false)
  private Long logPointId;

  @Column(name = "recorded_by_user_id", nullable = false)
  private Long recordedByUserId;

  @Column(name = "measured_at", nullable = false)
  private LocalDateTime measuredAt;

  @Column(name = "temperature_c", nullable = false, precision = 5, scale = 2)
  private BigDecimal temperatureC;

  @Column(name = "is_alert", nullable = false)
  private Boolean isAlert = false;

  @Column(name = "note_text", columnDefinition = "TEXT")
  private String noteText;

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
