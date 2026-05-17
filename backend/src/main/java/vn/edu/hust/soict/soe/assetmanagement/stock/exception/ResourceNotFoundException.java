package vn.edu.hust.soict.soe.assetmanagement.stock.exception;

public class ResourceNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public ResourceNotFoundException(String message) { super(message); }
}
