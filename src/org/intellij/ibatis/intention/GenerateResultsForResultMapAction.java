package org.intellij.ibatis.intention;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomManager;
import com.intellij.javaee.dataSource.DatabaseTableFieldData;
import org.intellij.ibatis.dom.sqlMap.ResultMap;
import org.intellij.ibatis.provider.IbatisClassShortcutsReferenceProvider;
import org.intellij.ibatis.provider.TableColumnReferenceProvider;
import org.intellij.ibatis.util.IbatisUtil;
import org.jetbrains.annotations.NotNull;

/**
 * intention action to create results for result map
 */
public class GenerateResultsForResultMapAction extends PsiIntentionBase {

    private static final String NAME = "GenerateResultsForResultMap";
    private static final String TEXT = "Generate results for resultMap element.";

    @NotNull public String getFamilyName() {
        return NAME;
    }

    protected boolean isAvailable(Project project, Editor editor, PsiFile file, @NotNull PsiElement element) {
        if (file instanceof XmlFile && element instanceof XmlTag) {
            XmlTag xmlTag = (XmlTag) element;
            if (xmlTag.getName().equals("resultMap") && xmlTag.findSubTags("result").length == 0) {
                DomElement domElement = DomManager.getDomManager(project).getDomElement(xmlTag);
                if (domElement != null && domElement instanceof ResultMap) {
                    return true;
                }
            }
        }
        return false;
    }

    @NotNull public String getText() {
        return TEXT;
    }

    protected void invoke(Project project, Editor editor, PsiFile file, @NotNull PsiElement element) {
        if (file instanceof XmlFile && element instanceof XmlTag) {
            XmlTag xmlTag = (XmlTag) element;
            if (xmlTag.getName().equals("resultMap") && xmlTag.findSubTags("result").length == 0) {
                DomElement domElement = DomManager.getDomManager(project).getDomElement(xmlTag);
                if (domElement != null && domElement instanceof ResultMap) {
                    ResultMap resultMap = (ResultMap) domElement;
                    PsiClass psiClass = resultMap.getClazz().getValue();
                    if (psiClass != null) {
                        XmlElementFactory psiElementFactory = XmlElementFactory.getInstance(project);
                        PsiMethod[] psiMethods = psiClass.getMethods();
                        try {
                            xmlTag.getValue().setText(null);
                            for (PsiMethod psiMethod : psiMethods) {
                                if (psiMethod.getName().startsWith("set") && psiMethod.getParameterList().getParametersCount() == 1) {
                                    DatabaseTableFieldData tableFieldData = TableColumnReferenceProvider.getDatabaseTableFieldData(psiMethod);
                                    if (tableFieldData != null) {   //setter method contains @column tag
                                        PsiType psiType = psiMethod.getParameterList().getParameters()[0].getType();
                                        String propertyName = StringUtil.decapitalize(psiMethod.getName().replace("set", ""));
                                        StringBuilder builder = new StringBuilder();
                                        builder.append("<result property=\"").append(propertyName).append("\"");
                                        builder.append(" column=\"").append(tableFieldData.getName()).append("\"");
                                        if (psiType.equals(PsiType.BOOLEAN)) {
                                            builder.append(" nullValue=\"false\"");
                                        } else if (psiType instanceof PsiPrimitiveType) {
                                            builder.append(" nullValue=\"0\"");
                                        }
                                        builder.append("/>");
                                        xmlTag.add(psiElementFactory.createTagFromText(builder.toString()));
                                    } else {
                                        // no @column tag, so just assume a reasonable default value
                                        PsiType psiType = psiMethod.getParameterList().getParameters()[0].getType();
                                        String propertyName = StringUtil.decapitalize(psiMethod.getName().replace("set", ""));
                                        StringBuilder builder = new StringBuilder();
                                        builder.append("<result property=\"").append(propertyName).append("\"");
                                        builder.append(" column=\"").append(IbatisUtil.convertToUnderscore(propertyName)).append("\"");
                                        if (psiType.equals(PsiType.BOOLEAN)) {
                                            builder.append(" nullValue=\"false\"");
                                        } else if (psiType instanceof PsiPrimitiveType) {
                                            builder.append(" nullValue=\"0\"");
                                        }
                                        builder.append("/>");
                                        xmlTag.add(psiElementFactory.createTagFromText(builder.toString()));
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // where do we go from here?
                        }
                    }
                }
            }
        }
    }
}
