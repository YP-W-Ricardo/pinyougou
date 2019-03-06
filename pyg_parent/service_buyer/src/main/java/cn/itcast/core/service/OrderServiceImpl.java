package cn.itcast.core.service;

import cn.itcast.core.common.Constants;
import cn.itcast.core.common.IdWorker;
import cn.itcast.core.dao.log.PayLogDao;
import cn.itcast.core.dao.order.OrderDao;
import cn.itcast.core.dao.order.OrderItemDao;
import cn.itcast.core.pojo.entity.BuyerCart;
import cn.itcast.core.pojo.log.PayLog;
import cn.itcast.core.pojo.order.Order;
import cn.itcast.core.pojo.order.OrderItem;
import com.alibaba.dubbo.config.annotation.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    @Autowired
    private PayLogDao payLogDao;

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private OrderItemDao orderItemDao;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private IdWorker idWorker;

    @Override
    public void add(Order order) {
        //1. 根据用户名到redis中获取购物车集合
        List<BuyerCart> cartList = (List<BuyerCart>)redisTemplate.boundHashOps(Constants.REDIS_CARTLIST).get(order.getUserId());
        List<String> orderIdList=new ArrayList();//订单ID列表
        double total_money=0;//总金额 （元）


        //2. 遍历购物车集合
        if (cartList != null) {
            for (BuyerCart cart : cartList) {
                Order tbOrder = new Order();
                //使用分布式id生成器, 生成订单id, 保证唯一
                long orderId = idWorker.nextId();
                tbOrder.setOrderId(orderId);
                //TODO 一堆set方法, 设置订单对象属性
                tbOrder.setUserId(order.getUserId());//用户名
                tbOrder.setPaymentType(order.getPaymentType());//支付类型
                tbOrder.setStatus("1");//状态：未付款
                tbOrder.setCreateTime(new Date());//订单创建日期
                tbOrder.setUpdateTime(new Date());//订单更新日期
                tbOrder.setReceiverAreaName(order.getReceiverAreaName());//地址
                tbOrder.setReceiverMobile(order.getReceiverMobile());//手机号
                tbOrder.setReceiver(order.getReceiver());//收货人
                tbOrder.setSourceType(order.getSourceType());//订单来源
                tbOrder.setSellerId(cart.getSellerId());//商家ID
                //循环购物车明细
                double money=0;

                if (cart.getOrderItemList() != null) {
                    //3. 遍历购物车中的购物项集合
                    for (OrderItem orderItem : cart.getOrderItemList()) {
                        //使用分布式id生成器, 生成订单详情id
                        long orderItemId = idWorker.nextId();
                        orderItem.setId(orderItemId);
                        //TODO 一堆set方法, 设置orderItem对象属性
                        orderItem.setOrderId( orderId  );//订单ID
                        orderItem.setSellerId(cart.getSellerId());
                        money+=orderItem.getTotalFee().doubleValue();//金额累加

                        //保存订单详情
                        orderItemDao.insertSelective(orderItem);
                    }
                }

                //保存订单
                tbOrder.setPayment(new BigDecimal(money));
                orderDao.insertSelective(tbOrder);

                //4. 计算总价格
                orderIdList.add(orderId+"");//添加到订单列表
                total_money+=money;//累加到总金额

            }
        }

        //保存支付日志
        if("1".equals(order.getPaymentType())){//如果是微信支付
            PayLog payLog=new PayLog();
            String outTradeNo=  idWorker.nextId()+"";//支付订单号
            payLog.setOutTradeNo(outTradeNo);//支付订单号
            payLog.setCreateTime(new Date());//创建时间
            //订单号列表，逗号分隔
            String ids=orderIdList.toString().replace("[", "").replace("]", "").replace(" ", "");
            payLog.setOrderList(ids);//订单号列表，逗号分隔
            payLog.setPayType("1");//支付类型
            payLog.setTotalFee( (long)(total_money*100 ) );//总金额(分)
            payLog.setTradeState("0");//支付状态
            payLog.setUserId(order.getUserId());//用户ID
            payLogDao.insertSelective(payLog);//插入到支付日志表

            //将支付日志根据用户名缓存到redis中一份
            redisTemplate.boundHashOps(Constants.REDIS_PAYLOG).put(order.getUserId(), payLog);//放入缓存
        }

        //保存完订单, 那么redis中的购物车列表就没有用了, 可以删除掉
        redisTemplate.boundHashOps(Constants.REDIS_CARTLIST).delete(order.getUserId());

    }

    @Override
    public void updatePayLogAndOrderStatus(String out_trade_no) {
        //1. 根据支付单号获取支付日志对象
        PayLog payLog = payLogDao.selectByPrimaryKey(out_trade_no);
        payLog.setTradeState("1");
        //2. 保存支付日志为已支付
        payLogDao.updateByPrimaryKeySelective(payLog);
        //3. 根据支付日志中的订单号集合修改订单状态为已支付
        String orderIdStrList = payLog.getOrderList();
        String[] orderIdArray = orderIdStrList.split(",");
        if (orderIdArray != null) {
            for (String orderId : orderIdArray) {
                Order order = new Order();
                order.setOrderId(Long.parseLong(orderId));
                order.setStatus("2");
                orderDao.updateByPrimaryKeySelective(order);
            }
        }
        //4. 清除redis中的待支付日志对象
        redisTemplate.boundHashOps(Constants.REDIS_PAYLOG).delete(payLog.getUserId());
    }
}
