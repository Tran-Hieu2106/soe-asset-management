package vn.edu.hust.soict.soe.assetmanagement.handover.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.hust.soict.soe.assetmanagement.asset.enums.AssetStatus;
import vn.edu.hust.soict.soe.assetmanagement.asset.service.FixedAssetService;
import vn.edu.hust.soict.soe.assetmanagement.audit.service.AuditLogService;
import vn.edu.hust.soict.soe.assetmanagement.exception.BusinessRuleException;
import vn.edu.hust.soict.soe.assetmanagement.exception.ResourceNotFoundException;
import vn.edu.hust.soict.soe.assetmanagement.handover.dto.CreateHandoverRequest;
import vn.edu.hust.soict.soe.assetmanagement.handover.dto.HandoverDto;
import vn.edu.hust.soict.soe.assetmanagement.handover.entity.HandoverRequest;
import vn.edu.hust.soict.soe.assetmanagement.handover.entity.HandoverStatus;
import vn.edu.hust.soict.soe.assetmanagement.handover.repository.HandoverRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HandoverService Tests")
class HandoverServiceTest {

    @Mock private HandoverRepository handoverRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private FixedAssetService fixedAssetService;
    @Mock private HandoverDocumentService handoverDocumentService;

    @InjectMocks private HandoverService handoverService;

    private UUID assetId;
    private UUID requestId;
    private UUID fromUnitId;
    private UUID toUnitId;
    private String initiator;
    private String approver;
    private HandoverRequest existingRequest;

    @BeforeEach
    void setUp() {
        assetId = UUID.randomUUID();
        requestId = UUID.randomUUID();
        fromUnitId = UUID.randomUUID();
        toUnitId = UUID.randomUUID();
        initiator = "asset.manager";
        approver = "approver.user";

        existingRequest = HandoverRequest.builder()
                .id(requestId)
                .requestCode("BG-2024-001")
                .assetId(assetId)
                .fromUnitId(fromUnitId)
                .toUnitId(toUnitId)
                .initiatedBy(initiator)
                .reason("Transfer for sales team")
                .handoverDate(LocalDate.of(2024, 7, 1))
                .assetCondition("GOOD")
                .status(HandoverStatus.PENDING_APPROVAL)
                .build();
    }

    @Nested
    @DisplayName("Create handover")
    class CreateHandoverTests {

        @Test
        @DisplayName("Should create a new handover request in DRAFT status")
        void createHandover_success() {
            CreateHandoverRequest dto = new CreateHandoverRequest();
            dto.setAssetId(assetId);
            dto.setFromUnitId(fromUnitId);
            dto.setToUnitId(toUnitId);
            dto.setReason("Chuyển giao nội bộ");

            when(handoverRepository.hasActiveRequestForAsset(eq(assetId), anyList())).thenReturn(false);
            when(handoverRepository.save(any(HandoverRequest.class))).thenAnswer(invocation -> {
                HandoverRequest saved = invocation.getArgument(0);
                saved.setId(requestId);
                return saved;
            });

            HandoverDto result = handoverService.createHandover(dto, initiator);

            assertThat(result.getStatus()).isEqualTo(HandoverStatus.DRAFT);
            assertThat(result.getInitiatedBy()).isEqualTo(initiator);
            verify(auditLogService).log(eq("HANDOVER"), eq("CREATE"), anyString(), anyString(), isNull(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should reject duplicate active handover requests")
        void createHandover_activeRequestExists_throwsException() {
            CreateHandoverRequest dto = new CreateHandoverRequest();
            dto.setAssetId(assetId);
            dto.setFromUnitId(fromUnitId);
            dto.setToUnitId(toUnitId);

            when(handoverRepository.hasActiveRequestForAsset(eq(assetId), anyList())).thenReturn(true);

            assertThatThrownBy(() -> handoverService.createHandover(dto, initiator))
                    .isInstanceOf(BusinessRuleException.class);

            verify(handoverRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Approve and complete")
    class WorkflowTests {

        @Test
        @DisplayName("Should approve without transferring asset")
        void approveHandover_success() {
            when(handoverRepository.findById(requestId)).thenReturn(Optional.of(existingRequest));
            when(handoverRepository.save(any(HandoverRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

            HandoverDto result = handoverService.approveHandover(requestId, "OK", approver);

            assertThat(result.getStatus()).isEqualTo(HandoverStatus.APPROVED);
            verify(fixedAssetService, never()).updateAssetStatusAndUnit(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should not allow the initiator to approve their own request")
        void approveHandover_initiatorCannotApprove_throwsException() {
            when(handoverRepository.findById(requestId)).thenReturn(Optional.of(existingRequest));

            assertThatThrownBy(() -> handoverService.approveHandover(requestId, "OK", initiator))
                    .isInstanceOf(BusinessRuleException.class);

            verify(handoverRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should transfer asset on completion")
        void completeHandover_success() {
            existingRequest.setStatus(HandoverStatus.CONFIRMED);
            when(handoverRepository.findById(requestId)).thenReturn(Optional.of(existingRequest));
            when(handoverDocumentService.generateDocument(any())).thenReturn("DOC-001");
            when(handoverRepository.save(any(HandoverRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

            HandoverDto result = handoverService.completeHandover(requestId, approver);

            assertThat(result.getStatus()).isEqualTo(HandoverStatus.COMPLETED);
            verify(fixedAssetService).updateAssetStatusAndUnit(
                    eq(assetId),
                    eq(AssetStatus.TRANSFERRED),
                    eq(toUnitId),
                    anyString(),
                    eq(approver)
            );
        }

        @Test
        @DisplayName("Should reject a pending handover request")
        void rejectHandover_success() {
            when(handoverRepository.findById(requestId)).thenReturn(Optional.of(existingRequest));
            when(handoverRepository.save(any(HandoverRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

            HandoverDto result = handoverService.rejectHandover(requestId, "Reason not valid", approver);

            assertThat(result.getStatus()).isEqualTo(HandoverStatus.REJECTED);
            verify(fixedAssetService, never()).updateAssetStatusAndUnit(any(), any(), any(), any(), any());
        }
    }

    @Test
    @DisplayName("Should throw if handover request is not found")
    void getHandoverById_notFound_throwsException() {
        when(handoverRepository.findById(requestId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handoverService.getHandoverById(requestId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
