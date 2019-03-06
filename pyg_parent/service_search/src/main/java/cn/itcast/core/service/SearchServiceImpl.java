package cn.itcast.core.service;

import cn.itcast.core.common.Constants;
import cn.itcast.core.pojo.item.Item;
import com.alibaba.dubbo.config.annotation.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.*;
import org.springframework.data.solr.core.query.result.*;

import java.util.*;


@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private SolrTemplate solrTemplate;

    @Autowired
    private RedisTemplate redisTemplate;

//    @Override
//    public Map<String, Object> search(Map paramMap) {
//        /**
//         * 获取从页面传入的查询参数
//         */
//        //获取查询关键字
//        String keywords = String.valueOf(paramMap.get("keywords"));
//        //获取当前页
//        Integer pageNo = Integer.parseInt(String.valueOf(paramMap.get("pageNo")));
//        //获取每页查询条数
//        Integer pageSize = Integer.parseInt(String.valueOf(paramMap.get("pageSize")));
//
//        /**
//         * 创建查询对象
//         */
//        Query query = new SimpleQuery();
//
//        /**
//         * 创建查询条件对象
//         * contains和is区别?
//         * contains方法: 这个方法有点类似于sql语句中的like模糊查询的功能, 将关键字当成一个整体进行模糊查询
//         * is方法: 会根据当前使用的分词器对关键字进行切分词, 然后根据切分出来的词再进行一个一个查询.
//         */
//        Criteria criteria = new Criteria("item_keywords").is(keywords);
//        query.addCriteria(criteria);
//
//        /**
//         * 设置分页
//         */
//        //设置从第几条开始查询
//        if (pageNo == null || pageNo <= 0) {
//            pageNo = 1;
//        }
//        if (pageSize == null || pageSize <= 0) {
//            pageSize = 40;
//        }
//        Integer start = (pageNo - 1) * pageSize;
//        query.setOffset(start);
//        //设置每页查询多少条数据
//        query.setRows(pageSize);
//
//        /**
//         * 查询并得到结果
//         */
//        ScoredPage<Item> items = solrTemplate.queryForPage(query, Item.class);
//
//        /**
//         * 封装返回的数据
//         */
//        Map<String, Object> resultMap = new HashMap<>();
//        //查询到的结果集
//        resultMap.put("rows", items.getContent());
//        //查询到的总页数
//        resultMap.put("totalPages", items.getTotalPages());
//        //查询到的总记录条数
//        resultMap.put("total", items.getTotalElements());
//        return resultMap;
//    }

    @Override
    public Map<String, Object> search(Map paramMap) {
        //获取页面上用户点击的分类
        String categoryName = String.valueOf(paramMap.get("category"));

        //1. 根据关键字高亮分页查询
        Map<String, Object> resultMap = hightPageSearch(paramMap);

        //2. 根据关键字到solr索引库中分组查询分类, 分组的目的是给分类名称去重
        List<String> categoryNameList = findGroupPageCategoryName(paramMap);
        resultMap.put("categoryList", categoryNameList);

        //3. 根据分类名称到redis中查询对应的模板id, 根据模板id查询对应的品牌集合和规格集合
        if (categoryName == null || "".equals(categoryName)) {
            //如果消费者没有在页面上点击分类, 那么默认根据分类集合中的第一个分类查询对应的品牌集合和规格集合
            if (categoryNameList != null && categoryNameList.size() > 0) {
                categoryName = categoryNameList.get(0);
                Map<String, Object> brandAndSpecList = findBrandsAndSpecsByCategoryName(categoryName);
                resultMap.putAll(brandAndSpecList);
            }
        } else {
            //如果消费者在页面上点击分类, 则根据这个分类名称查询对应的品牌和规格集合
            Map<String, Object> brandAndSpecList = findBrandsAndSpecsByCategoryName(categoryName);
            resultMap.putAll(brandAndSpecList);
        }
        return resultMap;
    }

    /**
     * 根据关键字高亮分页查询
     * @param paramMap  查询参数
     * @return
     */
    public Map<String, Object> hightPageSearch(Map paramMap) {
        /**
         * 获取从页面传入的查询参数
         */
        //获取查询关键字
        String keywords = String.valueOf(paramMap.get("keywords"));
        if (keywords != null) {
            keywords = keywords.replaceAll(" ", "");
        }
        //获取当前页
        Integer pageNo = Integer.parseInt(String.valueOf(paramMap.get("pageNo")));
        //获取每页查询条数
        Integer pageSize = Integer.parseInt(String.valueOf(paramMap.get("pageSize")));
        //获取用户点击的品牌
        String brand = String.valueOf(paramMap.get("brand"));
        //获取用户点击的分类
        String category = String.valueOf(paramMap.get("category"));
        //获取用户点击的规格
        Map<String, String> specMap = (Map<String, String>)paramMap.get("spec");
        //获取用户点击的价格区间
        String priceStr = String.valueOf(paramMap.get("price"));
        //获取排序字段
        String sortField = String.valueOf(paramMap.get("sortField"));
        //获取排序方式
        String sortType = String.valueOf(paramMap.get("sort"));

        /**
         * 创建查询对象
         */
        HighlightQuery query = new SimpleHighlightQuery();

        /**
         * 创建查询条件对象
         * contains和is区别?
         * contains方法: 这个方法有点类似于sql语句中的like模糊查询的功能, 将关键字当成一个整体进行模糊查询
         * is方法: 会根据当前使用的分词器对关键字进行切分词, 然后根据切分出来的词再进行一个一个查询.
         */
        Criteria criteria = new Criteria("item_keywords").is(keywords);
        query.addCriteria(criteria);

        /**
         * 设置分页
         */
        //设置从第几条开始查询
        if (pageNo == null || pageNo <= 0) {
            pageNo = 1;
        }
        if (pageSize == null || pageSize <= 0) {
            pageSize = 40;
        }
        Integer start = (pageNo - 1) * pageSize;
        query.setOffset(start);
        //设置每页查询多少条数据
        query.setRows(pageSize);

        /**
         * 设置高亮选项
         */
        //创建高亮选项对象
        HighlightOptions options = new HighlightOptions();
        //设置需要高亮显示的域名
        options.addField("item_title");
        //设置高亮起始标签
        options.setSimplePrefix("<em style=\"color:red\">");
        //设置高亮结束标签
        options.setSimplePostfix("</em>");
        //将高亮选项对象放入查询对象中
        query.setHighlightOptions(options);

        /**
         * 设置过滤查询
         */
        //根据分类过滤
        if (category != null && !"".equals(category)) {
            //创建过滤查询对象
            FilterQuery filterQuery = new SimpleFilterQuery();
            //创建过滤条件对象
            Criteria filterCriteria = new Criteria("item_category").is(category);
            //将条件对象放入过滤对象中
            filterQuery.addCriteria(filterCriteria);
            //将过滤对象放入查询对象中
            query.addFilterQuery(filterQuery);
        }

        //根据品牌过滤
        if (brand != null && !"".equals(brand)) {
            //创建过滤查询对象
            FilterQuery filterQuery = new SimpleFilterQuery();
            //创建过滤条件对象
            Criteria filterCriteria = new Criteria("item_brand").is(brand);
            //将条件对象放入过滤对象中
            filterQuery.addCriteria(filterCriteria);
            //将过滤对象放入查询对象中
            query.addFilterQuery(filterQuery);
        }

        //根据规格过滤
        if (specMap != null && specMap.size() > 0) {
            Set<Map.Entry<String, String>> entries = specMap.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                //创建过滤查询对象
                FilterQuery filterQuery = new SimpleFilterQuery();
                //创建过滤条件对象
                Criteria filterCriteria = new Criteria("item_spec_" + entry.getKey()).is(entry.getValue());
                //将条件对象放入过滤对象中
                filterQuery.addCriteria(filterCriteria);
                //将过滤对象放入查询对象中
                query.addFilterQuery(filterQuery);
            }
        }

        //根据价格过滤查询
        if (priceStr != null && !"".equals(priceStr)) {
            String[] split = priceStr.split("-");
            if (split.length == 2) {
                //设置大于等于最小值
                if (!"0".equals(split[0])) {
                    //创建过滤查询对象
                    FilterQuery filterQuery = new SimpleFilterQuery();
                    //创建过滤条件对象
                    Criteria filterCriteria = new Criteria("item_price").greaterThanEqual(split[0]);
                    //将条件对象放入过滤对象中
                    filterQuery.addCriteria(filterCriteria);
                    //将过滤对象放入查询对象中
                    query.addFilterQuery(filterQuery);
                }
                //设置小于等于最大值
                if (!"*".equals(split[1])) {
                    //创建过滤查询对象
                    FilterQuery filterQuery = new SimpleFilterQuery();
                    //创建过滤条件对象
                    Criteria filterCriteria = new Criteria("item_price").lessThanEqual(split[1]);
                    //将条件对象放入过滤对象中
                    filterQuery.addCriteria(filterCriteria);
                    //将过滤对象放入查询对象中
                    query.addFilterQuery(filterQuery);
                }
            }
        }

        /**
         * 设置排序
         */
        if (sortField != null && !"".equals(sortField)) {
            //升序
            if ("ASC".equals(sortType)) {
                //创建排序对象, 第一个参数:排序方式, 第二个参数: 排序域名
                Sort sort = new Sort(Sort.Direction.ASC, "item_" + sortField);
                query.addSort(sort);
            }
            //降序
            if ("DESC".equals(sortType)) {
                Sort sort = new Sort(Sort.Direction.DESC, "item_" + sortField);
                query.addSort(sort);
            }
        }

        /**
         * 查询并得到结果
         */
        HighlightPage<Item> items = solrTemplate.queryForHighlightPage(query, Item.class);

        //获取带高亮标题的结果集
        List<HighlightEntry<Item>> highlighted = items.getHighlighted();
        //遍历集合
        List<Item> itemList = new ArrayList<>();
        if (highlighted != null) {
            for (HighlightEntry<Item> itemHighlightEntry : highlighted) {
                //获取不带高亮的完整的查询到的item实体对象
                Item item = itemHighlightEntry.getEntity();
                //获取高亮标题
                List<HighlightEntry.Highlight> highlights = itemHighlightEntry.getHighlights();
                if (highlights != null && highlights.size() > 0) {
                    if (highlights.get(0).getSnipplets() != null && highlights.get(0).getSnipplets().size() > 0) {
                        //这个就是获取到的高亮标题
                        String hightTitle = highlights.get(0).getSnipplets().get(0);
                        item.setTitle(hightTitle);
                    }
                }
                itemList.add(item);
            }
        }


        /**
         * 封装返回的数据
         */
        Map<String, Object> resultMap = new HashMap<>();
        //查询到的结果集
        resultMap.put("rows", itemList);
        //查询到的总页数
        resultMap.put("totalPages", items.getTotalPages());
        //查询到的总记录条数
        resultMap.put("total", items.getTotalElements());
        return resultMap;
    }

    /**
     * 分组分页查询, 目的是根据关键字获取分类名称, 分组的目的是为了给获取到的分类名称去重
     * @param paramMap  页面传入的查询条件
     * @return
     */
    public List<String> findGroupPageCategoryName(Map paramMap) {
        /**
         * 获取从页面传入的查询参数
         */
        //获取查询关键字
        String keywords = String.valueOf(paramMap.get("keywords"));
        if (keywords != null) {
            keywords = keywords.replaceAll(" ", "");
        }

        /**
         * 创建查询对象
         */
        Query query = new SimpleQuery();

        /**
         * 创建查询条件对象
         * contains和is区别?
         * contains方法: 这个方法有点类似于sql语句中的like模糊查询的功能, 将关键字当成一个整体进行模糊查询
         * is方法: 会根据当前使用的分词器对关键字进行切分词, 然后根据切分出来的词再进行一个一个查询.
         */
        Criteria criteria = new Criteria("item_keywords").is(keywords);
        query.addCriteria(criteria);

        /**
         * 设置分组
         */
        //创建分组选项对象
        GroupOptions options = new GroupOptions();
        //设置根据分类域进行分组
        options.addGroupByField("item_category");
        //将分组选项放入查询对象中
        query.setGroupOptions(options);

        /**
         * 查询并获取结果
         */
        GroupPage<Item> items = solrTemplate.queryForGroupPage(query, Item.class);
        //指定获取分组后的结果集
        GroupResult<Item> item_category = items.getGroupResult("item_category");

        List<String> categoryNameList = new ArrayList<>();
        Page<GroupEntry<Item>> groupEntries = item_category.getGroupEntries();
        List<GroupEntry<Item>> content = groupEntries.getContent();
        if (content != null) {
            for (GroupEntry<Item> itemGroupEntry : content) {
                //获取到分组后去重的分类名称
                String categoryName = itemGroupEntry.getGroupValue();
                categoryNameList.add(categoryName);
            }
        }


        return categoryNameList;
    }

    /**
     * 根据分类名称, 查询对应的模板id, 根据模板id查询对应的品牌集合和规格集合数据返回
     * @param categoryName   分类名称
     * @return
     */
    public Map<String, Object> findBrandsAndSpecsByCategoryName(String categoryName) {
        //1. 根据分类名称到redis中查询对应的模板id
        Long templateId = (Long) redisTemplate.boundHashOps(Constants.REDIS_CATEGORY_LIST).get(categoryName);
        //2. 根据模板id到redis中获取对应的品牌集合数据
        List<Map> brandList = (List<Map>)redisTemplate.boundHashOps(Constants.REDIS_BRAND_LIST).get(templateId);
        //3. 根据模板id到redis中获取对应的规格集合数据
        List<Map> specList = (List<Map>)redisTemplate.boundHashOps(Constants.REDIS_SPEC_LIST).get(templateId);
        //4. 将获取到的品牌集合和规格集合数据封装到Map中返回
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("brandList", brandList);
        resultMap.put("specList", specList);
        return resultMap;
    }
}
