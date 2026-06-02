package vn.edu.hust.soict.soe.assetmanagement.audit.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ==============================================================================
 * ENTITY: AuditLog
 * PURPOSE: Maps to the `audit_logs` table. Acts as the immutable, append-only 
 * ledger for the entire system's security and data changes (RP-03).
 * RULE COMPLIANCE: 
 * - Explicitly DOES NOT extend `BaseEntity`. Append-only tables cannot have 
 * an `updated_at` column per the database schema rules.
 * - All fields are mapped with lengths matching V5__create_audit_log.sql.
 * ==============================================================================
 */
@Entity
@Table(name = "audit_logs")
@Getter // Lombok generates getters for all fields
@Setter // Lombok generates setters, but they should be used with caution since this is an append-only entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String module; // E.g., ASSET, STOCK, HANDOVER

    @Column(nullable = false, length = 50)
    private String action; // E.g., CREATE, UPDATE_STATUS, APPROVE

    @Column(name = "record_id", length = 255)
    private String recordId; // The ID of the affected record

    @Column(name = "record_code", length = 100)
    private String recordCode; // The human-readable business code (e.g., TSCD-001)

    @Column(name = "performed_by", nullable = false, length = 100)
    private String performedBy; // Username of the actor

    @Column(name = "user_id")
    private UUID userId; // Internal ID of the actor

    @Column(name = "ip_address", length = 45)
    private String ipAddress; // Network IP (IPv4 or IPv6)

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue; // JSON snapshot BEFORE the action

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue; // JSON snapshot AFTER the action

    @Column(columnDefinition = "TEXT")
    private String description; // Human-readable summary

    @Builder.Default
    @Column(name = "performed_at", nullable = false, updatable = false)
    private LocalDateTime performedAt = LocalDateTime.now(); // Immutable timestamp
}