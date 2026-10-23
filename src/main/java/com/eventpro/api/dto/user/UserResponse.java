package com.eventpro.api.dto.user;



import com.eventpro.api.model.User;
import com.eventpro.api.model.enums.UserType;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private UserType userType;
    private String organization_name;

    public UserResponse(User user) {
    }
}
