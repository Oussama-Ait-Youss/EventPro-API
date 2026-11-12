package com.eventpro.api.model;


import com.eventpro.api.model.enums.UserType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor (access = AccessLevel.PROTECTED)
public abstract class User {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;



    @Column (name = "name")
    private String name;


    @Column (name = "email")
    private String email;


    @Column(name = "is_active", nullable = false)
    private boolean active = true;


    @Column (name = "password")
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private UserType role;


}
