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
package com.wabacus.system.inputbox;

import java.util.HashMap;
import java.util.Map;

import com.wabacus.config.Config;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.RegexTools;
import com.wabacus.util.Tools;

public class RichTextBox extends AbsInputBox
{
    public RichTextBox(String typename)
    {
        super(typename);
    }

    protected String doGetDisplayStringValue(ReportRequest rrequest,String value,String style_property,boolean isReadonly)
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append(this.getBeforedescription(rrequest));
        String inputboxid=getInputBoxId(rrequest);
        resultBuf.append("<textarea typename='"+typename+"' name='"+inputboxid+"'  id='"+inputboxid+"' ");
        if(style_property!=null) resultBuf.append(" ").append(style_property);
        resultBuf.append(">").append(getInputBoxValue(rrequest,value)).append("</textarea>");
        resultBuf.append(this.getAfterdescription(rrequest));
        String params=Tools.replaceAll(this.inputboxparams,"#INPUTBOXID#",inputboxid);
        if(isReadonly)
        {
            params=Tools.replaceAll(params,"#READONLY#","true");
        }else
        {
            params=Tools.replaceAll(params,"#READONLY#","false");
        }
        rrequest.getWResponse().addOnloadMethod("tinyMCE.init",params,true);
        return resultBuf.toString();
    }

    public String filledInContainer(String onblur)
    {
        return "";
    }

    public String getIndependentDisplayString(ReportRequest rrequest,String value,String dynstyleproperty,Object specificDataObj,boolean isReadonly)
    {
        return null;
    }

    public String createGetValueByIdJs()
    {
       return "return tinyMCE.get(id).getContent();";
    }
    
    public String getInputboxInnerType()
    {
        return "richtextbox";
    }
    
    public String createGetValueByInputBoxObjJs()
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("if(fillmode==2){");
        resultBuf.append("  value=tinyMCE.get('WX_RICHTEXT_BOX').getContent();label=value;");
        resultBuf.append("}else if(fillmode==1){");
        resultBuf.append("  value=tinyMCE.get(boxObj.getAttribute('id')).getContent(); label=value;");
        resultBuf.append("}");
        return resultBuf.toString();
    }
    
    public String createSetInputBoxValueByIdJs()
    {
       return "tinyMCE.get(id).setContent(newvalue);";
    }
    
    public void loadInputBoxConfig(IInputBoxOwnerBean ownerbean,XmlElementBean eleInputboxBean)
    {
        super.loadInputBoxConfig(ownerbean,eleInputboxBean);
        if(this.language!=null&&!this.language.trim().equals(""))
        {
            String dynparams=null;
            if(this.language.equals(Consts_Private.LANGUAGE_ZH))
            {
                dynparams="language:'cn'";
            }else
            {
                dynparams="language:'en'";
            }
            this.inputboxparams=Tools.mergeJsonValue(dynparams,this.inputboxparams);
        }
    }

    protected String getDefaultStylePropertyForDisplayMode2()
    {
        return "";
    }
    
    public void doPostLoad(IInputBoxOwnerBean ownerbean)
    {
        super.doPostLoad(ownerbean);
        this.inputboxparams=Tools.mergeJsonValue("elements:'#INPUTBOXID#',readonly:#READONLY#,init_instance_callback:\"initTinymce\"",this.inputboxparams);
        String defaultparams=Config.getInstance().getResourceString(null,null,"${richtextbox.params.default}",false);
        if(defaultparams!=null&&!defaultparams.trim().equals(""))
        {
            defaultparams=Tools.formatStringBlank(defaultparams);
            this.inputboxparams=Tools.mergeJsonValue(defaultparams,this.inputboxparams);
        }
        this.inputboxparams=Tools.replaceAll(this.inputboxparams,"\"","'");//因为稍后要把它们显示在<span/>的一个属性中，因此出现了"号会有问题，对于json字符串，用"和用'意义一样，所以这里做替换
        this.inputboxparams="{"+this.inputboxparams+"}";
        String tinymcejs=Config.webroot+"/webresources/component/tiny_mce/tiny_mce.js";
        tinymcejs=Tools.replaceAll(tinymcejs,"//","/");
        ownerbean.getReportBean().getPageBean().addMyJavascriptFile(tinymcejs,0);
        processRichTextBoxCssFiles(ownerbean);
    }
    
    private void processRichTextBoxCssFiles(IInputBoxOwnerBean ownerbean)
    {
        Map<String,String> mParams=new HashMap<String,String>();
        RegexTools.parseJsonValue(this.inputboxparams,mParams);
        if(mParams==null||mParams.size()==0) return;
        String themename=mParams.get("theme");
        themename=themename==null?"":themename.trim();
        if(themename.startsWith("'")&&themename.endsWith("'")) themename=themename.substring(1,themename.length()-1);
        if(themename.startsWith("\"")&&themename.endsWith("\"")) themename=themename.substring(1,themename.length()-1);
        String skinname=mParams.get("skin");
        skinname=skinname==null?"":skinname.trim();
        if(skinname.startsWith("'")&&skinname.endsWith("'")) skinname=skinname.substring(1,skinname.length()-1);
        if(skinname.startsWith("\"")&&skinname.endsWith("\"")) skinname=skinname.substring(1,skinname.length()-1);
        if(themename.equals("")||skinname.equals("")) return;
        String cssprex=Config.webroot+"/webresources/component/tiny_mce/themes/"+themename+"/skins/"+skinname+"/";
        cssprex=Tools.replaceAll(cssprex,"//","/");
        ownerbean.getReportBean().getPageBean().addMyCss(cssprex+"ui.css");
        ownerbean.getReportBean().getPageBean().addMyCss(cssprex+"content.css");
    }
    
    protected void addJsValidateOnBlurEvent(AbsReportType reportTypeObj,IInputBoxOwnerBean ownerbean)
    {
        if(this.fillmode==2) return;
        super.addJsValidateOnBlurEvent(reportTypeObj,ownerbean);
    }

    protected void processStylePropertyForFillInContainer()
    {
        throw new WabacusConfigLoadingException("报表"+this.owner.getReportBean().getPath()+"对应的可编辑报表类型不支持富文本编辑器richtextbox");
    }
    
}
