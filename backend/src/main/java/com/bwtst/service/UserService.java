package com.bwtst.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bwtst.common.BusinessException;
import com.bwtst.common.JwtUtil;
import com.bwtst.common.LoginResult;
import com.bwtst.common.PasswordUtil;
import com.bwtst.common.TenantContext;
import com.bwtst.entity.SysTenant;
import com.bwtst.entity.SysUser;
import com.bwtst.entity.SysUserRole;
import com.bwtst.entity.SysUserScope;
import com.bwtst.entity.Shop;
import com.bwtst.mapper.ShopMapper;
import com.bwtst.mapper.SysTenantMapper;
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
    // add by codebuddy 20260923: SaaS 登录令牌与租户
    private final JwtUtil jwtUtil;
    private final SysTenantMapper tenantMapper;

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
            u.setPassword(PasswordUtil.hash(u.getPassword()));
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
        u.setPassword(PasswordUtil.hash(newPassword));
        userMapper.updateById(u);
    }

    /**
     * 登录。
     * add by codebuddy 20260923: 改为多租户——先按企业编码定位租户，再在租户上下文内校验账号，成功后签发令牌
     */
    public LoginResult login(String username, String password, String tenantCode) {
        // add by codebuddy 20261014: 业务系统登录必须指定企业编码；
        // 平台管理端登录（4 参重载）允许留空并回落默认租户
        if (!StringUtils.hasText(tenantCode)) {
            throw new BusinessException("请输入企业编码");
        }
        return login(username, password, tenantCode, false);
    }

    /**
     * 登录。
     * add by codebuddy 20260923: requirePlatformAdmin=true 时仅允许平台超级管理员登录（平台管理端专用入口）
     */
    public LoginResult login(String username, String password, String tenantCode, boolean requirePlatformAdmin) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new BusinessException("请输入登录账户和密码");
        }
        long tenantId = resolveTenantId(tenantCode);
        TenantContext.set(tenantId, null, null);
        try {
            SysUser u = userMapper.selectOne(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
            if (u == null) {
                throw new BusinessException("账户或密码错误");
            }
            if (!"1".equals(u.getStatus())) {
                throw new BusinessException("账户已停用");
            }
            if (!PasswordUtil.matches(password, u.getPassword())) {
                throw new BusinessException("账户或密码错误");
            }
            // add by codebuddy 20260923: 历史 MD5 密码校验通过后自动升级为 PBKDF2，用户无感知
            if (PasswordUtil.needsUpgrade(u.getPassword())) {
                SysUser upgrade = new SysUser();
                upgrade.setId(u.getId());
                upgrade.setPassword(PasswordUtil.hash(password));
                userMapper.updateById(upgrade);
            }
            fillRelation(u);
            u.setPassword(null);
            boolean platformAdmin = "1".equals(u.getIsPlatformAdmin());
            // 平台管理端入口仅放行平台超管；校验放在密码校验之后，避免暴露账号是否存在
            if (requirePlatformAdmin && !platformAdmin) {
                throw new BusinessException("该账号无平台管理权限");
            }
            String token = jwtUtil.createToken(tenantId, u.getId(), u.getUsername(), platformAdmin);
            return new LoginResult(token, u, jwtUtil.getExpireMinutes());
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * 解析登录企业编码。
     * add by codebuddy 20260923: 未填写时，系统只有一个启用租户则自动使用，否则要求填写，兼容现有单租户登录习惯
     */
    private long resolveTenantId(String tenantCode) {
        if (StringUtils.hasText(tenantCode)) {
            SysTenant t = tenantMapper.selectOne(
                    new LambdaQueryWrapper<SysTenant>().eq(SysTenant::getTenantCode, tenantCode.trim()));
            if (t == null) {
                throw new BusinessException("企业编码不存在");
            }
            if (!"1".equals(t.getStatus())) {
                throw new BusinessException("该企业已停用");
            }
            return t.getId();
        }
        // add by codebuddy 20260923: 未填写企业编码时回落到默认租户，兼容原有单租户登录习惯
        long defaultId = TenantContext.getDefaultTenantId();
        SysTenant def = tenantMapper.selectById(defaultId);
        if (def == null) {
            // 默认租户不存在（如已被清理）时，仅有一个启用租户则直接使用
            List<SysTenant> enabled = tenantMapper.selectList(
                    new LambdaQueryWrapper<SysTenant>().eq(SysTenant::getStatus, "1"));
            if (enabled != null && enabled.size() == 1) {
                return enabled.get(0).getId();
            }
            throw new BusinessException("请填写企业编码");
        }
        if (!"1".equals(def.getStatus())) {
            throw new BusinessException("默认企业已停用，请填写企业编码");
        }
        return def.getId();
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
            u.setPassword(PasswordUtil.hash(username));
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
