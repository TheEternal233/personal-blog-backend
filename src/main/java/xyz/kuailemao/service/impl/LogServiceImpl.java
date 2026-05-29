package xyz.kuailemao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.kuailemao.constants.RespConst;
import xyz.kuailemao.domain.dto.LogDTO;
import xyz.kuailemao.domain.dto.LogDeleteDTO;
import xyz.kuailemao.domain.entity.Log;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.domain.vo.LogVO;
import xyz.kuailemao.domain.vo.PageVO;
import xyz.kuailemao.mapper.LogMapper;
import xyz.kuailemao.service.LogService;
import xyz.kuailemao.utils.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * (Log)表服务实现类
 *
 * @author kuailemao
 * @since 2023-12-12 09:12:32
 */
@Service("logService")
public class LogServiceImpl extends ServiceImpl<LogMapper, Log> implements LogService {

    @Resource
    private LogMapper logMapper;

    @Override
    public PageVO searchLog(LogDTO logDTO, Long current, Long pageSize) {
        LambdaQueryWrapper<Log> wrapper = new LambdaQueryWrapper<>();
        if (Objects.nonNull(logDTO)) {
            wrapper.like(StringUtils.isNotEmpty(logDTO.getIp()),Log::getIp, logDTO.getIp())
                    .like(StringUtils.isNotEmpty(logDTO.getModule()),Log::getModule, logDTO.getModule())
                    .like(StringUtils.isNotEmpty(logDTO.getUserName()),Log::getUserName, logDTO.getUserName())
                    .like(StringUtils.isNotEmpty(logDTO.getOperation()),Log::getOperation, logDTO.getOperation())
                    .eq(StringUtils.isNotNull(logDTO.getState()),Log::getState, logDTO.getState());
            if (StringUtils.isNotNull(logDTO.getLogTimeStart()) && StringUtils.isNotNull(logDTO.getLogTimeEnd())) {
                wrapper.gt(Log::getCreateTime, logDTO.getLogTimeStart()).and(a -> a.lt(Log::getCreateTime, logDTO.getLogTimeEnd()));
            }
        }
        wrapper.orderByDesc(Log::getCreateTime);
        Page<Log> page = new Page<>(current, pageSize);
        logMapper.selectPage(page,wrapper);
        List<LogVO> logVOS = page.getRecords().stream().map(log -> log.asViewObject(LogVO.class, v -> v.setLoginTime(log.getCreateTime()))).toList();

        return PageVO.builder().page(logVOS).total(page.getTotal()).build();
    }

/**
 * 删除日志信息的方法
 * 使用@Transactional注解确保事务的完整性
 * @param logDeleteDTO 包含要删除的日志ID集合的数据传输对象
 * @return 返回操作结果，成功返回ResponseResult.success()，失败返回ResponseResult.failure()
 */
    @Transactional
    @Override
    public ResponseResult<Void> deleteLog(LogDeleteDTO logDeleteDTO) {
    // 调用removeByIds方法根据ID集合删除日志记录
        if (this.removeByIds(logDeleteDTO.getIds())) {
        // 如果删除成功，返回成功响应
            return ResponseResult.success();
        }
    // 如果删除失败，返回失败响应
        return ResponseResult.failure();
    }

}
