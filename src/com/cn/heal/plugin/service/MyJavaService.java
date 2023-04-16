package com.cn.heal.plugin.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.util.Processor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

public class MyJavaService {
    private Project project;
    private JavaPsiFacade javaPsiFacade;

    /**<截取名称然后拼接Provider, provider实际类>,例如TestProvider**/
    private Map<String, PsiClass[]> providerCache = Maps.newHashMap();
    /**<真正的类名, provider实际类>,例如此处的key并非拼接,而是实际的服务提供者的类名**/
    private Map<String, PsiClass[]> serviceCache = Maps.newHashMap();

    public MyJavaService(Project project) {
        this.project = project;
        this.javaPsiFacade = JavaPsiFacade.getInstance(project);
    }

    /**
     * 获取Project里的MyJavaService类实例
     * @param project
     * @return
     */
    public static MyJavaService getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, MyJavaService.class);
    }

    /**
     * 对psiMethod进行处理,处理成服务提供者或者服务调用者
     */
    public void process(PsiMethod psiMethod, Processor<? super PsiMethod> processor) {
        //对有FeignClient注解的类进行处理
        PsiClass psiClass = psiMethod.getContainingClass();
        if (null != psiClass) {
            PsiAnnotation psiAnnotation = psiClass.getAnnotation("org.springframework.cloud.openfeign.FeignClient");
            if (psiAnnotation == null) {
                psiAnnotation = psiClass.getAnnotation("org.springframework.cloud.netflix.feign.FeignClient");
            }

            if (psiAnnotation != null) {
                //服务调用者
                this.processService(psiMethod, processor);
            } else {
                //服务提供者
                this.processProvider(psiMethod, processor);
            }
        }
    }

    /**
     * 按照我的逻辑,这里应该对应client;
     * 处理调用方,即服务的调用者
     * 例如: psiMethod是在order-api的OrderService类中, 那么需要在order-provider的OrderProvider类中找到psiMethod,然后调用processor处理pisMethod;
     * 并将Provider放入响应的cache;
     */
    private void processService(PsiMethod psiMethod, Processor<? super PsiMethod> processor) {
        PsiClass psiClass = psiMethod.getContainingClass();
        if (null != psiClass) {
            Project project = psiMethod.getProject();
            String className = psiClass.getName();
            if (!StringUtils.isBlank(className)) {
                //例如 newClassName =  TestServiceV1 -> TestProvider
                String newClassName = "";
                if (className.endsWith("ServiceV1")) {
                    newClassName = className.substring(0, className.indexOf("ServiceV1"));
                } else if (className.endsWith("ServiceV2")) {
                    newClassName = className.substring(0, className.indexOf("ServiceV2"));
                } else if (className.endsWith("V1")) {
                    newClassName = className.substring(0, className.indexOf("V1"));
                } else if (className.endsWith("V2")) {
                    newClassName = className.substring(0, className.indexOf("V2"));
                } else if (className.endsWith("Service")) {
                    newClassName = className.substring(0, className.indexOf("Service"));
                }

                if (!newClassName.endsWith("Provider")) {
                    newClassName = newClassName + "Provider";
                }

                //找到psiMethod所在模块的对应provider模块; 例如根据order-api找到order-provider模块
                GlobalSearchScope searchScope = this.getSearchScopeByModuleName(psiMethod, "api", "provider");
                //从查找域里获取类所在位置,并放入providerCache;computeIfAbsent返回的是所有的value值
                PsiClass[] providerNames = (PsiClass[])this.providerCache.computeIfAbsent(newClassName, (k) -> {
                    return PsiShortNamesCache.getInstance(project).getClassesByName(k, searchScope);
                });
                this.buildProcessor(psiMethod, providerNames, processor);
            }
        }
    }

    /**
     * 处理提供方,即服务的提供者
     * 例如: psiMethod是在order-provider的OrderService类中, 那么需要在order-api的OrderProvider类中找到psiMethod,然后调用processor处理pisMethod;
     * 并将service(服务提供方)放入对应的cache
     */
    private void processProvider(PsiMethod psiMethod, Processor<? super PsiMethod> processor) {
        PsiClass psiClass = psiMethod.getContainingClass();
        if (null != psiClass) {
            //例如; TestProvider
            String className = psiClass.getName();
            if (!StringUtils.isBlank(className)) {
                //只处理以Provider结尾的类.此处意味着所有的Controller都不能进行映射了,只能映射Provider;
                if (className.endsWith("Provider")) {
                    //已处理的就不再进行处理了;
                    PsiClass[] psiClasses = this.serviceCache.get(className);
                    if (psiClasses != null) {
                        this.buildProcessor(psiMethod, psiClasses, processor);
                        return;
                    }

                    //例如 Test
                    String shortClassName = className.substring(0, className.indexOf("Provider"));
                    //找psiMethod在哪个api模块
                    GlobalSearchScope searchScope = this.getSearchScopeByModuleName(psiMethod, "provider", "api");

                    //找到对应的service;
                    List<PsiClass> all = new ArrayList(8);
                    //例如; TestServiceV1,TestServiceV2,TestProviderServiceV1,TestProviderServiceV2,TestV1,TestV2
                    List<String> serviceClazzNames = new ArrayList<>();
                    serviceClazzNames.add(shortClassName + "ServiceV1");
                    serviceClazzNames.add(shortClassName + "ServiceV2");
                    serviceClazzNames.add(className + "ServiceV1");
                    serviceClazzNames.add(className + "ServiceV2");
                    serviceClazzNames.add(shortClassName + "V1");
                    serviceClazzNames.add(shortClassName + "V2");
                    serviceClazzNames.add(shortClassName + "Service");
                    for (String serviceClazzName : serviceClazzNames) {
                        //这里不知道为啥一直要判断<2, 不过不重要
                        if (all.size() < 2){
                            PsiClass[] classesByName = PsiShortNamesCache.getInstance(this.project).getClassesByName(serviceClazzName, searchScope);
                            all.addAll(Lists.newArrayList(classesByName));
                        }
                    }

                    PsiClass[] allArray = all.toArray(new PsiClass[0]);
                    //放入service缓存
                    this.serviceCache.put(className, allArray);
                    this.buildProcessor(psiMethod, allArray, processor);
                }

            }
        }
    }

    /**
     * 找到对应的类和方法,然后调用processor.process(method);
     * 例如:psiMethod是在order-api的OrderService类中, 那么需要在order-provider的OrderProvider类中找到psiMethod
     */
    private void buildProcessor(PsiMethod psiMethod, PsiClass[] psiClasses, Processor<? super PsiMethod> processor) {
        int classNum = psiClasses.length;
        for(int i = 0; i < classNum; ++i) {
            PsiClass psiClazz = psiClasses[i];
            PsiMethod[] methods = psiClazz.getMethods();
            int methodNum = methods.length;

            for(int j = 0; j < methodNum; ++j) {
                PsiMethod method = methods[j];
                if (method.getName().equals(psiMethod.getName())) {
                    processor.process(method);
                }
            }
        }
    }

    /**
     * 根据模块名字获取模块, 先找到psiElement所在模块,然后根据模块名称进行替换,将replaceSource替换成replaceTarget,然后找到该模块;如果没有找到这个模块,则返回这个项目
     * 例如 psiElement所在模块为order-api, replaceSource为api,replaceTarget为provider,那么最终返回的是order-provider模块,如果没有找到这个模块,则返回这个项目
     * @param psiElement psiMethod
     * @param replaceSource 被替换的字符串
     * @param replaceTarget 要替换成的字符串
     * @return 查找域
     */
    private GlobalSearchScope getSearchScopeByModuleName(PsiElement psiElement, String replaceSource, String replaceTarget) {
        Module module = ModuleUtil.findModuleForPsiElement(psiElement);
        if (module != null) {
            String moduleName = module.getName().replace(replaceSource, replaceTarget);
            Module moduleByName = ModuleManager.getInstance(this.project).findModuleByName(moduleName);
            if (moduleByName != null) {
                return GlobalSearchScope.moduleScope(moduleByName);
            }
        }

        return GlobalSearchScope.allScope(this.project);
    }

    public static void main(String[] args) {
        List<String> str = new ArrayList();
        str.add("456");
        str.add("123");
        String[] strings = (String[])str.toArray(new String[0]);
        String[] var3 = strings;
        int var4 = strings.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            String s = var3[var5];
            System.out.println(s);
        }

    }
}
