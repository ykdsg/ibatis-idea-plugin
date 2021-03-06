package org.intellij.ibatis.dom.sqlMap;

import com.intellij.javaee.model.xml.CommonDomModelElement;
import com.intellij.util.xml.GenericAttributeValue;

/**
 * statement element
 */
public interface SqlMapReference extends CommonDomModelElement {
    public GenericAttributeValue<String> getId();
}