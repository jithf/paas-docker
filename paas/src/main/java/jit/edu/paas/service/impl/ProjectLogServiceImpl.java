package jit.edu.paas.service.impl;

import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import jit.edu.paas.domain.entity.ProjectLog;
import jit.edu.paas.domain.enums.ProjectLogTypeEnum;
import jit.edu.paas.domain.enums.ResultEnum;
import jit.edu.paas.mapper.ProjectLogMapper;
import jit.edu.paas.service.ProjectLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * <p>
 * ProjectLog服务实现类
 * </p>
 *
 * @author jitwxs
 * @since 2018-06-27
 */
@Service
public class ProjectLogServiceImpl extends ServiceImpl<ProjectLogMapper, ProjectLog> implements ProjectLogService {
    @Autowired
    private ProjectLogMapper logMapper;

    @Override
    public void saveSuccessLog(String projectId, String containerId, ProjectLogTypeEnum projectLogTypeEnum) {
        ProjectLog log = new ProjectLog(projectId, containerId, projectLogTypeEnum.getCode(), projectLogTypeEnum.getMessage());
        logMapper.insert(log);
    }

    @Override
    public void saveErrorLog(String projectId, String containerId, ProjectLogTypeEnum projectLogTypeEnum, ResultEnum resultEnum) {
        String description = projectLogTypeEnum.getMessage() + "，原因：" + resultEnum.getMessage();
        ProjectLog log = new ProjectLog(projectId, containerId, projectLogTypeEnum.getCode(), description);
        logMapper.insert(log);
    }
}
