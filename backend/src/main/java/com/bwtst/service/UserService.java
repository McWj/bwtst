package com.bwtst.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bwtst.common.BusinessException;
import com.bwtst.common.Md5Util;
import com.bwtst.entity.SysUser;
import com.bwtst.entity.SysUserRole;
import com.bwtst.entity.SysUserScope;
import com.bwtst.entity.Shop;
import com.bwtst.mapper.ShopMapper;
import com.bwtst.mapper.SysUserMapper;
import com.bwtst.mapper.SysUserRoleMapper;
import com.bwtst.mapper.SysUserScopeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// add by codebuddy 20260819
@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper roleMapper;
    private final SysUserScopeMapper scopeMapper;
    private final ShopMapper shopMapper;

    public IPage<SysUser> page(int page, int size, String keyword) {
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            w.like(SysUser::getUsername, keyword).or().like(SysUser::getRealName, keyword);
        }
        w.orderByDesc(SysUser::getId);
        IPage<SysUser> p = userMapper.selectPage(new Page<>(page, size), w);
        for (SysUser u : p.getRecords()) {
            fillRelation(u);
        }
        return p;
    }

    public SysUser detail(Integer id) {
        SysUser u = userMapper.selectById(id);
        if (u != null) {
            fillRelation(u);
        }
        return u;
    }

    private void fillRelation(SysUser u) {
        u.setRoles(roleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, u.getId())).stream()
                .map(SysUserRole::getRole).collect(Collectors.toList()));
        List<SysUserScope> scopes = scopeMapper.selectList(
                new LambdaQueryWrapper<SysUserScope>().eq(SysUserScope::getUserId, u.getId()));
        List<Integer> shopIds = new ArrayList<>();
        List<Integer> dcIds = new ArrayList<>();
        Integer defaultShopId = null;
        Integer defaultDcId = null;
        for (SysUserScope s : scopes) {
            if ("shop".equals(s.getOrgType())) {
                shopIds.add(s.getOrgId());
                if ("1".equals(s.getIsDefault())) {
                    defaultShopId = s.getOrgId();
                }
            } else if ("dc".equals(s.getOrgType())) {
                dcIds.add(s.getOrgId());
                if ("1".equals(s.getIsDefault())) {
                    defaultDcId = s.getOrgId();
                }
            }
        }
        u.setShopIds(shopIds);
        u.setDcIds(dcIds);
        u.setDefaultShopId(defaultShopId);
        u.setDefaultDcId(defaultDcId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void save(SysUser u) {
        if (!StringUtils.hasText(u.getUsername()) || !StringUtils.hasText(u.getRealName())) {
            throw new BusinessException("登录账户和用户名不能为空");
        }
        if (u.getId() == null) {
            if (!StringUtils.hasText(u.getPassword())) {
                throw new BusinessException("新增用户必须设置密码");
            }
            u.setPassword(Md5Util.md5(u.getPassword()));
            if (!StringUtils.hasText(u.getStatus())) {
                u.setStatus("1");
            }
            userMapper.insert(u);
        } else {
            SysUser exist = userMapper.selectById(u.getId());
            if (exist == null) {
                throw new BusinessException("用户不存在");
            }
            // add by codebuddy 20260819: 编辑用户信息时不修改密码，仅通过修改密码接口变更
            exist.setRealName(u.getRealName());
            exist.setStatus(u.getStatus());
            userMapper.updateById(exist);
        }
        // 维护角色与组织范围
        Integer userId = u.getId();
        roleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (u.getRoles() != null) {
            for (String role : u.getRoles()) {
                if (!isValidRole(role)) {
                    continue;
                }
                SysUserRole r = new SysUserRole();
                r.setUserId(userId);
                r.setRole(role);
                roleMapper.insert(r);
            }
        }
        scopeMapper.delete(new LambdaQueryWrapper<SysUserScope>().eq(SysUserScope::getUserId, userId));
        if (u.getShopIds() != null) {
            for (Integer shopId : u.getShopIds()) {
                if (shopId == null) {
                    continue;
                }
                SysUserScope s = new SysUserScope();
                s.setUserId(userId);
                s.setOrgType("shop");
                s.setOrgId(shopId);
                s.setIsDefault(u.getDefaultShopId() != null && u.getDefaultShopId().equals(shopId) ? "1" : "0");
                scopeMapper.insert(s);
            }
        }
        if (u.getDcIds() != null) {
            for (Integer dcId : u.getDcIds()) {
                if (dcId == null) {
                    continue;
                }
                SysUserScope s = new SysUserScope();
                s.setUserId(userId);
                s.setOrgType("dc");
                s.setOrgId(dcId);
                s.setIsDefault(u.getDefaultDcId() != null && u.getDefaultDcId().equals(dcId) ? "1" : "0");
                scopeMapper.insert(s);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id) {
        userMapper.deleteById(id);
        roleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
        scopeMapper.delete(new LambdaQueryWrapper<SysUserScope>().eq(SysUserScope::getUserId, id));
    }

    // add by codebuddy 20260819: 单独修改用户密码
    public void resetPassword(Integer id, String newPassword) {
        if (!StringUtils.hasText(newPassword)) {
            throw new BusinessException("新密码不能为空");
        }
        SysUser u = userMapper.selectById(id);
        if (u == null) {
            throw new BusinessException("用户不存在");
        }
        u.setPassword(Md5Util.md5(newPassword));
        userMapper.updateById(u);
    }

    public SysUser login(String username, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new BusinessException("请输入登录账户和密码");
        }
        SysUser u = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (u == null) {
            throw new BusinessException("账户或密码错误");
        }
        if (!"1".equals(u.getStatus())) {
            throw new BusinessException("账户已停用");
        }
        if (!Md5Util.md5(password).equals(u.getPassword())) {
            throw new BusinessException("账户或密码错误");
        }
        fillRelation(u);
        u.setPassword(null);
        return u;
    }

    private boolean isValidRole(String role) {
        return "hq".equals(role) || "dc".equals(role) || "shop".equals(role);
    }

    // add by codebuddy 20260819: 按门店编码批量生成门店用户
    @Transactional(rollbackFor = Exception.class)
    public String generateShopUsers() {
        List<Shop> shops = shopMapper.selectList(null);
        if (shops == null || shops.isEmpty()) {
            return "没有可生成的门店";
        }
        int created = 0;
        int skipped = 0;
        for (Shop shop : shops) {
            if (shop.getShopNo() == null) {
                continue;
            }
            String username = shop.getShopNo();
            SysUser exist = userMapper.selectOne(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
            if (exist != null) {
                skipped++;
                continue;
            }
            SysUser u = new SysUser();
            u.setUsername(username);
            u.setRealName(shop.getShopName());
            u.setPassword(Md5Util.md5(username));
            u.setStatus("1");
            userMapper.insert(u);
            Integer userId = u.getId();
            SysUserRole role = new SysUserRole();
            role.setUserId(userId);
            role.setRole("shop");
            roleMapper.insert(role);
            SysUserScope scope = new SysUserScope();
            scope.setUserId(userId);
            scope.setOrgType("shop");
            scope.setOrgId(shop.getId());
            scope.setIsDefault("1");
            scopeMapper.insert(scope);
            created++;
        }
        return "生成完成：新增 " + created + " 个门店用户，跳过已存在 " + skipped + " 个";
    }
}
