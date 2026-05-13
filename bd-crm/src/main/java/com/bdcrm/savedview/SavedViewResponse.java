package com.bdcrm.savedview;

public record SavedViewResponse(
        Long id,
        String pageKey,
        String name,
        boolean shared,
        String configJson,
        Long ownerUserId,
        String ownerUserName) {

    public static SavedViewResponse from(SavedView view) {
        return new SavedViewResponse(
                view.getId(),
                view.getPageKey(),
                view.getName(),
                view.isShared(),
                view.getConfigJson(),
                view.getOwner().getId(),
                view.getOwner().getFullName());
    }
}
