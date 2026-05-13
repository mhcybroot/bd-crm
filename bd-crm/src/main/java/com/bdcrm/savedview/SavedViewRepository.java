package com.bdcrm.savedview;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedViewRepository extends JpaRepository<SavedView, Long> {

    @EntityGraph(attributePaths = {"owner"})
    List<SavedView> findByPageKeyAndOwnerIdOrPageKeyAndSharedTrueOrderByUpdatedAtDesc(
            String pageKey,
            Long ownerId,
            String samePageKey);
}
