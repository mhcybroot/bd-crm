package com.bdcrm.savedview;

import com.bdcrm.auth.SecurityUtils;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SavedViewService {

    private final SavedViewRepository savedViewRepository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public List<SavedViewResponse> list(String pageKey) {
        Long userId = securityUtils.currentUserEntity().getId();
        return savedViewRepository.findByPageKeyAndOwnerIdOrPageKeyAndSharedTrueOrderByUpdatedAtDesc(pageKey, userId, pageKey).stream()
                .map(SavedViewResponse::from)
                .toList();
    }

    @Transactional
    public SavedViewResponse save(SavedViewRequest request) {
        SavedView view = new SavedView();
        view.setOwner(securityUtils.currentUserEntity());
        view.setPageKey(request.pageKey().trim());
        view.setName(request.name().trim());
        view.setShared(request.shared() && securityUtils.hasAnyRole("ADMIN", "MANAGER"));
        view.setConfigJson(request.configJson());
        return SavedViewResponse.from(savedViewRepository.save(view));
    }
}
