package xyz.kuailemao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import xyz.kuailemao.domain.entity.User;


/**
 * (User)表数据库访问层
 *
 * @author kuailemao
 * @since 2023-10-13 15:02:40
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    @Select("SELECT id FROM sys_user WHERE username = #{username} AND is_deleted = 0")
    Long selectIdByUsername(String username);
}
