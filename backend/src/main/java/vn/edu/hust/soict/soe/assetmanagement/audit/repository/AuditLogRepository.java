package vn.edu.hust.soict.soe.assetmanagement.audit.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.edu.hust.soict.soe.assetmanagement.audit.entity.AuditLog;

import java.util.UUID;
/**
 * Audit log repository (RP-03).
 * Provides read-only search capabilities for the audit trail.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:module IS NULL OR a.module = :module) AND " +
           "(:action IS NULL OR a.action = :action)")
    Page<AuditLog> searchLogs(String module, String action, Pageable pageable);
}