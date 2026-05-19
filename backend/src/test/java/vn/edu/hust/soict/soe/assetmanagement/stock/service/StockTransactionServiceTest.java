package vn.edu.hust.soict.soe.assetmanagement.stock.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.hust.soict.soe.assetmanagement.stock.dto.IssueRequest;
import vn.edu.hust.soict.soe.assetmanagement.stock.dto.ReceiptRequest;
import vn.edu.hust.soict.soe.assetmanagement.stock.dto.StockTransactionDto;
import vn.edu.hust.soict.soe.assetmanagement.stock.entity.Material;
import vn.edu.hust.soict.soe.assetmanagement.stock.entity.StockTransaction;
import vn.edu.hust.soict.soe.assetmanagement.stock.entity.StorageLocation;
import vn.edu.hust.soict.soe.assetmanagement.stock.entity.TransactionType;
import vn.edu.hust.soict.soe.assetmanagement.stock.exception.InsufficientStockException;
import vn.edu.hust.soict.soe.assetmanagement.stock.repository.MaterialRepository;
import vn.edu.hust.soict.soe.assetmanagement.stock.repository.StockTransactionRepository;
import vn.edu.hust.soict.soe.assetmanagement.stock.repository.StorageLocationRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Stock Transaction Business Logic Tests")
class StockTransactionServiceTest {

    @Mock private StockTransactionRepository transactionRepository;
    @Mock private MaterialRepository materialRepository;
    @Mock private StorageLocationRepository locationRepository;

    @InjectMocks private StockTransactionService transactionService;

    private Material testMaterial;
    private StorageLocation testLocation;
    private UUID materialId;
    private UUID locationId;

    @BeforeEach
    void setUp() {
        materialId = UUID.randomUUID();
        locationId = UUID.randomUUID();

        testMaterial = Material.builder()
                .id(materialId)
                .materialCode("MAT-01")
                .name("RAM 16GB DDR4")
                .unitOfMeasure("Chiếc")
                .unitPrice(new BigDecimal("1500000.00")) // Standard catalog price
                .build();

        testLocation = StorageLocation.builder()
                .id(locationId)
                .code("KHO-IT")
                .name("Kho IT Chính")
                .build();
    }

    @Nested
    @DisplayName("Financial Integrity (CS-04 & CS-02)")
    class FinancialIntegrityTests {

        @Test
        @DisplayName("FLAW CHECK: Issue without unitPrice must fallback to Material's standard price to prevent NULL totalValue")
        void createIssue_nullUnitPrice_usesMaterialStandardPrice() {
            IssueRequest req = new IssueRequest();
            req.setMaterialId(materialId);
            req.setStorageLocationId(locationId);
            req.setQuantity(new BigDecimal("2.000"));
            req.setUnitPrice(null); // User leaves price blank on issue
            req.setRequestingDepartmentId(UUID.randomUUID());
            req.setDocumentRef("PXK-001");
            req.setDocumentDate(LocalDate.now());

            when(materialRepository.findById(materialId)).thenReturn(Optional.of(testMaterial));
            when(locationRepository.findById(locationId)).thenReturn(Optional.of(testLocation));
            when(transactionRepository.checkAvailableStock(materialId, locationId)).thenReturn(new BigDecimal("10.000"));
            when(transactionRepository.save(any())).thenAnswer(i -> {
                StockTransaction tx = i.getArgument(0);
                tx.prePersist(); // Trigger the JPA callback manually for testing
                return tx;
            });

            StockTransactionDto result = transactionService.createIssue(req, "tester");

            // Expecting 2.000 * 1,500,000 (from catalog) = 3,000,000
            assertThat(result.getUnitPrice()).isEqualByComparingTo(new BigDecimal("1500000.00"));
            assertThat(result.getTotalValue()).isEqualByComparingTo(new BigDecimal("3000000.00"));
        }
    }

    @Nested
    @DisplayName("Stock Boundary Limits (CS-03)")
    class StockBoundaryTests {

        @Test
        @DisplayName("SYSTEM FLUIDITY: Should successfully issue exact amount available (Boundary condition)")
        void createIssue_exactAvailableStock_success() {
            IssueRequest req = new IssueRequest();
            req.setMaterialId(materialId);
            req.setStorageLocationId(locationId);
            req.setQuantity(new BigDecimal("5.000"));
            req.setDocumentDate(LocalDate.now());
            req.setDocumentRef("PX-EXACT");

            when(materialRepository.findById(materialId)).thenReturn(Optional.of(testMaterial));
            when(locationRepository.findById(locationId)).thenReturn(Optional.of(testLocation));
            // Exactly 5 available
            when(transactionRepository.checkAvailableStock(materialId, locationId)).thenReturn(new BigDecimal("5.000"));
            when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            StockTransactionDto result = transactionService.createIssue(req, "tester");

            assertThat(result.getTransactionType()).isEqualTo(TransactionType.ISSUE);
            assertThat(result.getQuantity()).isEqualByComparingTo("5.000");
        }

        @Test
        @DisplayName("BUSINESS RULE: Prevent negative stock issuance")
        void createIssue_exceedsAvailableStock_throwsException() {
            IssueRequest req = new IssueRequest();
            req.setMaterialId(materialId);
            req.setStorageLocationId(locationId);
            req.setQuantity(new BigDecimal("6.000")); // Requesting 6

            when(materialRepository.findById(materialId)).thenReturn(Optional.of(testMaterial));
            when(locationRepository.findById(locationId)).thenReturn(Optional.of(testLocation));
            // Only 5 available
            when(transactionRepository.checkAvailableStock(materialId, locationId)).thenReturn(new BigDecimal("5.000"));

            assertThatThrownBy(() -> transactionService.createIssue(req, "tester"))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining("Tồn kho không đủ");

            verify(transactionRepository, never()).save(any());
        }
    }
}