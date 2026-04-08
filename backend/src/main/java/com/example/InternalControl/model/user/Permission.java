package com.example.InternalControl.model.user;

import jakarta.persistence.*;
import lombok.*;

/**
 * JPA Entity mapping to permission table.
 * Represents a granular permission that can be assigned to roles.
 */
@Entity
@Table(name = "permission")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_id")
    private Long permissionId;

    @Column(name = "permission_key", nullable = false, unique = true, length = 100)
    private String permissionKey;

    @Column(name = "description")
    private String description;
}
