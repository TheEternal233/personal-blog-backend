package xyz.kuailemao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.kuailemao.domain.dto.PermissionDTO;
import xyz.kuailemao.domain.entity.*;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.domain.vo.PermissionMenuVO;
import xyz.kuailemao.domain.vo.PermissionVO;
import xyz.kuailemao.mapper.*;
import xyz.kuailemao.service.PermissionService;
import xyz.kuailemao.utils.RedisCache;
import xyz.kuailemao.utils.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * (Permission)表服务实现类
 *
 * @author kuailemao
 * @since 2023-12-05 19:53:31
 */
@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {

    @Resource
    private PermissionMapper permissionMapper;

    @Resource
    private RolePermissionMapper rolePermissionMapper;

    @Resource
    private MenuMapper menuMapper;

    @Resource
    private UserRoleMapper userRoleMapper;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private RedisCache redisCache;
    // ========== 请求级缓存：同一次HTTP请求内只查一次权限 ==========
    private static final ThreadLocal<Map<Long, List<String>>> REQUEST_CACHE =
            ThreadLocal.withInitial(HashMap::new);
    // ========== Redis Key 常量 ==========
    private static final String USER_PERMISSION_KEY = "user:permission:";
    private static final long PERMISSION_TTL = 30;
    @Override
    public List<PermissionVO> selectPermission(String permissionDesc, String permissionKey, Long permissionMenuId) {
        LambdaQueryWrapper<Permission> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(Objects.nonNull(permissionDesc), Permission::getPermissionDesc, permissionDesc)
                .like(Objects.nonNull(permissionKey), Permission::getPermissionKey, permissionKey)
                .eq(Objects.nonNull(permissionMenuId), Permission::getMenuId, permissionMenuId);
        List<Permission> permissions = permissionMapper.selectList(wrapper);

        if (!permissions.isEmpty()) {
            List<Menu> menus = menuMapper.selectBatchIds(permissions.stream().map(Permission::getMenuId).toList());
            return permissions.stream().map(permission -> permission.asViewObject(PermissionVO.class, v -> {
                Optional<Menu> menu = menus.stream().filter(m -> m.getId().equals(permission.getMenuId())).findFirst();
                menu.ifPresent(m -> v.setMenuName(m.getTitle()));
            })).toList();
        }
        return new ArrayList<>();
    }

    @Override
    public List<PermissionMenuVO> selectPermissionMenu() {
        List<Permission> permissions = permissionMapper.selectList(null);
        if (!permissions.isEmpty()) {
            List<Menu> menus = menuMapper.selectBatchIds(permissions.stream().map(Permission::getMenuId).toList());
            List<PermissionMenuVO> vos = permissions.stream().map(permission -> permission.asViewObject(PermissionMenuVO.class, v -> {
                Optional<Menu> menu = menus.stream().filter(m -> m.getId().equals(permission.getMenuId())).findFirst();
                menu.ifPresent(m -> {
                    v.setMenuName(m.getTitle());
                    v.setMenuId(m.getId());
                });
            })).toList();
            return vos.stream()
                    .collect(Collectors.collectingAndThen(
                            Collectors.toMap(PermissionMenuVO::getMenuId, v -> v, (v1, v2) -> v1),
                            map -> new ArrayList<>(map.values())
                    ));
        }
        return new ArrayList<>();
    }

    @Transactional
    @Override
    public ResponseResult<Void> updateOrInsertPermission(PermissionDTO permissionDTO) {
        // 1. 先判空，防止 NPE
        String permKey = permissionDTO.getPermissionKey();
        if (StringUtils.isBlank(permKey)) {
            return ResponseResult.failure("权限字符不能为空");
        }

        // 2. 安全地 trim
        Permission isPermission = permissionMapper.selectOne(
                new LambdaQueryWrapper<Permission>()
                        .eq(Permission::getPermissionKey, permKey.trim())
        );

        // 3. 判重时也要判空（防止 isPermission 为 null 时 getId() NPE）
        if (isPermission != null && !isPermission.getId().equals(permissionDTO.getId())) {
            return ResponseResult.failure("权限字符不可重复");
        }

        Permission permission = permissionDTO.asViewObject(Permission.class, v -> {
            v.setMenuId(permissionDTO.getPermissionMenuId());
            v.setId(permissionDTO.getId());  // 确保 null 保持 null，防止 saveOrUpdate 误判
        });

        if (this.saveOrUpdate(permission)) {
            return ResponseResult.success();
        }
        return ResponseResult.failure();
    }

    @Override
    public PermissionDTO getPermission(Long id) {
        Permission permission = getById(id);
        return permission.asViewObject(PermissionDTO.class, v -> v.setPermissionMenuId(permission.getMenuId()));
    }

    @Transactional
    @Override
    public ResponseResult<Void> deletePermission(Long id) {
        if (permissionMapper.deleteById(id) > 0) {
            // 删除关系
            rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getPermissionId, id));
            return ResponseResult.success();
        }
        return ResponseResult.failure();
    }
    // ========== 用户权限缓存实现 ==========

    @Override
    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public List<String> getUserPermissions(Long userId) {
        // 1. 先查请求级缓存（同一次请求内）
        Map<Long, List<String>> reqCache = REQUEST_CACHE.get();
        if (reqCache.containsKey(userId)) {
            return reqCache.get(userId);
        }

        String cacheKey = USER_PERMISSION_KEY + userId;

        // 2. 查 Redis
        Object obj = redisCache.getCacheList(cacheKey);
        List<String> cached = null;
        if (obj instanceof List<?>) {
            cached = (List<String>) obj;
        }
        if (cached != null) {
            reqCache.put(userId, cached);  // 放入请求级缓存
            return cached;
        }

        // 3. Redis没有，查数据库
        List<String> permissions = loadPermissionsFromDb(userId);

        // 4. 写入 Redis（空值也缓存）
        if (permissions == null || permissions.isEmpty()) {
            List<String> empty = new ArrayList<>();  // 可变的空列表
            redisCache.setCacheList(cacheKey, empty);
            redisCache.expire(cacheKey, 5, TimeUnit.MINUTES);
            reqCache.put(userId, empty);
            return empty; // 空值短过期
        } else {
            redisCache.setCacheList(cacheKey, permissions);
            redisCache.expire(cacheKey, PERMISSION_TTL, TimeUnit.MINUTES);
        }

        // 5. 放入请求级缓存
        reqCache.put(userId, permissions);
        return permissions;
    }

    // 清理请求级缓存（在拦截器中调用）
    public static void clearRequestCache() {
        REQUEST_CACHE.remove();
    }
    /**
     * 从数据库加载权限（对应日志里的查询链）
     */
    protected  List<String> loadPermissionsFromDb(Long userId) {
        // 1. 查用户角色 sys_user_role
        List<UserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getUserId, userId)
        );

        if (userRoles.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> roleIds = userRoles.stream()
                .map(UserRole::getRoleId)
                .collect(Collectors.toList());

        // 2. 查角色（过滤禁用/删除的）
        List<Role> roles = roleMapper.selectList(
                new LambdaQueryWrapper<Role>()
                        .in(Role::getId, roleIds)
                        .eq(Role::getIsDeleted, 0)
                        .eq(Role::getStatus, 0)
        );

        if (roles.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> validRoleIds = roles.stream()
                .map(Role::getId)
                .collect(Collectors.toList());

        // 3. 查角色权限关联 sys_role_permission
        List<RolePermission> rolePermissions = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermission>()
                        .in(RolePermission::getRoleId, validRoleIds)
        );

        if (rolePermissions.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> permissionIds = rolePermissions.stream()
                .map(RolePermission::getPermissionId)
                .distinct()
                .collect(Collectors.toList());

        // 4. 查权限表 sys_permission
        List<Permission> permissions = permissionMapper.selectList(
                new LambdaQueryWrapper<Permission>()
                        .in(Permission::getId, permissionIds)
                        .eq(Permission::getIsDeleted, 0)
        );

        // 返回权限字符列表
        return permissions.stream()
                .map(Permission::getPermissionKey)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public void evictUserPermissionCache(Long userId) {
        redisCache.deleteObject(USER_PERMISSION_KEY + userId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void evictAllPermissionCache() {
        // 用 keys 查找所有 user:permission:* 的 key
        // TODO: 生产环境需改用 SCAN 命令，避免 KEYS 阻塞 Redis
        Collection<String> keys = redisCache.keys(USER_PERMISSION_KEY + "*");
        if (keys != null && !keys.isEmpty()) {
            redisCache.deleteObject(keys);
        }
    }
}
