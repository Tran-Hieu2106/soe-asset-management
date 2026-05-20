package vn.edu.hust.soict.soe.assetmanagement.audit.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import vn.edu.hust.soict.soe.assetmanagement.audit.entity.AuditLog;
import vn.edu.hust.soict.soe.assetmanagement.audit.repository.AuditLogRepository;
import vn.edu.hust.soict.soe.assetmanagement.user.entity.User;
import static org.mockito.Mockito.lenient;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditLogService Tests")
class AuditLogServiceTest {

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;
    @InjectMocks private AuditLogService auditLogService;

    private User currentUser;
    private ServletRequestAttributes attributes;

    @BeforeEach
    void setUp() {
        currentUser = User.builder()
                .id(UUID.randomUUID())
                .username("audit.user")
                .passwordHash("secret")
                .fullName("Audit User")
                .build();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "192.168.1.100");
        attributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(attributes);

        lenient().when(authentication.isAuthenticated()).thenReturn(true);
        lenient().when(authentication.getPrincipal()).thenReturn(currentUser);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should persist audit log with current user and request IP")
    void log_savesAuditLogWithUserAndIp() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        auditLogService.log("HANDOVER", "APPROVE", "record-123", "BG-2024-001", "old", "new", "Approved handover");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getModule()).isEqualTo("HANDOVER");
        assertThat(saved.getAction()).isEqualTo("APPROVE");
        assertThat(saved.getRecordId()).isEqualTo("record-123");
        assertThat(saved.getRecordCode()).isEqualTo("BG-2024-001");
        assertThat(saved.getPerformedBy()).isEqualTo("audit.user");
        assertThat(saved.getUserId()).isEqualTo(currentUser.getId());
        assertThat(saved.getIpAddress()).isEqualTo("192.168.1.100");
        assertThat(saved.getOldValue()).isEqualTo("old");
        assertThat(saved.getNewValue()).isEqualTo("new");
        assertThat(saved.getDescription()).isEqualTo("Approved handover");
    }

    @Test
    @DisplayName("Should return paged audit logs from repository")
    void getAuditLogs_returnsPagedDtos() {
        AuditLog auditLog = AuditLog.builder()
                .id(UUID.randomUUID())
                .module("HANDOVER")
                .action("APPROVE")
                .recordId("record-123")
                .recordCode("BG-2024-001")
                .performedBy("audit.user")
                .ipAddress("192.168.1.100")
                .oldValue("old")
                .newValue("new")
                .description("Approved handover")
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<AuditLog> page = new PageImpl<>(List.of(auditLog), pageable, 1);
        when(auditLogRepository.searchLogs(eq("HANDOVER"), eq("APPROVE"), eq(pageable))).thenReturn(page);

        Page<?> result = auditLogService.getAuditLogs("HANDOVER", "APPROVE", pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).hasFieldOrPropertyWithValue("module", "HANDOVER");
    }
}
