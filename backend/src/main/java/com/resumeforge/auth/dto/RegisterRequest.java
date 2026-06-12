package com.resumeforge.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Nome e obrigatorio.")
    @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres.")
    private String name;

    @NotBlank(message = "Email e obrigatorio.")
    @Email(message = "Email invalido.")
    private String email;

    @NotBlank(message = "Senha e obrigatoria.")
    @Size(min = 8, message = "Senha deve ter no minimo 8 caracteres.")
    private String password;
}
