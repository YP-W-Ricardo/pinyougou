package cn.itcast.core.service;

import cn.itcast.core.dao.item.ItemDao;
import cn.itcast.core.pojo.item.Item;
import cn.itcast.core.pojo.item.ItemQuery;
import cn.itcast.core.pojo.seller.SellerQuery;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SimpleQuery;

import java.util.List;
import java.util.Map;

@Service
public class SolrManagerServiceImpl implements SolrManagerService {

    @Autowired
    private SolrTemplate solrTemplate;

    @Autowired
    private ItemDao itemDao;


    @Override
    public void importItemToSolr(Long goodsId) {
        //1. 查询库存表所有已审核通过的数据
        ItemQuery query = new ItemQuery();
        ItemQuery.Criteria criteria = query.createCriteria();
        //审核通过的
        criteria.andStatusEqualTo("1");
        //根据商品id查询
        criteria.andGoodsIdEqualTo(goodsId);
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

    @Override
    public void deleteSolrByGoodsId(Long goodsId) {
        //创建查询对象
        Query query = new SimpleQuery();
        //创建条件对象
        Criteria criteria = new Criteria("item_goodsid").is(goodsId);
        //将条件对象加入到查询对象中
        query.addCriteria(criteria);
        //根据查询对象删除
        solrTemplate.delete(query);
        //提交
        solrTemplate.commit();
    }
}
