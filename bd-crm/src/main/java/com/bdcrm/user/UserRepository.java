package com.bdcrm.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = "roles")
    Optional<User> findByUsernameIgnoreCase(String username);

    @EntityGraph(attributePaths = {"roles", "organization"})
    List<User> findAllByUsernameIgnoreCase(String username);

    @EntityGraph(attributePaths = "roles")
    List<User> findAllByOrderByFullNameAsc();

    @EntityGraph(attributePaths = {"roles", "organization"})
    List<User> findAllByOrganizationIdOrderByFullNameAsc(Long organizationId);

    @EntityGraph(attributePaths = "roles")
    Optional<User> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"roles", "organization"})
    List<User> findAllByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"roles", "organization"})
    Optional<User> findByIdAndOrganizationId(Long id, Long organizationId);

    @EntityGraph(attributePaths = {"roles", "organization"})
    Optional<User> findByUsernameIgnoreCaseAndOrganizationId(String username, Long organizationId);

    @EntityGraph(attributePaths = {"roles", "organization"})
    Optional<User> findByEmailIgnoreCaseAndOrganizationId(String email, Long organizationId);
}
