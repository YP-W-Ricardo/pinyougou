package cn.itcast.core.controller;

import cn.itcast.core.pojo.entity.Result;
import cn.itcast.core.pojo.log.PayLog;
import cn.itcast.core.service.OrderService;
import cn.itcast.core.service.PayService;
import com.alibaba.dubbo.config.annotation.Reference;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.management.relation.RelationSupport;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付业务
 */
@RestController
@RequestMapping("/pay")
public class PayController {

    @Reference
    private PayService payService;

    @Reference
    private OrderService orderService;

    /**
     * 生成支付链接
     * @return
     */
    @RequestMapping("/createNative")
    public Map<String, String> createNative() {
        //1. 获取当前登录用户的用户名
        String userName = SecurityContextHolder.getContext().getAuthentication().getName();
        //2. 根据当前登录的用户名到redis中获取支付日志对象
        PayLog payLog = payService.findPayLogFromRedisByUserName(userName);
        //3. 根据支付日志对象中的支付单号和总金额, 调用微信统一下单接口生成支付链接
        if (payLog != null) {
            //Map map = payService.createNative(payLog.getOutTradeNo(), String.valueOf(payLog.getTotalFee()));
            Map map = payService.createNative(payLog.getOutTradeNo(), "1");
            return map;
        } else {
            return new HashMap<>();
        }
    }

    /**
     * 根据支付单号调用微信的查询订单接口, 查询是否支付成功
     * @param out_trade_no  支付单号
     * @return
     */
    @RequestMapping("/queryPayStatus")
    public Result queryPayStatus(String out_trade_no) throws Exception {
        Result result = null;
        int flag = 1;
        //1. 死循环, 不停的查
        while(true) {
            //2. 根据支付单号调用微信查询订单接口
            Map map = payService.queryPayStatus(out_trade_no);
            //3. 判断返回值如果为空, 证明支付单号是作废的, 返回二维码超时信息
            if (map == null) {
                result = new Result(false, "二维码超时");
                break;
            }
            //4. 如果查询支付成功
            if ("SUCCESS".equals(map.get("trade_state"))) {
                //5. 修改支付日志表和订单表支付状态为支付成功, 删除redis中缓存的待支付日志对象
                orderService.updatePayLogAndOrderStatus(out_trade_no);
                result = new Result(true, "支付成功!");
                break;
            }

            //6. 每次查询睡3秒, 防止不停的查询, 服务器压力过大
            Thread.sleep(3000);
            flag++;
            if (flag >= 400) {
                result = new Result(false, "二维码超时");
                break;
            }
        }
        return result;
    }
}
