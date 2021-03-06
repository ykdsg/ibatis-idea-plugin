package org.intellij.ibatis.impl;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomUtil;
import org.intellij.ibatis.IbatisConfigurationModel;
import org.intellij.ibatis.IbatisManager;
import org.intellij.ibatis.IbatisProjectComponent;
import org.intellij.ibatis.IbatisSqlMapModel;
import org.intellij.ibatis.dom.sqlMap.*;
import org.intellij.ibatis.dom.configuration.TypeHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"ConstantConditions"})
public class IbatisManagerImpl extends IbatisManager {
    private Map<String, IbatisConfigurationModel> configurationModelMap = new HashMap<String, IbatisConfigurationModel>();

    public IbatisManagerImpl() {
    }

    @Nullable public IbatisConfigurationModel getConfigurationModel(@NotNull Module module) {
        IbatisProjectComponent projectComponent = IbatisProjectComponent.getInstance(module.getProject());
        List<IbatisConfigurationModel> models = projectComponent.getConfigurationModelFactory().getAllModels(module);
        if (models.size() > 0) return models.get(0);
        else return null;
    }

    /**
     * get unique name for xml tag if name space enabled
     *
     * @param fileElement file element
     * @param id          current id
     * @return unique name
     */
    private String getUniqueName(DomFileElement fileElement, String id) {
        IbatisConfigurationModel configurationModel = getConfigurationModel(ModuleUtil.findModuleForPsiElement(fileElement.getRootTag()));
        if (configurationModel.isUseStatementNamespaces()) {
            String namespace = fileElement.getRootTag().getAttributeValue("namespace");
            if (namespace != null && namespace.length() > 0) {
                return namespace + "." + id;
            } else {
                return id;
            }
        } else {
            return id;
        }
    }


    @Nullable public IbatisSqlMapModel getSqlMapModel(@Nullable PsiElement psiElement) {
        if (psiElement == null) return null;
        IbatisProjectComponent projectComponent = IbatisProjectComponent.getInstance(psiElement.getProject());
        return projectComponent.getSqlMapModelFactory().getModel(psiElement);
    }

    /**
     * get all type alias in all iBATIS SQL Map files
     *
     * @param psiElement requested psi element
     * @return type alias map
     */
    public Map<String, PsiClass> getAllTypeAlias(PsiElement psiElement) {
        Map<String, PsiClass> allAliasMap = new HashMap<String, PsiClass>();
        Module module = ModuleUtil.findModuleForPsiElement(psiElement);
        IbatisProjectComponent projectComponent = IbatisProjectComponent.getInstance(module.getProject());
        List<IbatisConfigurationModel> configurationModels = projectComponent.getConfigurationModelFactory().getAllModels(module);
        for (IbatisConfigurationModel configurationModel : configurationModels) {
            allAliasMap.putAll(configurationModel.getTypeAlias());
        }
        List<IbatisSqlMapModel> sqlMapModels = projectComponent.getSqlMapModelFactory().getAllModels(module);
        for (IbatisSqlMapModel sqlMapModel : sqlMapModels) {
            allAliasMap.putAll(sqlMapModel.getTypeAlias());
        }
        return allAliasMap;
    }

     /**
     * get all type handler in iBATIS
     *
     * @return type handler map
     */
    public Map<String, TypeHandler> getAllTypeHandlers(PsiElement psiElement) {
        Map<String, TypeHandler> typeHandlerMap = new HashMap<String, TypeHandler>();
        Module module = ModuleUtil.findModuleForPsiElement(psiElement);
        IbatisProjectComponent projectComponent = IbatisProjectComponent.getInstance(module.getProject());
        List<IbatisConfigurationModel> configurationModels = projectComponent.getConfigurationModelFactory().getAllModels(module);
        for (IbatisConfigurationModel configurationModel : configurationModels) {
            List<TypeHandler> typeHandlerList = configurationModel.getMergedModel().getTypeHandlers();
            for (TypeHandler typeHandler : typeHandlerList) {
                String javaType = typeHandler.getJavaType().getStringValue();
                if (StringUtil.isNotEmpty(javaType)) {
                    typeHandlerMap.put(javaType, typeHandler);
                }
            }
        }
        return typeHandlerMap;
    }

    /**
     * get all type alias in all iBATIS SQL Map files
     *
     * @param psiElement requested psi element
     * @return type alias map
     */
    public Map<String, XmlTag> getAllTypeAlias2(PsiElement psiElement) {
        Map<String, XmlTag> typeAlias = new HashMap<String, XmlTag>();
        Module module = ModuleUtil.findModuleForPsiElement(psiElement);
        IbatisProjectComponent projectComponent = IbatisProjectComponent.getInstance(module.getProject());
        List<IbatisConfigurationModel> configurationModels = projectComponent.getConfigurationModelFactory().getAllModels(module);
        for (IbatisConfigurationModel configurationModel : configurationModels) {
            typeAlias.putAll(configurationModel.getTypeAlias2());
        }
        List<IbatisSqlMapModel> models = getAllSqlMapModel(psiElement);
        for (IbatisSqlMapModel model : models) {
            List<TypeAlias> typeAliases = model.getMergedModel().getTypeAlias();
            for (TypeAlias alias : typeAliases) {
                XmlTag xmlTag = alias.getXmlTag();
                if (xmlTag != null) {
                    typeAlias.put(alias.getAlias().getValue(), xmlTag);
                }
            }
        }
        return typeAlias;
    }

    /**
     * get all result map in all iBATIS SQL Map files
     *
     * @param psiElement requested psi element
     * @return result map information
     */
    public Map<String, PsiClass> getAllResultMap(PsiElement psiElement) {
        Map<String, PsiClass> resultMap = new HashMap<String, PsiClass>();
        List<IbatisSqlMapModel> models = getAllSqlMapModel(psiElement);
        for (IbatisSqlMapModel model : models) {
            List<ResultMap> resultMapList = model.getMergedModel().getResultMaps();
            for (ResultMap map : resultMapList) {
                PsiClass psiClass = map.getClazz().getValue();
                if (psiClass != null) {
                    resultMap.put(getUniqueName(DomUtil.getFileElement(map), map.getId().getValue()), psiClass);
                }
            }
        }
        return resultMap;
    }

    /**
     * get all result map in all iBATIS SQL Map files
     *
     * @param psiElement requested psi element
     * @return result map information
     */
    public Map<String, XmlTag> getAllResultMap2(PsiElement psiElement) {
        Map<String, XmlTag> resultMapInfo = new HashMap<String, XmlTag>();
        List<IbatisSqlMapModel> models = getAllSqlMapModel(psiElement);
        for (IbatisSqlMapModel model : models) {
            List<ResultMap> resultMapList = model.getMergedModel().getResultMaps();
            for (ResultMap resultMap : resultMapList) {
                XmlTag xmlTag = resultMap.getClazz().getXmlTag();
                if (xmlTag != null) {
                    resultMapInfo.put(getUniqueName(DomUtil.getFileElement(resultMap), resultMap.getId().getValue()), xmlTag);
                }
            }
        }
        return resultMapInfo;
    }

    /**
     * get all parameter map in all iBATIS SQL Map files
     *
     * @param psiElement requested psi element
     * @return parameter map information
     */
    public Map<String, PsiClass> getAllParameterMap(PsiElement psiElement) {
        Map<String, PsiClass> parameterMap = new HashMap<String, PsiClass>();
        List<IbatisSqlMapModel> models = getAllSqlMapModel(psiElement);
        for (IbatisSqlMapModel model : models) {
            List<ParameterMap> parameterMapList = model.getMergedModel().getParameterMap();
            for (ParameterMap map : parameterMapList) {
                PsiClass psiClass = map.getClazz().getValue();
                if (psiClass != null) {
                    parameterMap.put(getUniqueName(DomUtil.getFileElement(map), map.getId().getValue()), psiClass);
                }
            }
        }
        return parameterMap;
    }

    /**
     * get all parameter map in all iBATIS SQL Map files
     *
     * @param psiElement requested psi element
     * @return parameter map information
     */
    public Map<String, XmlTag> getAllParameterMap2(PsiElement psiElement) {
        Map<String, XmlTag> parameterMap = new HashMap<String, XmlTag>();
        List<IbatisSqlMapModel> models = getAllSqlMapModel(psiElement);
        for (IbatisSqlMapModel model : models) {
            List<ParameterMap> parameterMapList = model.getMergedModel().getParameterMap();
            for (ParameterMap map : parameterMapList) {
                XmlTag xmlTag = map.getClazz().getXmlTag();
                if (xmlTag != null) {
                    parameterMap.put(getUniqueName(DomUtil.getFileElement(map), map.getId().getValue()), xmlTag);
                }
            }
        }
        return parameterMap;
    }

    /**
     * get all select in all iBATIS SQL Map files
     *
     * @param psiElement requested psi element
     * @return select information
     */
    public Map<String, Select> getAllSelect(PsiElement psiElement) {
        Map<String, Select> selectList = new HashMap<String, Select>();
        List<IbatisSqlMapModel> models = getAllSqlMapModel(psiElement);
        for (IbatisSqlMapModel model : models) {
            List<Select> selects = model.getMergedModel().getSelects();
            for (Select select : selects) {
                selectList.put(getUniqueName(DomUtil.getFileElement(select), select.getId().getStringValue()), select);
            }
        }
        return selectList;
    }

    /**
     * get all sql in all iBATIS SQL Map files
     *
     * @param psiElement requested psi element
     * @return sql information
     */
    public Map<String, Sql> getAllSql(PsiElement psiElement) {
        Map<String, Sql> allSql = new HashMap<String, Sql>();
        List<IbatisSqlMapModel> models = getAllSqlMapModel(psiElement);
        for (IbatisSqlMapModel model : models) {
            List<Sql> sqls = model.getMergedModel().getSqls();
            for (Sql sql : sqls) {
                allSql.put(getUniqueName(DomUtil.getFileElement(sql), sql.getId().getValue()), sql);
            }
        }
        return allSql;
    }

    private List<IbatisSqlMapModel> getAllSqlMapModel(PsiElement psiElement) {
        Module module = ModuleUtil.findModuleForPsiElement(psiElement);
        IbatisProjectComponent projectComponent = IbatisProjectComponent.getInstance(module.getProject());
        return projectComponent.getSqlMapModelFactory().getAllModels(module);
    }

    private List<IbatisSqlMapModel> getAllSqlMapModel(Module module) {
        IbatisProjectComponent projectComponent = IbatisProjectComponent.getInstance(module.getProject());
        return projectComponent.getSqlMapModelFactory().getAllModels(module);
    }

    /**
     * get all insert in all iBATIS SQL Map files
     *
     * @param psiElement requested psi element
     * @return insert information
     */
    public Map<String, Insert> getAllInsert(PsiElement psiElement) {
        Map<String, Insert> allInsert = new HashMap<String, Insert>();
        List<IbatisSqlMapModel> models = getAllSqlMapModel(psiElement);
        for (IbatisSqlMapModel model : models) {
            List<Insert> inserts = model.getMergedModel().getInserts();
            for (Insert insert : inserts) {
                allInsert.put(getUniqueName(DomUtil.getFileElement(insert), insert.getId().getValue()), insert);
            }
        }
        return allInsert;
    }

    /**
     * get all update in all iBATIS SQL Map files
     *
     * @param psiElement requested psi element
     * @return update information
     */
    public Map<String, Update> getAllUpdate(PsiElement psiElement) {
        Map<String, Update> allUpdate = new HashMap<String, Update>();
        List<IbatisSqlMapModel> models = getAllSqlMapModel(psiElement);
        for (IbatisSqlMapModel model : models) {
            List<Update> updates = model.getMergedModel().getUpdates();
            for (Update update : updates) {
                allUpdate.put(getUniqueName(DomUtil.getFileElement(update), update.getId().getValue()), update);
            }
        }
        return allUpdate;
    }

    /**
     * get all delete in all iBATIS SQL Map files
     *
     * @param psiElement requested psi element
     * @return delete information
     */
    public Map<String, Delete> getAllDelete(PsiElement psiElement) {
        Map<String, Delete> allDelete = new HashMap<String, Delete>();
        List<IbatisSqlMapModel> models = getAllSqlMapModel(psiElement);
        for (IbatisSqlMapModel model : models) {
            List<Delete> deletes = model.getMergedModel().getDeletes();
            for (Delete delete : deletes) {
                allDelete.put(getUniqueName(DomUtil.getFileElement(delete), delete.getId().getValue()), delete);
            }
        }
        return allDelete;
    }

    public Map<String, Statement> getAllStatement(PsiElement psiElement) {
        Map<String, Statement> allStatement = new HashMap<String, Statement>();
        List<IbatisSqlMapModel> models = getAllSqlMapModel(psiElement);
        for (IbatisSqlMapModel model : models) {
            List<Statement> statements = model.getMergedModel().getStatements();
            for (Statement statement : statements) {
                allStatement.put(getUniqueName(DomUtil.getFileElement(statement), statement.getId().getValue()), statement);
            }
        }
        return allStatement;
    }

    /**
     * get all procedure in all iBATIS SQL Map files
     *
     * @param psiElement requested psi element
     * @return procedure information
     */
    public Map<String, Procedure> getAllProcedure(PsiElement psiElement) {
        Map<String, Procedure> allStatement = new HashMap<String, Procedure>();
        List<IbatisSqlMapModel> models = getAllSqlMapModel(psiElement);
        for (IbatisSqlMapModel model : models) {
            List<Procedure> procedures = model.getMergedModel().getProcedures();
            for (Procedure procedure : procedures) {
                allStatement.put(getUniqueName(DomUtil.getFileElement(procedure), procedure.getId().getValue()), procedure);
            }
        }
        return allStatement;
    }

    /**
     * get all SQL Map file in the module
     *
     * @param module Module object
     * @return all SQL Map files
     */
    public Map<String, DomElement> getAllSqlMapReference(Module module) {
        Map<String, DomElement> allReference = new HashMap<String, DomElement>();
        List<IbatisSqlMapModel> models = getAllSqlMapModel(module);
        for (IbatisSqlMapModel model : models) {
            List<DomElement> references = model.getMergedModel().getAllReference();
            for (DomElement reference : references) {
                allReference.put(getUniqueName(DomUtil.getFileElement(reference), reference.getXmlTag().getAttributeValue("id")), reference);
            }
        }
        return allReference;
    }

    /**
     * get all cache model for element
     *
     * @param psiElement requested psi element
     * @return cache model information
     */
    public Map<String, CacheModel> getAllCacheModel(PsiElement psiElement) {
        Map<String, CacheModel> allCacheModel = new HashMap<String, CacheModel>();
        List<IbatisSqlMapModel> models = getAllSqlMapModel(psiElement);
        for (IbatisSqlMapModel model : models) {
            List<CacheModel> cacheModels = model.getMergedModel().getCacheModels();
            for (CacheModel cacheModel : cacheModels) {
                allCacheModel.put(getUniqueName(DomUtil.getFileElement(cacheModel), cacheModel.getId().getValue()), cacheModel);
            }
        }
        return allCacheModel;
    }

}
