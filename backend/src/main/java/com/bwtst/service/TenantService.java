package com.bwtst.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bwtst.common.BusinessException;
import com.bwtst.common.PasswordUtil;
import com.bwtst.common.TenantContext;
import com.bwtst.common.TenantSaveRequest;
import com.bwtst.entity.SysTenant;
import com.bwtst.entity.SysUser;
import com.bwtst.entity.SysUserRole;
import com.bwtst.mapper.SysTenantMapper;
import com.bwtst.mapper.SysUserMapper;
import com.bwtst.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

// add by codebuddy 20260923: SaaS 租户管理——开通、启停、重置管理员密码，并初始化租户基础数据
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

    private final SysTenantMapper tenantMapper;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper roleMapper;
    private final SysConfigService configService;

    private static final String DEFAULT_ADMIN_USERNAME = "admin";

    public IPage<SysTenant> page(int page, int size, String keyword) {
        LambdaQueryWrapper<SysTenant> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            w.like(SysTenant::getTenantCode, keyword).or().like(SysTenant::getTenantName, keyword);
        }
        w.orderByAsc(SysTenant::getId);
        return tenantMapper.selectPage(new Page<>(page, size), w);
    }

    /** 开通租户：写租户表，并在该租户上下文内创建管理员与默认配置 */
    @Transactional(rollbackFor = Exception.class)
    public void create(TenantSaveRequest req) {
        if (!StringUtils.hasText(req.getTenantCode()) || !StringUtils.hasText(req.getTenantName())) {
            throw new BusinessException("企业编码与企业名称不能为空");
        }
        String code = req.getTenantCode().trim();
        Long exist = tenantMapper.selectCount(
                new LambdaQueryWrapper<SysTenant>().eq(SysTenant::getTenantCode, code));
        if (exist != null && exist > 0) {
            throw new BusinessException("企业编码已存在：" + code);
        }
        if (!StringUtils.hasText(req.getAdminPassword())) {
            throw new BusinessException("管理员初始密码不能为空");
        }
        String username = StringUtils.hasText(req.getAdminUsername())
                ? req.getAdminUsername().trim() : DEFAULT_ADMIN_USERNAME;

        SysTenant t = new SysTenant();
        t.setTenantCode(code);
        t.setTenantName(req.getTenantName().trim());
        t.setStatus("1");
        t.setErpUrl(req.getErpUrl());
        t.setErpDigiKey(req.getErpDigiKey());
        t.setErpProd(req.getErpProd());
        t.setErpAcct(req.getErpAcct());
        t.setErpEntId(req.getErpEntId());
        t.setCreateDatetime(LocalDateTime.now());
        tenantMapper.insert(t);

        Long tenantId = t.getId();
        String tenantName = t.getTenantName();
        // 切到新租户上下文，保证管理员、角色、配置都落到该租户名下
        inTenant(tenantId, () -> {
            SysUser u = new SysUser();
            u.setUsername(username);
            u.setRealName(tenantName + "管理员");
            u.setPassword(PasswordUtil.hash(req.getAdminPassword()));
            u.setStatus("1");
            u.setIsPlatformAdmin("0");
            userMapper.insert(u);

            SysUserRole r = new SysUserRole();
            r.setUserId(u.getId());
            r.setRole("hq");
            roleMapper.insert(r);

            configService.initDefaultConfigs();
        });
        log.info("已开通租户 {} (id={})，管理员账号 {}", code, tenantId, username);
    }

    public void update(TenantSaveRequest req) {
        if (req.getId() == null) {
            throw new BusinessException("租户 id 不能为空");
        }
        SysTenant exist = tenantMapper.selectById(req.getId());
        if (exist == null) {
            throw new BusinessException("租户不存在");
        }
        if (StringUtils.hasText(req.getTenantName())) {
            exist.setTenantName(req.getTenantName().trim());
        }
        exist.setErpUrl(req.getErpUrl());
        exist.setErpDigiKey(req.getErpDigiKey());
        exist.setErpProd(req.getErpProd());
        exist.setErpAcct(req.getErpAcct());
        exist.setErpEntId(req.getErpEntId());
        tenantMapper.updateById(exist);
    }

    public void setStatus(Long id, String status) {
        if (id == null) {
            throw new BusinessException("租户 id 不能为空");
        }
        if (!"0".equals(status) && !"1".equals(status)) {
            throw new BusinessException("状态值不合法");
        }
        SysTenant t = tenantMapper.selectById(id);
        if (t == null) {
            throw new BusinessException("租户不存在");
        }
        if ("0".equals(status) && id == TenantContext.getDefaultTenantId()) {
            throw new BusinessException("默认租户不允许停用");
        }
        t.setStatus(status);
        tenantMapper.updateById(t);
    }

    /** 重置指定租户下某账号的密码 */
    public void resetAdminPassword(Long tenantId, String username, String newPassword) {
        if (tenantId == null) {
            throw new BusinessException("租户 id 不能为空");
        }
        if (!StringUtils.hasText(newPassword)) {
            throw new BusinessException("新密码不能为空");
        }
        SysTenant t = tenantMapper.selectById(tenantId);
        if (t == null) {
            throw new BusinessException("租户不存在");
        }
        String name = StringUtils.hasText(username) ? username.trim() : DEFAULT_ADMIN_USERNAME;
        inTenant(tenantId, () -> {
            SysUser u = userMapper.selectOne(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, name));
            if (u == null) {
                throw new BusinessException("该租户下不存在账号：" + name);
            }
            SysUser upd = new SysUser();
            upd.setId(u.getId());
            upd.setPassword(PasswordUtil.hash(newPassword));
            userMapper.updateById(upd);
        });
    }

    /**
     * 临时切换到目标租户上下文执行。
     * 租户拦截器按当前上下文写 tenant_id，跨租户开通必须先切换，执行完恢复原上下文。
     */
    private void inTenant(Long tenantId, Runnable action) {
        long originTenant = TenantContext.getTenantId();
        Integer originUser = TenantContext.getUserId();
        String originName = TenantContext.getUsername();
        boolean originPa = TenantContext.isPlatformAdmin();
        TenantContext.set(tenantId, null, null);
        try {
            action.run();
        } finally {
            TenantContext.set(originTenant, originUser, originName);
            TenantContext.setPlatformAdmin(originPa);
        }
    }
}
