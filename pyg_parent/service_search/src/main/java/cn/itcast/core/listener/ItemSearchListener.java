package cn.itcast.core.listener;

import cn.itcast.core.service.SolrManagerService;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.factory.annotation.Autowired;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

/**
 * 自定义监听器, 监听来自于消息服务器发送来的消息, 也就是商品id
 * 根据商品id到数据库获取商品详细数据, 放入solr索引库中供portal系统搜索使用
 */
public class ItemSearchListener implements MessageListener {

    @Autowired
    private SolrManagerService solrManagerService;

    @Override
    public void onMessage(Message message) {
        ActiveMQTextMessage activeMQTextMessage = (ActiveMQTextMessage)message;
        try {
            //1. 获取消息, 商品id
            String goodsId = activeMQTextMessage.getText();
            //2. 根据商品id到数据库获取商品详细数据放入solr索引库供portal系统搜索使用
            solrManagerService.importItemToSolr(Long.parseLong(goodsId));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
