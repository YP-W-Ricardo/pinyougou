package cn.itcast.core.service;

import cn.itcast.core.common.Constants;
import cn.itcast.core.dao.item.ItemCatDao;
import cn.itcast.core.pojo.item.ItemCat;
import cn.itcast.core.pojo.item.ItemCatQuery;
import com.alibaba.dubbo.config.annotation.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

@Service
public class ItemCatServiceImpl implements ItemCatService {

    @Autowired
    private ItemCatDao itemCatDao;


    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public List<ItemCat> findByParentId(Long parentId) {
        /**
         * 查询分类所有数据, 缓存到redis中, 分类名称作为key, 模板id作为value
         */
        List<ItemCat> catList = itemCatDao.selectByExample(null);
        if (catList != null) {
            for (ItemCat itemCat : catList) {
                redisTemplate.boundHashOps(Constants.REDIS_CATEGORY_LIST).put(itemCat.getName(), itemCat.getTypeId());
            }
        }

        /**
         * 查询数据
         */
        ItemCatQuery query = new ItemCatQuery();
        ItemCatQuery.Criteria criteria = query.createCriteria();
        criteria.andParentIdEqualTo(parentId);
        List<ItemCat> itemCats = itemCatDao.selectByExample(query);
        return itemCats;
    }

    @Override
    public ItemCat findOne(Long id) {
        return itemCatDao.selectByPrimaryKey(id);
    }

    @Override
    public List<ItemCat> findAll() {
        return itemCatDao.selectByExample(null);
    }
}
