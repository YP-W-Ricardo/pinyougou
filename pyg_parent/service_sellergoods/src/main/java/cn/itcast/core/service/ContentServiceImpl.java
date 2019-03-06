package cn.itcast.core.service;

import cn.itcast.core.common.Constants;
import cn.itcast.core.dao.ad.ContentDao;
import cn.itcast.core.pojo.ad.Content;
import cn.itcast.core.pojo.ad.ContentCategoryQuery;
import cn.itcast.core.pojo.ad.ContentQuery;
import cn.itcast.core.pojo.entity.PageResult;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ContentServiceImpl implements ContentService {

    @Autowired
    private ContentDao contentDao;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public PageResult search(Integer page, Integer rows, Content content) {
        PageHelper.startPage(page, rows);
        Page<Content> contentList = (Page<Content>)contentDao.selectByExample(null);
        return new PageResult(contentList.getTotal(), contentList.getResult());
    }

    @Override
    public void add(Content content) {
        //1. 根据广告的分类id删除redis中对应的广告集合数据
        redisTemplate.boundHashOps(Constants.REDIS_CONTENT_LIST).delete(content.getCategoryId());
        //2. 将广告数据添加到数据库中
        contentDao.insertSelective(content);
    }

    @Override
    public Content findOne(Long id) {
        return contentDao.selectByPrimaryKey(id);
    }

    @Override
    public void update(Content content) {
        //1. 根据页面传入的广告对象的主键id, 到数据库中查询对应的广告对象
        Content oldContent = findOne(content.getId());
        //2. 根据数据库中查询到的广告对象中的分类id, 删除redis中对应的广告集合数据
        redisTemplate.boundHashOps(Constants.REDIS_CONTENT_LIST).delete(oldContent.getCategoryId());
        //3. 根据页面传入的新的广告对象中的分类id, 删除redis中对应的广告集合数据
        redisTemplate.boundHashOps(Constants.REDIS_CONTENT_LIST).delete(content.getCategoryId());
        //4. 将页面传入的新的广告对象保存到数据库中
        contentDao.updateByPrimaryKeySelective(content);
    }

    @Override
    public void delete(Long[] ids) {
        if (ids != null) {
            for (Long id : ids) {
                //1. 根据广告id到数据库中查询到广告对象
                Content content = findOne(id);
                //2. 根据广告对象中的分类id删除redis中的对应的广告集合数据
                redisTemplate.boundHashOps(Constants.REDIS_CONTENT_LIST).delete(content.getCategoryId());
                //3. 根据广告id删除数据库中对应的广告数据
                contentDao.deleteByPrimaryKey(id);
            }
        }
    }

    @Override
    public List<Content> findByCategoryId(Long categoryId) {
        ContentQuery query = new ContentQuery();
        ContentQuery.Criteria criteria = query.createCriteria();
        criteria.andCategoryIdEqualTo(categoryId);
        List<Content> contents = contentDao.selectByExample(query);
        return contents;
    }

    /**
     * 存入redis中广告数据的格式
     * Map<String, Map<广告分类id, List<Content>>>
     * @param categoryId
     * @return
     */
    @Override
    public List<Content> findByCategoryIdFromRedis(Long categoryId) {
        //1. 先从redis中根据分类id查询广告集合数据
        List<Content> contentList = (List<Content>)redisTemplate.boundHashOps(Constants.REDIS_CONTENT_LIST).get(categoryId);
        //2. 判断是否有广告集合数据
        if (contentList != null && contentList.size() > 0) {
            //3. 如果有广告集合数据则直接返回
            return contentList;
        } else {
            //4. 如果没有广告集合数据则从数据库中查询, 查询到后放入redis中一份, 下回就可以直接从redis中查询到了
            contentList = findByCategoryId(categoryId);
            redisTemplate.boundHashOps(Constants.REDIS_CONTENT_LIST).put(categoryId, contentList);
            return contentList;
        }

    }
}
