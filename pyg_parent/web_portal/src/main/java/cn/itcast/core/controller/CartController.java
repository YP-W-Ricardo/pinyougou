package cn.itcast.core.controller;

import cn.itcast.core.common.Constants;
import cn.itcast.core.common.CookieUtil;
import cn.itcast.core.pojo.entity.BuyerCart;
import cn.itcast.core.pojo.entity.Result;
import cn.itcast.core.service.BuyerCartService;
import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 购物车业务
 */
@RestController
@RequestMapping("/cart")
public class CartController {


    @Reference
    private BuyerCartService cartService;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private HttpServletResponse response;

    /**
     * 添加商品到购物车中
     * @param itemId    库存id
     * @param num       购买数量
     * @return
     */
    @RequestMapping("/addGoodsToCartList")
    @CrossOrigin(origins="http://localhost:8087",allowCredentials="true")
    public Result addGoodsToCartList(Long itemId, Integer num) {
        //1. 获取当前登录用户名称
        String userName = SecurityContextHolder.getContext().getAuthentication().getName();
        //2. 获取购物车列表
        List<BuyerCart> cartList = findCartList();
        //3. 将当前商品加入到购物车列表
        cartList = cartService.addItemToCartList(cartList, itemId, num);
        //4. 判断当前用户是否登录, 未登录用户名为"anonymousUser"
        if ("anonymousUser".equals(userName)) {
            //4.a.如果未登录, 则将购物车列表存入cookie中
            String cartListJsonStr = JSON.toJSONString(cartList);
            CookieUtil.setCookie(request, response, Constants.COOKIE_CARTLIST, cartListJsonStr, 60 * 60 * 24 * 30, "utf-8");
        } else {
            //4.b.如果已登录, 则将购物车列表存入redis中
            cartService.setCartListToRedis(userName, cartList);
        }

        return new Result(true, "添加成功!");
    }

    /**
     * 查询当前用户购物车列表数据并返回
     * 如果用户登录了, 则根据用户名去redis中查询, 如果没有登录则去用户浏览器cookie中查询
     * @return
     */
    @RequestMapping("/findCartList")
    public List<BuyerCart> findCartList() {
        //1. 获取当前登录用户名称
        String userName = SecurityContextHolder.getContext().getAuthentication().getName();
        //2. 从cookie中获取购物车列表json格式字符串
        String cookieJsonStr = CookieUtil.getCookieValue(request, Constants.COOKIE_CARTLIST, "utf-8");
        //3. 如果购物车列表json串为空则返回"[]"
        if (cookieJsonStr == null || "".equals(cookieJsonStr)) {
            cookieJsonStr = "[]";
        }
        //4. 将购物车列表json转换为对象
        List<BuyerCart> cookieCartList = JSON.parseArray(cookieJsonStr, BuyerCart.class);
        //5. 判断用户是否登录, 未登录用户为"anonymousUser"
        if ("anonymousUser".equals(userName)) {
            //5.a. 未登录, 返回cookie中的购物车列表对象
            return cookieCartList;
        } else {
            //5.b.1.已登录, 从redis中获取购物车列表对象
            List<BuyerCart> redisCartList = cartService.getCartListFromRedis(userName);
            //5.b.2.判断cookie中是否存在购物车列表
            if (cookieCartList != null && cookieCartList.size() > 0) {
                //如果cookie中存在购物车列表则和redis中的购物车列表合并成一个对象
                redisCartList = cartService.mergeCookieCartListToRedisCartList(cookieCartList, redisCartList);
                //删除cookie中购物车列表
                CookieUtil.deleteCookie(request, response, Constants.COOKIE_CARTLIST);
                //将合并后的购物车列表存入redis中
                cartService.setCartListToRedis(userName, redisCartList);
            }
            //5.b.3.返回购物车列表对象
            return redisCartList;
        }
    }
}
