package com.github.pigeon.test;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.ValidationException;

import com.github.pigeon.api.utils.VelocityUtil;

public class VelocityScriptTest {

    public static void main(String[] args) throws ValidationException {
        String str ="$!{notifyUrl}";
        Map<String,Object> ctx = new HashMap<String,Object>();
        //ctx.put("notifyUrl", "1234");
        System.out.println(VelocityUtil.getString(str,ctx));
    }
}
