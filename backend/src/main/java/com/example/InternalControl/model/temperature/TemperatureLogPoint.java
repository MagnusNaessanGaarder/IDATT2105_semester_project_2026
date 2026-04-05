package com.example.InternalControl.model.temperature;

import com.example.InternalControl.model.organization.Location;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "temperature_log_point")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemperatureLogPoint {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "log_point_id")
  private Long logPointId;

  @Column(name = "org_number", nullable = false)
  private Integer orgNumber;

  @Column(name = "location_id", nullable = false)
  private Long locationId;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private Boolean isActive = true;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "location_id", insertable = false, updatable = false)
  private Location location;
}
