package booking.entity;

import java.time.LocalDateTime;

import booking.enums.BookingStatus;
import booking.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;



@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
//@Builder
public class Roles {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
		
	 	@Enumerated(EnumType.STRING)
	    @Column(nullable = false, unique = true, length = 30)
	    private Role name;
	 
	    public Roles(Role name) {
	        this.name = name;
	        
	    }
}
