package cn.itcast.test;

import cn.itcast.core.dao.good.BrandDao;
import cn.itcast.core.pojo.good.Brand;
import cn.itcast.core.pojo.good.BrandQuery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath*:spring/applicationContext*.xml"})
public class TestDao {

    @Autowired
    private BrandDao brandDao;

    @Test
    public void testFindBrandOne() {
        Brand brand = brandDao.selectByPrimaryKey(1L);
        System.out.println("=====" + brand);
    }

    @Test
    public void testFindAll() {
        List<Brand> brands = brandDao.selectByExample(null);
        System.out.println("=========" + brands);
    }

    /**
     * 复杂查询
     * select distinct id,name from tb_brand order by id desc
     */
    @Test
    public void testFindByQuery() {
        //创建查询对象
        BrandQuery query = new BrandQuery();
        //设置去重, 如果不设置默认为false
        query.setDistinct(true);
        //设置需要查询的列名, 如果不设置默认是*
        //query.setFields("id, name");
        //设置根据id倒序排序
        query.setOrderByClause("id desc");
        //创建where条件对象
        BrandQuery.Criteria criteria = query.createCriteria();
        //根据名称模糊查询
        criteria.andNameLike("%联%");
        //根据首字母精确查询
        criteria.andFirstCharEqualTo("L");

        //查询并返回结果
        List<Brand> brands = brandDao.selectByExample(query);
        System.out.println("=======" + brands);
    }
}
