package cn.itcast.core.service;

import cn.itcast.core.dao.good.GoodsDao;
import cn.itcast.core.dao.good.GoodsDescDao;
import cn.itcast.core.dao.item.ItemCatDao;
import cn.itcast.core.dao.item.ItemDao;
import cn.itcast.core.pojo.good.Goods;
import cn.itcast.core.pojo.good.GoodsDesc;
import cn.itcast.core.pojo.item.Item;
import cn.itcast.core.pojo.item.ItemCat;
import cn.itcast.core.pojo.item.ItemQuery;
import com.alibaba.dubbo.config.annotation.Service;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import javax.servlet.ServletContext;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CmsServiceImpl implements CmsService , ServletContextAware{

    @Autowired
    private GoodsDao goodsDao;

    @Autowired
    private GoodsDescDao descDao;

    @Autowired
    private ItemDao itemDao;

    @Autowired
    private ItemCatDao catDao;

    @Autowired
    private FreeMarkerConfigurer freeMarkerConfigurer;


    private ServletContext servletContext;

    @Override
    public void createStaticPage(Long goodsId, Map<String, Object> rootMap) throws Exception{
        //1. 获取freemarker初始化对象
        Configuration configuration = freeMarkerConfigurer.getConfiguration();
        //2. 通过初始化对象获取模板对象, 并且指定模板的名称
        Template template = configuration.getTemplate("item.ftl");
        //3. 定义静态页面的名称   页面名称 = 商品id + .html
        String path = goodsId + ".html";
        //4. 通过页面相对路径转化成绝对路径(保存生成后的页面的位置),
        // 例如: goodsId.html 转换为D:\intellijWorkSpace2\pyg_parent\service_page\target\service_page\goodsId.html
        String url = getRealPath(path);
        //5. 定义输出流
        Writer out = new OutputStreamWriter(new FileOutputStream(new File(url)), "utf-8");
        //6. 生成静态化页面
        template.process(rootMap, out);
        //7. 关闭流
        out.close();
    }

    @Override
    public Map<String, Object> findGoods(Long goodsId) {
        Map<String, Object> rootMap = new HashMap<>();
        //1. 根据商品id获取商品对象
        Goods goods = goodsDao.selectByPrimaryKey(goodsId);
        //2. 根据商品id获取商品详情对象
        GoodsDesc goodsDesc = descDao.selectByPrimaryKey(goodsId);
        //3. 根据商品id获取库存集合对象
        ItemQuery query = new ItemQuery();
        ItemQuery.Criteria criteria = query.createCriteria();
        criteria.andGoodsIdEqualTo(goodsId);
        List<Item> items = itemDao.selectByExample(query);
        //4. 根据商品对象的分类id, 获取分类名称
        if (goods != null) {
            ItemCat itemCat1 = catDao.selectByPrimaryKey(goods.getCategory1Id());
            ItemCat itemCat2 = catDao.selectByPrimaryKey(goods.getCategory2Id());
            ItemCat itemCat3 = catDao.selectByPrimaryKey(goods.getCategory3Id());
            rootMap.put("itemCat1", itemCat1.getName());
            rootMap.put("itemCat2", itemCat2.getName());
            rootMap.put("itemCat3", itemCat3.getName());
        }

        //5. 将以上获取的对象封装到Map中返回
        rootMap.put("goods", goods);
        rootMap.put("goodsDesc", goodsDesc);
        rootMap.put("itemList", items);
        return rootMap;
    }

    /**
     * 相对路径转换成绝对路径
     * @param path  相对路径
     * @return
     */
    private String  getRealPath(String path) {
        String realPath = servletContext.getRealPath(path);
        return realPath;
    }

    /**
     * spring实例化了ServletContextAware接口, 并且实例化了里面的ServletContext对象
     * 所以这个类实现ServletContextAware接口目的是为了获取里面实例化过的ServletContext对象, 拿到这个对象后
     * 给当前类的全局变量ServletContext赋值, 也就相当于给本类中的全局变量ServletContext对象初始化
     * @param servletContext
     */
    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }
}
