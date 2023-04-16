package com.cn.heal.plugin.reference;


import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

/**
 * 引用控制器,感觉和这个插件的功能不符合,估计是没用的类
 */
public class MyReference extends PsiReferenceContributor {

    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar psiReferenceRegistrar) {
        psiReferenceRegistrar.registerReferenceProvider(PlatformPatterns.psiElement(PsiLiteralExpression.class), new PsiReferenceProvider() {
            @NotNull
            public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext processingContext) {
                if (element == null) {
                    return null;
                }
                if (processingContext == null) {
                    return null;
                }

                PsiLiteralExpression literalExpression = (PsiLiteralExpression)element;
                String value = literalExpression.getValue() instanceof String ? (String)literalExpression.getValue() : null;
                PsiReference[] psiReferences;
                if (value != null && value.startsWith("simple:")) {
                    psiReferences = new PsiReference[]{new MyRef(element, new TextRange(8, value.length() + 1))};
                    if (psiReferences == null) {
                        return null;
                    }

                    return psiReferences;
                } else {
                    psiReferences = new PsiReference[0];
                    if (psiReferences == null) {
                        return null;
                    }

                    return psiReferences;
                }
            }
        });
    }

}
