package dc.pay.admin.modular.system.service;

/**
 * 菜单服务
 */
public interface IMenuService {

    /**
     * 删除菜单
     */
    void delMenu(Integer menuId);

    /**
     * 删除菜单包含所有子菜单
     */
    void delMenuContainSubMenus(Integer menuId);
}
