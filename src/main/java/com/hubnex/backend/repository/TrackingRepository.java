package com.hubnex.backend.repository;

import com.hubnex.backend.model.Tracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TrackingRepository extends JpaRepository<Tracking, Long> {
    List<Tracking> findByUtilisateurId(Long userId);

    List<Tracking> findByDocketRecordId(Long docketRecordId);

    boolean existsByDocketRecordIdAndStatutAndNote(Long docketRecordId, String statut, String note);

    @Modifying
    @Query("delete from Tracking tracking where tracking.docketRecord.id in :docketRecordIds")
    int deleteByDocketRecordIds(@Param("docketRecordIds") List<Long> docketRecordIds);
}
