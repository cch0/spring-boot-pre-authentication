package com.demo.config;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.util.Collections;

/**
 * Custom implementation of AuthenticationUserDetailsService interface to be used in pre-authenticated flow.
 */
public class AuthorizationUserDetailsService implements AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {

    /**
     * Loads user from data store and creates UserDetails object based on principal and/or credential.
     *
     * Role name needs to have "ROLE_" prefix.
     *
     * @param token instance of PreAuthenticatedAuthenticationToken
     * @return UserDetails object which contains role information for the given user.
     * @throws UsernameNotFoundException
     */
    @Override
    public UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken token) throws UsernameNotFoundException {
        final String principal = (String)token.getPrincipal();
        final String credential = (String)token.getCredentials();

        // TODO this is only for illustration purpose. Should retrieve user from data store and determine user roles
        if (principal.equals("joe")) {
            // TODO some user lookup and then create User object with roles

            return new User("admin-user", "", Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
        } else {
            return new User("normal-user", "", Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        }
    }
}
