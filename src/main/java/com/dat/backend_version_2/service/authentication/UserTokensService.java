package com.dat.backend_version_2.service.authentication;

import com.dat.backend_version_2.domain.authentication.UserTokens;
import com.dat.backend_version_2.domain.authentication.Users;
import com.dat.backend_version_2.repository.authentication.UserTokensRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserTokensService {
    @Autowired
    private UserTokensRepository userTokensRepository;

    @Autowired
    private UsersService usersService;

    @Value("${jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenExpiration;

    public UserTokens getUserTokensByIdUserAndDevice(String idUser, String idDevice) {
        Users user = usersService.getUserById(idUser);
        return userTokensRepository.findByUserAndIdDevice(user, idDevice)
                .orElseGet(() -> createUserTokens(user, idDevice));
    }

    public UserTokens getUserTokensByidAccountAndDevice(String idAccount, String idDevice) {
        Users user = usersService.getUserByIdAccount(idAccount);
        return userTokensRepository.findByUserAndIdDevice(user, idDevice)
                .orElseGet(() -> createUserTokens(user, idDevice));
    }

    public UserTokens createUserTokens(Users user, String idDevice) {
        UserTokens userTokens = new UserTokens();
        userTokens.setUser(user);
        userTokens.setIdDevice(idDevice);
        userTokens.setExpiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration));
        return userTokensRepository.save(userTokens);
    }

    public void updateUserTokens(String token, String idUser, String idDevice) {
        UserTokens currentUser = getUserTokensByIdUserAndDevice(idUser, idDevice);
        if (currentUser != null) {
            currentUser.setRefreshToken(token);
            userTokensRepository.save(currentUser);
        }
    }

    public UserTokens getUserTokensByRefreshTokenAndIdAccountAndIdDevice(
            String refreshToken, String idUser, String idDevice) {
        UserTokens currentUser = getUserTokensByIdUserAndDevice(idUser, idDevice);
        if (currentUser != null) {
            currentUser.setRefreshToken(refreshToken);
            userTokensRepository.save(currentUser);
        }
        return currentUser;
    }
}
