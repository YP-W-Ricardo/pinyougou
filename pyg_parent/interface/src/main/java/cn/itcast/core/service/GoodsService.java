package cn.itcast.core.service;

import cn.itcast.core.pojo.entity.GoodsEntity;
import cn.itcast.core.pojo.entity.PageResult;
import cn.itcast.core.pojo.entity.Result;
import cn.itcast.core.pojo.good.Goods;


public interface GoodsService {

    public void add(GoodsEntity goodsEntity);

    public PageResult search(Integer page, Integer rows, Goods goods);

    public GoodsEntity findOne(Long id);

    public  void update(GoodsEntity goodsEntity);

    public void delete(Long[] ids);

    public void updateStatus(Long[] ids, String status);
}
