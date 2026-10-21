package com.eventpro.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "ticket_categories",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_ticket_category_name",
                        columnNames = "name"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class TicketCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @OneToMany(mappedBy = "ticketCategory", fetch = FetchType.LAZY)
    private List<TicketCategoriesPerEvent> eventCategories =
            new ArrayList<>();
}