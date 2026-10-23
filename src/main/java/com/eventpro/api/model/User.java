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


    @Column (name = "isActive",nullable = false)
    private boolean isActive;


    @Column (name = "password")
    private String Password;

    @Enumerated
    private UserType role;


}
