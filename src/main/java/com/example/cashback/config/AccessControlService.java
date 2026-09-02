package com.example.cashback.config;

import com.example.cashback.merchant.repository.MerchantRepository;
import com.example.cashback.user.model.Role;
import com.example.cashback.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("accessControl")
@RequiredArgsConstructor
public class AccessControlService {

    private final UserRepository userRepository;
    private final MerchantRepository merchantRepository;

    public boolean canAccessUser(UUID userId) {
        Authentication authentication = currentAuthentication();
        if (hasRole(authentication, Role.ADMIN) || hasRole(authentication, Role.COMPLIANCE)) {
            return true;
        }
        return authentication != null
                && userRepository.findByEmail(authentication.getName())
                        .map(user -> user.getId().equals(userId))
                        .orElse(false);
    }

    public boolean canAccessMerchant(UUID merchantId) {
        Authentication authentication = currentAuthentication();
        if (hasRole(authentication, Role.ADMIN) || hasRole(authentication, Role.COMPLIANCE)) {
            return true;
        }
        return authentication != null
                && merchantRepository.findById(merchantId)
                        .map(merchant -> merchant.getEmail().equalsIgnoreCase(authentication.getName()))
                        .orElse(false);
    }

    private Authentication currentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private boolean hasRole(Authentication authentication, Role role) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role.name()));
    }
}