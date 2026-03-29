package com.example.InternalControl.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * JPA Entity mapping to role table.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Entity
@Table(name = "role")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "role_name", nullable = false, unique = true)
    private String roleName;

    @Column(name = "description")
    private String description;

    @Column(name = "is_system_role", nullable = false)
    private Boolean isSystemRole = false;

    public String getAuthority() {
        return "ROLE_" + roleName;
    }
}
