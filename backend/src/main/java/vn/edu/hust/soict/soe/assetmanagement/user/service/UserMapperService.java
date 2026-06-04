package vn.edu.hust.soict.soe.assetmanagement.user.service;

import org.springframework.stereotype.Service;
import vn.edu.hust.soict.soe.assetmanagement.user.dto.UserDto;
import vn.edu.hust.soict.soe.assetmanagement.user.entity.User;

import java.util.stream.Collectors;

@Service
public class UserMapperService {

    /** 
     * This service centralizes all mapping logic between User entities and UserDto.
     * It is injected into other services to avoid code duplication and ensure consistent mapping.
     */
    public UserDto toDto(User user) {
        if (user == null) {
            return null;
        }

        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .isActive(user.isActive())
                .roles(user.getRoles().stream()
                        .map(r -> r.getCode())
                        .collect(Collectors.toSet()))
                .managingUnitCodes(user.getManagingUnits().stream()
                        .map(u -> u.getCode())
                        .collect(Collectors.toSet()))
                .build();
    }
}