package com.itheima.reggie.controller;

import com.itheima.reggie.common.ReggieResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 冷运支付基础数据接口。
 */
@RestController
@RequestMapping("/cold-chain/pay")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ColdChainPayDictionaryController {



}
