package cn.itcast.core.service;

import cn.itcast.core.pojo.address.Address;
import cn.itcast.core.pojo.entity.BuyerCart;

import java.util.List;

public interface AddressService {

    public List<Address> findAddressListByUserName(String userName);
}
