package com.dat.backend_version_2.controller.authentication;

import com.dat.backend_version_2.domain.authentication.UserTokens;
import com.dat.backend_version_2.domain.authentication.Users;
import com.dat.backend_version_2.domain.training.Student;
import com.dat.backend_version_2.dto.authentication.LoginReq;
import com.dat.backend_version_2.dto.authentication.LoginRes;
import com.dat.backend_version_2.service.authentication.UserTokensService;
import com.dat.backend_version_2.service.authentication.UsersService;
import com.dat.backend_version_2.util.SecurityUtil;
import com.dat.backend_version_2.util.error.AuthenticationException;
import com.dat.backend_version_2.util.error.IdInvalidException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
    @RequestMapping("/api/v1/auth")
public class AuthenticationController {
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final SecurityUtil securityUtil;
    private final UsersService userService;
    private final UserTokensService userTokensService;

    @Value("${jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenExpiration;

    public AuthenticationController(
            AuthenticationManagerBuilder authenticationManagerBuilder,
            SecurityUtil securityUtil,
            UsersService userService,
            UserTokensService userTokensService) {
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.securityUtil = securityUtil;
        this.userService = userService;
        this.userTokensService = userTokensService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginRes> login(@Valid @RequestBody LoginReq.UserBase loginReq) {
        // Nạp input gồm username/passwork vào Security
        UsernamePasswordAuthenticationToken authenticationToken
                = new UsernamePasswordAuthenticationToken(loginReq.getIdAccount(), loginReq.getPassword());

        // Xác thực người dùng => cần viết hàm loadUserByUsername
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        // Set thông tin người dùng đăng nhập vào context (có thể sử dụng sau này)
        SecurityContextHolder.getContext().setAuthentication(authentication);

        LoginRes loginRes = new LoginRes();
        Users currentUserDB = userService.getUserByIdAccount(loginReq.getIdAccount());

        if (currentUserDB != null) {
            LoginRes.UserLogin userLogin = new LoginRes.UserLogin(
                    // .get phải thứ tự UserLogin
                    currentUserDB.getIdAccount(),
                    currentUserDB.getStatus(),
                    currentUserDB.getRole().getIdRole(),
                    currentUserDB.getCreatedAt().toString()
            );
            loginRes.setUser(userLogin);
        }

        // Lưu mã thiết bị
        loginRes.setIdDevice(loginReq.getIdDevice());

        // Create access token
        assert currentUserDB != null;
        String accessToken = securityUtil.createAccessToken(currentUserDB.getIdUser(), loginRes.getUser());
        loginRes.setAccessToken(accessToken);

        // Create refresh token
        String refreshToken = securityUtil.createRefreshToken(currentUserDB.getIdUser(), loginRes);
        loginRes.setRefreshToken(refreshToken);

        // update user
        userTokensService.updateUserTokens(refreshToken, currentUserDB.getIdUser().toString(), loginReq.getIdDevice());

        ResponseCookie responseCookie = ResponseCookie
                .from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(refreshTokenExpiration)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
                .body(loginRes);
    }

    @GetMapping("/account")
    public ResponseEntity<LoginRes.UserGetAccount> getAccount() {
        String idAccount = SecurityUtil.getCurrentUserLogin().isPresent() ?
                SecurityUtil.getCurrentUserLogin().get() : null;
        Users currentUserDB = userService.getUserByIdAccount(idAccount);
        LoginRes.UserLogin userLogin = new LoginRes.UserLogin();
        LoginRes.UserGetAccount userGetAccount = new LoginRes.UserGetAccount();
        if (currentUserDB != null) {
            userLogin.setIdAccount(currentUserDB.getIdAccount());
            userLogin.setStatus(currentUserDB.getStatus());
            userLogin.setRole(currentUserDB.getRole().getIdRole());
            userGetAccount.setUserLogin(userLogin);
        }
        return ResponseEntity.ok().body(userGetAccount);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginRes> getRefreshToken(
//            @CookieValue(name = "refresh_token", defaultValue = "") String refreshToken
        @RequestBody LoginReq.RefreshRequest request
    ) throws AuthenticationException, IdInvalidException {
        String refreshToken = request.getRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) {
//            throw new IdInvalidException("Bạn không có refresh token ở cookies");
            throw new IdInvalidException("Bạn không có refresh token");
        }

        Jwt decodedToken = securityUtil.checkValidRefreshToken(refreshToken);
        String idUser = decodedToken.getSubject();
        String idDevice = decodedToken.getClaim("id_device");

        // check user by token + idAccount + idDevice
        UserTokens currentUserDB = userTokensService
                .getUserTokensByRefreshTokenAndIdAccountAndIdDevice(refreshToken, idUser, idDevice);
        if (currentUserDB == null) {
            throw new AuthenticationException("Token không hợp lệ hoặc thiết bị không khớp");
        }

        // issue new token/set refresh token as cookies
        LoginRes loginRes = new LoginRes();
        Users userDB = userService.getUserById(idUser);
        System.out.println("UserDB: " + userDB);
        if (userDB != null) {
            System.out.println("IdAccount: " + userDB.getIdAccount());
            System.out.println("Status: " + userDB.getStatus());
            System.out.println("Role ID: " + (userDB.getRole() != null ? userDB.getRole().getIdRole() : "NULL ROLE OBJECT")); // Kiểm tra kỹ Role
            System.out.println("CreatedAt: " + userDB.getCreatedAt());
            LoginRes.UserLogin userLogin = new LoginRes.UserLogin(
                    userDB.getIdAccount(),
                    userDB.getStatus(),
                    userDB.getRole().getIdRole(),
                    userDB.getCreatedAt().toString()
            );
            loginRes.setUser(userLogin);
        }
        loginRes.setIdDevice(idDevice);

        // Create access token
        assert userDB != null;
        String accessToken = securityUtil.createAccessToken(userDB.getIdUser(), loginRes.getUser());
        loginRes.setAccessToken(accessToken);

        // Create refresh token
        String new_refreshToken = securityUtil.createRefreshToken(userDB.getIdUser(), loginRes);

        loginRes.setRefreshToken(new_refreshToken);

        // update user
        userTokensService.updateUserTokens(new_refreshToken, idUser, idDevice);

        // set cookies
//        ResponseCookie responseCookie = ResponseCookie
//                .from("refresh_token", refreshToken)
//                .httpOnly(true)
//                .secure(true)
//                .path("/")
//                .maxAge(refreshTokenExpiration)
//                .build();
//
//        return ResponseEntity.ok()
//                .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
//                .body(loginRes);
        return ResponseEntity.ok(loginRes);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
//            @CookieValue(name = "refresh_token", defaultValue = "") String refreshToken
            @RequestBody LoginReq.RefreshRequest request
    ) throws IdInvalidException, AuthenticationException {
        String refreshToken = request.getRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) {
//            throw new IdInvalidException("Không tìm thấy refresh token trong cookie");
            throw new IdInvalidException("Không tìm thấy refresh token");
        }

        Jwt decodedToken = securityUtil.checkValidRefreshToken(refreshToken);

        String idUser = decodedToken.getSubject();
        // Lấy claim "user" và convert về UserLogin
        Object userObj = decodedToken.getClaim("user");
        LoginRes.UserLogin userLogin = new ObjectMapper()
                .convertValue(userObj, LoginRes.UserLogin.class);

        // Lấy claim "id_device" an toàn
        String idDevice = decodedToken.getClaim("id_device");
        System.out.println("idDevice: " + idDevice);

        // update refresh token = null
        userTokensService.updateUserTokens(null, idUser, idDevice);

        // Xóa cookie refresh token trên client
        ResponseCookie deleteSpringCookie = ResponseCookie
                .from("refresh_token", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteSpringCookie.toString())
                .build();
    }
}
