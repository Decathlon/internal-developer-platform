<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="xml" indent="yes" encoding="UTF-8"/>

  <!-- Entry point: delegate to <report> template -->
  <xsl:template match="/">
    <xsl:apply-templates select="report"/>
  </xsl:template>

  <!-- Root coverage element with global counters -->
  <xsl:template match="report">
    <xsl:variable name="coveredLines"    select="sum(counter[@type='LINE']/@covered)"/>
    <xsl:variable name="missedLines"     select="sum(counter[@type='LINE']/@missed)"/>
    <xsl:variable name="coveredBranches" select="sum(counter[@type='BRANCH']/@covered)"/>
    <xsl:variable name="missedBranches"  select="sum(counter[@type='BRANCH']/@missed)"/>
    <xsl:variable name="totalLines"    select="$coveredLines + $missedLines"/>
    <xsl:variable name="totalBranches" select="$coveredBranches + $missedBranches"/>
    <coverage>
      <xsl:attribute name="line-rate">
        <xsl:choose>
          <xsl:when test="$totalLines &gt; 0">
            <xsl:value-of select="$coveredLines div $totalLines"/>
          </xsl:when>
          <xsl:otherwise>1</xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
      <xsl:attribute name="branch-rate">
        <xsl:choose>
          <xsl:when test="$totalBranches &gt; 0">
            <xsl:value-of select="$coveredBranches div $totalBranches"/>
          </xsl:when>
          <xsl:otherwise>1</xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
      <xsl:attribute name="lines-covered"><xsl:value-of select="$coveredLines"/></xsl:attribute>
      <xsl:attribute name="lines-valid"><xsl:value-of select="$totalLines"/></xsl:attribute>
      <xsl:attribute name="branches-covered"><xsl:value-of select="$coveredBranches"/></xsl:attribute>
      <xsl:attribute name="branches-valid"><xsl:value-of select="$totalBranches"/></xsl:attribute>
      <xsl:attribute name="complexity">0</xsl:attribute>
      <xsl:attribute name="version">0.1</xsl:attribute>
      <xsl:attribute name="timestamp">0</xsl:attribute>
      <sources>
        <source>.</source>
      </sources>
      <packages>
        <xsl:apply-templates select="package"/>
      </packages>
    </coverage>
  </xsl:template>

  <!-- Package element -->
  <xsl:template match="package">
    <xsl:variable name="coveredLines"    select="sum(counter[@type='LINE']/@covered)"/>
    <xsl:variable name="missedLines"     select="sum(counter[@type='LINE']/@missed)"/>
    <xsl:variable name="coveredBranches" select="sum(counter[@type='BRANCH']/@covered)"/>
    <xsl:variable name="missedBranches"  select="sum(counter[@type='BRANCH']/@missed)"/>
    <xsl:variable name="totalLines"    select="$coveredLines + $missedLines"/>
    <xsl:variable name="totalBranches" select="$coveredBranches + $missedBranches"/>
    <package>
      <xsl:attribute name="name"><xsl:value-of select="translate(@name, '/', '.')"/></xsl:attribute>
      <xsl:attribute name="line-rate">
        <xsl:choose>
          <xsl:when test="$totalLines &gt; 0"><xsl:value-of select="$coveredLines div $totalLines"/></xsl:when>
          <xsl:otherwise>1</xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
      <xsl:attribute name="branch-rate">
        <xsl:choose>
          <xsl:when test="$totalBranches &gt; 0"><xsl:value-of select="$coveredBranches div $totalBranches"/></xsl:when>
          <xsl:otherwise>1</xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
      <xsl:attribute name="complexity">0</xsl:attribute>
      <classes>
        <xsl:apply-templates select="class"/>
      </classes>
    </package>
  </xsl:template>

  <!-- Class element — line-rate from the class counters; line details from the
       corresponding sourcefile element (same package, same filename) -->
  <xsl:template match="class">
    <xsl:variable name="sname" select="@sourcefilename"/>
    <xsl:variable name="sf"    select="../sourcefile[@name=$sname]"/>
    <xsl:variable name="coveredLines"    select="sum(counter[@type='LINE']/@covered)"/>
    <xsl:variable name="missedLines"     select="sum(counter[@type='LINE']/@missed)"/>
    <xsl:variable name="coveredBranches" select="sum(counter[@type='BRANCH']/@covered)"/>
    <xsl:variable name="missedBranches"  select="sum(counter[@type='BRANCH']/@missed)"/>
    <xsl:variable name="totalLines"    select="$coveredLines + $missedLines"/>
    <xsl:variable name="totalBranches" select="$coveredBranches + $missedBranches"/>
    <class>
      <xsl:attribute name="name"><xsl:value-of select="translate(@name, '/', '.')"/></xsl:attribute>
      <xsl:attribute name="filename"><xsl:value-of select="concat(../@name, '/', @sourcefilename)"/></xsl:attribute>
      <xsl:attribute name="line-rate">
        <xsl:choose>
          <xsl:when test="$totalLines &gt; 0"><xsl:value-of select="$coveredLines div $totalLines"/></xsl:when>
          <xsl:otherwise>1</xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
      <xsl:attribute name="branch-rate">
        <xsl:choose>
          <xsl:when test="$totalBranches &gt; 0"><xsl:value-of select="$coveredBranches div $totalBranches"/></xsl:when>
          <xsl:otherwise>1</xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
      <xsl:attribute name="complexity">0</xsl:attribute>
      <methods>
        <xsl:apply-templates select="method"/>
      </methods>
      <lines>
        <xsl:apply-templates select="$sf/line"/>
      </lines>
    </class>
  </xsl:template>

  <!-- Method element -->
  <xsl:template match="method">
    <xsl:variable name="coveredLines"    select="sum(counter[@type='LINE']/@covered)"/>
    <xsl:variable name="missedLines"     select="sum(counter[@type='LINE']/@missed)"/>
    <xsl:variable name="coveredBranches" select="sum(counter[@type='BRANCH']/@covered)"/>
    <xsl:variable name="missedBranches"  select="sum(counter[@type='BRANCH']/@missed)"/>
    <xsl:variable name="totalLines"    select="$coveredLines + $missedLines"/>
    <xsl:variable name="totalBranches" select="$coveredBranches + $missedBranches"/>
    <method>
      <xsl:attribute name="name"><xsl:value-of select="@name"/></xsl:attribute>
      <xsl:attribute name="signature"><xsl:value-of select="@desc"/></xsl:attribute>
      <xsl:attribute name="line-rate">
        <xsl:choose>
          <xsl:when test="$totalLines &gt; 0"><xsl:value-of select="$coveredLines div $totalLines"/></xsl:when>
          <xsl:otherwise>1</xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
      <xsl:attribute name="branch-rate">
        <xsl:choose>
          <xsl:when test="$totalBranches &gt; 0"><xsl:value-of select="$coveredBranches div $totalBranches"/></xsl:when>
          <xsl:otherwise>1</xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
      <xsl:attribute name="complexity">0</xsl:attribute>
    </method>
  </xsl:template>

  <!-- Sourcefile line element: maps nr→number, ci→hits, mb/cb→branch info -->
  <xsl:template match="line">
    <line>
      <xsl:attribute name="number"><xsl:value-of select="@nr"/></xsl:attribute>
      <xsl:attribute name="hits"><xsl:value-of select="@ci"/></xsl:attribute>
      <xsl:choose>
        <xsl:when test="(@mb + @cb) &gt; 0">
          <xsl:attribute name="branch">true</xsl:attribute>
          <xsl:attribute name="condition-coverage">
            <xsl:value-of select="round(100 * @cb div (@mb + @cb))"/>
            <xsl:text>% (</xsl:text>
            <xsl:value-of select="@cb"/>
            <xsl:text>/</xsl:text>
            <xsl:value-of select="@mb + @cb"/>
            <xsl:text>)</xsl:text>
          </xsl:attribute>
        </xsl:when>
        <xsl:otherwise>
          <xsl:attribute name="branch">false</xsl:attribute>
        </xsl:otherwise>
      </xsl:choose>
    </line>
  </xsl:template>

</xsl:stylesheet>
