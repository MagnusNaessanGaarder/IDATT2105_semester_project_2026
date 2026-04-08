package com.example.InternalControl.model.temperature;

import com.example.InternalControl.model.organization.Location;
import com.example.InternalControl.model.user.AppUser;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "temperature_log_entry")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
  @Builder.Default
  private Boolean isAlert = false;

  @Column(name = "note_text", columnDefinition = "TEXT")
  private String noteText;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "log_point_id", insertable = false, updatable = false)
  @JsonIgnore
  private TemperatureLogPoint logPoint;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "recorded_by_user_id", insertable = false, updatable = false)
  @JsonIgnore
  private AppUser recordedBy;
}
