package com.cn.heal.plugin.reference;


import com.intellij.openapi.util.TextRange;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 这个类和当前这个插件应该没啥关系,应该是一个测试的类
 */
public class MyRef extends PsiReferenceBase<PsiElement> {
    public MyRef(@NotNull PsiElement element, TextRange rangeInElement) {
        super(element, rangeInElement);
    }

    public MyRef(@NotNull PsiElement element) {
        super(element);
    }

    /**
     * 从当前项目中找到 com.xsz.springboot.util.Hello 这个类
     */
    @Nullable
    public PsiElement resolve() {
        String str = "com.xsz.springboot.util.Hello";
        PsiElement element = this.getElement();
        JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(element.getProject());
        PsiClass aClass = javaPsiFacade.findClass(str, GlobalSearchScope.allScope(element.getProject()));
        return aClass;
    }

    @NotNull
    public Object[] getVariants() {
        Object[] obj = new Object[0];
        return obj;
    }
}
