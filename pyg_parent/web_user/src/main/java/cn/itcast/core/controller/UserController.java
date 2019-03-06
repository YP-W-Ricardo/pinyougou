package cn.itcast.core.controller;

import cn.itcast.core.common.PhoneFormatCheckUtils;
import cn.itcast.core.pojo.entity.Result;
import cn.itcast.core.pojo.user.User;
import cn.itcast.core.service.UserService;
import com.alibaba.dubbo.config.annotation.Reference;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
@RequestMapping("/user")
public class UserController {

    @Reference
    private UserService userService;

    /**
     * 向手机号发送一个验证码
     * @param phone 手机号
     * @return
     */
    @RequestMapping("/sendCode")
    public Result sendCode(String phone) {
        try {
            //1. 校验手机号的正确性
            if (phone == null || "".equals(phone)) {
                return new Result(false, "请正确填写手机号!");
            }
            if (!PhoneFormatCheckUtils.isPhoneLegal(phone)) {
                return new Result(false, "手机号格式不正确!");
            }
            //2. 如果手机号格式正确则发送验证码
            userService.sendCode(phone);
            return new Result(true, "发送成功!");
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false, "发送失败!");
        }
    }

    /**
     * 判断验证码是否正确, 如果正确保存用户对象
     * @param user      用户对象
     * @param smscode   验证码
     * @return
     */
    @RequestMapping("/add")
    public Result add(@RequestBody User user, String smscode) {
        try {
            //1. 判断验证码是否正确
            boolean isCheck = userService.checkSmsCode(user.getPhone(), smscode);
            if (!isCheck) {
                return new Result(false, "手机号或者验证码错误!");
            }
            //2. 保存用户对象
            user.setCreated(new Date());
            user.setUpdated(new Date());
            //默认为pc端注册
            user.setSourceType("1");
            //使用状态默认为正常
            user.setStatus("Y");
            userService.add(user);
            return new Result(true, "注册成功!");
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false, "注册失败!");
        }
    }
}
