package vn.edu.hust.soict.soe.assetmanagement.asset.entity;

import vn.edu.hust.soict.soe.assetmanagement.asset.enums.AssetStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "assets")
public class FixedAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // --- Identity ---
    @Column(name = "asset_code", nullable = false, unique = true, length = 50)
    private String assetCode;

    @Column(nullable = false)
    private String name;

    @Column(name = "category_id", nullable = false)
    private Integer categoryId;

    @Column(name = "managing_unit_id", nullable = false)
    private UUID managingUnitId;

    // --- Technical parameters (FA-01) ---
    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    private String manufacturer;
    
    private String model;

    @Column(name = "country_of_origin", length = 100)
    private String countryOfOrigin;

    @Column(name = "technical_specs", columnDefinition = "TEXT")
    private String technicalSpecs;

    private String location;

    // --- Financial (FA-01, FA-02) ---
    @Column(name = "original_cost", nullable = false, precision = 18, scale = 2)
    private BigDecimal originalCost;

    @Column(name = "acquisition_date", nullable = false)
    private LocalDate acquisitionDate;

    @Column(name = "funding_source", length = 100)
    private String fundingSource;

    @Column(name = "useful_life_years", nullable = false)
    private Integer usefulLifeYears;

    @Column(name = "salvage_value", nullable = false, precision = 18, scale = 2)
    private BigDecimal salvageValue = BigDecimal.ZERO;

    @Column(name = "depreciation_method", nullable = false, length = 20)
    private String depreciationMethod = "STRAIGHT_LINE";

    // --- Depreciation computed fields (FA-02) ---
    @Column(name = "accumulated_depreciation", nullable = false, precision = 18, scale = 2)
    private BigDecimal accumulatedDepreciation = BigDecimal.ZERO;

    @Column(name = "net_book_value", nullable = false, precision = 18, scale = 2)
    private BigDecimal netBookValue = BigDecimal.ZERO;

    @Column(name = "depreciation_start_date")
    private LocalDate depreciationStartDate;

    @Column(name = "depreciation_end_date")
    private LocalDate depreciationEndDate;

    @Column(name = "annual_depreciation_rate", precision = 8, scale = 4)
    private BigDecimal annualDepreciationRate;

    // --- Operational status (FA-03) ---
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AssetStatus status;

    @Column(name = "status_reason", columnDefinition = "TEXT")
    private String statusReason;

    @Column(name = "status_changed_at")
    private LocalDateTime statusChangedAt;

    @Column(name = "status_changed_by", length = 100)
    private String statusChangedBy;

    // --- Supporting documents & Audit ---
    @Column(name = "purchase_document_ref")
    private String purchaseDocumentRef;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getAssetCode() {
        return assetCode;
    }

    public void setAssetCode(String assetCode) {
        this.assetCode = assetCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public UUID getManagingUnitId() {
        return managingUnitId;
    }

    public void setManagingUnitId(UUID managingUnitId) {
        this.managingUnitId = managingUnitId;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getCountryOfOrigin() {
        return countryOfOrigin;
    }

    public void setCountryOfOrigin(String countryOfOrigin) {
        this.countryOfOrigin = countryOfOrigin;
    }

    public String getTechnicalSpecs() {
        return technicalSpecs;
    }

    public void setTechnicalSpecs(String technicalSpecs) {
        this.technicalSpecs = technicalSpecs;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public BigDecimal getOriginalCost() {
        return originalCost;
    }

    public void setOriginalCost(BigDecimal originalCost) {
        this.originalCost = originalCost;
    }

    public LocalDate getAcquisitionDate() {
        return acquisitionDate;
    }

    public void setAcquisitionDate(LocalDate acquisitionDate) {
        this.acquisitionDate = acquisitionDate;
    }

    public String getFundingSource() {
        return fundingSource;
    }

    public void setFundingSource(String fundingSource) {
        this.fundingSource = fundingSource;
    }

    public Integer getUsefulLifeYears() {
        return usefulLifeYears;
    }

    public void setUsefulLifeYears(Integer usefulLifeYears) {
        this.usefulLifeYears = usefulLifeYears;
    }

    public BigDecimal getSalvageValue() {
        return salvageValue;
    }

    public void setSalvageValue(BigDecimal salvageValue) {
        this.salvageValue = salvageValue;
    }

    public String getDepreciationMethod() {
        return depreciationMethod;
    }

    public void setDepreciationMethod(String depreciationMethod) {
        this.depreciationMethod = depreciationMethod;
    }

    public BigDecimal getAccumulatedDepreciation() {
        return accumulatedDepreciation;
    }

    public void setAccumulatedDepreciation(BigDecimal accumulatedDepreciation) {
        this.accumulatedDepreciation = accumulatedDepreciation;
    }

    public BigDecimal getNetBookValue() {
        return netBookValue;
    }

    public void setNetBookValue(BigDecimal netBookValue) {
        this.netBookValue = netBookValue;
    }

    public LocalDate getDepreciationStartDate() {
        return depreciationStartDate;
    }

    public void setDepreciationStartDate(LocalDate depreciationStartDate) {
        this.depreciationStartDate = depreciationStartDate;
    }

    public LocalDate getDepreciationEndDate() {
        return depreciationEndDate;
    }

    public void setDepreciationEndDate(LocalDate depreciationEndDate) {
        this.depreciationEndDate = depreciationEndDate;
    }

    public BigDecimal getAnnualDepreciationRate() {
        return annualDepreciationRate;
    }

    public void setAnnualDepreciationRate(BigDecimal annualDepreciationRate) {
        this.annualDepreciationRate = annualDepreciationRate;
    }

    public AssetStatus getStatus() {
        return status;
    }

    public void setStatus(AssetStatus status) {
        this.status = status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public LocalDateTime getStatusChangedAt() {
        return statusChangedAt;
    }

    public void setStatusChangedAt(LocalDateTime statusChangedAt) {
        this.statusChangedAt = statusChangedAt;
    }

    public String getStatusChangedBy() {
        return statusChangedBy;
    }

    public void setStatusChangedBy(String statusChangedBy) {
        this.statusChangedBy = statusChangedBy;
    }

    public String getPurchaseDocumentRef() {
        return purchaseDocumentRef;
    }

    public void setPurchaseDocumentRef(String purchaseDocumentRef) {
        this.purchaseDocumentRef = purchaseDocumentRef;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

}