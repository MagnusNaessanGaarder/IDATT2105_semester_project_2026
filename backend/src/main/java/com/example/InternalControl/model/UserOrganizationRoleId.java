package com.example.InternalControl.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for UserOrganizationRole entity.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserOrganizationRoleId implements Serializable {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "org_number", nullable = false)
    private Integer orgNumber;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserOrganizationRoleId that = (UserOrganizationRoleId) o;
        return Objects.equals(userId, that.userId) &&
               Objects.equals(orgNumber, that.orgNumber) &&
               Objects.equals(roleId, that.roleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, orgNumber, roleId);
    }
}
