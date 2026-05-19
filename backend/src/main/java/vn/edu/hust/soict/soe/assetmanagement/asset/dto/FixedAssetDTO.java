package vn.edu.hust.soict.soe.assetmanagement.asset.dto;

import vn.edu.hust.soict.soe.assetmanagement.asset.enums.AssetStatus;
import jakarta.validation.constraints.*; 
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Data Transfer Object for Fixed Assets.
 * Integrated validation for frontend hand-off (FA-01).
 */
public class FixedAssetDTO {

    private UUID id;

    @NotBlank(message = "Asset code cannot be blank")
    private String assetCode;

    @NotBlank(message = "Asset name cannot be blank")
    private String name;

    @NotNull(message = "Asset category is required")
    private Integer categoryId;

    @NotNull(message = "Managing unit is required")
    private UUID managingUnitId;

    private String serialNumber;
    private String manufacturer;
    private String model;
    private String countryOfOrigin;
    private String technicalSpecs;
    private String location;

    @NotNull(message = "Original cost cannot be null")
    @DecimalMin(value = "0.0", inclusive = true, message = "Original cost must be greater than or equal to 0")
    private BigDecimal originalCost;

    @NotNull(message = "Acquisition date is required")
    @PastOrPresent(message = "Acquisition date cannot be in the future")
    private LocalDate acquisitionDate;

    private String fundingSource;

    @NotNull(message = "Useful life years is required")
    @Min(value = 1, message = "Useful life must be at least 1 year")
    private Integer usefulLifeYears;

    private BigDecimal salvageValue;

    @NotBlank(message = "Depreciation method is required")
    private String depreciationMethod; // "STRAIGHT_LINE" or "DECLINING_BALANCE"

    // System-calculated fields (Read-only for frontend)
    private BigDecimal accumulatedDepreciation;
    private BigDecimal netBookValue;
    private AssetStatus status;

    // --- GETTERS AND SETTERS ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getAssetCode() { return assetCode; }
    public void setAssetCode(String assetCode) { this.assetCode = assetCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

    public UUID getManagingUnitId() { return managingUnitId; }
    public void setManagingUnitId(UUID managingUnitId) { this.managingUnitId = managingUnitId; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getCountryOfOrigin() { return countryOfOrigin; }
    public void setCountryOfOrigin(String countryOfOrigin) { this.countryOfOrigin = countryOfOrigin; }

    public String getTechnicalSpecs() { return technicalSpecs; }
    public void setTechnicalSpecs(String technicalSpecs) { this.technicalSpecs = technicalSpecs; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public BigDecimal getOriginalCost() { return originalCost; }
    public void setOriginalCost(BigDecimal originalCost) { this.originalCost = originalCost; }

    public LocalDate getAcquisitionDate() { return acquisitionDate; }
    public void setAcquisitionDate(LocalDate acquisitionDate) { this.acquisitionDate = acquisitionDate; }

    public String getFundingSource() { return fundingSource; }
    public void setFundingSource(String fundingSource) { this.fundingSource = fundingSource; }

    public Integer getUsefulLifeYears() { return usefulLifeYears; }
    public void setUsefulLifeYears(Integer usefulLifeYears) { this.usefulLifeYears = usefulLifeYears; }

    public BigDecimal getSalvageValue() { return salvageValue; }
    public void setSalvageValue(BigDecimal salvageValue) { this.salvageValue = salvageValue; }

    public String getDepreciationMethod() { return depreciationMethod; }
    public void setDepreciationMethod(String depreciationMethod) { this.depreciationMethod = depreciationMethod; }

    public BigDecimal getAccumulatedDepreciation() { return accumulatedDepreciation; }
    public void setAccumulatedDepreciation(BigDecimal accumulatedDepreciation) { this.accumulatedDepreciation = accumulatedDepreciation; }

    public BigDecimal getNetBookValue() { return netBookValue; }
    public void setNetBookValue(BigDecimal netBookValue) { this.netBookValue = netBookValue; }

    public AssetStatus getStatus() { return status; }
    public void setStatus(AssetStatus status) { this.status = status; }
}