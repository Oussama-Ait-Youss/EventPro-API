package com.eventpro.api.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Entity
@Table (name = "client")
@Getter
@Setter
@NoArgsConstructor
public class Client extends User {



    @Column
    private String phone;

    @OneToMany (mappedBy = "client",fetch = FetchType.EAGER)
    private List<Reservation> reservationList;
}
