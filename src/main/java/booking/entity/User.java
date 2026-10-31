package booking.entity;

import booking.enums.Role;
import booking.enums.SlotStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SlotStatus status = SlotStatus.AVAILABLE;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    public Role getRole() {
        if (roles != null && !roles.isEmpty()) {
            return roles.iterator().next();
        }
        return Role.ROLE_USER;
    }

    public void setRole(Role role) {
        if (this.roles == null) {
            this.roles = new HashSet<>();
        }
        this.roles.clear();
        if (role != null) {
            this.roles.add(role);
        }
    }

    public static class UserBuilder {
        public UserBuilder role(Role role) {
            this.roles$value = new HashSet<>();
            if (role != null) {
                this.roles$value.add(role);
            }
            this.roles$set = true;
            return this;
        }
    }
}
