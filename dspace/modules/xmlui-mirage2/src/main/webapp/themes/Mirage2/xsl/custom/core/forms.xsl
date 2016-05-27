<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->
<!--
    Templates to cover the forms and forms fields.

    Author: art.lowel at atmire.com
    Author: lieven.droogmans at atmire.com
    Author: ben at atmire.com
    Author: Alexey Maslov

-->

<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
                xmlns:dri="http://di.tamu.edu/DRI/1.0/"
                xmlns:mets="http://www.loc.gov/METS/"
                xmlns:xlink="http://www.w3.org/TR/xlink/"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
                xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
                xmlns:xhtml="http://www.w3.org/1999/xhtml"
                xmlns:mods="http://www.loc.gov/mods/v3"
                xmlns:dc="http://purl.org/dc/elements/1.1/"
                xmlns="http://www.w3.org/1999/xhtml"
                exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc">

    <xsl:output indent="yes"/>

    <!-- A set of standard attributes common to all fields -->
    <xsl:template name="fieldAttributes">
        <xsl:call-template name="standardAttributes">
            <xsl:with-param name="class">
                <xsl:text>ds-</xsl:text><xsl:value-of select="@type"/><xsl:text>-field </xsl:text>
                <xsl:choose>
                    <xsl:when test="@type='button'">
                        <xsl:text>btn</xsl:text>
                        <xsl:if test="not(contains(@rend, 'btn-'))">
                            <xsl:text> btn-default</xsl:text>
                        </xsl:if>
                    </xsl:when>
                    <xsl:when test="not(@type='file')">
                        <xsl:text>form-control </xsl:text>
                    </xsl:when>
                </xsl:choose>

                <xsl:if test="@rend">
                    <xsl:text> </xsl:text>
                    <xsl:value-of select="@rend
                    "/>
                </xsl:if>
                <xsl:if test="dri:error or parent::node()[@type='composite']/dri:error">
                    <xsl:text>input-with-feedback </xsl:text>
                </xsl:if>
            </xsl:with-param>
            <xsl:with-param name="placeholder">
                <xsl:if test="@placeholder">
                    <xsl:value-of select="@placeholder"/>
                </xsl:if>
            </xsl:with-param>
        </xsl:call-template>
        <xsl:if test="@disabled='yes' or ../@rend = 'disabled'">
            <xsl:attribute name="disabled">disabled</xsl:attribute>
        </xsl:if>
        <xsl:if test="@type != 'checkbox' and @type != 'radio' ">
            <xsl:attribute name="name"><xsl:value-of select="@n"/></xsl:attribute>
        </xsl:if>
        <xsl:if test="@type != 'select' and @type != 'textarea' and @type != 'checkbox' and @type != 'radio' ">
            <xsl:attribute name="type">
                <xsl:choose>
                    <xsl:when test="@n = 'login_email'">
                        <xsl:text>email</xsl:text>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="@type"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>

        </xsl:if>
        <xsl:if test="@type= 'textarea'">
            <xsl:attribute name="onfocus">javascript:tFocus(this);</xsl:attribute>
        </xsl:if>
    </xsl:template>



    </xsl:stylesheet>