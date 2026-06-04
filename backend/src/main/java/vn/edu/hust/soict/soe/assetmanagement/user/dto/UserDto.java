package vn.edu.hust.soict.soe.assetmanagement.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * User DTO — safe to return from API (no password hash).
 */
@Getter
@Builder
public class UserDto {

    private UUID id;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private boolean isActive;
    private Set<String> roles;
    private Set<String> managingUnitCodes;
}