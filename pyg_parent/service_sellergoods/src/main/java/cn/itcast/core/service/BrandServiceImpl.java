package cn.itcast.core.service;

import cn.itcast.core.dao.good.BrandDao;
import cn.itcast.core.pojo.entity.PageResult;
import cn.itcast.core.pojo.good.Brand;
import cn.itcast.core.pojo.good.BrandQuery;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

@Service
public class BrandServiceImpl implements BrandService {

    @Autowired
    private BrandDao brandDao;

    @Override
    public List<Brand> findAll() {
        return brandDao.selectByExample(null);
    }

    @Override
    public PageResult findPage(Brand brand, Integer page, Integer rows) {
        //创建查询对象
        BrandQuery query = new BrandQuery();
        if (brand != null) {
            //创建sql语句中的where查询条件对象
            BrandQuery.Criteria criteria = query.createCriteria();
            if (brand.getName() != null && !"".equals(brand.getName())) {
                criteria.andNameLike("%"+brand.getName()+"%");
            }
            if (brand.getFirstChar() != null && !"".equals(brand.getFirstChar())) {
                criteria.andFirstCharEqualTo(brand.getFirstChar());
            }
        }
        PageHelper.startPage(page, rows);
        Page<Brand> brandList = (Page<Brand>)brandDao.selectByExample(query);
        return new PageResult(brandList.getTotal(), brandList.getResult());
    }

    @Override
    public void add(Brand brand) {
        //直接添加, 不管brand品牌对象中的属性值为什么, 都进行添加
        //brandDao.insert(brand);
        //添加前先判断brand对象中的属性是否为null, 如果为null不参与拼接sql语句不参与添加
        brandDao.insertSelective(brand);
    }

    @Override
    public Brand findOne(Long id) {
        return brandDao.selectByPrimaryKey(id);
    }

    /**
     * update tb_brand set name=xxx, firstchar=xxx where name like 'xxx'
     * @param brand
     */
    @Override
    public void update(Brand brand) {
        //根据主键进行更新, 并且会判断传入的brand品牌对象中的参数是否为null, 如果为null不参与更新, 如果不为null进行更新
        brandDao.updateByPrimaryKeySelective(brand);
        //根据主键进行更新
        //brandDao.updateByPrimaryKey(brand);
        //根据查询条件对象进行更新. 条件是只非主键条件
        //brandDao.updateByExample(, );
        //根据查询条件对象进行更新. 条件是只非主键条件, 这里对于传入的更新对象brand中的各种属性进行判断, 如果有为null值的不参与更新
        //brandDao.updateByExampleSelective(, );
    }

    @Override
    public void delete(Long[] ids) {
        if (ids != null) {
            for (Long id : ids) {
                //根据主键删除
                brandDao.deleteByPrimaryKey(id);
                //根据查询条件对象删除, 这个条件是非主键条件
                //brandDao.deleteByExample();
            }
        }
    }

    @Override
    public List<Map> selectOptionList() {
        List<Map> list = brandDao.selectOptionList();
        return list;
    }
}
