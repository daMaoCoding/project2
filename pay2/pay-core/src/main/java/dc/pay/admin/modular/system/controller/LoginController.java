package dc.pay.admin.modular.system.controller;

import com.google.code.kaptcha.Constants;
import dc.pay.admin.common.annotion.Permission;
import dc.pay.admin.common.constant.Const;
import dc.pay.admin.common.controller.BaseController;
import dc.pay.admin.common.exception.InvalidKaptchaException;
import dc.pay.admin.common.node.MenuNode;
import dc.pay.mapper.admin.MenuMapper;
import dc.pay.mapper.admin.UserMapper;
import dc.pay.entity.admin.User;
import dc.pay.admin.core.log.LogManager;
import dc.pay.admin.core.log.factory.LogTaskFactory;
import dc.pay.admin.core.shiro.ShiroKit;
import dc.pay.admin.core.shiro.ShiroUser;
import dc.pay.admin.core.util.ToolUtil;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

import static dc.pay.admin.core.support.HttpKit.getIp;

/**
 * 登录控制器
 */
@Controller
public class LoginController extends BaseController {

    @Autowired
    MenuMapper menuMapper;

    @Autowired
    UserMapper userMapper;

    /**
     * 跳转到主页
     */
    @RequestMapping(value ={"","/"}, method = RequestMethod.GET)
    public String index(Model model) {
        if(!ShiroKit.getSubject().isAuthenticated()){
            return "login.html";
        }
        return REDIRECT +  "/reqPayList";
    }


    /**
     * 管理页
     * @param model
     * @return
     */
    @RequestMapping(value = "/admin", method = RequestMethod.GET)
    @Permission(Const.ADMIN_NAME)
    public String admin(Model model) {
        //获取菜单列表
        List<Integer> roleList = ShiroKit.getUser().getRoleList();
        if(roleList == null || roleList.size() == 0){
            ShiroKit.getSubject().logout();
            model.addAttribute("tips", "该用户没有角色，无法登陆");
            return "login.html";
        }
        List<MenuNode> menus = menuMapper.getMenusByRoleIds(roleList);
        List<MenuNode> titles = MenuNode.buildTitle(menus);
        model.addAttribute("titles", titles);

        //获取用户头像
        Integer id = ShiroKit.getUser().getId();
        User user = userMapper.selectByPrimaryKey(id);
        String avatar = user.getAvatar();
        model.addAttribute("avatar", avatar);

        return "index.html";
    }






    /**
     * 跳转到登录页面
     */
    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String login() {
        if (ShiroKit.isAuthenticated() || ShiroKit.getUser() != null) {
            return REDIRECT + "/";
        } else {
            return "login.html";
        }
    }

    /**
     * 点击登录执行的动作
     */
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public String loginVali() {

        String username = super.getPara("username").trim();
        String password = super.getPara("password").trim();

        //验证验证码是否正确
        if(ToolUtil.getKaptchaOnOff()){
            String kaptcha = super.getPara("kaptcha").trim();
            String code = (String) super.getSession().getAttribute(Constants.KAPTCHA_SESSION_KEY);
            if(ToolUtil.isEmpty(kaptcha) || !kaptcha.equals(code)){
                throw new InvalidKaptchaException();
            }
        }

        Subject currentUser = ShiroKit.getSubject();
        UsernamePasswordToken token = new UsernamePasswordToken(username, password.toCharArray());
        token.setRememberMe(true);

        currentUser.login(token);

        ShiroUser shiroUser = ShiroKit.getUser();
        super.getSession().setAttribute("shiroUser", shiroUser);
        super.getSession().setAttribute("username", shiroUser.getAccount());

        LogManager.me().executeLog(LogTaskFactory.loginLog(shiroUser.getId(), getIp()));

        ShiroKit.getSession().setAttribute("sessionFlag",true);

        return REDIRECT + "/reqPayList";
    }

    /**
     * 退出登录
     */
    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    public String logOut() {
        LogManager.me().executeLog(LogTaskFactory.exitLog(ShiroKit.getUser().getId(), getIp()));
        ShiroKit.getSubject().logout();
        return REDIRECT + "/login";
    }
}
