package vn.edu.hust.soict.soe.assetmanagement.stock.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.hust.soict.soe.assetmanagement.stock.dto.CreateMaterialRequest;
import vn.edu.hust.soict.soe.assetmanagement.stock.dto.MaterialDto;
import vn.edu.hust.soict.soe.assetmanagement.stock.dto.UpdateMaterialRequest;
import vn.edu.hust.soict.soe.assetmanagement.stock.service.MaterialService;

import java.util.List;
import java.util.UUID;

/**
 * CS-01: Material catalogue REST API
 *
 * GET  /api/materials          — list (paginated, filterable by category)
 * GET  /api/materials/search   — search by name
 * GET  /api/materials/{id}     — get one
 * POST /api/materials          — add new
 * PUT  /api/materials/{id}     — update
 */
@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    @GetMapping
    public ResponseEntity<Page<MaterialDto>> getAll(
            @RequestParam(required = false) Integer categoryId,
            Pageable pageable) {

        return ResponseEntity.ok(
                categoryId != null
                        ? materialService.getByCategory(categoryId, pageable)
                        : materialService.getAll(pageable)
        );
    }

    @GetMapping("/search")
    public ResponseEntity<List<MaterialDto>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(materialService.search(keyword));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaterialDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(materialService.getById(id));
    }

    @PostMapping
    public ResponseEntity<MaterialDto> create(@Valid @RequestBody CreateMaterialRequest req) {
        // TODO: replace "system" with username from JWT SecurityContext
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(materialService.create(req, "system"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MaterialDto> update(
            @PathVariable UUID id,
            @RequestBody UpdateMaterialRequest req) {

        return ResponseEntity.ok(materialService.update(id, req));
    }
}
