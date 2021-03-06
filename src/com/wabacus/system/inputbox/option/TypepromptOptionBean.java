/* 
 * Copyright (C) 2010---2013 星星(wuweixing)<349446658@qq.com>
 * 
 * This file is part of Wabacus 
 * 
 * Wabacus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.wabacus.system.inputbox.option;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.wabacus.config.Config;
import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.database.type.AbsDatabaseType;
import com.wabacus.config.typeprompt.ITypePromptOptionMatcher;
import com.wabacus.config.typeprompt.TypePromptBean;
import com.wabacus.config.typeprompt.TypePromptColBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.inputbox.AbsInputBox;
import com.wabacus.system.inputbox.TextBox;
import com.wabacus.util.Tools;

public class TypepromptOptionBean extends AbsOptionBean
{
    private Map<String,String> mPromptcolValues;//各联想列<promptcol/>上label和value列对应的值，只有是常量选项时才会在<option/>中指定这些列的值，如果是其它选项数据类型，则此变量为空
    
    public TypepromptOptionBean(AbsInputBox ownerInputboxObj)
    {
        super(ownerInputboxObj);
    }

    public void setMPromptcolValues(Map<String,String> promptcolValues)
    {
        mPromptcolValues=promptcolValues;
    }

    public List<Map<String,String>> getLstRuntimeOptions(ReportRequest rrequest,String txtValue)
    {
        List<Map<String,String>> lstResults=null;
        if(this.optionDatasourceObj==null)
        {//没有指定动态获取选项数据的数据源对象，则说明此<option/>就是一个常量选项的配置
            lstResults=new ArrayList<Map<String,String>>();
            lstResults.add((Map<String,String>)((HashMap<String,String>)mPromptcolValues).clone());
        }else
        {
            lstResults=this.optionDatasourceObj.getLstTypePromptOptions(rrequest,txtValue);
        }
        if(!(this.optionDatasourceObj instanceof SQLOptionDatasource))
        {
            TypePromptBean typePromptBean=((TextBox)this.getOwnerInputboxObj()).getTypePromptBean();
            ITypePromptOptionMatcher matcherObj=typePromptBean.getTypePromptMatcherObj();
            if(matcherObj==null) matcherObj=TypePromptBean.DEFAULT_OPTION_MATCHER;
            int count=0;
            List<Map<String,String>> lstTmp=new ArrayList<Map<String,String>>();
            for(Map<String,String> mOptionTmp:lstResults)
            {
                if(matcherObj.isMatch(rrequest,typePromptBean,mOptionTmp,txtValue))
                {
                    lstTmp.add(mOptionTmp);
                    if(++count>typePromptBean.getResultcount()) break;
                }
            }
            lstResults=lstTmp;
        }
        return lstResults;
    }
    
    public void doPostLoad()
    {
        if(this.optionDatasourceObj instanceof SQLOptionDatasource)
        {
            ReportBean rbean=this.ownerInputboxObj.getOwner().getReportBean();
            String sql=((SQLOptionDatasource)this.optionDatasourceObj).getSql();
            sql=Tools.formatStringBlank(sql).trim();
            if(!sql.toLowerCase().startsWith("select")||sql.toLowerCase().indexOf("from")<=0)
            {
                throw new WabacusConfigLoadingException("为报表"+rbean.getPath()+"配置的输入联想配置的SQL语句"+sql+"不合法");
            }
            sql=sql.substring("select".length()).trim();
            if(sql.toLowerCase().indexOf("distinct")!=0)
            {
                sql=" distinct "+sql;
            }
            sql="select "+sql;
            String sqlOuter=null;
            List<ConditionBean> lstConditions=((SQLOptionDatasource)this.optionDatasourceObj).getLstConditions();
            if(lstConditions!=null&&lstConditions.size()>0)
            {
                if(sql.indexOf("{#condition#}")<0)
                {
                    sqlOuter="select * from ("+sql+") tblTypepromptTmp where {#condition#}";
                }
            }
            if(sql.indexOf(TypePromptBean.MATCHCONDITION_PLACEHOLDER)<0)
            {
                if(sqlOuter==null)
                {
                    sqlOuter="select * from ("+sql+") tblTypepromptTmp where "+TypePromptBean.MATCHCONDITION_PLACEHOLDER;
                }else
                {
                    sqlOuter+=" and "+TypePromptBean.MATCHCONDITION_PLACEHOLDER;
                }
            }
            if(sqlOuter!=null) sql=sqlOuter;
            ((SQLOptionDatasource)this.optionDatasourceObj).setSql(Tools.replaceAll(sql,TypePromptBean.MATCHCONDITION_PLACEHOLDER,getMatchColConditionExpression()));
        }
        super.doPostLoad();
    }

    private String getMatchColConditionExpression()
    {
        TypePromptBean typePromptBean=((TextBox)this.ownerInputboxObj).getTypePromptBean();
        List<TypePromptColBean> lstPromptColBeans=typePromptBean.getLstPColBeans();
        StringBuffer resultBuf=new StringBuffer();
        String labelcolTmp,expressionTmp;
        AbsDatabaseType dbtype=Config.getInstance().getDataSource(((SQLOptionDatasource)this.optionDatasourceObj).getDatasource()).getDbType();
        for(TypePromptColBean tpColbeanTmp:lstPromptColBeans)
        {
            if(tpColbeanTmp.getMatchmode()<=0) continue;
            expressionTmp=tpColbeanTmp.getMatchexpression();
            if(expressionTmp==null||expressionTmp.trim().equals(""))
            {
                labelcolTmp=tpColbeanTmp.getLabel();
                if(!typePromptBean.isCasesensitive()) labelcolTmp=dbtype.getLowerMethodname()+"("+labelcolTmp+")";
                expressionTmp=labelcolTmp+" like ";
                if(!typePromptBean.isCasesensitive()) expressionTmp+=dbtype.getLowerMethodname()+"(";
                expressionTmp+="'";
                if(tpColbeanTmp.getMatchmode()==2) expressionTmp+="%";
                expressionTmp+="#data#%'";
                if(!typePromptBean.isCasesensitive()) expressionTmp+=")"; 
            }
            resultBuf.append(expressionTmp).append(" or ");
        }
        if(resultBuf.toString().trim().endsWith(" or")) resultBuf.delete(resultBuf.length()-" or".length(),resultBuf.length());
        return "("+resultBuf.toString()+")";
    }
    
    public TypepromptOptionBean clone(AbsInputBox newOwnerInputboxObj)
    {
        TypepromptOptionBean newOptionBean=(TypepromptOptionBean)super.clone(newOwnerInputboxObj);
        if(mPromptcolValues!=null)
        {
            newOptionBean.mPromptcolValues=(Map<String,String>)((HashMap<String,String>)this.mPromptcolValues).clone();
        }
        return newOptionBean;
    }
}

