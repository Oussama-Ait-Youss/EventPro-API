package com.eventpro.api.repository;



import com.eventpro.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.cdi.JpaRepositoryExtension;


public interface UserRepository extends JpaRepository<User,Long>{

    boolean existsByEmail(String email);
}
