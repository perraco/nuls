/*
 *
 * MIT License
 *
 * Copyright (c) 2017-2018 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package io.nuls.core.utils.spring.lite.core;

import io.nuls.core.constant.ErrorCode;
import io.nuls.core.constant.ModuleStatusEnum;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.module.BaseModuleBootstrap;
import io.nuls.core.module.manager.ServiceManager;
import io.nuls.core.utils.spring.lite.core.interceptor.BeanMethodInterceptorManager;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

/**
 * @author Niels
 * @date 2018/1/31
 */
public class ModularServiceMethodInterceptor implements MethodInterceptor {

    private ThreadLocal<Integer> threadLocal = new ThreadLocal<>();

    @Override
    public Object intercept(Object obj, Method method, Object[] params, MethodProxy methodProxy) throws Throwable {
        threadLocal.set(0);
        Throwable throwable = null;
        while (threadLocal.get() < 10) {
            try {
                return this.doIntercept(obj, method, params, methodProxy);
            } catch (Throwable e) {
                threadLocal.set(threadLocal.get() + 1);
                throwable = e;
                Thread.sleep(100L);
            }
        }
        throw throwable;
    }

    private Object doIntercept(Object obj, Method method, Object[] params, MethodProxy methodProxy) throws Throwable {
        if (!method.getDeclaringClass().equals(Object.class)) {
            String className = obj.getClass().getCanonicalName();
            className = className.substring(0, className.indexOf("$$"));
            Class clazz = Class.forName(className);

            BaseModuleBootstrap module = ServiceManager.getInstance().getModule(clazz);
            if (module == null) {
                throw new NulsRuntimeException(ErrorCode.DATA_ERROR, "Access to a service of an un start module!" + method.toString());
            }
            if (module.getStatus() != ModuleStatusEnum.STARTING && module.getStatus() != ModuleStatusEnum.RUNNING) {
                throw new NulsRuntimeException(ErrorCode.DATA_ERROR, "Access to a service of an un start module!" + method.toString());
            }
            boolean isOk = SpringLiteContext.checkBeanOk(obj);
            if (!isOk) {
                throw new NulsRuntimeException(ErrorCode.DATA_ERROR, "Service has not autowired");
            }
        }
        if (null == method.getDeclaredAnnotations() || method.getDeclaredAnnotations().length == 0) {
            return methodProxy.invokeSuper(obj, params);
        }
        return BeanMethodInterceptorManager.doFilter(method.getDeclaredAnnotations(), obj, method, params, methodProxy);

    }
}
