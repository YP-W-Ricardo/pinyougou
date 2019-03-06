package cn.itcast.core.controller;

import cn.itcast.core.pojo.entity.GoodsEntity;
import cn.itcast.core.pojo.entity.PageResult;
import cn.itcast.core.pojo.entity.Result;
import cn.itcast.core.pojo.good.Goods;
import cn.itcast.core.service.GoodsService;
import cn.itcast.core.service.SolrManagerService;
import com.alibaba.dubbo.config.annotation.Reference;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/goods")
public class GoodsController {

    @Reference
    private GoodsService goodsService;

//    @Reference
//    private SolrManagerService solrManagerService;

    /**
     * 商品添加
     * @param goodsEntity
     * @return
     */
    @RequestMapping("/add")
    public Result add(@RequestBody GoodsEntity goodsEntity) {
        try {
            //获取当前登录用户的用户名
            String userName = SecurityContextHolder.getContext().getAuthentication().getName();
            goodsEntity.getGoods().setSellerId(userName);
            goodsService.add(goodsEntity);

            return new Result(true, "添加成功!");
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false, "添加失败!");
        }
    }

    /**
     * 商品分页查询
     * @param page  当前页
     * @param rows  每页展示多少条数据
     * @param goods 页面传入的查询条件对象
     */
    @RequestMapping("/search")
    public PageResult search(Integer page, Integer rows, @RequestBody Goods goods) {
        //获取当前登录用户的用户名
        String userName = SecurityContextHolder.getContext().getAuthentication().getName();
        goods.setSellerId(userName);
        PageResult pageResult = goodsService.search(page, rows, goods);
        return pageResult;
    }

    /**
     * 商品修改前查询回显数据
     * @param id
     * @return
     */
    @RequestMapping("/findOne")
    public GoodsEntity findOne(Long id) {
        GoodsEntity one = goodsService.findOne(id);
        return one;
    }

    /**
     * 保存修改
     * @param goodsEntity
     * @return
     */
    @RequestMapping("/update")
    public Result update(@RequestBody GoodsEntity goodsEntity) {

        try {
            //1. 获取当前登录用户的用户名
            String userName = SecurityContextHolder.getContext().getAuthentication().getName();
            //2. 判断当前商品是否为当前用户添加的商品, 如果不是不允许修改
            if (!goodsEntity.getGoods().getSellerId().equals(userName)) {
                return new Result(false, "您没有权限修改此商品!");
            }
            //3. 调用service执行修改
            goodsService.update(goodsEntity);
            return new Result(true, "修改成功!");
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false, "修改失败!");
        }

    }

    @RequestMapping("/delete")
    public Result delte(Long[] ids) {
        try {
            //1. 根据商品id到数据库中逻辑删除商品
            goodsService.delete(ids);
            //2. 根据商品id删除solr索引库中对应的数据
//            if (ids != null) {
//                for (Long goodsId : ids) {
//                    solrManagerService.deleteSolrByGoodsId(goodsId);
//                }
//            }
            return new Result(true, "删除成功!");
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false, "删除失败!");
        }
    }
}
