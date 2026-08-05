package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.constant.MessageConstant;
import com.sky.entity.AddressBook;
import com.sky.exception.AddressBookBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.service.AddressBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * 地址簿业务层
 */
@Service
@Slf4j
public class AddressBookServiceImpl implements AddressBookService {
    @Autowired
    private AddressBookMapper addressBookMapper;

    /**
     * 条件查询
     *
     * @param addressBook
     * @return
     */
    public List<AddressBook> list(AddressBook addressBook) {
        return addressBookMapper.list(addressBook);
    }

    /**
     * 新增地址
     *
     * @param addressBook
     */
    public void save(AddressBook addressBook) {
        // 入参校验：收货人和手机号不能为空
        if (addressBook.getConsignee() == null || addressBook.getConsignee().trim().isEmpty()
                || addressBook.getPhone() == null || addressBook.getPhone().trim().isEmpty()) {
            throw new AddressBookBusinessException("收货人和手机号不能为空");
        }
        addressBook.setUserId(BaseContext.getCurrentId());
        addressBook.setIsDefault(0);
        addressBookMapper.insert(addressBook);
        log.info("新增地址成功：userId={}, addressBookId={}", addressBook.getUserId(), addressBook.getId());
    }

    /**
     * 根据id查询
     *
     * @param id
     * @return
     */
    public AddressBook getById(Long id) {
        AddressBook addressBook = addressBookMapper.getById(id);
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_NOT_FOUND);
        }
        // 越权校验：只能查询自己的地址
        if (!addressBook.getUserId().equals(BaseContext.getCurrentId())) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_NOT_BELONG_TO_USER);
        }
        return addressBook;
    }

    /**
     * 根据id修改地址
     *
     * @param addressBook
     */
    public void update(AddressBook addressBook) {
        // 越权校验：只能修改自己的地址
        checkBelongToCurrentUser(addressBook.getId());
        addressBookMapper.update(addressBook);
    }

    /**
     * 设置默认地址
     *
     * @param addressBook
     */
    @Transactional
    public void setDefault(AddressBook addressBook) {
        // 越权校验：只能将地址簿中的地址设为默认
        checkBelongToCurrentUser(addressBook.getId());

        //1、将当前用户的所有地址修改为非默认地址 update address_book set is_default = ? where user_id = ?
        addressBook.setIsDefault(0);
        addressBook.setUserId(BaseContext.getCurrentId());
        addressBookMapper.updateIsDefaultByUserId(addressBook);

        //2、将当前地址改为默认地址 update address_book set is_default = ? where id = ?
        addressBook.setIsDefault(1);
        addressBookMapper.update(addressBook);
    }

    /**
     * 根据id删除地址
     *
     * @param id
     */
    public void deleteById(Long id) {
        // 越权校验：只能删除自己的地址
        checkBelongToCurrentUser(id);
        addressBookMapper.deleteById(id);
    }

    /**
     * 校验地址是否存在且属于当前登录用户
     *
     * @param id 地址id
     */
    private void checkBelongToCurrentUser(Long id) {
        if (id == null) {
            throw new AddressBookBusinessException(MessageConstant.PARAM_ERROR);
        }
        AddressBook addressBook = addressBookMapper.getById(id);
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_NOT_FOUND);
        }
        if (!addressBook.getUserId().equals(BaseContext.getCurrentId())) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_NOT_BELONG_TO_USER);
        }
    }

}
