package com.bdcrm.pipeline;

import java.time.OffsetDateTime;

public record LeadStageHistoryResponse(
        Long id,
        Long stageId,
        String stageName,
        Long changedByUserId,
        String changedByUserName,
        String changeNote,
        OffsetDateTime enteredAt,
        OffsetDateTime exitedAt) {

    public static LeadStageHistoryResponse from(LeadStageHistory history) {
        return new LeadStageHistoryResponse(
                history.getId(),
                history.getStage().getId(),
                history.getStage().getName(),
                history.getChangedBy().getId(),
                history.getChangedBy().getFullName(),
                history.getChangeNote(),
                history.getEnteredAt(),
                history.getExitedAt());
    }
}
