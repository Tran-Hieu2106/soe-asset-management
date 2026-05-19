package vn.edu.hust.soict.soe.assetmanagement.handover.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.soict.soe.assetmanagement.asset.entity.FixedAsset;
import vn.edu.hust.soict.soe.assetmanagement.asset.repository.FixedAssetRepository;
import vn.edu.hust.soict.soe.assetmanagement.exception.ResourceNotFoundException;
import vn.edu.hust.soict.soe.assetmanagement.handover.dto.HandoverDto;
import vn.edu.hust.soict.soe.assetmanagement.handover.entity.HandoverRequest;
import vn.edu.hust.soict.soe.assetmanagement.handover.repository.HandoverRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HandoverDocumentService {

    private final HandoverRepository handoverRepository;
    private final FixedAssetRepository fixedAssetRepository; // M2 Injection

    @Transactional(readOnly = true)
    public HandoverDto generateDocument(UUID handoverId) {
        HandoverRequest request = handoverRepository.findById(handoverId)
                .orElseThrow(() -> new ResourceNotFoundException("Handover request not found: " + handoverId));

        // Fetch M2 Asset Data
        FixedAsset asset = fixedAssetRepository.findById(request.getAssetId())
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + request.getAssetId()));
        String asset_code = asset.getAssetCode();
        String asset_name = asset.getName();

        // Return only the strict DTO fields. Frontend handles the layout.
        return HandoverDto.from(request, asset_code, asset_name);
    }
      
}