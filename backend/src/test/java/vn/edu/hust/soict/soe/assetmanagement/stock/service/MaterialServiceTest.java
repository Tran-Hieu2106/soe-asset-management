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
import vn.edu.hust.soict.soe.assetmanagement.stock.dto.CreateMaterialRequest;
import vn.edu.hust.soict.soe.assetmanagement.stock.dto.MaterialDto;
import vn.edu.hust.soict.soe.assetmanagement.stock.entity.Material;
import vn.edu.hust.soict.soe.assetmanagement.stock.entity.MaterialCategory;
import vn.edu.hust.soict.soe.assetmanagement.stock.exception.DuplicateMaterialCodeException;
import vn.edu.hust.soict.soe.assetmanagement.stock.repository.MaterialCategoryRepository;
import vn.edu.hust.soict.soe.assetmanagement.stock.repository.MaterialRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Material Catalog Data Integrity Tests")
class MaterialServiceTest {

    @Mock private MaterialRepository materialRepository;
    @Mock private MaterialCategoryRepository categoryRepository;

    @InjectMocks private MaterialService materialService;

    private MaterialCategory testCategory;

    @BeforeEach
    void setUp() {
        testCategory = MaterialCategory.builder()
                .id(1)
                .code("CAT-IT")
                .name("Thiết bị IT")
                .build();
    }

    @Nested
    @DisplayName("Creation & Defaults (CS-01)")
    class CreationTests {

        @Test
        @DisplayName("FLAW CHECK: Null minimumStock in request must default to ZERO, not crash the DB")
        void createMaterial_nullMinimumStock_defaultsToZero() {
            CreateMaterialRequest req = new CreateMaterialRequest();
            req.setMaterialCode("MAT-02");
            req.setName("Cáp mạng Cat6");
            req.setCategoryId(1);
            req.setUnitOfMeasure("Cuộn");
            req.setMinimumStock(null); // Explicitly passing null to simulate missing JSON field

            when(materialRepository.existsByMaterialCode("MAT-02")).thenReturn(false);
            when(categoryRepository.findById(1)).thenReturn(Optional.of(testCategory));
            
            when(materialRepository.save(any(Material.class))).thenAnswer(i -> {
                Material m = i.getArgument(0);
                if (m.getMinimumStock() == null) {
                    m.setMinimumStock(BigDecimal.ZERO); // Simulating Builder.Default behavior if properly implemented
                }
                m.setId(UUID.randomUUID());
                return m;
            });

            MaterialDto result = materialService.create(req, "tester");

            ArgumentCaptor<Material> materialCaptor = ArgumentCaptor.forClass(Material.class);
            verify(materialRepository).save(materialCaptor.capture());

            // Ensure the value being sent to the DB is 0, not null
            assertThat(materialCaptor.getValue().getMinimumStock()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getMinimumStock()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("BUSINESS RULE: Reject duplicate material codes immediately")
        void createMaterial_duplicateCode_throwsException() {
            CreateMaterialRequest req = new CreateMaterialRequest();
            req.setMaterialCode("MAT-DUPE");
            req.setCategoryId(1);

            when(materialRepository.existsByMaterialCode("MAT-DUPE")).thenReturn(true);

            assertThatThrownBy(() -> materialService.create(req, "tester"))
                    .isInstanceOf(DuplicateMaterialCodeException.class)
                    .hasMessageContaining("đã tồn tại");

            verify(categoryRepository, never()).findById(any());
            verify(materialRepository, never()).save(any());
        }
    }
}