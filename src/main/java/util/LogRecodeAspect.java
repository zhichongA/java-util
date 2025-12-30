package util;





import cn.hutool.core.util.ArrayUtil;
import cn.hutool.json.JSONUtil;
import com.google.common.collect.Maps;
//import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


/**
 * @Author: zhichong
 * @Date: 2023/7/24 18:24
 * @Description: LogRecodeAspect
 * @Version 1.0.0
 */
@Aspect
@Component
@Slf4j
public class LogRecodeAspect {
    @Value("${configuration.isEnableAopLog:false}")
    private Boolean isEnableAopLog;

    @Value("${spring.application.name:notHave}")
    private String springApplicationName;


    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Pointcut("execution(public * com.controller.*.*(..))")
    public void controllerAspectse() {
    }


    @Around(value = "controllerAspectse()")
    public Object controllerAspectse(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        Object result = proceedingJoinPoint.proceed();
        if(isEnableAopLog){
            result = recordLog(proceedingJoinPoint);
        }
        return result;
    }
    public Object recordLog(ProceedingJoinPoint point)  throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = (HttpServletRequest) Objects.requireNonNull(attributes).getRequest();
        String methodName = point.getSignature().getName();
        Object result = point.proceed();
        if(methodName.startsWith("update")||methodName.startsWith("save")){
            // 打印请求相关参数
            long startTime = System.currentTimeMillis();
            String traceId = request.getAttribute("trace_id").toString();
//            final ControllerAspectseLog l = ControllerAspectseLog.builder()
//                    .ip(getIp(request))
//                    .url(request.getRequestURL().toString())
//                    .httpMethod(request.getMethod())
//                    .requestParams(getNameAndValue(point))
//                    .result(result)
//                    .timeCost(System.currentTimeMillis() - startTime)
//                    .traceId(traceId)
//                    .springApplicationName(springApplicationName)
//                    .build();
//            uploadData(l);
        }

        return result;
    }


//    /**
//     * 数据上传
//     * @param l
//     */
//    private void uploadData(ControllerAspectseLog l) {
//        String  uuid = UUID.randomUUID().toString().replace("-", "").toUpperCase();
//        String key = RedisConstant.REDIS_CONFIGURATION_AOP_LOG +uuid;
//        redisTemplate.opsForValue().setIfAbsent(key, l, 10, TimeUnit.MINUTES);
//    }

    private Map<String, Object> getNameAndValue(ProceedingJoinPoint joinPoint) {
        final Signature signature = joinPoint.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;
        final String[] names = methodSignature.getParameterNames();
        final Object[] args = joinPoint.getArgs();

        if (ArrayUtil.isEmpty(names) || ArrayUtil.isEmpty(args)) {
            return Collections.emptyMap();
        }
        if (names.length != args.length) {
            log.warn("{}方法参数名和参数值数量不一致", methodSignature.getName());
            return Collections.emptyMap();
        }
        Map<String, Object> map = Maps.newHashMap();
        for (int i = 0; i < names.length; i++) {
            map.put(names[i], args[i]);
        }
        return map;
    }

    private static final String UNKNOWN = "unknown";

    public static String getIp(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        String comma = ",";
        String localhost = "127.0.0.1";
        if (ip.contains(comma)) {
            ip = ip.split(",")[0];
        }
        if (localhost.equals(ip)) {
            // 获取本机真正的ip地址
            try {
                ip = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                log.error(e.getMessage(), e);
            }
        }
        return ip;
    }

}

