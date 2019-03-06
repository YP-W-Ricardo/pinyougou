package cn.itcast.core.controller;

import cn.itcast.core.pojo.entity.PageResult;
import cn.itcast.core.pojo.entity.Result;
import cn.itcast.core.pojo.good.Goods;
import cn.itcast.core.service.CmsService;
import cn.itcast.core.service.GoodsService;
import cn.itcast.core.service.SolrManagerService;
import com.alibaba.dubbo.config.annotation.Reference;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/goods")
public class GoodsController {

    @Reference
    private GoodsService goodsService;

//    @Reference
//    private SolrManagerService solrManagerService;
//
//    @Reference
//    private CmsService cmsService;

    @RequestMapping("/search")
    public PageResult search(Integer page, Integer rows, @RequestBody Goods goods) {
        String userName = SecurityContextHolder.getContext().getAuthentication().getName();
        goods.setSellerId(userName);
        return goodsService.search(page, rows, goods);
    }

    /**
     * 修改商品状态
     * @param ids   选中的商品id数组
     * @param status    状态码, 0为未审核, 1为审核通过, 2为驳回
     * @return
     */
    @RequestMapping("/updateStatus")
    public Result updateStatus(Long[] ids, String status) {
        try {
            //1. 改变数据库中商品的上架状态
            goodsService.updateStatus(ids, status);

            //判断审核通过则执行商品上架操作
//            if ("1".equals(status)) {
//                if (ids != null) {
//                    for (Long goodsId : ids) {
//                        //2. 将商品中的库存数据放入solr索引库供前台系统搜索使用
//                        solrManagerService.importItemToSolr(goodsId);
//
//                        //3. 根据商品id获取商品数据, 商品详情数据, 库存集合数据等然后根据模板生成静态化页面
//                        //获取模板中需要的数据
//                        Map<String, Object> rootMap = cmsService.findGoods(goodsId);
//                        //生成静态化页面
//                        cmsService.createStaticPage(goodsId, rootMap);
//                    }
//                }
//
//            }
            return new Result(true, "修改状态成功!");
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false, "修改状态失败!");
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
