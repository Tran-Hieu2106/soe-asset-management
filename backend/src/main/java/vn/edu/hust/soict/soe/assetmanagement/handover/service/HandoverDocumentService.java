package vn.edu.hust.soict.soe.assetmanagement.handover.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.edu.hust.soict.soe.assetmanagement.handover.entity.HandoverRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ==============================================================================
 * SERVICE: HandoverDocumentService
 * ==============================================================================
 * PURPOSE:
 *   Implements HL-03: Generates the formal "Biên bản bàn giao tài sản" (Asset
 *   Handover Record) document that is required by Vietnamese state-owned enterprise
 *   regulations whenever a fixed asset changes managing units.
 *
 * WHAT IS A "BIÊN BẢN BÀN GIAO":
 *   A mandatory state administrative document under Vietnamese law (Circular
 *   45/2013/TT-BTC and related regulations) that formally records the transfer
 *   of state-owned assets between units. It must capture:
 *     - Asset identification (code, name, specs)
 *     - Transferring party (from unit, contact person)
 *     - Receiving party (to unit, contact person)
 *     - Date of handover and physical condition
 *     - Signatures from both parties
 *
 * CURRENT IMPLEMENTATION (Phase 1 — Milestone 1 scope):
 *   This service currently generates a structured TEXT record and returns a
 *   document reference number. The reference number is stored in the
 *   `handover_requests.document_ref` column.
 *
 *   In a production system, this service would generate:
 *     a) A PDF file using a template engine (e.g. iText, Apache PDFBox, or
 *        Thymeleaf → HTML → PDF via Flying Saucer/WeasyPrint)
 *     b) Store the PDF in an object store (MinIO, S3) or filesystem
 *     c) Return the storage path/URL as the document reference
 *
 * INTEGRATION:
 *   Called by HandoverService.completeHandover() after transitioning to CONFIRMED.
 *   The returned documentRef string is stored back on the HandoverRequest entity.
 *
 * EXTENSION POINTS:
 *   To add real PDF generation:
 *   1. Add iText7 or Apache PDFBox to pom.xml
 *   2. Create a Thymeleaf HTML template at resources/templates/handover-document.html
 *   3. Replace the generateDocument() body with template rendering + PDF export
 *   4. Configure a storage location (local disk path or S3 bucket) in application.yml
 *
 * DOCUMENT REFERENCE FORMAT:
 *   BBGTS-YYYY-NNNN where:
 *     BB  = Biên Bản
 *     GTS = Giao Tài Sản
 *     YYYY = year
 *     NNNN = sequential number derived from the request code
 * ==============================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HandoverDocumentService {

    /**
     * Generates the formal handover document for a completed handover request.
     *
     * This method is called by HandoverService.completeHandover() as the final
     * step before marking the workflow as COMPLETED.
     *
     * CURRENT BEHAVIOR (stub for Phase 1):
     *   - Builds a document reference number based on the request code and timestamp.
     *   - Logs the document metadata to the application log.
     *   - Returns the reference number (no actual file is written to disk yet).
     *
     * PRODUCTION BEHAVIOR (to be implemented in a later phase):
     *   - Renders a Thymeleaf template with request data
     *   - Converts to PDF
     *   - Saves to configured storage path
     *   - Returns a URI or file path to the saved document
     *
     * @param handoverRequest The completed HandoverRequest entity.
     *                        Must be in CONFIRMED status when this is called.
     * @return A document reference string (e.g. "BBGTS-2025-0042") that will be
     *         stored in handover_requests.document_ref.
     */
    public String generateDocument(HandoverRequest handoverRequest) {
        // ── Build the document reference number ───────────────────────────
        // Convert the BG-YYYY-NNNN format of requestCode into BBGTS-YYYY-NNNN
        // E.g. "BG-2025-0042" → "BBGTS-2025-0042"
        String documentRef = handoverRequest.getRequestCode()
                .replace("BG-", "BBGTS-");

        // ── Log document generation (simulates the real document creation) ─
        log.info(
            "=== GENERATING HANDOVER DOCUMENT ===\n" +
            "Document Reference : {}\n" +
            "Request Code       : {}\n" +
            "Asset ID           : {}\n" +
            "From Unit          : {}\n" +
            "To Unit            : {}\n" +
            "Initiated By       : {}\n" +
            "Approved By        : {}\n" +
            "Confirmed By       : {}\n" +
            "Asset Condition    : {}\n" +
            "Handover Date      : {}\n" +
            "Generated At       : {}\n" +
            "=====================================",
            documentRef,
            handoverRequest.getRequestCode(),
            handoverRequest.getAssetId(),
            handoverRequest.getFromUnitId(),
            handoverRequest.getToUnitId(),
            handoverRequest.getInitiatedBy(),
            handoverRequest.getDeptApprovedBy(),
            handoverRequest.getConfirmedBy(),
            handoverRequest.getAssetCondition(),
            handoverRequest.getHandoverDate(),
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
        );

        // ── TODO: Replace with real PDF generation in production ──────────
        // Example (iText7):
        //   PdfDocument pdfDoc = new PdfDocument(new PdfWriter("/storage/handovers/" + documentRef + ".pdf"));
        //   Document doc = new Document(pdfDoc, PageSize.A4);
        //   doc.add(new Paragraph("BIÊN BẢN BÀN GIAO TÀI SẢN").setFontSize(16).setBold());
        //   // ... populate all fields from handoverRequest ...
        //   doc.close();
        //   return "/storage/handovers/" + documentRef + ".pdf";

        return documentRef;
    }

    /**
     * Marks a handover document as "signed" by updating the entity's documentSigned flag.
     * In a real implementation, this might verify a digital signature certificate,
     * update a document management system, or apply a signature watermark to the PDF.
     *
     * NOTE: This method only returns a flag value. The caller (HandoverController or
     * HandoverService) is responsible for persisting the updated entity.
     *
     * @param handoverRequest The request whose document is being signed.
     * @return Always true (stub — real implementation would verify the signature).
     */
    public boolean markDocumentSigned(HandoverRequest handoverRequest) {
        log.info("Handover document {} marked as signed for request {}",
                handoverRequest.getDocumentRef(),
                handoverRequest.getRequestCode());
        // In production: verify digital signature, stamp PDF, update DMS
        return true;
    }
}