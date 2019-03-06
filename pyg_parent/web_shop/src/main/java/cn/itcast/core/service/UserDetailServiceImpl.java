package cn.itcast.core.service;

import cn.itcast.core.pojo.seller.Seller;
import com.alibaba.dubbo.config.annotation.Reference;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * 自定义实现类
 * 根据springSecurity传入的用户名到数据库获取对应的用户对象
 * 如果能够获取, 那么返回springSecurity要求的User对象
 */
public class UserDetailServiceImpl implements UserDetailsService {


    private SellerService sellerService;

    public void setSellerService(SellerService sellerService) {
        this.sellerService = sellerService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        //创建权限集合
        List<GrantedAuthority> authList = new ArrayList<GrantedAuthority>();
        //向权限集合中添加权限
        authList.add(new SimpleGrantedAuthority("ROLE_SELLER"));

        //1. 根据用户名到卖家表查询卖家对象
        Seller seller = sellerService.findOne(username);
        //2. 如果卖家对象不为空
        if (seller != null) {
            //3. 判断卖家是否审核已经通过
            if ("1".equals(seller.getStatus())) {
                //4. 如果卖家审核已经通过, 那么返回SpringSecurity的用户对象
                return new User(username, seller.getPassword(), authList);
            }
        }
        return null;
    }
}
