package com.qatang.admin.web.controller.profile;

import com.qatang.admin.entity.user.User;
import com.qatang.admin.service.user.UserService;
import com.qatang.admin.web.form.user.UserForm;
import com.qatang.admin.web.shiro.authentication.PasswordHelper;
import com.qatang.admin.web.validator.profile.ChangePasswordValidator;
import com.qatang.core.controller.BaseController;
import com.qatang.core.exception.ValidateFailedException;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.Date;

/**
 * @author qatang
 * @since 2014-12-20 13:41
 */
@Controller
@RequestMapping("/profile")
public class ProfileController extends BaseController {
    @Autowired
    private UserService userService;
    @Autowired
    private PasswordHelper passwordHelper;

    @Autowired
    private ChangePasswordValidator changePasswordValidator;

    @RequiresPermissions("user:profile:changePwd")
    @RequestMapping(value = "/password/change", method = RequestMethod.GET)
    public String updateInput(@ModelAttribute UserForm userForm, ModelMap modelMap) {
        User user = (User) SecurityUtils.getSubject().getPrincipal();
        if (user == null) {
            return "redirect:/signin";
        }
        user = userService.get(user.getId());
        if (user == null) {
            logger.error("查看个人信息失败：未查询到userId={}的用户", user.getId());
            return "redirect:/signin";
        }

        if (modelMap.containsKey(BINDING_RESULT_KEY)) {
            modelMap.addAttribute(BindingResult.class.getName().concat(".userForm"), modelMap.get(BINDING_RESULT_KEY));
        }

        userForm.setUser(user);
        modelMap.addAttribute(FORWARD_URL_KEY, "/profile/info");
        return "profile/password";
    }

    @RequiresPermissions("user:profile:changePwd")
    @RequestMapping(value = "/password/change", method = RequestMethod.POST)
    public String update(UserForm userForm, BindingResult result, RedirectAttributes redirectAttributes, ModelMap modelMap) {
        try {
            changePasswordValidator.validate(userForm);
        } catch (ValidateFailedException e) {
            logger.error(e.getMessage(), e);
            result.addError(new ObjectError(e.getField(), e.getMessage()));
        }

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute(BINDING_RESULT_KEY, result);
            redirectAttributes.addFlashAttribute(userForm);
            return "redirect:/profile/password/change";
        }
        User currentUser = (User) SecurityUtils.getSubject().getPrincipal();
        currentUser.setPassword(userForm.getNewPassword());
        passwordHelper.encryptPassword(currentUser);
        currentUser.setUpdatedTime(new Date());
        userService.update(currentUser);

        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_KEY, "修改密码成功！");
        redirectAttributes.addFlashAttribute(FORWARD_URL_KEY, "/profile/info");
        return "redirect:/success";
    }

    @RequiresPermissions("user:profile:view")
    @RequestMapping(value = "/info", method = RequestMethod.GET)
    public String view(ModelMap modelMap) {
        User user = (User) SecurityUtils.getSubject().getPrincipal();
        if (user == null) {
            return "redirect:/signin";
        }
        user = userService.get(user.getId());
        if (user == null) {
            logger.error("查看个人信息失败：未查询到userId={}的用户", user.getId());
            return "redirect:/signin";
        }

        UserForm userForm = new UserForm();
        userForm.setUser(user);
        modelMap.addAttribute(userForm);
        return "profile/detail";
    }
}
