package vn.edu.hust.soict.soe.assetmanagement.audit.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.hust.soict.soe.assetmanagement.audit.entity.AuditLog;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ==============================================================================
 * REPOSITORY: AuditLogRepository
 * PURPOSE: Provides database access for the `audit_logs` table.
 * RULE COMPLIANCE: Features a robust dynamic query to satisfy the RP-03 
 * filtering requirements (by user, date, and entity/module).
 * ==============================================================================
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /**
     * Dynamic search query. If a parameter is NULL, that specific filter is ignored.
     */
    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:module IS NULL OR a.module = :module) AND " +
           "(:action IS NULL OR a.action = :action) AND " +
           "(:performedBy IS NULL OR a.performedBy = :performedBy) AND " +
           "(cast(:startDate as timestamp) IS NULL OR a.performedAt >= :startDate) AND " +
           "(cast(:endDate as timestamp) IS NULL OR a.performedAt <= :endDate)")
    Page<AuditLog> searchLogs(
            @Param("module") String module, 
            @Param("action") String action, 
            @Param("performedBy") String performedBy,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
}