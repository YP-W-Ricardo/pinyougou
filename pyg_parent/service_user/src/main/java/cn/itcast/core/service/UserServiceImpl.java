package cn.itcast.core.service;

import cn.itcast.core.dao.user.UserDao;
import cn.itcast.core.pojo.user.User;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.Session;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    private ActiveMQQueue smsDestination;

    @Value("${sign_name}")
    private String signName;
    @Value("${template_code}")
    private String templateCode;

    @Autowired
    private UserDao userDao;

    @Override
    public void sendCode(final String phone) {
        //1. 生成一个小于等于6位的随机数作为验证码
        final long code = (long)(Math.random() * 1000000);
        //2. 将手机号作为key, 验证码作为value存入redis
        redisTemplate.boundValueOps(phone).set(code, 10, TimeUnit.MINUTES);
        //3. 将手机号, 验证码, 模板编号, 签名等数据封装成map格式的消息发送给消息服务器
        jmsTemplate.send(smsDestination, new MessageCreator() {
            @Override
            public Message createMessage(Session session) throws JMSException {
                MapMessage mapMessage = new ActiveMQMapMessage();
                //手机号
                mapMessage.setString("phone", phone);
                //验证码
                mapMessage.setString("signName", signName);
                //模板编号
                mapMessage.setString("templateCode", templateCode);

                //短信内容
                Map<String, String> codeMap = new HashMap<>();
                codeMap.put("code", String.valueOf(code));
                mapMessage.setString("param", JSON.toJSONString(codeMap));
                return mapMessage;
            }
        });
    }

    @Override
    public boolean checkSmsCode(String phone, String smsCode) {
        //1. 判断手机号或者是验证码为空返回false
        if (phone == null || "".equals(phone) || smsCode == null || "".equals(smsCode)) {
            return false;
        }
        //2. 通过手机号到redis中获取验证码
        Long redisSmsCode = (Long) redisTemplate.boundValueOps(phone).get();
        //3. 判断redis中获取的验证码如果为空则返回false
        if (redisSmsCode == null || "".equals(redisSmsCode)) {
            return false;
        }
        //4. 校验页面输入的验证码和redis中获取的验证码如果一致返回true
        if (String.valueOf(redisSmsCode).equals(smsCode)) {
            return true;
        }
        return false;
    }

    @Override
    public void add(User user) {
        userDao.insertSelective(user);
    }

    public static void main(String[] args) {
        long s = (long)(Math.random() * 1000000);
        System.out.println("======" + s);
    }
}
