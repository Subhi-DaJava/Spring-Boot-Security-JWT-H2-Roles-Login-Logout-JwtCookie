package com.uyghurjava.spring.security.login.security;

import com.uyghurjava.spring.security.login.security.service.UserDetailsServiceImpl;
import com.uyghurjava.spring.security.login.security.service.jwt.AuthEntryPointJwt;
import com.uyghurjava.spring.security.login.security.service.jwt.AuthTokenFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * @EnableWebSecurity allows Spring to find and automatically apply the class to the global Web Security.
 *
 * @EnableGlobalMethodSecurity provides AOP security on methods. It enables @PreAuthorize, @PostAuthorize, it also supports JSR-250.
 * You can find more parameters in configuration in Method Security Expressions.
 */
@Configuration
@EnableWebSecurity  //
@EnableGlobalMethodSecurity(
        // securedEnabled = true,
        // jsr250Enabled = true,
        prePostEnabled = true
)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    /**
     *  We override the configure (HttpSecurity http) method from WebSecurityConfigurerAdapter interface.
     *  It tells Spring Security how we configure CORS and CSRF, when we want to require all users to be authenticated or not,
     *  which filter (AuthTokenFilter) and when we want it to work (filter before UsernamePasswordAuthenticationFilter),
     *  which Exception Handler is chosen (AuthEntryPointJwt).
     */

    /**
     * spring.h2.console.path=/h2-ui is for H2 console???s url,
     * so the default url http://localhost:8080/h2-console will change to http://localhost:8080/h2-ui.
     */
    @Value("${spring.h2.console.path}")
    private String h2ConsolePath;

    @Autowired
    UserDetailsServiceImpl userDetailsService;

    /**
     * AuthenticationEntryPoint will catch authentication error.
     * AuthEntryPointJwt implements AuthenticationEntryPoint
     */
    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    /**
     * AuthTokenFilter extends OncePerRequestFilter,
     * OncePerRequestFilter makes a single execution for each request to our API.
     * It provides a doFilterInternal() method that we will implement parsing & validating JWT,
     * loading User details (using UserDetailsService), checking Authorization (using UsernamePasswordAuthenticationToken).
     * @return
     */
    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter(){
        return new AuthTokenFilter();
    }

    /**
     * Spring Security will load User details to perform authentication & authorization.
     * So it has UserDetailsService interface that we need to implement.
     *
     * The implementation of UserDetailsService will be used for configuring DaoAuthenticationProvider by AuthenticationManagerBuilder.
     * userDetailsService() method.
     * @param auth
     * @throws Exception
     */
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
       auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception{
        return super.authenticationManagerBean();
    }

    /**
     * We also need a PasswordEncoder for the DaoAuthenticationProvider. If we don???t specify, it will use plain text.
     * @return
     */
    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    /**
     * We override the method configure(HttpSecurity http) method from WebSecurityConfigurerAdapter interface.
     * It tells Spring Security how we configure CORS and CSRF, when we want to require all users to be authenticated or not,
     * which filter (AuthTokenFilter) and when we want it to work (filter before UsernamePasswordAuthenticationFilter),
     * which Exception Handler is chosen (AuthEntryPointJwt).
     * @param http
     * @throws Exception
     */


    @Override
    protected void configure(HttpSecurity http) throws Exception {
        //Not synchronise for JWT
        http.cors().and().csrf().disable()
                .exceptionHandling().authenticationEntryPoint(unauthorizedHandler).and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                .authorizeRequests().antMatchers("/api/auth/**").permitAll()
                .antMatchers("/api/test/**").permitAll()
                .antMatchers(h2ConsolePath + "/**").permitAll()
                .anyRequest().authenticated();

        // fix H2 database console: Refused to display ' in a frame because it set 'X-Frame-Options' to 'deny'
        http.headers().frameOptions().sameOrigin();

        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
    }

}
