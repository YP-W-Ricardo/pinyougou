package cn.itcast.core.service;

import cn.itcast.core.pojo.entity.BuyerCart;

import java.util.List;

public interface BuyerCartService {

    /**
     * 添加商品到这个用户的现有购物车集合中, 然后返回加入商品后的购物车集合
     * @param cartList  这个用户现在拥有的购物车集合
     * @param itemId    商品库存id
     * @param num       购买数量
     * @return
     */
    public List<BuyerCart> addItemToCartList(List<BuyerCart> cartList, Long itemId, Integer num);

    /**
     * 将购物车集合根据用户名存入redis中
     * @param userName  用户名
     * @param cartList  购物车集合
     */
    public void setCartListToRedis(String userName, List<BuyerCart> cartList);

    /**
     * 根据用户名, 获取这个人redis中的购物车集合
     * @param userName  用户名
     * @return
     */
    public List<BuyerCart> getCartListFromRedis(String userName);

    /**
     * 将cookie中的购物车集合合并到redis的购物车集合中, 并返回合并后的购物车集合
     * @param cookieCartList    cookie中的购物车集合
     * @param redisCartList     redis中的购物车集合
     * @return
     */
    public List<BuyerCart> mergeCookieCartListToRedisCartList(List<BuyerCart> cookieCartList, List<BuyerCart> redisCartList);
}
