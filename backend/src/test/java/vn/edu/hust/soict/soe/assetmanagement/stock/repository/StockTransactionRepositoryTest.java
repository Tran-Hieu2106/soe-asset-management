package vn.edu.hust.soict.soe.assetmanagement.stock.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import vn.edu.hust.soict.soe.assetmanagement.stock.dto.StockBalanceDto;
import vn.edu.hust.soict.soe.assetmanagement.stock.entity.*;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
    "spring.flyway.enabled=false",
    "spring.liquibase.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@DisplayName("Database Queries Integrity Tests (CS-03, CS-04)")
class StockTransactionRepositoryTest {

    @Autowired private StockTransactionRepository transactionRepository;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private StorageLocationRepository locationRepository;
    @Autowired private MaterialCategoryRepository categoryRepository;

    private Material testMaterial;
    private StorageLocation testLocation;

    @BeforeEach
    void setUp() {
        // 1. Tạo dữ liệu gốc (Master Data) thật trong DB ảo
        MaterialCategory category = categoryRepository.save(MaterialCategory.builder()
                .code("CAT-01").name("Thiết bị").build());

        testMaterial = materialRepository.save(Material.builder()
                .materialCode("MAT-100")
                .name("Ổ cứng SSD 1TB")
                .unitOfMeasure("Cái")
                .minimumStock(new BigDecimal("5.000"))
                .category(category)
                .createdBy("tester")
                .build());

        testLocation = locationRepository.save(StorageLocation.builder()
                .code("KHO-A").name("Kho A").unitId(UUID.randomUUID()).build());
    }

    @Test
    @DisplayName("SYSTEM FLAW CHECK: On-the-fly Balance calculation (RECEIPT - ISSUE)")
    void getBalanceByMaterial_calculatesCorrectly() {
        // Kịch bản: 
        // 1. Nhập kho lần 1: +10 cái
        // 2. Xuất kho lần 1: -3 cái
        // 3. Nhập kho lần 2: +5 cái
        // 4. Xuất kho lần 2: -4 cái
        // => Kỳ vọng: Tồn kho thực tế = 10 - 3 + 5 - 4 = 8 cái.

        saveTx(TransactionType.RECEIPT, "10.000", "1500000");
        saveTx(TransactionType.ISSUE, "3.000", "1500000");
        saveTx(TransactionType.RECEIPT, "5.000", "1500000");
        saveTx(TransactionType.ISSUE, "4.000", "1500000");

        // Chạy thẳng câu lệnh JPQL trong Repository
        List<StockBalanceDto> balances = transactionRepository.getBalanceByMaterial(testMaterial.getId());

        assertThat(balances).hasSize(1);
        StockBalanceDto balance = balances.get(0);

        // Kiểm tra tính chính xác của thuật toán SUM
        assertThat(balance.getCurrentBalance()).isEqualByComparingTo("8.000");
        
        // Cảnh báo tồn kho tối thiểu (Tồn 8 > Min 5 => isBelowMinimum = false)
        assertThat(balance.getIsBelowMinimum()).isFalse();
    }

    @Test
    @DisplayName("BOUNDARY CHECK: Balance drops below minimum stock threshold")
    void getBalanceByMaterial_belowMinimum_flagsTrue() {
        // Kịch bản: Nhập 10, Xuất 7. Tồn kho còn 3. (Min = 5)
        saveTx(TransactionType.RECEIPT, "10.000", "1500000");
        saveTx(TransactionType.ISSUE, "7.000", "1500000");

        List<StockBalanceDto> balances = transactionRepository.getBalanceByMaterial(testMaterial.getId());
        
        assertThat(balances.get(0).getCurrentBalance()).isEqualByComparingTo("3.000");
        // Kiểm tra xem hệ thống có tự động phất cờ cảnh báo (true) không
        assertThat(balances.get(0).getIsBelowMinimum()).isTrue();
    }

    @Test
    @DisplayName("SYSTEM FLAW CHECK: Department Usage calculates sum of ISSUES only")
    void getDepartmentUsage_ignoresReceipts_calculatesOnlyIssues() {
        UUID departmentA = UUID.randomUUID();
        
        // Nhập kho 10 (Không được tính vào Usage)
        saveTx(TransactionType.RECEIPT, "10.000", "1500000", null);
        
        // Phòng ban A rút kho 2 cái (2 * 1,500,000 = 3,000,000)
        saveTx(TransactionType.ISSUE, "2.000", "1500000", departmentA);
        
        // Phòng ban A rút thêm 1 cái (1 * 1,500,000 = 1,500,000)
        saveTx(TransactionType.ISSUE, "1.000", "1500000", departmentA);

        var usageList = transactionRepository.getDepartmentUsage(null, null);

        assertThat(usageList).hasSize(1);
        var usage = usageList.get(0);

        // Tổng số lượng rút = 3
        assertThat(usage.getTotalIssued()).isEqualByComparingTo("3.000");
        
        // Tổng giá trị rút = 4,500,000 (đảm bảo logic nhân tiền totalValue chạy đúng)
        assertThat(usage.getTotalValue()).isEqualByComparingTo("4500000.00");
        assertThat(usage.getDepartmentId()).isEqualTo(departmentA);
    }

    // Helper method để tiết kiệm code
    private void saveTx(TransactionType type, String qty, String price) {
        saveTx(type, qty, price, null);
    }

    private void saveTx(TransactionType type, String qty, String price, UUID deptId) {
        StockTransaction tx = StockTransaction.builder()
                .material(testMaterial)
                .storageLocation(testLocation)
                .transactionType(type)
                .quantity(new BigDecimal(qty))
                .unitOfMeasure(testMaterial.getUnitOfMeasure())
                .unitPrice(new BigDecimal(price))
                .requestingDepartmentId(deptId)
                .documentRef("DOC-" + System.currentTimeMillis())
                .documentDate(LocalDate.now())
                .createdBy("tester")
                .build();
        tx.prePersist(); // trigger tính totalValue
        transactionRepository.save(tx);
    }
}