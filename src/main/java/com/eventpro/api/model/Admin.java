package com.eventpro.api.model;


import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table (name = "admin")
@Getter
@Setter
@NoArgsConstructor
public class Admin extends User{

}
