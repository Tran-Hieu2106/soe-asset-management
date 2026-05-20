package vn.edu.hust.soict.soe.assetmanagement.handover.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.hust.soict.soe.assetmanagement.asset.entity.FixedAsset;
import vn.edu.hust.soict.soe.assetmanagement.asset.enums.AssetStatus;
import vn.edu.hust.soict.soe.assetmanagement.asset.repository.FixedAssetRepository;
import vn.edu.hust.soict.soe.assetmanagement.asset.service.FixedAssetService;
import vn.edu.hust.soict.soe.assetmanagement.audit.service.AuditLogService;
import vn.edu.hust.soict.soe.assetmanagement.exception.BusinessRuleException;
import vn.edu.hust.soict.soe.assetmanagement.exception.ResourceNotFoundException;
import vn.edu.hust.soict.soe.assetmanagement.handover.dto.CreateHandoverRequest;
import vn.edu.hust.soict.soe.assetmanagement.handover.dto.HandoverDto;
import vn.edu.hust.soict.soe.assetmanagement.handover.entity.HandoverRequest;
import vn.edu.hust.soict.soe.assetmanagement.handover.entity.HandoverStatus;
import vn.edu.hust.soict.soe.assetmanagement.handover.repository.HandoverRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HandoverService Tests")
class HandoverServiceTest {

    @Mock private HandoverRepository handoverRepository;
    @Mock private AuditLogService auditLogService;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();
    @Mock private FixedAssetRepository fixedAssetRepository;
    @Mock private FixedAssetService fixedAssetService;

    @InjectMocks private HandoverService handoverService;

    private UUID assetId;
    private UUID requestId;
    private UUID fromUnitId;
    private UUID toUnitId;
    private String initiator;
    private String approver;
    private FixedAsset fixedAsset;
    private HandoverRequest existingRequest;

    @BeforeEach
    void setUp() {
        assetId = UUID.randomUUID();
        requestId = UUID.randomUUID();
        fromUnitId = UUID.randomUUID();
        toUnitId = UUID.randomUUID();
        initiator = "asset.manager";
        approver = "approver.user";

        fixedAsset = new FixedAsset();
        fixedAsset.setId(assetId);
        fixedAsset.setAssetCode("TS-2024-001");
        fixedAsset.setName("Máy tính xách tay Dell Latitude 5540");
        fixedAsset.setStatus(AssetStatus.IN_USE);
        fixedAsset.setOriginalCost(new BigDecimal("25000000"));
        fixedAsset.setAcquisitionDate(LocalDate.of(2024, 1, 15));
        fixedAsset.setUsefulLifeYears(5);
        fixedAsset.setCreatedAt(LocalDateTime.now());
        fixedAsset.setUpdatedAt(LocalDateTime.now());

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
                .status(HandoverStatus.PENDING)
                .build();
    }

    @Nested
    @DisplayName("Create request")
    class CreateRequestTests {

        @Test
        @DisplayName("Should create a new handover request when no pending request exists")
        void createRequest_success() {
            CreateHandoverRequest dto = new CreateHandoverRequest();
            dto.setAssetId(assetId);
            dto.setFromUnitId(fromUnitId);
            dto.setToUnitId(toUnitId);
            dto.setReason("Chuyển giao nội bộ");
            dto.setHandoverDate(LocalDate.of(2024, 7, 1));
            dto.setAssetCondition("GOOD");
            dto.setNotes("Kiểm tra trước khi bàn giao");

            when(handoverRepository.existsByAssetIdAndStatus(assetId, HandoverStatus.PENDING)).thenReturn(false);
            when(fixedAssetRepository.findById(assetId)).thenReturn(Optional.of(fixedAsset));
            when(handoverRepository.save(any(HandoverRequest.class))).thenAnswer(invocation -> {
                HandoverRequest saved = invocation.getArgument(0);
                saved.setId(requestId);
                saved.setRequestCode("BG-2024-999");
                return saved;
            });

            HandoverDto result = handoverService.createRequest(dto, initiator);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(requestId);
            assertThat(result.getAssetCode()).isEqualTo("TS-2024-001");
            assertThat(result.getStatus()).isEqualTo(HandoverStatus.PENDING);
            assertThat(result.getInitiatedBy()).isEqualTo(initiator);

            verify(auditLogService).log(
                    eq("HANDOVER"),
                    eq("CREATE"),
                    eq(requestId.toString()),
                    eq("BG-2024-999"),
                    isNull(),
                    anyString(),
                    eq("Handover CREATE")
            );
        }

        @Test
        @DisplayName("Should reject duplicate pending handover requests")
        void createRequest_duplicatePending_throwsException() {
            CreateHandoverRequest dto = new CreateHandoverRequest();
            dto.setAssetId(assetId);
            dto.setFromUnitId(fromUnitId);
            dto.setToUnitId(toUnitId);

            when(handoverRepository.existsByAssetIdAndStatus(assetId, HandoverStatus.PENDING)).thenReturn(true);

            assertThatThrownBy(() -> handoverService.createRequest(dto, initiator))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("PENDING handover request already exists");

            verify(handoverRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Approve and reject request")
    class ApprovalTests {

        @Test
        @DisplayName("Should approve a pending handover request")
        void approveRequest_success() {
            when(handoverRepository.findById(requestId)).thenReturn(Optional.of(existingRequest));
            when(fixedAssetRepository.findById(assetId)).thenReturn(Optional.of(fixedAsset));
            when(handoverRepository.save(any(HandoverRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(fixedAssetService.updateAssetStatus(assetId, AssetStatus.TRANSFERRED, "Handover approved", approver))
                    .thenReturn(fixedAsset);

            HandoverDto result = handoverService.approveRequest(requestId, approver);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HandoverStatus.APPROVED);
            assertThat(result.getAssetCode()).isEqualTo("TS-2024-001");

            verify(fixedAssetService).updateAssetStatus(assetId, AssetStatus.TRANSFERRED, "Handover approved", approver);
            verify(auditLogService).log(eq("HANDOVER"), eq("APPROVE"), eq(requestId.toString()), eq("BG-2024-001"), anyString(), anyString(), eq("Handover APPROVE"));
        }

        @Test
        @DisplayName("Should not allow the initiator to approve their own request")
        void approveRequest_initiatorCannotApprove_throwsException() {
            when(handoverRepository.findById(requestId)).thenReturn(Optional.of(existingRequest));
            when(fixedAssetRepository.findById(assetId)).thenReturn(Optional.of(fixedAsset));

            assertThatThrownBy(() -> handoverService.approveRequest(requestId, initiator))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Initiator cannot approve");

            verify(fixedAssetService, never()).updateAssetStatus(any(), any(), any(), any());
            verify(auditLogService, never()).log(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should reject a pending handover request")
        void rejectRequest_success() {
            when(handoverRepository.findById(requestId)).thenReturn(Optional.of(existingRequest));
            when(fixedAssetRepository.findById(assetId)).thenReturn(Optional.of(fixedAsset));
            when(handoverRepository.save(any(HandoverRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

            HandoverDto result = handoverService.rejectRequest(requestId, approver, "Reason not valid");

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HandoverStatus.REJECTED);
            assertThat(result.getNotes()).isEqualTo("Reason not valid");

            verify(auditLogService).log(eq("HANDOVER"), eq("REJECT"), eq(requestId.toString()), eq("BG-2024-001"), anyString(), anyString(), eq("Handover REJECT"));
        }

        @Test
        @DisplayName("Should not allow the initiator to reject their own request")
        void rejectRequest_initiatorCannotReject_throwsException() {
            when(handoverRepository.findById(requestId)).thenReturn(Optional.of(existingRequest));
            when(fixedAssetRepository.findById(assetId)).thenReturn(Optional.of(fixedAsset));

            assertThatThrownBy(() -> handoverService.rejectRequest(requestId, initiator, "Bad reason"))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Initiator cannot reject");

            verify(handoverRepository, never()).save(any());
            verify(auditLogService, never()).log(any(), any(), any(), any(), any(), any(), any());
        }
    }

    @Test
    @DisplayName("Should return document data for a handover request")
    void getDocumentData_returnsDto() {
        when(handoverRepository.findById(requestId)).thenReturn(Optional.of(existingRequest));
        when(fixedAssetRepository.findById(assetId)).thenReturn(Optional.of(fixedAsset));

        HandoverDto result = handoverService.getDocumentData(requestId);

        assertThat(result).isNotNull();
        assertThat(result.getAssetCode()).isEqualTo("TS-2024-001");
        assertThat(result.getRequestCode()).isEqualTo("BG-2024-001");
    }

    @Test
    @DisplayName("Should throw if handover request is not found")
    void getDocumentData_notFound_throwsException() {
        when(handoverRepository.findById(requestId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handoverService.getDocumentData(requestId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Handover request not found");
    }
}
