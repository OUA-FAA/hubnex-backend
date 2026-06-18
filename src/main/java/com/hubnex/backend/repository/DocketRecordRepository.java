package com.hubnex.backend.repository;

import com.hubnex.backend.model.DocketRecord;
import com.hubnex.backend.model.EtatColis;
import com.hubnex.backend.model.TypeFlux;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocketRecordRepository extends JpaRepository<DocketRecord, Long> {
    Optional<DocketRecord> findByConnote(String connote);

    boolean existsByConnote(String connote);

    List<DocketRecord> findByEtat(EtatColis etat);

    List<DocketRecord> findByHubPrincipalId(Long hubId);

    List<DocketRecord> findByHubSecondaireId(Long hubId);

    List<DocketRecord> findByImportBatchId(String importBatchId);

    List<DocketRecord> findByImportBatchIdIn(List<String> importBatchIds);

    List<DocketRecord> findByImportBatchIdIsNotNull();

    List<DocketRecord> findByReceptionRecoveryId(String receptionRecoveryId);

    List<DocketRecord> findByReceptionRecoveryIdIsNotNull();

    List<DocketRecord> findByReceptionRecoveryIdAndTypeFlux(String receptionRecoveryId, TypeFlux typeFlux);

    List<DocketRecord> findByReceptionRecoveryIdIsNotNullAndTypeFlux(TypeFlux typeFlux);

    List<DocketRecord> findByTypeFluxAndConveyorSentTrueAndRecoveredAtIsNull(TypeFlux typeFlux);
}
