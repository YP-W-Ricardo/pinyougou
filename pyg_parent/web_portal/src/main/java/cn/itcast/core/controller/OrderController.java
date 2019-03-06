package cn.itcast.core.controller;

import cn.itcast.core.pojo.entity.Result;
import cn.itcast.core.pojo.order.Order;
import cn.itcast.core.service.OrderService;
import com.alibaba.dubbo.config.annotation.Reference;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单操作
 */
@RestController
@RequestMapping("/order")
public class OrderController {

    @Reference
    private OrderService orderService;

    /**
     * 保存订单
     * @param order
     * @return
     */
    @RequestMapping("/add")
    public Result add(@RequestBody Order order) {
        try {
            //1. 获取当前登录用户的用户名
            String userName = SecurityContextHolder.getContext().getAuthentication().getName();
            order.setUserId(userName);
            //2. 根据用户名和页面传入的订单对象, 保存订单
            orderService.add(order);
            return new Result(true, "订单保存成功!");
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false, "订单保存失败!");
        }
    }
}
