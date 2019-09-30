package dc.pay.mapper.admin;

import dc.pay.admin.common.node.ZTreeNode;
import dc.pay.entity.admin.Dept;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 部门表 Mapper 接口
 * </p>
 *
 */
public interface DeptMapper extends Mapper<Dept> {
    /**
     * 获取ztree的节点列表
     * @return
     */
    List<ZTreeNode> tree();

    List<Map<String, Object>> list(@Param("condition") String condition);
}