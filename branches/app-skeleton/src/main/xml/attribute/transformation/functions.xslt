<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fn="http://www.w3.org/2005/xpath-functions"
                xmlns:af="http://griddynamics.com/attribute/transformation/functions">
	<xsl:function name="af:package">
		<xsl:param name="name"/>
		<xsl:variable name="nameSeq" select="fn:tokenize($name, '\.')"/>
		<xsl:value-of select="fn:string-join(fn:subsequence($nameSeq, 1, fn:count($nameSeq) - 2), '.')"/>
	</xsl:function>
	
	<xsl:function name="af:class">
		<xsl:param name="name"/>
		<xsl:variable name="nameSeq" select="fn:tokenize($name, '\.')"/>
		<xsl:choose>
			<xsl:when test="fn:count($nameSeq) > 1">
				<xsl:value-of select="fn:subsequence($nameSeq, fn:count($nameSeq) - 1, 1)"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:message terminate="yes">Class name is empty</xsl:message>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:function>
	
	<xsl:function name="af:fullClass">
		<xsl:param name="name"/>
		<xsl:variable name="nameSeq" select="fn:tokenize($name, '\.')"/>
		<xsl:value-of select="fn:concat(af:package($name), '.', af:class($name))"/>
	</xsl:function>

	<xsl:function name="af:firstName">
		<xsl:param name="name"/>
		<xsl:variable name="nameSeq" select="fn:tokenize($name, '\.')"/>
		<xsl:value-of select="fn:string-join(fn:subsequence($nameSeq, 1, fn:count($nameSeq) - 1), '.')"/>
	</xsl:function>
	
	<xsl:function name="af:lastName">
		<xsl:param name="name"/>
		<xsl:variable name="nameSeq" select="fn:tokenize($name, '\.')"/>
		<xsl:value-of select="fn:subsequence($nameSeq, fn:count($nameSeq))"/>
	</xsl:function>
</xsl:stylesheet>