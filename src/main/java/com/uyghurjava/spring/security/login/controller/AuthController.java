package com.uyghurjava.spring.security.login.controller;

import com.uyghurjava.spring.security.login.models.ERole;
import com.uyghurjava.spring.security.login.models.Role;
import com.uyghurjava.spring.security.login.models.User;
import com.uyghurjava.spring.security.login.payload.request.LoginRequest;
import com.uyghurjava.spring.security.login.payload.request.SignupRequest;
import com.uyghurjava.spring.security.login.payload.response.MessageResponse;
import com.uyghurjava.spring.security.login.payload.response.UserInfoResponse;
import com.uyghurjava.spring.security.login.repository.RoleRepository;
import com.uyghurjava.spring.security.login.repository.UserRepository;
import com.uyghurjava.spring.security.login.security.service.UserDetailsImpl;
import com.uyghurjava.spring.security.login.security.service.jwt.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This controller provides APIs for register and login, logout actions.
 *
 * – /api/auth/signup
 *
 * check existing username/email
 * create new User (with ROLE_USER if not specifying role)
 * save User to database using UserRepository
 * – /api/auth/signin
 *
 * authenticate { username, password }
 * update SecurityContext using Authentication object
 * generate JWT
 * get UserDetails from Authentication object
 * response contains JWT and UserDetails data
 * – /api/auth/signout: clear the Cookie.
 */

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    AuthenticationManager authenticationManager;
    @Autowired
    UserRepository userRepository;
    @Autowired
    RoleRepository roleRepository;
    @Autowired
    PasswordEncoder encoder;
    @Autowired
    JwtUtils jwtUtils;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest){
        //authenticate { username, password }
        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
        //update SecurityContext using Authentication object
        SecurityContextHolder.getContext().setAuthentication(authentication);

        //get UserDetails from Authentication object
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        //generate JWT cookie
        ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(userDetails);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        logger.info("Successfully Sign-in (AuthController)");

        //response contains JWT and UserDetails data
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body(new UserInfoResponse(userDetails.getId(),
                        userDetails.getUsername(),
                        userDetails.getEmail(),
                        roles));

    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signupRequest){
        //check existing username/email
        if(userRepository.existsByUsername(signupRequest.getUsername())){
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken!"));
        }
        if (userRepository.existsByEmail(signupRequest.getEmail())){
            return ResponseEntity.badRequest().body(new MessageResponse("Error Email is already in use!"));
        }
        //Create new user's account(with ROLE_USER if not specifying role)
        User user = new User(signupRequest.getUsername(),
                signupRequest.getEmail(),
                encoder.encode(signupRequest.getPassword()));

        Set<String> strRoles = signupRequest.getRole();

        Set<Role> roles = new HashSet<>();
        //(with ROLE_USER if not specifying role)
        if(strRoles == null || strRoles.isEmpty()){
            Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                    .orElseThrow( () -> new RuntimeException("Error: Role is not found"));
            roles.add(userRole);
        }else {
            strRoles.forEach(role -> {
                switch (role){
                    case "admin":
                        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                                .orElseThrow( () -> new RuntimeException("Error: is not found"));
                        roles.add(adminRole);
                        break;
                    case "mod":
                        Role modRole = roleRepository.findByName(ERole.ROLE_MODERATOR)
                                .orElseThrow( () -> new RuntimeException("Error: Role is not found"));
                        roles.add(modRole);
                        break;
                    default:
                        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                                .orElseThrow( () -> new RuntimeException("Error: Role is not found"));
                        roles.add(userRole);
                }
            });
        }
        user.setRoles(roles);
        userRepository.save(user);
        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    @PostMapping("/signout")
    public ResponseEntity<?> logoutUser(){
        //clear the Cookie
        ResponseCookie cookie = jwtUtils.getCleanJwtCookie();

        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new MessageResponse("You have been signed out!"));
    }

}
