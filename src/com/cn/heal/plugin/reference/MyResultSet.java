package com.cn.heal.plugin.reference;


import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.ReferenceSetBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 感觉和这个插件的功能不符合,估计是没用的类
 */
public class MyResultSet extends ReferenceSetBase<PsiReference> {
    public MyResultSet(String text, @NotNull PsiElement element, int offset, char separator) {
        super(text, element, offset, separator);
    }

    /**
     * 创建引用
     * @param range
     * @param index
     * @return
     */
    @Nullable
    protected PsiReference createReference(TextRange range, int index) {
        PsiElement element = this.getElement();
        if (element instanceof PsiMethod) {
        }

        return super.createReference(range, index);
    }
}
