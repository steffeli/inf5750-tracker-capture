package org.hisp.dhis.jdbc.statementbuilder;

/*
 * Copyright (c) 2004-2015, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.util.List;

/**
 * @author Lars Helge Overland
 */
public class MySQLStatementBuilder
    extends AbstractStatementBuilder
{    
    @Override
    public String getDoubleColumnType()
    {
        return "decimal(26,1)";
    }

    @Override
    public String getLongVarBinaryType()
    {
        return "BLOB";
    }

    @Override
    public String getColumnQuote()
    {
        return "`";
    }

    @Override
    public String getVacuum( String table )
    {
        return "optimize table " + table + ";";
    }

    @Override
    public String getTableOptions( boolean autoVacuum )
    {
        return ""; //TODO implement
    }

    @Override
    public String getRegexpMatch()
    {
        return "regexp";
    }

    @Override
    public String getRegexpWordStart()
    {
        return "[[:<:]]";
    }

    @Override
    public String getRegexpWordEnd()
    {
        return "[[:>:]]";
    }
    
    @Override
    public String getDeleteZeroDataValues()
    {
        return
            "DELETE FROM datavalue " +
            "USING datavalue, dataelement " +
            "WHERE datavalue.dataelementid = dataelement.dataelementid " +
            "AND dataelement.aggregationtype = 'sum' " +
            "AND dataelement.zeroissignificant = false " +
            "AND datavalue.value = '0'";
    }

    @Override
    public String getMoveDataValueToDestination( int sourceId, int destinationId )
    {
        return "UPDATE datavalue AS d1 SET sourceid=" + destinationId + " " + "WHERE sourceid=" + sourceId + " "
        + "AND NOT EXISTS ( " + "SELECT * from ( SELECT * FROM datavalue ) AS d2 " + "WHERE d2.sourceid=" + destinationId + " "
        + "AND d1.dataelementid=d2.dataelementid " + "AND d1.periodid=d2.periodid "
        + "AND d1.categoryoptioncomboid=d2.categoryoptioncomboid );";
    }

    @Override
    public String getSummarizeDestinationAndSourceWhereMatching( int sourceId, int destId )
    {
        return "UPDATE datavalue AS d1 SET value=( " + "SELECT SUM( value ) " + "FROM (SELECT * FROM datavalue) as d2 "
            + "WHERE d1.dataelementid=d2.dataelementid " + "AND d1.periodid=d2.periodid "
            + "AND d1.categoryoptioncomboid=d2.categoryoptioncomboid " + "AND d2.sourceid IN ( " + destId + ", "
            + sourceId + " ) ) " + "WHERE d1.sourceid=" + destId + " "
            + "AND d1.dataelementid in ( SELECT dataelementid FROM dataelement WHERE valuetype='int' );";
    }

    @Override
    public String getUpdateDestination( int destDataElementId, int destCategoryOptionComboId,
        int sourceDataElementId, int sourceCategoryOptionComboId )
    {
        
        return "UPDATE datavalue d1 LEFT JOIN datavalue d2 ON d2.dataelementid = " + destDataElementId
            + " AND d2.categoryoptioncomboid = " + destCategoryOptionComboId
            + " AND d1.periodid = d2.periodid AND d1.sourceid = d2.sourceid SET d1.dataelementid = "
            + destDataElementId + ", d1.categoryoptioncomboid = " + destCategoryOptionComboId
            + " WHERE d1.dataelementid = " + sourceDataElementId + " AND d1.categoryoptioncomboid = "
            + sourceCategoryOptionComboId + " AND d2.dataelementid IS NULL";
    }

    @Override
    public String getMoveFromSourceToDestination( int destDataElementId, int destCategoryOptionComboId,
        int sourceDataElementId, int sourceCategoryOptionComboId )
    {
        return "UPDATE datavalue d1, datavalue d2 SET d1.value=d2.value,d1.storedby=d2.storedby,d1.lastupdated=d2.lastupdated,d1.comment=d2.comment,d1.followup=d2.followup "
            + "WHERE d1.periodid=d2.periodid "
            + "AND d1.sourceid=d2.sourceid "
            + "AND d1.lastupdated<d2.lastupdated "
            + "AND d1.dataelementid="
            + destDataElementId
            + " AND d1.categoryoptioncomboid="
            + destCategoryOptionComboId
            + " "
            + "AND d2.dataelementid="
            + sourceDataElementId
            + " AND d2.categoryoptioncomboid=" + sourceCategoryOptionComboId + ";";
    }

    @Override
    public String getStandardDeviation( int dataElementId, int categoryOptionComboId, int organisationUnitId ){
    	
    	return "SELECT STDDEV( value ) FROM datavalue " +
            "WHERE dataelementid='" + dataElementId + "' " +
            "AND categoryoptioncomboid='" + categoryOptionComboId + "' " +
            "AND sourceid='" + organisationUnitId + "'";        
    }

    @Override
    public String getAverage( int dataElementId, int categoryOptionComboId, int organisationUnitId ){
    	 return "SELECT AVG( value ) FROM datavalue " +
            "WHERE dataelementid='" + dataElementId + "' " +
            "AND categoryoptioncomboid='" + categoryOptionComboId + "' " +
            "AND sourceid='" + organisationUnitId + "'";
    }

    @Override
    public String getAddDate( String dateField, int days )
    {
        return "ADDDATE(" + dateField + "," + days + ")";
    }

    @Override
    public String queryDataElementStructureForOrgUnit()
    {
        StringBuilder sqlsb = new StringBuilder();
        
        sqlsb.append( "(SELECT DISTINCT de.dataelementid, concat(de.name, \" \", cc.name) AS DataElement " );
        sqlsb.append( "FROM dataelement AS de " );
        sqlsb.append( "INNER JOIN categorycombos_optioncombos cat_opts on de.categorycomboid = cat_opts.categorycomboid ");
        sqlsb.append( "INNER JOIN categoryoptioncombo cc on cat_opts.categoryoptioncomboid = cc.categoryoptioncomboid ");
        sqlsb.append( "ORDER BY DataElement) " );
        
        return sqlsb.toString();
    }

    @Override
    public String queryRawDataElementsForOrgUnitBetweenPeriods(Integer orgUnitId, List<Integer> betweenPeriodIds)
    {
        StringBuilder sqlsb = new StringBuilder();

        int i = 0;
        
        for ( Integer periodId : betweenPeriodIds )
        {
            i++;

            sqlsb.append( "SELECT de.dataelementid, concat(de.name, \" \" , cc.name) AS DataElement, dv.value AS counts_of_aggregated_values, p.periodid AS PeriodId, p.startDate AS ColumnHeader " );
            sqlsb.append( "FROM dataelement AS de " );
            sqlsb.append( "INNER JOIN datavalue AS dv ON (de.dataelementid = dv.dataelementid) " );
            sqlsb.append( "INNER JOIN period p ON (dv.periodid = p.periodid) " );
            sqlsb.append( "INNER JOIN categorycombos_optioncombos cat_opts on de.categorycomboid = cat_opts.categorycomboid ");
            sqlsb.append( "INNER JOIN categoryoptioncombo cc on cat_opts.categoryoptioncomboid = cc.categoryoptioncomboid ");
            sqlsb.append( "WHERE dv.sourceid = " + orgUnitId + " " );
            sqlsb.append( "AND dv.periodid = " + periodId + " " );

            sqlsb.append( i == betweenPeriodIds.size() ? "ORDER BY ColumnHeader,dataelement" : " UNION " );
        }
        
        return sqlsb.toString();
    }    
}
