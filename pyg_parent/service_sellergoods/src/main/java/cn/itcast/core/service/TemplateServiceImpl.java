package cn.itcast.core.service;

import cn.itcast.core.common.Constants;
import cn.itcast.core.dao.specification.SpecificationOptionDao;
import cn.itcast.core.dao.template.TypeTemplateDao;
import cn.itcast.core.pojo.entity.PageResult;
import cn.itcast.core.pojo.specification.SpecificationOption;
import cn.itcast.core.pojo.specification.SpecificationOptionQuery;
import cn.itcast.core.pojo.template.TypeTemplate;
import cn.itcast.core.pojo.template.TypeTemplateQuery;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional
public class TemplateServiceImpl implements TemplateService {

    @Autowired
    private TypeTemplateDao templateDao;

    @Autowired
    private SpecificationOptionDao optionDao;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public PageResult findPage(Integer page, Integer rows, TypeTemplate template) {
        /**
         * 查询模板所有数据, 以模板id作为key, 对应的品牌集合作为value存入Redis中
         * 查询模板所有数据, 以模板id作为key, 对应的规格集合作为value存入Redis中
         */
        List<TypeTemplate> templates = templateDao.selectByExample(null);
        if (templates != null) {
            for (TypeTemplate typeTemplate : templates) {
                //根据模板id作为key, 品牌集合作为value存入reids
                String brandJsonStr = typeTemplate.getBrandIds();
                List<Map> brandList = JSON.parseArray(brandJsonStr, Map.class);
                redisTemplate.boundHashOps(Constants.REDIS_BRAND_LIST).put(typeTemplate.getId(), brandList);

                //模板id作为key, 规格集合作为value存入redis
                List<Map> specList = findBySpecList(typeTemplate.getId());
                redisTemplate.boundHashOps(Constants.REDIS_SPEC_LIST).put(typeTemplate.getId(), specList);
            }
        }


        /**
         * 分页查询
         */
        TypeTemplateQuery query = new TypeTemplateQuery();
        TypeTemplateQuery.Criteria criteria = query.createCriteria();
        if (template != null) {
            if (template.getName() != null && !"".equals(template.getName())) {
                criteria.andNameLike("%"+template.getName()+"%");
            }
        }

        PageHelper.startPage(page, rows);
        Page<TypeTemplate> templateList = (Page<TypeTemplate>)templateDao.selectByExample(query);
        return new PageResult(templateList.getTotal(), templateList.getResult());
    }

    @Override
    public void add(TypeTemplate template) {
        templateDao.insertSelective(template);
    }

    @Override
    public TypeTemplate findOne(Long id) {
        return templateDao.selectByPrimaryKey(id);
    }

    @Override
    public void update(TypeTemplate template) {
        templateDao.updateByPrimaryKeySelective(template);
    }

    @Override
    public void delete(Long[] ids) {
        if (ids != null) {
            for (Long id : ids) {
                templateDao.deleteByPrimaryKey(id);
            }
        }
    }

    @Override
    public List<Map> findBySpecList(Long id) {
        //1. 根据模板id查询模板对象
        TypeTemplate typeTemplate = templateDao.selectByPrimaryKey(id);
        //2. 从模板对象中获取规格的json字符串
        String jsonStr = typeTemplate.getSpecIds();
        //3. 解析规格json字符串
        List<Map> maps = JSON.parseArray(jsonStr, Map.class);

        //4. 遍历规格集合
        if (maps != null) {
            for (Map map : maps) {
                //5. 遍历过程中, 根据规格id, 查询对应的规格选项集合
                Long specId = Long.parseLong(String.valueOf(map.get("id")));
                SpecificationOptionQuery query = new SpecificationOptionQuery();
                SpecificationOptionQuery.Criteria criteria = query.createCriteria();
                criteria.andSpecIdEqualTo(specId);
                List<SpecificationOption> optionList = optionDao.selectByExample(query);
                //6. 将规格选项集合, 封装回规格集合中
                map.put("options", optionList);

            }
        }
        return maps;
    }
}
