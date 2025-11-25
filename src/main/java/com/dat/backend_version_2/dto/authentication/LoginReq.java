package com.dat.backend_version_2.dto.authentication;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class LoginReq {
    @Data
    public static class UserBase {
//        private UUID idUser; // optional khi update

        @NotBlank(message = "Username/AccountId must not be blank")
        private String idAccount;

        @NotBlank(message = "Password must not be blank")
        private String password; // raw password (sẽ mã hóa trong service)

        @NotBlank(message = "IdDevice must not be blank")
        private String idDevice;
    }

    @Data
    public static class RefreshRequest{
        @NotBlank(message = "RefreshToken must not be blank")
        private String refreshToken;
    }
}
