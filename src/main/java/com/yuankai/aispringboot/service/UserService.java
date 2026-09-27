package com.yuankai.aispringboot.service;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuankai.aispringboot.DTO.command.UserLoginCommandDTO;
import com.yuankai.aispringboot.DTO.command.UserRegisterCommandDTO;
import com.yuankai.aispringboot.common.ResultCode;
import com.yuankai.aispringboot.DTO.response.UserLoginResponseDTO;
import com.yuankai.aispringboot.entity.User;
import com.yuankai.aispringboot.enumclass.UserType;
import com.yuankai.aispringboot.exception.BusinessException;
import com.yuankai.aispringboot.mapper.UserMapper;
import com.yuankai.aispringboot.service.convert.UserConvert;
import com.yuankai.aispringboot.util.JwtTokenUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import static com.yuankai.aispringboot.util.JwtTokenUtil.generateToken;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Resource
    private UserMapper userMapper;

    @Resource
    private RedisTokenBlacklist redisTokenBlacklist;

    private final BCryptPasswordEncoder PasswordEncoder = new BCryptPasswordEncoder();

    public UserLoginResponseDTO login(UserLoginCommandDTO commandDTO) {
        // 构建查询条件
        LambdaQueryWrapper<User> userquery = new LambdaQueryWrapper<>();
        userquery.eq(User::getUsername, commandDTO.getUsername())
                .or()
                .eq(User::getEmail, commandDTO.getUsername());
        // 调用MP API查询
        User user = userMapper.selectOne(userquery);

        log.debug("查询用户: {}", user);
        // 判断用户是否存在
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST.getCode(), ResultCode.USER_NOT_EXIST.getMsg());
        }

        // 验证密码
        String inputPassword = commandDTO.getPassword().trim();
        // 防御：库中密码若非BCrypt格式（脏数据/手动改库），matches会抛异常导致500，统一按密码错误处理
        if (user.getPassword() == null || !user.getPassword().startsWith("$2")) {
            throw new BusinessException("密码错误");
        }
        if (!PasswordEncoder.matches(inputPassword, user.getPassword())) {
            throw new BusinessException("密码错误");
        }

        // 检查用户状态
        if (!user.isActive()) {
            throw new BusinessException("用户已禁用");
        }

        // 生成token
        String token = generateToken(user.getId(), user.getUsername(), user.getUserType());
        log.info("用户 {} 登录成功", user.getUsername());
        UserLoginResponseDTO.UserDetailResponseDTO userInfo = UserConvert.entityToDetailResponse(user);
        return UserConvert.entityToDetailResponse(token, userInfo);
    }

    public UserLoginResponseDTO.UserDetailResponseDTO register(UserRegisterCommandDTO commandDTO) {
        log.info("用户注册: {}", commandDTO.getUsername());
        // 验证密码是否一致
        if (!commandDTO.getPassword().equals(commandDTO.getConfirmPassword())) {
            throw new BusinessException("两次输入密码不一致");
        }

        // 检查用户名是否存在
        LambdaQueryWrapper<User> usernamequery = new LambdaQueryWrapper<>();
        usernamequery.eq(User::getUsername, commandDTO.getUsername());
        if (userMapper.selectCount(usernamequery) > 0) {
            throw new BusinessException(ResultCode.ACCOUNT_SAME.getCode(), ResultCode.ACCOUNT_SAME.getMsg());
        }

        // 检查邮箱是否存在
        LambdaQueryWrapper<User> emailquery = new LambdaQueryWrapper<>();
        emailquery.eq(User::getEmail, commandDTO.getEmail());
        if (userMapper.selectCount(emailquery) > 0) {
            throw new BusinessException("邮箱已存在");
        }

        // 安全校验：注册接口只允许创建普通用户，禁止通过注册创建管理员
        Integer regType = commandDTO.getUserType() == null ? UserType.USER.getCode() : commandDTO.getUserType();
        if (!UserType.USER.getCode().equals(regType)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "注册仅支持普通用户类型");
        }

        // 创建用户
        String password = commandDTO.getPassword().trim();
        String encodedPassword = PasswordEncoder.encode(password);
        User user = UserConvert.registerCommandToEntity(commandDTO, encodedPassword);

        // 输入数据库
        userMapper.insert(user);

        return UserConvert.entityToDetailResponse(user);
    }

    public UserLoginResponseDTO.UserDetailResponseDTO getUserById(Long id) {
        log.info("用户查询: {}", id);
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST.getCode(), ResultCode.USER_NOT_EXIST.getMsg());
        }
        return UserConvert.entityToDetailResponse(user);
    }

    public void logout(Long userId) {
        log.info("用户 {} 登出", userId);
        // 获取当前 Token
        String token = JwtTokenUtil.getCurrentToken();
        if (token != null) {
            // 解析 Token 过期时间，计算剩余有效期（防止redis内存无限增大）
            DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
            long expireMillis = jwt.getExpiresAt().getTime() - System.currentTimeMillis();
            // 将 Token 加入 Redis 黑名单
            redisTokenBlacklist.addToBlacklist(token, expireMillis);
        }
    }
}
