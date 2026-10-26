package com.example.demo.Service;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.ibatis.annotations.Param;
import org.bouncycastle.crypto.RuntimeCryptoException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Mapper.UserMapper;
import com.example.demo.pojo.User;
import com.example.demo.utils.JwtUtils;
import com.example.demo.utils.PasswordHashUtils;
import com.example.demo.utils.RedisData;
import com.example.demo.utils.isPhoneLegal;

import ch.qos.logback.core.testUtil.RandomUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.lettuce.core.json.JsonObject;
import jakarta.servlet.http.HttpSession;


@Service
public class UserServiceImpl implements UserService{

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public String login(User user,String Phone,String code) {
        //校验手机号
        if(!isPhoneLegal.isLegalPhone(Phone)){
            return null;
        }

        //获取发送的验证码
        String codeString = stringRedisTemplate.opsForValue().get("user:" + Phone);

        //校验验证码
        if(!code.equals(codeString)){
            //验证码错误，返回
            return null;
        }

        //验证码正确，查询用户是否存在
        User newUser = userMapper.getUserByPhone(Phone);
        if (newUser != null && !"USER".equals(newUser.getRole())) {
            return null;
        }
        //用户不存在，创建用户，存入redis
        if(newUser == null){
            newUser = new User();
            newUser.setPhone(Phone);
            newUser.setUsername("user_" + Phone.substring(7)); // 给个默认用户名
            newUser.setStatus(1);
            newUser.setRole("USER");
            newUser.setCreateTime(new Date());
            if (userMapper.insert(newUser) != 1) {
                throw new IllegalStateException("用户注册失败");
            }
        }
        if (!Integer.valueOf(1).equals(newUser.getStatus())) {
            return null;
        }
        String token = issueToken(newUser);
        stringRedisTemplate.delete("user:" + Phone);
        return token;
    }

    @Override
    public String adminLogin(String username, String password) {
        if (StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
            return null;
        }

        User admin = userMapper.getAdminByUsername(username.trim());
        if (admin == null
                || !Integer.valueOf(1).equals(admin.getStatus())
                || !PasswordHashUtils.matches(password, admin.getPassword())) {
            return null;
        }

        return issueToken(admin);
    }

    private String issueToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId().toString());
        claims.put("role", user.getRole());
        Map<String, Object> newClaims = new HashMap<>(claims);

        String tokenString = JwtUtils.generateJwt(claims);
        stringRedisTemplate.opsForHash().putAll("token:" + tokenString, newClaims);
        stringRedisTemplate.expire("token:" + tokenString, 30, TimeUnit.MINUTES);
        return tokenString;
    }

    @Override
    public String sendCode(String Phone,HttpSession session){
        //校验手机号(需要用到正则表达式匹配，需要自行实现工具类)
        if(!isPhoneLegal.isLegalPhone(Phone)){
            //不符合规范，返回
            return "手机号错误";
        }

        //符合手机号规范，生成验证码(随机数字生成，目前没有搞这个东西)
        String code = cn.hutool.core.util.RandomUtil.randomString(6);

        //发送验证码(用第三方平台进行，目前没有实现)

        //将验证码存入Redis
        stringRedisTemplate.opsForValue().set("user:" + Phone,code,2,TimeUnit.MINUTES);

        return code;
    }

    @Override
    public ArrayList<User> getAllUser()
    {
        ArrayList<User> userList = userMapper.getAlluser();
        return userList;
    }

    @Override
    public User getUserById(Long id){
        //利用redis缓存来查询
        User user = queryWithBreakDown(id);

        return user;

    }

    public User queryWithBreakDown(Long id){

        //1.查询redis
        String userStr = stringRedisTemplate.opsForValue().get("cache:user" + id.toString());

        //redis存在，将其转化成user对象返回
        if(StrUtil.isNotBlank(userStr)){
            User user = JSONUtil.toBean(userStr, User.class);

            return user;
        }
        //看是否命中空值
        if(userStr != null){

            return null;
        }

        //redis没有命中，尝试获取互斥锁
        boolean getLock = tryGetLock("lock");

        if(getLock){
            //获取成功，开始重建缓存
            //redis不存在，查询数据库   
            User userByid = userMapper.getUserByid(id);

            //查询到结果，将其转化为对应的存储类型存储到redis
            if(userByid == null){
                stringRedisTemplate.opsForValue().set("cache:null","",5,TimeUnit.MINUTES);
                unlock("lock");

                return null;
            }

            String userJsonString = JSONUtil.toJsonStr(userByid);
            stringRedisTemplate.opsForValue().set("cache:user" + id,userJsonString,30,TimeUnit.MINUTES);
            unlock("lock");

            return userByid;
        }else{
            //没有获取到互斥锁，等待一段时间后再进行查询缓存
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new RuntimeCryptoException();
            }
            return queryWithBreakDown(id);
        }

    }

    private static final ExecutorService executorService = Executors.newFixedThreadPool(8);

    public User queryWithLogicExpireTime(Long id){
        //1.查询redis
        String userStr = stringRedisTemplate.opsForValue().get("cache:user" + id.toString());

        //redis不存在，直接返回null
        if(StrUtil.isBlank(userStr)){

            return null;
        }

        //redis命中，检查是否过期
        
        RedisData redisData = JSONUtil.toBean(userStr, RedisData.class);
        User user = JSONUtil.toBean((JSONObject)redisData.getData(), User.class);

        if(LocalDateTime.now().isAfter(redisData.getExpireTime())){
            //没有过期直接返回数据
            return user;
        }

        //过期了，需要缓存重建,获取互斥锁
        boolean islock = tryGetLock("lock:user" + id.toString());
        if(islock){
            executorService.submit(()->{
                try {
                    this.redisUserData(id);
                } catch (Exception e) {
                    e.printStackTrace();
                }finally{
                    unlock("cache:user");
                }
            });
        }


        return user;

    }

    @Override
    public User updateUserById(Long id,User user)
    {
        if (id == null || user == null) {
            throw new IllegalArgumentException("用户信息不能为空");
        }
        if (user.getUsername() == null && user.getNickname() == null && user.getPhone() == null) {
            throw new IllegalArgumentException("没有可更新的用户资料");
        }
        user.setId(id);
        user.setPassword(null);
        user.setRole(null);
        user.setStatus(null);
        user.setCreateTime(null);
        if (userMapper.updateUserById(user) != 1) {
            throw new IllegalStateException("用户不存在或资料未更新");
        }

        User userUpdated = userMapper.getUserByid(id);

        stringRedisTemplate.delete("cache:user" + id.toString());

        return userUpdated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAccountStatus(Long id, Integer status) {
        if (id == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (!Integer.valueOf(0).equals(status) && !Integer.valueOf(1).equals(status)) {
            throw new IllegalArgumentException("账号状态只能设置为0或1");
        }

        User account = userMapper.getAccountById(id);
        if (account == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (status.equals(account.getStatus())) {
            return;
        }
        if (Integer.valueOf(0).equals(status)
                && "ADMIN".equals(account.getRole())
                && Integer.valueOf(1).equals(account.getStatus())
                && userMapper.getActiveAdminIdsForUpdate().size() <= 1) {
            throw new IllegalStateException("不能禁用最后一个启用状态的管理员");
        }
        if (userMapper.updateAccountStatus(id, status) != 1) {
            throw new IllegalStateException("账号状态更新失败");
        }
        stringRedisTemplate.delete("cache:user" + id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        if (userId == null) {
            throw new IllegalArgumentException("当前用户未登录");
        }
        if (StrUtil.isBlank(oldPassword) || StrUtil.isBlank(newPassword)) {
            throw new IllegalArgumentException("旧密码和新密码不能为空");
        }
        if (newPassword.length() < 8) {
            throw new IllegalArgumentException("新密码长度不能少于8位");
        }

        String oldPasswordHash = userMapper.getPasswordHashById(userId);
        if (oldPasswordHash == null) {
            throw new IllegalArgumentException("用户不存在或当前账号未设置密码");
        }
        if (!PasswordHashUtils.matches(oldPassword, oldPasswordHash)) {
            throw new IllegalArgumentException("旧密码错误");
        }
        if (PasswordHashUtils.matches(newPassword, oldPasswordHash)) {
            throw new IllegalArgumentException("新密码不能与旧密码相同");
        }

        String newPasswordHash = PasswordHashUtils.encode(newPassword);
        if (userMapper.updatePassword(userId, oldPasswordHash, newPasswordHash) != 1) {
            throw new IllegalStateException("密码状态已变化，请重新操作");
        }
    }

    private boolean tryGetLock(String key){

        Boolean isTried = stringRedisTemplate.opsForValue().setIfAbsent(key, "success", 10, TimeUnit.MINUTES);

        return BooleanUtil.isTrue(isTried);

    }

    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }
    
    private RedisData redisUserData(Long id){

        User user = userMapper.getUserByid(id);
        
        RedisData redisUser = new RedisData();
        redisUser.setExpireTime(LocalDateTime.now().plusSeconds(10));
        redisUser.setData(user);

        String redisJsonStr = JSONUtil.toJsonStr(redisUser);;

        stringRedisTemplate.opsForValue().set("cache:user:" + id.toString(),redisJsonStr);

        return redisUser;
    }
}
