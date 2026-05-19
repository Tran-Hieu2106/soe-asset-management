package vn.edu.hust.soict.soe.assetmanagement.stock.exception;

public class DuplicateMaterialCodeException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public DuplicateMaterialCodeException(String message) { super(message); }
}
