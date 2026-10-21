package com.eventpro.api.model;


import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table (name = "agent_control")
@Getter
@Setter
@NoArgsConstructor
public class AgentConrol extends User{

}
