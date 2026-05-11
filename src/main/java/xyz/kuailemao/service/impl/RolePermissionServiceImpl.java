package xyz.kuailemao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import xyz.kuailemao.domain.dto.RolePermissionDTO;
import xyz.kuailemao.domain.entity.Role;
import xyz.kuailemao.domain.entity.RolePermission;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.domain.vo.RoleAllVO;
import xyz.kuailemao.mapper.RoleMapper;
import xyz.kuailemao.mapper.RolePermissionMapper;
import xyz.kuailemao.service.RolePermissionService;

import java.util.List;

/**
 * (RolePermission)表服务实现类
 *
 * @author kuailemao
 * @since 2023-10-13 15:02:40
 */
@Service
public class RolePermissionServiceImpl extends ServiceImpl<RolePermissionMapper, RolePermission> implements RolePermissionService {

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private RolePermissionMapper rolePermissionMapper;

    /**
     * 根据权限ID查询角色（0=拥有该权限，1=未拥有该权限）
     */
    @Override
    public List<RoleAllVO> selectRoleByPermissionId(Long permissionId, String roleName, String roleKey, Integer type) {
        // 1. 先查询所有绑定了该权限的角色ID
        LambdaQueryWrapper<RolePermission> rpWrapper = new LambdaQueryWrapper<>();
        rpWrapper.eq(RolePermission::getPermissionId, permissionId);
        List<Long> boundRoleIds = rolePermissionMapper.selectList(rpWrapper).stream()
                .map(RolePermission::getRoleId)
                .toList();

        // 2. 构建角色查询条件
        LambdaQueryWrapper<Role> roleWrapper = new LambdaQueryWrapper<>();
        // 模糊查询角色名/角色标识
        roleWrapper.like(roleName != null, Role::getRoleName, roleName);
        roleWrapper.like(roleKey != null, Role::getRoleKey, roleKey);

        // 3. 根据类型筛选：0=已绑定，1=未绑定
        if (type == 0) {
            // 已拥有权限：IN 已绑定角色ID
            if (CollectionUtils.isEmpty(boundRoleIds)) {
                return List.of();
            }
            roleWrapper.in(Role::getId, boundRoleIds);
        } else {
            // 未拥有权限：NOT IN 已绑定角色ID
            roleWrapper.notIn(!CollectionUtils.isEmpty(boundRoleIds), Role::getId, boundRoleIds);
        }

        // 4. 一次性查询所有角色（替换原循环查库，性能大幅提升）
        List<Role> roleList = roleMapper.selectList(roleWrapper);

        // 5. 转换VO返回
        return roleList.stream()
                .map(role -> role.asViewObject(RoleAllVO.class))
                .toList();
    }

    /**
     * 批量添加角色权限关联
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResponseResult<Void> addRolePermission(RolePermissionDTO rolePermissionDTO) {
        List<Long> roleIds = rolePermissionDTO.getRoleId();
        List<Long> permissionIds = rolePermissionDTO.getPermissionId();

        // 空值校验
        if (CollectionUtils.isEmpty(roleIds) || CollectionUtils.isEmpty(permissionIds)) {
            return ResponseResult.failure("角色ID或权限ID不能为空");
        }

        // 1. 先删除原有关联（避免重复）
        LambdaQueryWrapper<RolePermission> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.in(RolePermission::getRoleId, roleIds)
                .in(RolePermission::getPermissionId, permissionIds);
        rolePermissionMapper.delete(delWrapper);

        // 2. 批量构建新关联
        List<RolePermission> rolePermissions = roleIds.stream()
                .flatMap(roleId -> permissionIds.stream()
                        .map(permissionId -> RolePermission.builder()
                                .roleId(roleId)
                                .permissionId(permissionId)
                                .build()))
                .toList();

        // 3. 批量保存
        boolean saveSuccess = saveBatch(rolePermissions);
        return saveSuccess ? ResponseResult.success() : ResponseResult.failure("保存失败");
    }

    /**
     * 删除角色权限关联
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResponseResult<Void> deleteRolePermission(RolePermissionDTO rolePermissionDTO) {
        List<Long> roleIds = rolePermissionDTO.getRoleId();
        List<Long> permissionIds = rolePermissionDTO.getPermissionId();

        // 空值校验
        if (CollectionUtils.isEmpty(roleIds) || CollectionUtils.isEmpty(permissionIds)) {
            return ResponseResult.failure("角色ID或权限ID不能为空");
        }

        LambdaQueryWrapper<RolePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(RolePermission::getRoleId, roleIds)
                .in(RolePermission::getPermissionId, permissionIds);

        int deleteCount = rolePermissionMapper.delete(wrapper);
        return deleteCount > 0 ? ResponseResult.success() : ResponseResult.failure("删除失败，无匹配数据");
    }
}