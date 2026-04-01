package com.example.InternalControl.model;

import com.example.InternalControl.model.enums.LocationType;
import com.example.InternalControl.model.Organization;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "location")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Location {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "location_id")
  private Long locationId;

  @Column(name = "org_number", nullable = false)
  private Integer orgNumber;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(length = 255)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "location_type", nullable = false, length = 20)
  @Builder.Default
  private LocationType locationType = LocationType.OTHER;

  @Column(name = "temp_min_c", precision = 5, scale = 2)
  private BigDecimal tempMinC;

  @Column(name = "temp_max_c", precision = 5, scale = 2)
  private BigDecimal tempMaxC;

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
  @JoinColumn(name = "org_number", insertable = false, updatable = false)
  private Organization organization;
}
