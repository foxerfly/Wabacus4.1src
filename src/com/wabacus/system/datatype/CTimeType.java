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
package com.wabacus.system.datatype;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.database.type.AbsDatabaseType;

public class CTimeType extends AbsDateTimeType
{
    private final static Log log=LogFactory.getLog(CTimeType.class);

    private final static Map<String,AbsDateTimeType> mDatetimeTypeObjects=new HashMap<String,AbsDateTimeType>();
    
    public Object getColumnValue(ResultSet rs,String column,AbsDatabaseType dbtype)
            throws SQLException
    {
        java.sql.Time ctt=rs.getTime(column);
        if(ctt==null) return null;
        Calendar cd=Calendar.getInstance();
        cd.setTimeInMillis(ctt.getTime());
        return cd;
    }

    public Object getColumnValue(ResultSet rs,int iindex,AbsDatabaseType dbtype)
            throws SQLException
    {
        java.sql.Time ctt=rs.getTime(iindex);
        if(ctt==null) return null;
        Calendar cd=Calendar.getInstance();
        cd.setTimeInMillis(ctt.getTime());
        return cd;
    }

    public void setPreparedStatementValue(int iindex,String value,PreparedStatement pstmt,
            AbsDatabaseType dbtype) throws SQLException
    {
        log.debug("setTime("+iindex+","+value+")");
        pstmt.setTime(iindex,(java.sql.Time)label2value(value));
    }

    public void setPreparedStatementValue(int index,Date dateValue,PreparedStatement pstmt)
            throws SQLException
    {
        log.debug("setTime("+index+","+dateValue+")");
        if(dateValue==null)
        {
            pstmt.setTime(index,null);
        }else
        {
            pstmt.setTime(index,new java.sql.Time(dateValue.getTime()));
        }
    }

    public Object label2value(String label)
    {
        if(label==null||label.trim().equals("")) return null;
        SimpleDateFormat sdf=new SimpleDateFormat(dateformat);
        try
        {
            Date date=sdf.parse(label.trim());
            return new java.sql.Time(date.getTime());
        }catch(ParseException e)
        {
            log.error(label+"非法的日期格式,不能格式化为"+dateformat+"形式的日期类型",e);
            return null;
        }
    }

    public String value2label(Object value)
    {
        if(value==null) return "";
        if(!(value instanceof Calendar))
        {
            return String.valueOf(value);
        }
        return new SimpleDateFormat(dateformat).format(((Calendar)value).getTime());
    }

    public Class getJavaTypeClass()
    {
        return Calendar.class;
    }
    
    protected Map<String,AbsDateTimeType> getMDatetimeTypeObjects()
    {
        return mDatetimeTypeObjects;
    }
}
