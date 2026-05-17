package vn.edu.hust.soict.soe.assetmanagement.stock.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.soict.soe.assetmanagement.stock.dto.DepartmentUsageDto;
import vn.edu.hust.soict.soe.assetmanagement.stock.dto.StockBalanceDto;
import vn.edu.hust.soict.soe.assetmanagement.stock.repository.StockTransactionRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * CS-03: Real-time stock balance — computed on-the-fly, no stored column.
 * CS-04: Department-wise usage summary.
 *
 * Per V3 schema comment:
 *   current_balance = SUM(RECEIPT qty) - SUM(ISSUE qty)
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockBalanceService {

    private final StockTransactionRepository transactionRepository;

    /** GET /api/stock/balance — all materials */
    public List<StockBalanceDto> getAllBalances() {
        return transactionRepository.getAllBalances();
    }

    /** GET /api/stock/balance/{materialId} — one material */
    public List<StockBalanceDto> getBalanceByMaterial(UUID materialId) {
        return transactionRepository.getBalanceByMaterial(materialId);
    }

    /** GET /api/stock/usage?startDate=&endDate= */
    public List<DepartmentUsageDto> getDepartmentUsage(LocalDate startDate, LocalDate endDate) {
        return transactionRepository.getDepartmentUsage(startDate, endDate);
    }
}
