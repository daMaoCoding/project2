package dc.pay.admin.modular.system.factory;

import dc.pay.admin.modular.system.transfer.UserDto;
import dc.pay.entity.admin.User;
import org.springframework.beans.BeanUtils;

/**
 * 用户创建工厂
 */
public class UserFactory {

    public static User createUser(UserDto userDto){
        if(userDto == null){
            return null;
        }else{
            User user = new User();
            BeanUtils.copyProperties(userDto,user);
            return user;
        }
    }
}
