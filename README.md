# Securing Spring Boot Application With Spring Security

 # Goal
 
 The goal is to provide a working example to illustrate how to secure a Spring Boot application using Spring Security.
 
 
 # Example
 
 The application contains two endpoints, one is secured and requires caller to provide information for authorization 
 to access the endpoint. Authentication is assumed to have been done and information is provided through HTTP request headers. 
 This is also called [Pre-Authentication Scenarios](https://docs.spring.io/spring-security/site/docs/5.2.x/reference/html5/#preauth)
 
 >There are situations where you want to use Spring Security for authorization, but the user has already been reliably authenticated by some external system prior to accessing the application. We refer to these situations as "pre-authenticated" scenarios
 
 This use case can be useful when user authentication is already performed through other mechanism such as X.509 certificate.
 
 # Build and Run the Application
 
```
mvn clean verify

java -jar target/spring-boot-pre-authentication-0.0.1-SNAPSHOT.jar

```
 
# Endpoints

## Service Endpoints
## /v1/hello

This is a secure endpoint and requires caller to have `ADMIN` role.

## /v1/bye
 
This is an unsecure endpoint and does not require caller to be associated with any role.
 
## Actuator Endpoints

We would like these endpoints to remain accessible without authentication or authorization  
 
# Spring Security Configuration
 
## maven pom.xml 

```
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
``` 
 
## Method Level Security Configuration

Method level security configuration offers greater flexibility compared to configuration through bean.

First thing to do is to enable global method security

```
@Configuration
@EnableGlobalMethodSecurity(
        prePostEnabled = true,
        securedEnabled = true,
        jsr250Enabled = true)
public class MethodSecurityConfig extends GlobalMethodSecurityConfiguration {
}
```

Then, we would like to use meta-annotation which allows us to decouple the business logic from underlying security 
implementation.

In the following example, we define **isAdmin** annotation which can be used in any places where admin role is needed in
order to access the endpoint. What it translates into is for caller to have the **ADMIN** role in the system. Later on 
if rule has changed to also allow user with other role to also access the resources, then this is the only place where we need
to make the change. 

```
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('ADMIN')")
public @interface IsAdmin {
}
```

Using custom meta-annotation is not the only way to provide method level security but it does provide the greatest
flexibility over other pre-defined annotations.


## Spring Security Configuration

We create a custom configuration class which extends existing **WebSecurityConfigurerAdapter** to customize endpoints 
for our need. We also specify using **RequestHeaderAuthenticationFilter** to allow us to extract authentication information
from the HTTP headers in order to determine the level of authorization a user has.

We also allow access to actuator endpoints, static asset and all the endpoints which has `v1` as prefix. All other endpoints
access are denied regardless if authentication information is provided or not. Because of the authentication filter, all allowed endpoints are still going through the authentication
filter but for actuator and static assets, since normally authentication information is not provided in the HTTP headers,
user information will not be loaded from data store.  

```java

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                // configure pre-authentication flow
                .addFilterBefore(siteminderFilter(), RequestHeaderAuthenticationFilter.class)
                .authorizeRequests()
                // allow access to actuator endpoints
                .antMatchers("/actuator/**").permitAll()
                // allow access to favicon.ico endpoint
                .antMatchers("/favicon.ico").permitAll()
                // allow access to v1 endpoints, still subject to authorization check defined on the method level
                .antMatchers("/v1/**").permitAll()
                // deny all other endpoints
                .anyRequest().denyAll()
        ;
    }
    
    
```

Inside the same **SecurityConfig** class, the following configuration of authentication filter specifies which HTTP header is for communicating principal information
and which header is for credential. Principal header is required while credential is optional. We also disable throwing
exception when headers are not provided. This is needed for actuator and static asset where headers are normally
not provided. Otherwise, we won't be able to access actuator endpoints and static asset.

```java

public RequestHeaderAuthenticationFilter siteminderFilter() {
    RequestHeaderAuthenticationFilter requestHeaderAuthenticationFilter = new RequestHeaderAuthenticationFilter();

    // set the header name which provides principal information
    requestHeaderAuthenticationFilter.setPrincipalRequestHeader("x-actor-id");

    // set the header name which provides credential information
    requestHeaderAuthenticationFilter.setCredentialsRequestHeader("x-grantor-id");

    requestHeaderAuthenticationFilter.setAuthenticationManager(authenticationManager());

    // do not throw exception when header is not present.
    // one use case is for actuator endpoints and static assets where security headers are not required.
    requestHeaderAuthenticationFilter.setExceptionIfHeaderMissing(false);

    return requestHeaderAuthenticationFilter;
}
```

We create a class which implements **AuthenticationUserDetailsService** interface and provide implementation of
how user information is loaded from data store using authentication information. The following example is NOT complete
in the sense that data access part is not added but rather hard-coded. The main purpose of this example is to show
how to configure role information a user has. 

```java

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
``` 

## Endpoints Configuration

**/v1/hello** endpoint has **@IsAdmin** annotation and user needs to have the **ADMIN** role in order to access that endpoint.

**/v1/bye** endpoint, on the other hand, is accessible without needing the authorization.


```java
@RestController
@RequestMapping(value = "/v1")
public class Controller {

    /**
     * Endpoint which requires ADMIN role to access.
     * @return
     */
    @RequestMapping({ "/hello" })
    @IsAdmin
    public String hello() {
        return "Hello World";
    }

    /**
     * Endpoint which does not require any authorization.
     * @return
     */
    @RequestMapping({ "/bye" })
    public String bye() {
        return "bye";
    }
}
```