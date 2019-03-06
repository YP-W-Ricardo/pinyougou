package cn.itcast.core.serivce;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.ArrayList;
import java.util.List;

/**
 * 赋予权限
 * cas和springSecurity整合后, 需要在cas服务器中完成登录, 登录后跳转回当前系统, 进入到这里
 * 由springSecurity负责赋予权限工作
 */
public class UserDetailServiceImpl implements UserDetailsService {


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        //创建权限集合
        List<GrantedAuthority> authList = new ArrayList<>();
        //向权限集合中加入权限
        authList.add(new SimpleGrantedAuthority("ROLE_USER"));
        return new User(username, "", authList);
    }
}
