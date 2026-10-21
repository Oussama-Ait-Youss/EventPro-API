package com.eventpro.api.model;


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

    @Column (name = "first_name")
    private String FirstName;

    @Column (name = "last_name")
    private String LastName;


    @Column (name = "email")
    private String email;


    @Column (name = "password_hash")
    private String PasswordHash;


}
