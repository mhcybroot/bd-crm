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
        Long organizationId = securityUtils.currentOrganizationId();
        return savedViewRepository.findByOrganizationIdAndPageKeyAndOwnerIdOrOrganizationIdAndPageKeyAndSharedTrueOrderByUpdatedAtDesc(
                        organizationId, pageKey, userId, organizationId, pageKey).stream()
                .map(SavedViewResponse::from)
                .toList();
    }

    @Transactional
    public SavedViewResponse save(SavedViewRequest request) {
        SavedView view = new SavedView();
        view.setOwner(securityUtils.currentUserEntity());
        view.setOrganization(securityUtils.currentOrganizationEntity());
        view.setPageKey(request.pageKey().trim());
        view.setName(request.name().trim());
        view.setShared(request.shared() && securityUtils.hasAnyRole("PLATFORM_ADMIN", "ORG_ADMIN", "ORG_MANAGER"));
        view.setConfigJson(request.configJson());
        return SavedViewResponse.from(savedViewRepository.save(view));
    }
}
