package com.fidelity.moneytransfer.controller;

import com.fidelity.moneytransfer.dto.DeactivateUserRequest;
import com.fidelity.moneytransfer.dto.LinkBankRequest;
import com.fidelity.moneytransfer.dto.LinkBankResponse;
import com.fidelity.moneytransfer.dto.UserResponseDto;
import com.fidelity.moneytransfer.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock private UserService userService;
    @Mock private Authentication authentication;
    @InjectMocks private UserController controller;

    @Test
    void getAllUsers() {
        when(userService.getAllUsers()).thenReturn(List.of(new UserResponseDto()));
        assertEquals(1, controller.getAllUsers().getBody().size());
    }

    @Test
    void getUserById() {
        UserResponseDto dto = UserResponseDto.builder().id(1L).username("john").build();
        when(userService.getUserById(1L)).thenReturn(dto);
        assertEquals("john", controller.getUserById(1L).getBody().getUsername());
    }

    @Test
    void linkBankAccount_UsesAuthenticatedUsername() {
        when(authentication.getName()).thenReturn("john");
        LinkBankResponse resp = LinkBankResponse.builder()
                .accountId(1001L).holderName("John")
                .balance(new BigDecimal("100.00")).message("ok").build();
        when(userService.linkBankAccount("john", 1001L)).thenReturn(resp);

        ResponseEntity<LinkBankResponse> r =
                controller.linkBankAccount(new LinkBankRequest(1001L), authentication);

        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals(1001L, r.getBody().getAccountId());
    }

    @Test
    void activateUser() {
        UserResponseDto dto = UserResponseDto.builder().id(1L).status("ACTIVE").build();
        when(userService.activateUser(1L)).thenReturn(dto);
        assertEquals("ACTIVE", controller.activateUser(1L).getBody().getStatus());
    }

    @Test
    void deactivateUser() {
        DeactivateUserRequest req = new DeactivateUserRequest(1L, "reason");
        UserResponseDto dto = UserResponseDto.builder().id(1L).status("INACTIVE").build();
        when(userService.deactivateUser(req)).thenReturn(dto);
        assertEquals("INACTIVE", controller.deactivateUser(req).getBody().getStatus());
    }
}
