package vn.edu.hust.soict.soe.assetmanagement.auth.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Response body for POST /api/auth/login
 * Returns the JWT token and basic user info.
 * Frontend stores the token in authStore.ts (Zustand).
 */
@Getter
@Builder
@NoArgsConstructor 
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String username;
}