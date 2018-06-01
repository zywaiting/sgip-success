package com.zy.sgip.controller;

import com.zy.sgip.service.SgipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping(value = "/ceshi")
public class SgipController {

    @Autowired
    private SgipService sgipService;

    @RequestMapping(value = "/smsSend")
    @ResponseBody
    public String smsSend(HttpServletRequest request) {
        return sgipService.smsSend(request);
    }
}
