package vn.edu.hust.soict.soe.assetmanagement.stock.exception;

import java.math.BigDecimal;

public class InsufficientStockException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final BigDecimal required;
    private final BigDecimal available;

    public InsufficientStockException(String message) {
        super(message);
        this.required  = null;
        this.available = null;
    }

    public InsufficientStockException(String message, BigDecimal required, BigDecimal available) {
        super(message);
        this.required  = required;
        this.available = available;
    }

    public BigDecimal getRequired()  { return required; }
    public BigDecimal getAvailable() { return available; }
}
