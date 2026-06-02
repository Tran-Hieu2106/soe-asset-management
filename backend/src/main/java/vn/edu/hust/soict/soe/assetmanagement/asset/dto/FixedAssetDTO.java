package vn.edu.hust.soict.soe.assetmanagement.asset.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import vn.edu.hust.soict.soe.assetmanagement.asset.enums.AssetStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * ==============================================================================
 * DTO: FixedAssetDTO
 * PURPOSE: Protects JPA entities from being exposed to the API. 
 * RULE CHECK: Enforces API Spec validation constraints (400 Bad Request if failed).
 * ==============================================================================
 */
@Getter
@Setter
@Builder
public class FixedAssetDTO {
    private UUID id;

    @NotBlank(message = "Mã tài sản không được để trống")
    @Size(max = 50, message = "Mã tài sản tối đa 50 ký tự")
    private String assetCode;

    @NotBlank(message = "Tên tài sản không được để trống")
    private String name;

    @NotNull(message = "Danh mục không được để trống")
    private Integer categoryId;

    @NotNull(message = "Đơn vị quản lý không được để trống")
    private UUID managingUnitId;

    private String serialNumber;
    private String manufacturer;
    private String model;
    private String countryOfOrigin;
    private String technicalSpecs;
    private String location;

    @NotNull(message = "Nguyên giá không được để trống")
    @DecimalMin(value = "0.0", inclusive = true, message = "Nguyên giá phải >= 0")
    private BigDecimal originalCost;

    @NotNull(message = "Ngày ghi tăng không được để trống")
    @PastOrPresent(message = "Ngày ghi tăng không thể ở tương lai")
    private LocalDate acquisitionDate;

    @NotNull(message = "Thời gian sử dụng không được để trống")
    @Min(value = 1, message = "Thời gian sử dụng phải lớn hơn 0")
    private Integer usefulLifeYears;

    private BigDecimal salvageValue;
    private String depreciationMethod;
    
    // Read-only fields populated by FA-02 calculation
    private BigDecimal accumulatedDepreciation;
    private BigDecimal netBookValue;
    
    private AssetStatus status;
    private String notes;

    // Read-only enriched fields
    private String categoryCode;
    private String categoryName;
    private String managingUnitCode;
    private String managingUnitName;
    private BigDecimal annualDepreciationAmount;
    private BigDecimal annualDepreciationRate;
    private LocalDate depreciationStartDate;
    private LocalDate depreciationEndDate;
}