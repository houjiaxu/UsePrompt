package com.cn.heal.plugin.provider;

import java.util.Collection;
import java.util.Objects;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.icons.AllIcons.Ide;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.util.CommonProcessors.CollectProcessor;
import com.cn.heal.plugin.service.MyJavaService;
import org.jetbrains.annotations.NotNull;

/**
 * LineMarker 行标记;可以用图标来标注代码。这些标记可以为相关代码提供导航目标
 * 可参考 https://zhuanlan.zhihu.com/p/594472572
 */
public class MyLineMarkerProvider extends RelatedItemLineMarkerProvider {
    public MyLineMarkerProvider() {
    }

    /**
     * 收集导航标记
     */
    protected void collectNavigationMarkers( @NotNull PsiElement element,  @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        //PsiNameIdentifierOwner是psi名称鉴别器的拥有者;
        if (element instanceof PsiNameIdentifierOwner && element instanceof PsiMethod) {
            PsiMethod psiMethod = (PsiMethod)element;
            //收集处理器,其process方法就是添加到集合当中
            CollectProcessor<PsiMethod> processor = new CollectProcessor();
            //解析方法,将其添加到processor的集合中.
            MyJavaService.getInstance(element.getProject()).process(psiMethod, processor);
            //获取添加好的集合
            Collection<PsiMethod> results = processor.getResults();
            if (!results.isEmpty()) {
                //导航所在行的图标构建,将results里所有的方法都进行标记
                NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Ide.Rating).setTargets(results).setTooltipText("Navigate to provider");
                //这个大概是找到哪一行? 或者是这个方法所在行的信息;
                result.add(builder.createLineMarkerInfo(Objects.requireNonNull(((PsiNameIdentifierOwner)element).getNameIdentifier())));
            }
        }

    }
}
