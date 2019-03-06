package cn.itcast.core.service;

import cn.itcast.core.pojo.order.Order;

public interface OrderService {

    public void add(Order order);

    public  void updatePayLogAndOrderStatus(String out_trade_no);
}
