package com.eventpro.api.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table (name = "organizers")
@Getter
@Setter
@NoArgsConstructor
public class Organizer extends User {


    @Column (name = "organisation_name")
    private String nom_organisation;


    @OneToMany (mappedBy = "organizer",fetch = FetchType.EAGER)
    private List<Event> eventList = new ArrayList<>();


}
