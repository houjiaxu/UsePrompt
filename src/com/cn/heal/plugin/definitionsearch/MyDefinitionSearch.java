package com.cn.heal.plugin.definitionsearch;


import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiTypeParameterListOwner;
import com.intellij.util.Processor;
import com.cn.heal.plugin.service.MyJavaService;
import org.jetbrains.annotations.NotNull;

/**
 * 自定义search,用来处理每个方法对应的service和provider,并放入缓存
 */
public class MyDefinitionSearch extends QueryExecutorBase<PsiMethod, PsiElement> {

    public MyDefinitionSearch() {
        super(true);
    }

    /**
     * 处理每个方法对应的service和provider,并放入缓存
     */
    public void processQuery(@NotNull PsiElement element, @NotNull Processor<? super PsiMethod> processor) {
        /*if (element == null) {
            $$$reportNull$$$0(0);
            这里的意思应该是上报异常后面的(0)应该是代表本方法里第几个上报异常;
            例如$$$reportNull$$$0(1); 应该是表示第2个上报异常的地方;
        }*/

        if (element instanceof PsiTypeParameterListOwner) {
            if (element instanceof PsiMethod) {
                PsiMethod psiMethod = (PsiMethod)element;
                //获取Project里的MyJavaService类实例
                MyJavaService.getInstance(element.getProject()).process(psiMethod, processor);
            }

        }
    }
}