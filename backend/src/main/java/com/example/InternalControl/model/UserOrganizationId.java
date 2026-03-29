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
 * Composite primary key for UserOrganization entity.
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
public class UserOrganizationId implements Serializable {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "org_number", nullable = false)
    private Integer orgNumber;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserOrganizationId that = (UserOrganizationId) o;
        return Objects.equals(userId, that.userId) &&
               Objects.equals(orgNumber, that.orgNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, orgNumber);
    }
}
