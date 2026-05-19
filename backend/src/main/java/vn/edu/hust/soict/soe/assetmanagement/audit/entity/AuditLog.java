package vn.edu.hust.soict.soe.assetmanagement.audit.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;
/**
 * Audit log entity — maps to the `audit_logs` table (RP-03).
 * Immutable record of all system data changes across modules.
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String module;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "record_id", length = 255)
    private String recordId;

    @Column(name = "record_code", length = 100)
    private String recordCode;

    @Column(name = "performed_by", nullable = false, length = 100)
    private String performedBy;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(name = "performed_at", nullable = false, updatable = false)
    private LocalDateTime performedAt = LocalDateTime.now();
}