package com.demo.controller;

import com.demo.config.IsAdmin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
