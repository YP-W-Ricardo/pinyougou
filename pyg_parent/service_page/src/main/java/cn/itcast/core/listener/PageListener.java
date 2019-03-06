package cn.itcast.core.listener;

import cn.itcast.core.service.CmsService;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.factory.annotation.Autowired;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import java.util.Map;

/**
 * 自定义监听器
 * 监听来自于消息服务器发送过来的消息也就是商品id
 * 根据商品id到数据库获取商品详细数据然后根据模板生成静态化页面
 */
public class PageListener implements MessageListener {

    @Autowired
    private CmsService cmsService;

    @Override
    public void onMessage(Message message) {
        ActiveMQTextMessage activeMQTextMessage = (ActiveMQTextMessage)message;
        try {
            //1. 获取消息, 商品id
            String goodsId = activeMQTextMessage.getText();
            //2. 根据商品id到数据库中获取商品详细数据
            Map<String, Object> rootMap = cmsService.findGoods(Long.parseLong(goodsId));
            //3. 根据数据和模板生成静态化页面, 供portal访问详情页面使用
            cmsService.createStaticPage(Long.parseLong(goodsId), rootMap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
