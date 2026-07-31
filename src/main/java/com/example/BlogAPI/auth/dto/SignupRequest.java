package com.example.BlogAPI.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {

    @NotBlank
    @Size(min = 1, max = 50)
    private String username;

    @NotBlank
    @Size(min = 1, max = 50)
    private String email;

    @NotBlank
    @Size(min = 8, max = 30)
    private String password;
}
