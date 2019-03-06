package cn.itcast.core.util;

import cn.itcast.core.dao.item.ItemDao;
import cn.itcast.core.pojo.item.Item;
import cn.itcast.core.pojo.item.ItemQuery;
import com.alibaba.dubbo.common.json.ParseException;

import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 查询库存表所有已审核通过的库存数据, 导入到solr索引库, 对solr索引库进行初始化操作
 */
@Component
public class ImportItemToSolr {

    @Autowired
    private SolrTemplate solrTemplate;

    @Autowired
    private ItemDao itemDao;

    public void importItem() {
        //1. 查询库存表所有已审核通过的数据
        ItemQuery query = new ItemQuery();
        ItemQuery.Criteria criteria = query.createCriteria();
        criteria.andStatusEqualTo("1");
        List<Item> itemList = itemDao.selectByExample(query);
        //遍历库存集合
        if (itemList != null) {
            for (Item item : itemList) {
                //获取规格json字符串
                String specJsonStr = item.getSpec();
                //规格json字符串解析成map对象
                Map<String, String> map = JSON.parseObject(specJsonStr, Map.class);
                item.setSpecMap(map);
            }
        }
        //2. 将库存集合数据存入solr索引库
        solrTemplate.saveBeans(itemList);
        //3. 提交
        solrTemplate.commit();
    }

    public static void main(String[] args) {
        //1. 创建spring运行环境对象
        ApplicationContext application = new ClassPathXmlApplicationContext("classpath*:spring/applicationContext*.xml");
        //2. 获取当前类的实例化对象
        ImportItemToSolr importItemToSolr = (ImportItemToSolr)application.getBean("importItemToSolr");
        //3. 调用当前类的方法
        importItemToSolr.importItem();
    }
}
