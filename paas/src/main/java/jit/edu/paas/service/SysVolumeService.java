package jit.edu.paas.service;

import com.baomidou.mybatisplus.service.IService;
import jit.edu.paas.domain.entity.SysVolume;
import jit.edu.paas.domain.vo.ResultVo;

public interface SysVolumeService extends IService<SysVolume> {
    /**
     *
     * @author jitwxs
     * @since 2018/7/4 17:33
     */
    SysVolume getById(String id);

    SysVolume getByName(String name);

     /**
     * 获取挂载列表
     */
    ResultVo listByContainerId(String containerId, String uid);

    /**
     * 查看挂载信息
     */
    ResultVo inspectVolumes(String id, String uid);

    /**
     * 获取本地所有数据卷
     * @author jitwxs
     * @since 2018/7/5 13:03
     */
    ResultVo listFromLocal();

    /**
     * 清理无效数据卷
     * @author jitwxs
     * @since 2018/7/5 13:03
     */
    ResultVo cleanVolumes();
}
