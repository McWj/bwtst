package com.bwtst.common;

import com.bwtst.entity.SysUser;
import lombok.Data;

// add by codebuddy 20260923: 登录返回体——令牌 + 用户信息
@Data
public class LoginResult {

    /** 访问令牌，前端需在后续请求头 Authorization: Bearer <token> 中携带 */
    private String token;

    /** 当前登录用户（含角色与数据范围） */
    private SysUser user;

    /** 令牌有效期（分钟） */
    private long expireMinutes;

    public LoginResult(String token, SysUser user, long expireMinutes) {
        this.token = token;
        this.user = user;
        this.expireMinutes = expireMinutes;
    }
}
